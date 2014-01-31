
package net.binaryparadox.kerplapp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import net.binaryparadox.kerplapp.repo.KerplappRepo;

public class AppSelectActivity extends FragmentActivity {
    private final String TAG = "AppSelectActivity";
    private AppListFragment appListFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_select_activity);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (appListFragment == null)
            appListFragment = (AppListFragment) getSupportFragmentManager().findFragmentById(
                    R.id.fragment_app_list);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.app_select_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                new UpdateAsyncTask(this, appListFragment.getSelectedApps()).execute();
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    class UpdateAsyncTask extends AsyncTask<Void, String, Void> {
        private ProgressDialog progressDialog;
        private String[] selectedApps;

        public UpdateAsyncTask(Context c, String[] apps) {
            selectedApps = apps;
            progressDialog = new ProgressDialog(c);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setTitle(R.string.updating);
        }

        @Override
        protected void onPreExecute() {
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            KerplappRepo repo = ((KerplappApplication) getApplication()).getKerplappRepo();
            try {
                publishProgress(getString(R.string.deleting_repo));
                repo.deleteRepo();
                for (String app : selectedApps) {
                    publishProgress(String.format(getString(R.string.adding_apks_format), app));
                    repo.addApp(app);
                }
                publishProgress(getString(R.string.writing_index_xml));
                repo.writeIndexXML();
                publishProgress(getString(R.string.writing_index_jar));
                repo.writeIndexJar();
                publishProgress(getString(R.string.linking_apks));
                repo.copyApksToRepo();
                publishProgress(getString(R.string.copying_icons));
                repo.copyIconsToRepo();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            super.onProgressUpdate(progress);
            progressDialog.setMessage(progress[0]);
        }

        @Override
        protected void onPostExecute(Void result) {
            progressDialog.dismiss();
            setResult(Activity.RESULT_OK);
            Toast.makeText(getBaseContext(), R.string.updated_local_repo, Toast.LENGTH_SHORT)
                    .show();
            finish();
        }
    }
}
