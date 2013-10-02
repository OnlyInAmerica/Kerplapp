package net.binaryparadox.kerplapp;

import java.util.Locale;

import fi.iki.elonen.SimpleWebServer;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class KerplappActivity extends Activity 
{
    private static final String TAG = PackageReceiver.class.getCanonicalName();
    
    private KerplappRepo repo = null;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
         
        final Button b = (Button) findViewById(R.id.plopBtn);      
        final Context ctx = getApplicationContext();
        
        if(repo == null)
          repo = new KerplappRepo(ctx);
        
        b.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v)
          {
            Toast toast = Toast.makeText(v.getContext().getApplicationContext(),
                                           "Building Repo", Toast.LENGTH_SHORT);
              
            try
            {
              repo.init();
            } catch (Exception e) {
              Log.e(TAG, e.getMessage());
            }
            
            toast.show();
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
    
  
}