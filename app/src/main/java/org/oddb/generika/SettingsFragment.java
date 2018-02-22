/*
 *  Generika Android
 *  Copyright (C) 2018 ywesee GmbH
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.oddb.generika;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;

import java.lang.Boolean;
import java.lang.Object;
import java.util.Locale;

import org.oddb.generika.SettingsActivity;
import org.oddb.generika.preference.AppListPreference;


public class SettingsFragment extends PreferenceFragmentCompat {

  private static final String TAG = "SettingsFragment";

  private final static String kSearchType = "kSearchType";
  private final static String kSearchLang = "kSearchLang";
  private final static String kAppUseSystemLocale = "kAppUseSystemLocale";
  private final static String kAppLocale = "kAppLocale";
  // TODO: enable cloud storage support
  //private final static String kRecordSync = "kRecordSync";

  private SharedPreferences sharedPreferences;

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String _s) {
    // load settings from xml file
    addPreferencesFromResource(R.xml.user_settings);

    Context activity = getActivity();
    this.sharedPreferences = PreferenceManager
      .getDefaultSharedPreferences(activity);

    initLocales();
  }

  private void initLocales() {
    Preference useSystemLocale = findPreference(kAppUseSystemLocale);
    Preference appLocale = findPreference(kAppLocale);

    Boolean checked = sharedPreferences.getBoolean(kAppUseSystemLocale, false);
    Log.d(TAG, "(initLocaleStates) checked: " + checked);
    if (checked) {
      Locale systemLocale = Resources.getSystem()
        .getConfiguration().locale;
      setAppLocaleValue(systemLocale.getLanguage());

      appLocale.setEnabled(false);
    }

    // callback (app locale)
    appLocale.setOnPreferenceChangeListener(
      new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(
          Preference _preference, Object newValue) {
          Locale newLocale = new Locale(newValue.toString());
          changeCurrentLocale(newLocale);
          return true;
        }
      }
    );

    // callback (use system locale check)
    useSystemLocale.setOnPreferenceChangeListener(
      new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(
          Preference _preference, Object newValue) {

          Preference appLocale_ = findPreference(kAppLocale);
          if ((Boolean)newValue) {
            Locale systemLocale = Resources.getSystem()
              .getConfiguration().locale;

            setAppLocaleValue(systemLocale.getLanguage());
            appLocale_.setEnabled(false);

            // all activity extends BaseActivity (has currentLocale)
            if (systemLocale != (
                (SettingsActivity)getActivity()).currentLocale) {
              changeCurrentLocale(systemLocale);
            }
          } else {
            appLocale_.setEnabled(true);
          }
          return true;
        }
    });
  }

  /**
   * Set application locale based on system locale (language) value.
   * `de` is our default.
   */
  private void setAppLocaleValue(String language) {
    Log.d(TAG, "(initLocaleStates) language: " + language);

    AppListPreference appLocale = (AppListPreference)findPreference(
      kAppLocale);
    switch (language) {
      case "de": case "fr": case "en":
        appLocale.setValue(language);
        break;
      default:
        appLocale.setValue("de");
        break;
    }
  }

  /**
   * Change current (resource) locale.
   * SettingsActivity will be refreshed.
   */
  private void changeCurrentLocale(Locale newLocale) {
    Resources resources = getResources(); 
    DisplayMetrics metrics = resources.getDisplayMetrics(); 
    Configuration configuration = resources.getConfiguration(); 
    configuration.locale = newLocale; 
    resources.updateConfiguration(configuration, metrics); 

    // refresh
    ((SettingsActivity)getActivity()).finish();
    Intent intent = new Intent(getContext(), SettingsActivity.class); 
    startActivity(intent); 
  }
}
