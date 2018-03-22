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

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import org.oddb.generika.BaseActivity;


public class ImporterActivity extends BaseActivity {
  private static final String TAG = "ImporterActivity";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // e.g.
    //
    // * https://dl.grauwoelfchen.net/data/RZ_2018-01-13T183946.amk
    // * file:///storage/emulated/0/Download/RZ_2017-12-20T141403.amk
    Uri uri = getIntent().getData();
    Log.d(TAG, "(onCreate) uri: " + uri);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
  }
}
