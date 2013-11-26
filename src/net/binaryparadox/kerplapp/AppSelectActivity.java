
package net.binaryparadox.kerplapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.util.Log;
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
                new UpdateAsyncTask(this).execute();
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    class UpdateAsyncTask extends AsyncTask<Void, Integer, Void> {
        private ProgressDialog progressDialog;

        public UpdateAsyncTask(Context c) {
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
            KerplappRepo repo = ((KerplappApplication) getApplication()).getRepo();
            try {
                publishProgress(R.string.deleting_repo);
                repo.deleteRepo();
                publishProgress(R.string.writing_index_xml);
                repo.writeIndexXML();
                publishProgress(R.string.writing_index_jar);
                repo.writeIndexJar();
                publishProgress(R.string.linking_apks);
                repo.copyApksToRepo();
                publishProgress(R.string.copying_icons);
                repo.copyIconsToRepo();
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            progressDialog.setMessage(getString(progress[0]));
        }

        @Override
        protected void onPostExecute(Void result) {
            progressDialog.dismiss();
            Toast.makeText(getBaseContext(), R.string.updated_local_repo, Toast.LENGTH_SHORT)
                    .show();
            finish();
        }
    }
}
