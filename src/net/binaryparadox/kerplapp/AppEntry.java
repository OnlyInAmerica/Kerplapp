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
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;

import java.io.File;

public class AppEntry {

    private final AppListLoader loader;
    private final ApplicationInfo info;
    private final File apkFile;
    private boolean enabled;
    private String label;
    private Drawable icon;
    private boolean mounted;

    public AppEntry(AppListLoader loader, ApplicationInfo info) {
        this.loader = loader;
        this.info = info;
        apkFile = new File(info.sourceDir);
    }

    public ApplicationInfo getApplicationInfo() {
        return info;
    }

    public String getPackageName() {
        return info.packageName;
    }

    public String getLabel() {
        return label;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Drawable getIcon() {
        if (icon == null) {
            if (apkFile.exists()) {
                icon = info.loadIcon(loader.pm);
                return icon;
            } else {
                mounted = false;
            }
        } else if (!mounted) {
            // If the app wasn't mounted but is now mounted, reload its icon
            if (apkFile.exists()) {
                mounted = true;
                icon = info.loadIcon(loader.pm);
            }
        } else {
            return icon;
        }

        return loader.getContext().getResources().getDrawable(android.R.drawable.sym_def_app_icon);
    }

    @Override
    public String toString() {
        return label;
    }

    public void loadLabel(Context context) {
        if (label == null || !mounted) {
            if (!apkFile.exists()) {
                mounted = false;
                label = info.packageName;
            } else {
                mounted = true;
                CharSequence label = info.loadLabel(context.getPackageManager());
                this.label = label != null ? label.toString() : info.packageName;
            }
        }
    }
}
