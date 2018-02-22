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
package org.oddb.generika.app;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;

import java.util.Locale;


public class BaseActivity extends AppCompatActivity {

  private final static String kAppLocale = "kAppLocale";

  public Locale currentLocale;

  @Override
  protected void onStart() {
    super.onStart();

    currentLocale = getResources().getConfiguration().locale;
  }

  @Override
  protected void onRestart() {
    super.onRestart();
    Locale locale = getLocale(this);

    if (!locale.equals(currentLocale)) {
      currentLocale = locale;
      recreate();
    }
  }

  private static Locale getLocale(Context context) {
    Locale locale;

    SharedPreferences sharedPreferences = PreferenceManager
      .getDefaultSharedPreferences(context);
    String language = sharedPreferences.getString(kAppLocale, "de");
    switch (language) {
      case "de": case "fr": case "en":
        locale = new Locale(language);
        break;
      default:
        locale = new Locale("de");
        break;
    }
    return locale;
  }
}
