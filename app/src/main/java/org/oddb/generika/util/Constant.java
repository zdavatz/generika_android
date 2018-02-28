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
  // [Settings]
  // -- preference keys
  public final static String kSearchType = "kSearchType";
  public final static String kSearchLang = "kSearchLang";
  public final static String kAppUseSystemLocale = "kAppUseSystemLocale";
  public final static String kAppLocale = "kAppLocale";
  // TODO: enable cloud storage support
  //private final static String kRecordSync = "kRecordSync";
  // -- preference values (see also user_settings.xml)
  public final static String TYPE_PV = "pv";
  public final static String TYPE_PI = "pi";
  public final static String TYPE_FI = "fi";

  public final static String LANG_DE = "de";
  public final static String LANG_FR = "fr";
  public final static String LANG_EN = "en";


  // [BarcodeCapture]
  // -- intent keys
  public static final String kAutoFocus = "kAutoFocus";
  public static final String kUseFlash  = "kUseFlash";
  public static final String kBarcode = "kBarcode";

  // -- constents
  public static final int RC_BARCODE_CAPTURE = 9000;
  public static final int RC_HANDLE_GMS = 9001;
  public static final int RC_HANDLE_CAMERA_PERM = 2;


  // [DataFetch]
  // -- intent keys
  public static final String kApiKey = "kApiKey";
  public static final String kBaseUrl = "kBaseUrl";

  // -- http client (HttpsURLConnection)
  public final static String API_URL_BASE =
    "https://ch.oddb.org/de/mobile/api_search/ean/";

  public static int HUC_READ_TIMEOUT = 3000;
  public static int HUC_CONNECT_TIMEOUT = 3000;


  // [WebView]
  // -- intent keys
  public static final String kEan = "kEan";
  public static final String kReg = "kReg";
  public static final String kSeq = "kSeq";

  // -- web view client (WebViewClient)
  public final static String WEB_USER_AGENT = "org.oddb.generikacc";

  public final static String WEB_URL_HOST = "ch.oddb.org";
  public final static String WEB_URL_PATH_COMPARE =
    "%s/mobile/compare/ean13/%s";
  public final static String WEB_URL_PATH_PATINFO =
    "%s/mobile/patinfo/reg/%s/seq/%s";
  public final static String WEB_URL_PATH_FACHINFO =
    "%s/mobile/fachinfo/reg/%s";


  // [Product Item ListView]
  // -- place holder values
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
