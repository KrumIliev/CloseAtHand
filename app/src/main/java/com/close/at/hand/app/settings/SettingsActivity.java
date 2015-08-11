package com.close.at.hand.app.settings;

import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

import com.close.at.hand.app.R;
import com.close.at.hand.app.Utility;
import com.close.at.hand.app.data.DatabaseMap;
import com.close.at.hand.app.data.FetchPlacesListService;

public class SettingsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener {

    private final String LOG_TAG = SettingsActivity.class.getSimpleName();

    // since we use the preference change initially to populate the summary
    // field, we'll ignore that change at start of the activity
    private boolean mBindingPreference;

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setupSimplePreferencesScreen();
    }

    private void setupSimplePreferencesScreen() {
        // Add 'general' preferences.
        addPreferencesFromResource(R.xml.pref_general);

        // Bind the summaries of preferences to
        // their values. When their values change, their summaries are updated
        // to reflect the new value, per the Android Design guidelines.
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_radius_key)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_types_key)));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        String stringValue = o.toString();

        Log.d(LOG_TAG, preference.getKey());

        // are we starting the preference activity?
        if (!mBindingPreference) {
            if (preference instanceof SelectRangePreference || preference instanceof ListPreference) {
                Intent intent = new Intent(this, FetchPlacesListService.class);
                startService(intent);
            } else {
                // notify code that weather may be impacted
                getContentResolver().notifyChange(DatabaseMap.PlaceEntry.CONTENT_URI, null);
            }
        }

        if (preference instanceof SelectRangePreference) {
            int radius = PreferenceManager.getDefaultSharedPreferences(this)
                    .getInt(preference.getKey(),
                            getResources().getInteger(R.integer.def_search_radius));
            preference.setSummary(getString(R.string.pref_radius_summary)
                    + " " + Utility.formatRadius(radius));
        } else if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(stringValue);
            if (prefIndex >= 0) {
                preference.setSummary(listPreference.getEntries()[prefIndex]);
            }
        } else {
            preference.setSummary(stringValue);
        }
        return true;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     */
    private void bindPreferenceSummaryToValue(Preference preference) {
        mBindingPreference = true;
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(this);

        // Trigger the listener immediately with the preference's
        // current value.
        if (preference instanceof SelectRangePreference) {
            onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getInt(preference.getKey(),
                                    getResources().getInteger(R.integer.def_search_radius)));
        } else {
            onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));
        }
        mBindingPreference = false;
    }
}
