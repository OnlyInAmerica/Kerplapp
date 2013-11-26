
package net.binaryparadox.kerplapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import net.binaryparadox.kerplapp.repo.KerplappRepo;

public class AppSelectActivity extends FragmentActivity {
    private final String TAG = AppSelectActivity.class.getName();
    private ListFragment appListFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_select);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (appListFragment == null)
            appListFragment = (ListFragment) getSupportFragmentManager().findFragmentById(
                    R.id.fragment_app_list);
    }

    private void updateLocalRepo() {
        KerplappRepo repo = ((KerplappApplication) getApplication()).getRepo();
        repo.update();
        Toast.makeText(this, R.string.updated_local_repo, Toast.LENGTH_SHORT).show();
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
                updateLocalRepo();
                finish();
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
