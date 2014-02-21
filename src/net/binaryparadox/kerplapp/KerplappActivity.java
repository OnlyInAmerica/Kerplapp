
package net.binaryparadox.kerplapp;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import net.binaryparadox.kerplapp.network.KerplappHTTPD;
import net.binaryparadox.kerplapp.network.NsdHelper;
import net.binaryparadox.kerplapp.repo.KerplappRepo;

import org.fdroid.fdroid.data.Repo;
import org.spongycastle.operator.OperatorCreationException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Locale;

public class KerplappActivity extends Activity {
    private static final String TAG = "KerplappActivity";
    private ProgressDialog repoProgress;

    private WifiManager wifiManager;
    private ToggleButton repoSwitch;
    private int ipAddress = 0;
    private int port = 8888;
    private String ipAddressString = null;
    private Repo repo = new Repo();

    private int SET_IP_ADDRESS = 0x7345;
    private int SEND_TEST_REPO = 0x7346;
    private Thread webServerThread = null;
    private Handler handler = null;
    
    private NsdHelper nsdHelper = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.kerplapp_activity);

        repoSwitch = (ToggleButton) findViewById(R.id.repoSwitch);
        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        
        nsdHelper = new NsdHelper(this);
        nsdHelper.initializeNsd();
    }

    @Override
    public void onResume() {
        super.onResume();
        int wifiState = wifiManager.getWifiState();
        if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (ipAddress != wifiInfo.getIpAddress()) {
                setIpAddressFromWifi();
                if (repoSwitch.isChecked()) {
                    stopWebServer();
                    startWebServer();
                }
            }

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
                startActivityForResult(new Intent(this, AppSelectActivity.class), SET_IP_ADDRESS);
                return true;
            case R.id.menu_send_to_fdroid:
                if (repo.address == null) {
                    Toast.makeText(this, "The repo is not configured yet!", Toast.LENGTH_LONG)
                            .show();
                } else {
                    // TODO check if F-Droid is actually installed instead of
                    // just crashing
                    Intent intent = new Intent(Intent.ACTION_VIEW, getSharingUri());
                    intent.setClassName("org.fdroid.fdroid", "org.fdroid.fdroid.ManageRepo");
                    startActivityForResult(intent, SEND_TEST_REPO);
                }
                return true;
            case R.id.menu_settings:
                startActivityForResult(new Intent(this, SettingsActivity.class), SET_IP_ADDRESS);
                return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SET_IP_ADDRESS && resultCode == Activity.RESULT_OK) {
            setIpAddressFromWifi();
        } else if (requestCode == SEND_TEST_REPO) {
            Intent intent = new Intent();
            intent.setClassName("org.fdroid.fdroid", "org.fdroid.fdroid.ManageRepo");
            startActivity(intent);
        }
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

    @TargetApi(14)
    private void setIpAddressFromWifi() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean useHttps = prefs.getBoolean("use_https", false);

        final KerplappApplication appCtx = (KerplappApplication) getApplication();
        final KerplappRepo kerplappRepo = appCtx.getKerplappRepo();

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String ssid = wifiInfo.getSSID().replaceAll("^\"(.*)\"$", "$1");
        KerplappKeyStore keyStore = appCtx.getKeyStore();

        ipAddress = wifiInfo.getIpAddress();
        ipAddressString = String.format(Locale.ENGLISH, "%d.%d.%d.%d",
                (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
        kerplappRepo.setIpAddressString(ipAddressString);

        String scheme;
        if (useHttps)
            scheme = "https";
        else
            scheme = "http";
        repo.address = String.format(Locale.ENGLISH, "%s://%s:%d/fdroid/repo",
                scheme, ipAddressString, port);
        repo.fingerprint = keyStore.getFingerprint();

        // the fingerprint is not useful on the button label
        String buttonLabel = repo.address.replaceAll("\\?.*$", "");
        repoSwitch.setText(buttonLabel);
        repoSwitch.setTextOn(buttonLabel);
        repoSwitch.setTextOff(buttonLabel);
        ImageView repoQrCodeImageView = (ImageView) findViewById(R.id.repoQrCode);
        // fdroidrepo:// and fdroidrepos:// ensures it goes directly to F-Droid
        Uri fdroidrepoUri = getSharingUri();
        kerplappRepo.setUriString(repo.address);
        kerplappRepo.writeIndexPage(fdroidrepoUri);
        // set URL to UPPER for compact QR Code, FDroid will translate it back
        Bitmap qrBitmap = Utils.generateQrCode(this,
                fdroidrepoUri.toString().toUpperCase(Locale.ENGLISH));
        repoQrCodeImageView.setImageBitmap(qrBitmap);

        TextView wifiNetworkNameTextView = (TextView) findViewById(R.id.wifiNetworkName);
        wifiNetworkNameTextView.setText(ssid);

        TextView fingerprintTextView = (TextView) findViewById(R.id.fingerprint);
        if (repo.fingerprint != null) {
            fingerprintTextView.setVisibility(View.VISIBLE);
            fingerprintTextView.setText(repo.fingerprint);
        } else {
            fingerprintTextView.setVisibility(View.GONE);
        }

        // Once the IP address is known we need to generate a self signed
        // certificate to use for HTTPS that has a CN field set to the
        // ipAddressString. We'll generate it even if useHttps is false
        // to simplify having to detect when that preference changes.
        try {
            keyStore.setupHTTPSCertificate(ipAddressString);
        } catch (UnrecoverableKeyException e) {
            Log.e(TAG, e.getMessage());
        } catch (CertificateException e) {
            Log.e(TAG, e.getMessage());
        } catch (OperatorCreationException e) {
            Log.e(TAG, e.getMessage());
        } catch (KeyStoreException e) {
            Log.e(TAG, e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, e.getMessage());
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        // the required NFC API was added in 4.0 aka Ice Cream Sandwich
        if (Build.VERSION.SDK_INT < 14) {
            return;
        }
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null)
            return;
        nfcAdapter.setNdefPushMessage(new NdefMessage(new NdefRecord[] {
                NdefRecord.createUri(getSharingUri()),
        }), this);
        }
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
                Log.e(TAG, e.getMessage());
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
        final boolean useNSD   = prefs.getBoolean("use_nsd", true);

        Runnable webServer = new Runnable() {
            @SuppressLint("HandlerLeak") //Tell Eclipse this is not a leak because of Looper use.
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

                if(nsdHelper != null && useNSD)
                {
                    Log.i(TAG, "Registering Kerplapp service with NSD");
                    nsdHelper.registerService(port);
                }

                Looper.prepare(); // must be run before creating a Handler
                handler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        // the only message this Thread responds to is STOP!
                        Log.i(TAG, "we've been asked to stop the webserver: " + msg.obj);
                        kerplappSrv.stop();

                        if (nsdHelper != null)
                        {
                            nsdHelper.tearDown();
                        }
                    }
                };
                try {
                    kerplappSrv.start();
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }
                Looper.loop(); // start the message receiving loop
            }
        };
        webServerThread = new Thread(webServer);
        webServerThread.start();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // ignore orientation/keyboard change
        super.onConfigurationChanged(newConfig);
    }

    private void stopWebServer() {
        Log.i(TAG, "stop the webserver");
        if (handler == null) {
            Log.i(TAG, "null handler in stopWebServer");
            return;
        }
        Message msg = handler.obtainMessage();
        msg.obj = handler.getLooper().getThread().getName() + " says stop";
        handler.sendMessage(msg);

        super.onPause();
    }

    // this is from F-Droid RepoDetailsActivity
    protected Uri getSharingUri() {
        Uri uri = Uri.parse(repo.address.replaceFirst("http", "fdroidrepo"));
        Uri.Builder b = uri.buildUpon();
        b.appendQueryParameter("fingerprint", repo.fingerprint);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String ssid = wifiInfo.getSSID().replaceAll("^\"(.*)\"$", "$1");
        String bssid = wifiInfo.getBSSID();
        if (!TextUtils.isEmpty(bssid)) {
            b.appendQueryParameter("bssid", Uri.encode(bssid));
            if (!TextUtils.isEmpty(ssid))
                b.appendQueryParameter("ssid", Uri.encode(ssid));
        }
        return b.build();
    }

}
