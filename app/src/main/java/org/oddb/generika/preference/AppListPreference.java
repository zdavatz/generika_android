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
package org.oddb.generika.preference;

import android.content.Context;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.ListPreference;
import android.util.AttributeSet;

import java.lang.CharSequence;
import java.lang.Object;


public class AppListPreference extends ListPreference {

  private final static String TAG = AppListPreference.class.getName();

  public AppListPreference(Context context, AttributeSet attrs) {
    super(context, attrs);

    initOnChangeListener();
  }

  public AppListPreference(Context context) {
    super(context);

    initOnChangeListener();
  }

  private void initOnChangeListener() {
    // set current value as summary of preference
    setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(
        Preference preference, Object _newValue) {

        preference.setSummary(getEntry());
        return true;
      }
    });
  }

  @Override
  public CharSequence getSummary() {
    return super.getEntry();
  }
}
