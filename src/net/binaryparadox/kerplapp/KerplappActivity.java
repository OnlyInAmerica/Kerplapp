
package net.binaryparadox.kerplapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import fi.iki.elonen.SimpleWebServer;

import net.binaryparadox.kerplapp.repo.Crypto;
import net.binaryparadox.kerplapp.repo.KerplappRepo;
import net.binaryparadox.kerplapp.repo.KerplappRepo.ScanListener;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Locale;

@SuppressLint("DefaultLocale")
public class KerplappActivity extends Activity {
    private static final String TAG = PackageReceiver.class.getCanonicalName();
    private ProgressDialog repoProgress;

    private String uriString = null;
    private File app_keystore;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        app_keystore = getDir("keystore", Context.MODE_PRIVATE);

        final Context ctx = getApplicationContext();


        final Button w = (Button) findViewById(R.id.startBtn);

        w.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
                    int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
                    final String formatedIpAddress = String.format(Locale.CANADA, "%d.%d.%d.%d",
                            (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                            (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));

                    uriString = "http://" + formatedIpAddress + ":8888/repo";
                    Toast toast = Toast.makeText(v.getContext().getApplicationContext(),
                            "Please access! " + uriString,
                            Toast.LENGTH_SHORT);
                    toast.show();

                    Runnable webServer = new Runnable() {
                        @Override
                        public void run() {
                            SimpleWebServer kerplappSrv = new SimpleWebServer(formatedIpAddress,
                                    8888,
                                    ctx.getFilesDir(), false);

                            try {
                                String jksPath = new File(app_keystore, "keystore.jks")
                                        .getAbsolutePath();
                                String password = Crypto.KEYSTORE_PASS;

                                KeyStore store = Crypto.createKeyStore(new File(jksPath));

                                // SSLServerSocketFactory factory =
                                // NanoHTTPD.makeSSLSocketFactory(jksPath,
                                // password.toCharArray());
                                // kerplappSrv.makeSecure(factory);
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
                            }
                        }

                    };

                    Thread webServerThread = new Thread(webServer);
                    webServerThread.start();
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        });
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
