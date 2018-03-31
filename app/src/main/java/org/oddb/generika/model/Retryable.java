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
package org.oddb.generika.model;

import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.icu.util.TimeZone;
import android.util.Log;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.RealmResults;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;


public interface Retryable {

  public interface WithRetry {
    void execute(final int currentCount);
  }

  /**
   * Utility provides retry block for primary key collision etc.
   *
   * Retryable.withRetry(2, new Retryable.WithRetry() {
   *   @Override
   *   public void execute(final int currentCount) {
   *     // do something
   *   }
   * });
   */
  public static void withRetry(final int limit, WithRetry f) {
    for (int c = 0;; c++) {
      try {
        final int j = c;
        f.execute(j);
        break;
      } catch (Exception e) {
        if (c < limit) {
          continue;
        } else {
          e.printStackTrace();
          throw e;
        }
      }
    }
  }
}
