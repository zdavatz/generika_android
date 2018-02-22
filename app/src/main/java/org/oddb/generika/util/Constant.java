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

import java.util.HashMap;


public class Constant extends Object {
  public final static String API_URL_BASE =
    "https://ch.oddb.org/de/mobile/api_search/ean/";

  // settings keys
  public final static String kSearchType = "kSearchType";
  public final static String kSearchLang = "kSearchLang";
  public final static String kAppUseSystemLocale = "kAppUseSystemLocale";
  public final static String kAppLocale = "kAppLocale";
  // TODO: enable cloud storage support
  //private final static String kRecordSync = "kRecordSync";

  // as place holder values
  public static final HashMap<String, String> initData =
    new HashMap<String, String>() {{
      put("ean", "EAN 13");
      put("name", "Name");
      put("size", "Size");
      put("datetime", "Scanned At");
      put("price", "Price (CHF)");
      put("deduction", "Deduction (%)");
      put("category", "Category");
    }};
}
