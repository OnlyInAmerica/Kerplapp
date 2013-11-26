/*
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Based on Paul Blundell's Tutorial:
http://blog.blundell-apps.com/tut-asynctask-loader-using-support-library/

which is originally based on:
https://developer.android.com/reference/android/content/AsyncTaskLoader.html
 */

package net.binaryparadox.kerplapp;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.ListView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppListFragment extends ListFragment implements LoaderCallbacks<List<AppEntry>> {

    private AppListAdapter adapter;
    private Set<String> selectedApps = new HashSet<String>();

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setEmptyText(getString(R.string.no_applications_found));

        adapter = new AppListAdapter(getActivity());
        setListAdapter(adapter);
        setListShown(false);

        // Prepare the loader
        // either reconnect with an existing one or start a new one
        getLoaderManager().initLoader(0, null, this);
    }

    public String[] getSelectedApps() {
        return selectedApps.toArray(new String[0]);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        AppEntry appEntry = (AppEntry) adapter.getItem(position);
        appEntry.setEnabled(!appEntry.isEnabled());
        if (appEntry.isEnabled()) {
            selectedApps.add(appEntry.getPackageName());
            v.setBackgroundColor(getResources().getColor(R.color.app_selected));
        } else {
            selectedApps.remove(appEntry.getPackageName());
            v.setBackgroundColor(getResources().getColor(android.R.color.background_dark));
        }
    }

    @Override
    public Loader<List<AppEntry>> onCreateLoader(int id, Bundle args) {
        // This is called when a new loader needs to be created.
        // This sample only has one Loader with no arguments, so it is simple.
        return new AppListLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<List<AppEntry>> loader, List<AppEntry> data) {
        adapter.setData(data);

        // The list should now be shown
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<AppEntry>> loader) {
        // Clear the data in the adapter
        adapter.setData(null);
    }
}
