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

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppListLoader extends AsyncTaskLoader<List<AppEntry>> {

    private static final String TAG = "AppListLoader";
    InterestingConfigChanges lastConfig = new InterestingConfigChanges();
    public final PackageManager pm;

    List<AppEntry> apps;
    PackageIntentReceiver packageObserver;

    public AppListLoader(Context context) {
        super(context);

        // Retrieve the package manager for later use; note we don't
        // use 'context' directly but instead the safe global application
        // context returned by getContext().
        pm = getContext().getPackageManager();
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        // AsyncTaskLoader doesnt start unless you forceLoad
        // http://code.google.com/p/android/issues/detail?id=14944
        if (apps != null) {
            deliverResult(apps);
        }
        if (takeContentChanged() || apps == null) {
            forceLoad();
        }
    }

    /**
     * This is where the bulk of the work. This function is called in a
     * background thread and should generate a new set of data to be published
     * by the loader.
     */
    @Override
    public List<AppEntry> loadInBackground() {
        // Retrieve all known applications
        List<ApplicationInfo> apps = pm.getInstalledApplications(
                PackageManager.GET_UNINSTALLED_PACKAGES |
                        PackageManager.GET_DISABLED_COMPONENTS);

        if (apps == null) {
            apps = new ArrayList<ApplicationInfo>();
        }

        final Context context = getContext();
        final KerplappApplication app = (KerplappApplication) context.getApplicationContext();
        final File repoDir = app.getLocalRepo().repoDir;

        // Create corresponding array of entries and load their labels
        List<AppEntry> entries = new ArrayList<AppEntry>(apps.size());
        for (ApplicationInfo applicationInfo : apps) {
            AppEntry entry = new AppEntry(this, applicationInfo);
            entry.loadLabel(context);

            // Need to load the package info to get the app version code.
            // We can use the repo dir, the package name and the version code to
            // determine if the app is already in the repo to preselect it.
            PackageInfo packageInfo = null;
            try {
            	packageInfo = pm.getPackageInfo(entry.getPackageName(), 0);
            } catch (NameNotFoundException e) {
                Log.e(TAG, e.getMessage());
                continue; //We need the package info for versionCode, skip this app
            }

            String apkName = packageInfo.packageName + "_" + packageInfo.versionCode +".apk";
            File apkFile = new File(repoDir, apkName);

            if(apkFile.exists())
            	entry.setEnabled(true);

            entries.add(entry); //Add the entry if nothing has gone wrong
        }

        Collections.sort(entries, Comparator.ALPHA_COMPARATOR);

        return entries;
    }

    /**
     * Called when there is new data to deliver to the client. The super class
     * will take care of delivering it; the implementation just adds a little
     * more logic
     */
    @Override
    public void deliverResult(List<AppEntry> apps) {
        if (isReset()) {
            // An async query came in while the loader is stopped. We don't need
            // the result
            if (apps != null) {
                onReleaseResources(apps);
            }
        }
        List<AppEntry> oldApps = this.apps;
        this.apps = apps;

        if (isStarted()) {
            // If the loader is currently started, we can immediately deliver a
            // result
            super.deliverResult(apps);
        }

        // At this point we can release the resources associated with 'oldApps'
        // if needed;
        // now that the new result is delivered we know that it is no longer in
        // use
        if (oldApps != null) {
            onReleaseResources(oldApps);
        }
    }

    @Override
    protected void onStopLoading() {
        // Attempts to cancel the current load task if possible
        cancelLoad();
    }

    @Override
    public void onCanceled(List<AppEntry> apps) {
        super.onCanceled(apps);

        // At this point we can release the resources associated with 'apps' if
        // needed
        onReleaseResources(apps);
    }

    /**
     * Handles request to completely reset the loader
     */
    @Override
    protected void onReset() {
        super.onReset();

        // ensure the loader is stopped
        onStopLoading();

        // At this point we can release the resources associated with 'apps' if
        // needed
        if (apps != null) {
            onReleaseResources(apps);
            apps = null;
        }

        // Stop monitoring for changes
        if (packageObserver != null) {
            getContext().unregisterReceiver(packageObserver);
            packageObserver = null;
        }
    }

    /**
     * Helper function to take care of releasing resources associated with an
     * actively loaded data set
     */
    private void onReleaseResources(List<AppEntry> apps) {
        // For a simple list there is nothing to do
        // but for a Cursor we would close it here
    }

    public static class InterestingConfigChanges {
        final Configuration lastConfiguration = new Configuration();
        int lastDensity;

        protected boolean applyNewConfig(Resources res) {
            int configChanges = lastConfiguration.updateFrom(res.getConfiguration());
            boolean densityChanged = lastDensity != res.getDisplayMetrics().densityDpi;
            if (densityChanged
                    || (configChanges & (ActivityInfo.CONFIG_LOCALE | ActivityInfo.CONFIG_UI_MODE | ActivityInfo.CONFIG_SCREEN_LAYOUT)) != 0) {
                lastDensity = res.getDisplayMetrics().densityDpi;
                return true;
            }
            return false;
        }
    }
}
