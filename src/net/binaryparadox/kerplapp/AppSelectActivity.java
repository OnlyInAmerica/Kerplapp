
package net.binaryparadox.kerplapp;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import net.binaryparadox.kerplapp.repo.KerplappRepo;

public class AppSelectActivity extends FragmentActivity {
    private final String TAG = AppSelectActivity.class.getName();
    private AppListAdapter dataAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_select);
        // Show the Up button in the action bar.
        setupActionBar();

        final KerplappApplication appCtx = (KerplappApplication) getApplication();
        final KerplappRepo repo = appCtx.getRepo();

        final Button b = (Button) findViewById(R.id.repoCreateBtn);
        b.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    for (int i = 0; i < dataAdapter.getCount(); i++)
                        repo.addAppToRepo(((AppEntry) dataAdapter.getItem(i)).getPackageName());

                    repo.writeIndexXML();
                    repo.writeIndexJar();
                    repo.copyApksToRepo();

                    Toast toast = Toast.makeText(v.getContext().getApplicationContext(),
                            "Repo Created",
                            Toast.LENGTH_SHORT);
                    toast.show();
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        });

    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.app_select, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
