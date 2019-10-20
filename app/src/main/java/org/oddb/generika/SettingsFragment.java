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
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import android.util.Log;

import java.lang.Boolean;
import java.lang.Object;
import java.util.Locale;

import org.oddb.generika.preference.AppListPreference;
import org.oddb.generika.util.AppLocale;
import org.oddb.generika.util.Constant;


public class SettingsFragment extends PreferenceFragmentCompat {
  private static final String TAG = "SettingsFragment";

  private SharedPreferences sharedPreferences;

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String _s) {
    // load settings from xml file
    addPreferencesFromResource(R.xml.user_settings);

    Context context = getContext();
    this.sharedPreferences = PreferenceManager
      .getDefaultSharedPreferences(context);

    initLocales();
  }

  private void initLocales() {
    Preference useSystemLocale = findPreference(Constant.kAppUseSystemLocale);
    Preference appLocale = findPreference(Constant.kAppLocale);

    Boolean checked = sharedPreferences.getBoolean(
      Constant.kAppUseSystemLocale, false);
    Log.d(TAG, "(initLocaleStates) kAppUseSystemLocale checked: " + checked);
    if (checked) {
      Locale systemLocale = Resources.getSystem()
        .getConfiguration().locale;
      updateAppLocaleValue(systemLocale.getLanguage());

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

          Preference appLocale_ = findPreference(Constant.kAppLocale);
          if ((Boolean)newValue) {
            Locale systemLocale = Resources.getSystem()
              .getConfiguration().locale;

            updateAppLocaleValue(systemLocale.getLanguage());
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
   * Update application locale based on system locale (language) value.
   * `de` is our default.
   */
  private void updateAppLocaleValue(String language) {
    Log.d(TAG, "(initLocaleStates) language: " + language);

    AppListPreference appLocale = (AppListPreference)findPreference(
      Constant.kAppLocale);
    switch (language) {
      case Constant.LANG_DE: case Constant.LANG_FR: case Constant.LANG_EN:
        appLocale.setValue(language);
        break;
      default:
        appLocale.setValue(Constant.LANG_DE);
        break;
    }
  }

  /**
   * Change current (resource) locale.
   * SettingsActivity will be refreshed.
   */
  private void changeCurrentLocale(Locale newLocale) {
    AppLocale.setLocale(getContext(), newLocale);

    // refresh
    ((SettingsActivity)getActivity()).finish();
    Intent intent = new Intent(getContext(), SettingsActivity.class); 
    startActivity(intent); 
  }
}
