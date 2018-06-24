/* 
 * Copyright (C) 2014 DarkKat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nitrogen.settings.fragments;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.internal.logging.nano.MetricsProto;

import com.android.settings.development.DevelopmentSettings;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.nitrogen.settings.preferences.CustomSeekBarPreference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.util.Log;

public class LockScreenWeatherSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String KEY_OMNIJAWS = "omnijaws";

    private static final String WEATHER_ICON_PACK = "weather_icon_pack";
    private static final String DEFAULT_WEATHER_ICON_PACKAGE = "org.omnirom.omnijaws";
    private static final String CHRONUS_ICON_PACK_INTENT = "com.dvtonder.chronus.ICON_PACK";
    private static final String WEATHER_SERVICE_PACKAGE = "org.omnirom.omnijaws";

    private ListPreference mWeatherIconPack;

    private ContentResolver mResolver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.lock_screen_weather_settings);
        mResolver = getActivity().getContentResolver();
        PreferenceScreen prefs = getPreferenceScreen();

        if (!isOmniJawsServiceInstalled()) {
            getPreferenceScreen().removePreference(findPreference(KEY_OMNIJAWS));
            getPreferenceScreen().removePreference(findPreference(WEATHER_ICON_PACK));
        } else {
            initweather();
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.NITROGEN_SETTINGS;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mWeatherIconPack) {
                String value = (String) newValue;
                Settings.System.putString(getContentResolver(),
                    Settings.System.OMNIJAWS_WEATHER_ICON_PACK, value);
                int valueIndex = mWeatherIconPack.findIndexOfValue(value);
                mWeatherIconPack.setSummary(mWeatherIconPack.getEntries()[valueIndex]);
               return true;
        }
        return false;
    }

    private boolean isOmniJawsServiceInstalled() {
         return DevelopmentSettings.isPackageInstalled(getActivity(), WEATHER_SERVICE_PACKAGE);
     }

    public void initweather() {
        String settingsJaws = Settings.System.getString(getContentResolver(),
             Settings.System.OMNIJAWS_WEATHER_ICON_PACK);
         if (settingsJaws == null) {
             settingsJaws = DEFAULT_WEATHER_ICON_PACKAGE;
         }
         mWeatherIconPack = (ListPreference) findPreference(WEATHER_ICON_PACK);

         List<String> entriesJaws = new ArrayList<String>();
         List<String> valuesJaws = new ArrayList<String>();
         getAvailableWeatherIconPacks(entriesJaws, valuesJaws);
         mWeatherIconPack.setEntries(entriesJaws.toArray(new String[entriesJaws.size()]));
         mWeatherIconPack.setEntryValues(valuesJaws.toArray(new String[valuesJaws.size()]));

         int valueJawsIndex = mWeatherIconPack.findIndexOfValue(settingsJaws);
         if (valueJawsIndex == -1) {
             // no longer found
             settingsJaws = DEFAULT_WEATHER_ICON_PACKAGE;
             Settings.System.putString(getContentResolver(),
                     Settings.System.OMNIJAWS_WEATHER_ICON_PACK, settingsJaws);
             valueJawsIndex = mWeatherIconPack.findIndexOfValue(settingsJaws);
         }
         mWeatherIconPack.setValueIndex(valueJawsIndex >= 0 ? valueJawsIndex : 0);
         mWeatherIconPack.setSummary(mWeatherIconPack.getEntry());
         mWeatherIconPack.setOnPreferenceChangeListener(this);
    }

     private void getAvailableWeatherIconPacks(List<String> entries, List<String> values) {
         Intent i = new Intent();
         PackageManager packageManager = getPackageManager();
         i.setAction("org.omnirom.WeatherIconPack");
         for (ResolveInfo r : packageManager.queryIntentActivities(i, 0)) {
             String packageName = r.activityInfo.packageName;
             Log.d("maxwen", packageName);
             if (packageName.equals(DEFAULT_WEATHER_ICON_PACKAGE)) {
                 values.add(0, r.activityInfo.name);
             } else {
                 values.add(r.activityInfo.name);
             }
             String label = r.activityInfo.loadLabel(getPackageManager()).toString();
             if (label == null) {
                 label = r.activityInfo.packageName;
             }
             if (packageName.equals(DEFAULT_WEATHER_ICON_PACKAGE)) {
                 entries.add(0, label);
             } else {
                 entries.add(label);
             }
         }
         i = new Intent(Intent.ACTION_MAIN);
         i.addCategory(CHRONUS_ICON_PACK_INTENT);
         for (ResolveInfo r : packageManager.queryIntentActivities(i, 0)) {
             String packageName = r.activityInfo.packageName;
             values.add(packageName + ".weather");
             String label = r.activityInfo.loadLabel(getPackageManager()).toString();
             if (label == null) {
                 label = r.activityInfo.packageName;
             }
             entries.add(label);
         }
     }

     private boolean isOmniJawsEnabled() {
         final Uri SETTINGS_URI
             = Uri.parse("content://org.omnirom.omnijaws.provider/settings");

         final String[] SETTINGS_PROJECTION = new String[] {
             "enabled"
         };

         final Cursor c = getContentResolver().query(SETTINGS_URI, SETTINGS_PROJECTION,
                 null, null, null);
         if (c != null) {
             int count = c.getCount();
             if (count == 1) {
                 c.moveToPosition(0);
                 boolean enabled = c.getInt(0) == 1;
                 return enabled;
             }
         }
         return true;
     }
}
