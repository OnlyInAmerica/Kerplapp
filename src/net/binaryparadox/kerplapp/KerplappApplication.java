
package net.binaryparadox.kerplapp;

import android.app.Application;
import android.util.Log;

import net.binaryparadox.kerplapp.repo.KerplappRepo;

public class KerplappApplication extends Application
{
    private static final String TAG = KerplappApplication.class.getName();
    private KerplappRepo repo = null;

    @Override
    public void onCreate()
    {
        super.onCreate();

        if (repo == null)
        {
            repo = new KerplappRepo(getApplicationContext());

            try
            {
                repo.init();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    public KerplappRepo getRepo()
    {
        return repo;
    }
}
