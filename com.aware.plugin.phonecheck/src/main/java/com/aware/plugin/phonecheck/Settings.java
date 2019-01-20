package com.aware.plugin.phonecheck;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.aware.Aware;

import java.util.Objects;

public class Settings extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    //Plugin settings in XML @xml/preferences
    public static final String STATUS_PLUGIN_TEMPLATE = "status_plugin_template";
    public static final String SUMMARY_TIME_GRANULARITY = "summary_time_granularity";

    //Plugin settings UI elements
    private static CheckBoxPreference status;
    private static ListPreference summaryTimeGranularity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        status = (CheckBoxPreference) findPreference(STATUS_PLUGIN_TEMPLATE);
        summaryTimeGranularity = (ListPreference) findPreference(SUMMARY_TIME_GRANULARITY);
        if( Aware.getSetting(this, STATUS_PLUGIN_TEMPLATE).length() == 0 ) {
            Aware.setSetting( this, STATUS_PLUGIN_TEMPLATE, true ); //by default, the setting is true on install
        }
        if (Aware.getSetting(this, SUMMARY_TIME_GRANULARITY).length() == 0) {
            String defaultValue = summaryTimeGranularity.getValue();
            Aware.setSetting(this, SUMMARY_TIME_GRANULARITY, defaultValue);
        }

        status.setChecked(Aware.getSetting(getApplicationContext(), STATUS_PLUGIN_TEMPLATE).equals("true"));

        setSummaryTimeGranularity(Aware.getSetting(this, SUMMARY_TIME_GRANULARITY));
    }

    private void setSummaryTimeGranularity(String value) {
        String summaryTimeDefaultTitle = getResources().getString(R.string.summary_time);
        String[] stringArray = getResources().getStringArray(R.array.phonecheck_granularity_readable);
        if (Objects.equals(String.valueOf(getResources().getInteger(R.integer.phonecheck_last_24_hours)), value)){
            summaryTimeGranularity.setTitle(summaryTimeDefaultTitle + ": " + stringArray[0]);
        } else if (Objects.equals(String.valueOf(getResources().getInteger(R.integer.phonecheck_last_7_days)), value)){
            summaryTimeGranularity.setTitle(summaryTimeDefaultTitle + ": " + stringArray[1]);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference setting = findPreference(key);
        if( setting.getKey().equals(STATUS_PLUGIN_TEMPLATE) ) {
            Aware.setSetting(this, key, sharedPreferences.getBoolean(key, false));
            status.setChecked(sharedPreferences.getBoolean(key, false));
        } else if (setting.getKey().equals(SUMMARY_TIME_GRANULARITY)) {
            Aware.setSetting(this, key, sharedPreferences.getString(key, "24"));
            setSummaryTimeGranularity(sharedPreferences.getString(key, "24"));
        }

        if (Aware.getSetting(this, STATUS_PLUGIN_TEMPLATE).equals("true")) {
            Aware.startPlugin(getApplicationContext(), "com.aware.plugin.phonecheck");
        } else {
            Aware.stopPlugin(getApplicationContext(), "com.aware.plugin.phonecheck");
        }
    }
}
