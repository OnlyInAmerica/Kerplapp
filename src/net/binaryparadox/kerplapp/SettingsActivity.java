
package net.binaryparadox.kerplapp;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.text.TextUtils;

@SuppressWarnings("deprecation") //See Task #2955
public class SettingsActivity extends PreferenceActivity
        implements OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
        prefs.registerOnSharedPreferenceChangeListener(this);
        EditTextPreference pref = (EditTextPreference)findPreference("repo_name");
        String current = pref.getText();
        if (TextUtils.isEmpty(current)) {
            String defaultValue = getDefaultRepoName();
            pref.setText(defaultValue);
        }
        setSummaries();
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals("use_https")) {
            setResult(Activity.RESULT_OK);
        } else if (key.equals("repo_name")) {
            setSummaries();
        }
    }

    private void setSummaries() {
        EditTextPreference pref = (EditTextPreference)findPreference("repo_name");
        String current = pref.getText();
        if (current.equals(getDefaultRepoName()))
            pref.setSummary(R.string.local_repo_name_summary);
        else
            pref.setSummary(current);
    }

    public static String getDefaultRepoName() {
        return (Build.BRAND + " " + Build.MODEL).replaceAll(" ", "-");
    }
}
