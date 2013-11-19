
package net.binaryparadox.kerplapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.encode.Contents;
import com.google.zxing.encode.QRCodeEncoder;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.SimpleWebServer;

import net.binaryparadox.kerplapp.repo.Crypto;
import net.binaryparadox.kerplapp.repo.KerplappRepo;
import net.binaryparadox.kerplapp.repo.KerplappRepo.ScanListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Locale;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLServerSocketFactory;

@SuppressLint("DefaultLocale")
public class KerplappActivity extends Activity {
    private static final String TAG = PackageReceiver.class.getCanonicalName();
    private ProgressDialog repoProgress;

    private ToggleButton repoSwitch;
    private WifiManager wifiManager;
    private String wifiNetworkName = "";
    private int ipAddress = 0;
    private String ipAddressString = null;
    private String repoUriString = null;
    private File app_keystore;

    private Thread webServerThread = null;
    private Handler handler = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        app_keystore = getDir("keystore", Context.MODE_PRIVATE);

        repoSwitch = (ToggleButton) findViewById(R.id.repoSwitch);
        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        int wifiState = wifiManager.getWifiState();
        if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
            setIpAddressFromWifi();
            wireRepoSwitchToWebServer();
        } else {
            repoSwitch.setText(R.string.enable_wifi);
            repoSwitch.setTextOn(getString(R.string.enabling_wifi));
            repoSwitch.setTextOff(getString(R.string.enable_wifi));
            repoSwitch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    enableWifi();
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.kerplapp_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_setup_repo:
                new ScanForAppsTask().execute();
                return true;
            case R.id.menu_send_to_fdroid:
                if (repoUriString == null) {
                    Toast.makeText(this, "The repo is not configured yet!", Toast.LENGTH_LONG)
                            .show();
                } else {
                    // TODO check if F-Droid is actually installed instead of
                    // just crashing
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(repoUriString));
                    intent.setClassName("org.fdroid.fdroid", "org.fdroid.fdroid.ManageRepo");
                    startActivity(intent);
                }
                return true;
            case R.id.menu_settings:
                return true;
        }
        return false;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case 0:
                repoProgress = new ProgressDialog(this);
                repoProgress.setMessage("Scanning Apps. Please wait...");
                repoProgress.setIndeterminate(false);
                repoProgress.setMax(100);
                repoProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                repoProgress.setCancelable(false);
                repoProgress.show();
                return repoProgress;
            default:
                return null;
        }
    }

    private void enableWifi() {
        wifiManager.setWifiEnabled(true);
        new WaitForWifiAsyncTask().execute();
    }

    private void wireRepoSwitchToWebServer() {
        repoSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (repoSwitch.isChecked())
                    startWebServer();
                else
                    stopWebServer();
            }
        });
    }

    private void setIpAddressFromWifi() {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        ipAddress = wifiInfo.getIpAddress();
        ipAddressString = String.format(Locale.CANADA, "%d.%d.%d.%d",
                (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
        repoUriString = "https://" + ipAddressString + ":8888/repo";

        repoSwitch.setText(repoUriString);
        repoSwitch.setTextOn(repoUriString);
        repoSwitch.setTextOff(repoUriString);
        ImageView repoQrCodeImageView = (ImageView) findViewById(R.id.repoQrCode);
        // F-Droid currently only understands fdroidrepo:// and fdroidrepos://
        String fdroidrepoUriString = repoUriString.replace("https", "fdroidrepos");
        repoQrCodeImageView.setImageBitmap(generateQrCode(fdroidrepoUriString));

        wifiNetworkName = wifiInfo.getSSID();
        TextView wifiNetworkNameTextView = (TextView) findViewById(R.id.wifiNetworkName);
        wifiNetworkNameTextView.setText(wifiNetworkName);
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    private Bitmap generateQrCode(String qrData) {
        Display display = getWindowManager().getDefaultDisplay();
        Point outSize = new Point();
        int x, y, qrCodeDimension;
        /* lame, got to use both the new and old APIs here */
        if (android.os.Build.VERSION.SDK_INT >= 13) {
            display.getSize(outSize);
            x = outSize.x;
            y = outSize.y;
        } else {
            x = display.getWidth();
            y = display.getHeight();
        }
        if (outSize.x < outSize.y)
            qrCodeDimension = x;
        else
            qrCodeDimension = y;
        Log.i(TAG, "generating QRCode Bitmap of " + qrCodeDimension + "x" + qrCodeDimension);
        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(qrData, null,
                Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeDimension);

        try {
            return qrCodeEncoder.encodeAsBitmap();
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return null;
    }

    public class WaitForWifiAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                while (!wifiManager.isWifiEnabled()) {
                    Log.i(TAG, "waiting for the wifi to be enabled...");
                    Thread.sleep(3000);
                    Log.i(TAG, "ever recover from sleep?");
                }
                Log.i(TAG, "0");
                ipAddress = wifiManager.getConnectionInfo().getIpAddress();
                Log.i(TAG, "1");
                while (ipAddress == 0) {
                    Log.i(TAG, "waiting for an IP address...");
                    Thread.sleep(3000);
                    ipAddress = wifiManager.getConnectionInfo().getIpAddress();
                }
                Log.i(TAG, "2");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Log.i(TAG, "onPostExecute " + ipAddress);
            repoSwitch.setChecked(false);
            if (wifiManager.isWifiEnabled() && ipAddress != 0) {
                setIpAddressFromWifi();
                wireRepoSwitchToWebServer();
            }
        }
    }

    private void startWebServer() {
        Runnable webServer = new Runnable() {
            @Override
            public void run() {
                final SimpleWebServer kerplappSrv = new SimpleWebServer(ipAddressString,
                        8888, getFilesDir(), false);
                Looper.prepare(); // must be run before creating a Handler
                handler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        // the only message this Thread responds to is STOP!
                        Log.i(TAG, "we've been asked to stop the webserver: " + msg.obj);
                        kerplappSrv.stop();
                    }
                };
                try {
                    File keyStoreFile = new File(app_keystore, "keystore.bks");
                    KeyStore store = Crypto.createKeyStore(keyStoreFile);

                    String password = Crypto.KEYSTORE_PASS;
                    InputStream keyStoreFileIS = new FileInputStream(keyStoreFile);
                    store.load(keyStoreFileIS, password.toCharArray());
                    
                    KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                    keyManagerFactory.init(store, password.toCharArray());
                    
                    SSLServerSocketFactory factory =
                      NanoHTTPD.makeSSLSocketFactory(store, keyManagerFactory);
                    kerplappSrv.makeSecure(factory);
                    kerplappSrv.start();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                } catch (KeyStoreException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (CertificateException e) {
                    e.printStackTrace();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                } catch (NoSuchProviderException e) {
                    e.printStackTrace();
                } catch (SignatureException e) {
                    e.printStackTrace();
                } catch (UnrecoverableKeyException e) {
                	e.printStackTrace();
                }
                Looper.loop(); // start the message receiving loop
            }
        };
        webServerThread = new Thread(webServer);
        webServerThread.start();
    }

    private void stopWebServer() {
        Log.i(TAG, "stop the webserver");
        Message msg = handler.obtainMessage();
        msg.obj = handler.getLooper().getThread().getName() + " says stop";
        handler.sendMessage(msg);
    }

    public class ScanForAppsTask extends AsyncTask<String, String, ArrayList<AppListEntry>>
            implements ScanListener {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showDialog(0);
        }

        /**
         * Downloading file in background thread
         */
        @Override
        protected ArrayList<AppListEntry> doInBackground(String... arg) {
            try {
                KerplappApplication appCtx = (KerplappApplication) getApplication();
                KerplappRepo repo = appCtx.getRepo();
                return repo.loadInstalledPackageNames(this);
            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }
            return null;
        }

        /**
         * Updating progress bar
         */
        @Override
        protected void onProgressUpdate(String... progress) {
            // setting progress percentage
            repoProgress.setProgress(Integer.parseInt(progress[0]));
        }

        @Override
        protected void onPostExecute(ArrayList<AppListEntry> pkgs) {
            dismissDialog(0);
            Intent i = new Intent(getApplicationContext(), AppSelectActivity.class);
            i.putParcelableArrayListExtra("packages", pkgs);
            startActivity(i);
        }

        @Override
        public void processedApp(String pkgName, int index, int total) {
            float progress = index / (float) total;
            int progressPercent = (int) (progress * 100);
            publishProgress(String.valueOf(progressPercent));
        }

    }

}
