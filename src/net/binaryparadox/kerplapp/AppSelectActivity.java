
package net.binaryparadox.kerplapp;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import net.binaryparadox.kerplapp.repo.KerplappRepo;

import java.util.ArrayList;
import java.util.List;

public class AppSelectActivity extends Activity {
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
            public void onClick(View v)
            {
                ArrayList<AppListEntry> appList = dataAdapter.appList;
                ArrayList<AppListEntry> checked = new ArrayList<AppListEntry>();

                for (AppListEntry a : appList)
                {
                    if (a.isChecked())
                        checked.add(a);
                }

                try
                {
                    for (AppListEntry e : checked)
                        repo.addAppToRepo(e.getPkgName());

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

        Bundle extras = getIntent().getExtras();

        List<AppListEntry> installedPackages = extras.getParcelableArrayList("packages");

        displayListView(installedPackages);
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

    private void displayListView(List<AppListEntry> installedPackages)
    {
        dataAdapter = new AppListAdapter(this, this.getApplicationContext(),
                R.layout.app_select_info,
                (ArrayList<AppListEntry>) installedPackages);
        ListView listView = (ListView) findViewById(R.id.appListView);
        listView.setAdapter(dataAdapter);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                /*
                 * App app = (App) parent.getItemAtPosition(position);
                 * Toast.makeText(getApplicationContext(), "Clicked on Row: " +
                 * app.id, Toast.LENGTH_LONG).show();
                 */
            }
        });
    }

}
