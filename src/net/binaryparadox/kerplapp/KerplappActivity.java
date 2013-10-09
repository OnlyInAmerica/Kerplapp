package net.binaryparadox.kerplapp;

import java.util.Locale;

import net.binaryparadox.kerplapp.KerplappRepo.ScanListener;

import fi.iki.elonen.SimpleWebServer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

@SuppressLint("DefaultLocale")
public class KerplappActivity extends Activity 
{
    private static final String TAG = PackageReceiver.class.getCanonicalName();
    private ProgressDialog repoProgress;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
         
        final Button b = (Button) findViewById(R.id.plopBtn);      
        final Context ctx = getApplicationContext();
                
        b.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v)
          {        
            new ScanForAppsTask().execute();
          } 
        });
        
        final Button w = (Button) findViewById(R.id.startBtn);
        
        w.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v)
          {
            try
            {
              WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
              int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
              final String formatedIpAddress = String.format(Locale.CANADA, "%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
              (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));

              Toast toast = Toast.makeText(v.getContext().getApplicationContext(),
                  "Please access! http://" + formatedIpAddress + ":"+ 8888,
                  Toast.LENGTH_SHORT);
              toast.show();

              SimpleWebServer kerplappSrv = new SimpleWebServer(formatedIpAddress, 8888, 
                                                                ctx.getFilesDir(), false);
           
              kerplappSrv.start();
            } catch(Exception e) {
              Log.e(TAG, e.getMessage());
            }
          }
        });
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
    
  public class ScanForAppsTask extends AsyncTask<String, String, String> implements ScanListener
  {
    @Override
    protected void onPreExecute()
    {
      super.onPreExecute();
      showDialog(0);
    }

    /**
     * Downloading file in background thread
     * */
    @Override
    protected String doInBackground(String... arg)
    {
      try
      {
        KerplappApplication appCtx = (KerplappApplication) getApplication();
        KerplappRepo repo = appCtx.getRepo();
        repo.scanForApps(this);   
      } catch (Exception e) {
        Log.e("Error: ", e.getMessage());
      }
      return null;
    }

    /**
     * Updating progress bar
     * */
    protected void onProgressUpdate(String... progress)
    {
      // setting progress percentage
      repoProgress.setProgress(Integer.parseInt(progress[0]));
    }

    @Override
    protected void onPostExecute(String file_url)
    {
      dismissDialog(0);
      Intent i = new Intent(getApplicationContext(), AppSelectActivity.class);
      startActivity(i);
    }

    @Override
    public void processedApp(String pkgName, int index, int total)
    {
      float progress = index / (float) total;
      int progressPercent = (int) (progress * 100);
      publishProgress(String.valueOf(progressPercent));
    }

  }
  
}