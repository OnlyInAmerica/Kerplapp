
package net.binaryparadox.kerplapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.preference.PreferenceManager;
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

import net.binaryparadox.kerplapp.repo.KerplappRepo;

import org.spongycastle.operator.OperatorCreationException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Locale;

@SuppressLint("DefaultLocale")
public class KerplappActivity extends Activity {
    private static final String TAG = KerplappActivity.class.getCanonicalName();
    private ProgressDialog repoProgress;

    private ToggleButton repoSwitch;
    private WifiManager wifiManager;
    private String wifiNetworkName = "";
    private int ipAddress = 0;
    private int port = 8888;
    private String ipAddressString = null;
    private String repoUriString = null;

    private Thread webServerThread = null;
    private Handler handler = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.kerplapp_activity);

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
                startActivity(new Intent(this, AppSelectActivity.class));
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
                startActivity(new Intent(this, SettingsActivity.class));
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
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean useHttps = prefs.getBoolean("use_https", false);
        
        final KerplappApplication appCtx = (KerplappApplication) getApplication();              
        final KerplappRepo repo = appCtx.getRepo();
               
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        ipAddress = wifiInfo.getIpAddress();
        ipAddressString = String.format(Locale.CANADA, "%d.%d.%d.%d",
                (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));

        repoUriString = String.format(Locale.CANADA, "%s://%s:%d/repo",
                useHttps ? "https" : "http",
                ipAddressString, port);
        

        repoSwitch.setText(repoUriString);
        repoSwitch.setTextOn(repoUriString);
        repoSwitch.setTextOff(repoUriString);
        ImageView repoQrCodeImageView = (ImageView) findViewById(R.id.repoQrCode);
        // F-Droid currently only understands fdroidrepo:// and fdroidrepos://
        String fdroidrepoUriString = repoUriString.replace("https", "fdroidrepos");
        fdroidrepoUriString = fdroidrepoUriString.replace("http", "fdroidrepo");
        repoQrCodeImageView.setImageBitmap(generateQrCode(fdroidrepoUriString));
        
        repo.writeIndexPage(fdroidrepoUriString);
        
        wifiNetworkName = wifiInfo.getSSID();
        TextView wifiNetworkNameTextView = (TextView) findViewById(R.id.wifiNetworkName);
        wifiNetworkNameTextView.setText(wifiNetworkName);

        KerplappKeyStore keyStore = appCtx.getKeyStore();

        // Once the IP address is known we need to generate a self signed
        // certificate to use for HTTPS that has a CN field set to the 
        // ipAddressString. We'll generate it even if useHttps is false
        // to simplify having to detect when that preference changes.
        try {
            keyStore.setupHTTPSCertificate(ipAddressString);
        } catch (UnrecoverableKeyException e1) {
            e1.printStackTrace();
        } catch (CertificateException e1) {
            e1.printStackTrace();
        } catch (OperatorCreationException e1) {
            e1.printStackTrace();
        } catch (KeyStoreException e1) {
            e1.printStackTrace();
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
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
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean useHttps = prefs.getBoolean("use_https", false);

        Runnable webServer = new Runnable() {
            @Override
            public void run() {
                final KerplappHTTPD kerplappSrv = new KerplappHTTPD(ipAddressString,
                        port, getFilesDir(), false);

                if (useHttps)
                {
                    KerplappApplication appCtx = (KerplappApplication) getApplication();
                    KerplappKeyStore keyStore = appCtx.getKeyStore();
                    kerplappSrv.enableHTTPS(keyStore);
                }

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
                    kerplappSrv.start();
                } catch (IOException e) {
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

}
