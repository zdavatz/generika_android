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

import android.support.v7.app.AppCompatActivity;

import java.util.Locale;

import org.oddb.generika.util.AppLocale;


public class BaseActivity extends AppCompatActivity {

  public Locale currentLocale;

  @Override
  protected void onStart() {
    super.onStart();

    currentLocale = getResources().getConfiguration().locale;
  }

  @Override
  protected void onRestart() {
    super.onRestart();
    Locale locale = AppLocale.getLocale(this);

    if (!locale.equals(currentLocale)) {
      currentLocale = locale;
      recreate();
    }
  }
}
