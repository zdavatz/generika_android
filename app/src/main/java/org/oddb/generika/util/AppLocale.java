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
package org.oddb.generika.util;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.support.v7.preference.PreferenceManager;
import android.util.DisplayMetrics;

import java.util.Locale;

import org.oddb.generika.util.Constant;


public class AppLocale extends Object {

  public static Locale getLocale(Context context) {
    Locale locale;

    SharedPreferences sharedPreferences = PreferenceManager
      .getDefaultSharedPreferences(context);
    String language = sharedPreferences.getString(
      Constant.kAppLocale, Constant.LANG_DE);
    switch (language) {
      case Constant.LANG_DE: case Constant.LANG_FR: case Constant.LANG_EN:
        locale = new Locale(language);
        break;
      default:
        locale = new Locale(Constant.LANG_DE);
        break;
    }
    return locale;
  }

  // for Application (at boot)
  public static void setLocale(Application application) {
    final Resources resources = application.getResources();
    final Configuration configuration = resources.getConfiguration();
    final Locale locale = getLocale(application);

    if (!configuration.locale.equals(locale)) {
      Locale.setDefault(locale);
      configuration.setLocale(locale);
      resources.updateConfiguration(configuration, null);
    }

  }

  // for Activity
  public static void setLocale(Context context, Locale newLocale) {
    final Resources resources = context.getResources();
    final DisplayMetrics metrics = resources.getDisplayMetrics();
    final Configuration configuration = resources.getConfiguration();

    Locale.setDefault(newLocale);
    configuration.setLocale(newLocale);
    resources.updateConfiguration(configuration, metrics);
  }
}
