package net.binaryparadox.kerplapp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import net.binaryparadox.kerplapp.KerplappRepo.Apk;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class KerplappActivity extends Activity 
{
    private static final String TAG = PackageReceiver.class.getCanonicalName();
    
    private final KerplappRepo repo = new KerplappRepo();
    
    private PackageManager pm;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
         
        final Button b = (Button) findViewById(R.id.plopBtn);
        
        final Context ctx = this.getApplicationContext();
        pm = ctx.getPackageManager();
        
        b.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v)
          {
            try
            {
              Toast toast = Toast.makeText(v.getContext().getApplicationContext(),
                                           "Scanning", 10);
                          
              toast.show();
              
              List<KerplappRepo.App> apps = repo.scanForApps(pm);
              
              File indexFile = new File(ctx.getFilesDir(), "index.xml");
              repo.buildIndexXML(apps, indexFile);
              
              ///data/data/net.binaryparadox.kerplapp/files/index.xml
              Log.i(TAG,"*************** "+ ctx.getFilesDir().toString() + "/index.xml");
              
            } catch (Exception e) {
              e.printStackTrace();
            }
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
              final String formatedIpAddress = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
              (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));

              Toast toast = Toast.makeText(v.getContext().getApplicationContext(),
                  "Please access! http://" + formatedIpAddress + ":"+ 8888,
                  10);
              toast.show();

              KerplappHTTPD server = new KerplappHTTPD(8888);
              server.start();
            } catch(Exception e) {
              Log.e(TAG, e.getMessage());
            }
          }
        });
    }
    
  
}