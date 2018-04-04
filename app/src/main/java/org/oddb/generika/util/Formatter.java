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

import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.icu.util.TimeZone;


public class Formatter extends Object {
  // utility to return formatted local date string fields for display
  // (scannedAt/importedAt, and expiresAt)
  public static String formatAsLocalDate(String date, String formatString) {
    String datetimeString = "";
    if (date == null || date.length() == 0 ||
        formatString == null || formatString.length() == 0) {
      return datetimeString;
    }
    try {
      // NOTE:
      // TimeZote.getDefaultZone() and Calendar.getInstance().getTimeZone()
      // both will return wrong timezone in Android 6.1 (>= 7.0 OK) :'(

      // from UTC
      SimpleDateFormat inFormatter = new SimpleDateFormat("yyyyMMddHHmmss");
      inFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
      // to local time
      SimpleDateFormat outFormatter = new SimpleDateFormat(formatString);
      outFormatter.setTimeZone(Calendar.getInstance().getTimeZone());

      datetimeString = outFormatter.format(inFormatter.parse(date));
    } catch (Exception e) {
      // nothing to display
      datetimeString = "";
    }
    return datetimeString;
  }
}
