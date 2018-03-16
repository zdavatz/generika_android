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

import android.os.Build;

import java.util.HashMap;


public class Constant extends Object {
  // [android]
  // https://developer.android.com/guide/topics/manifest/uses-sdk-element.html#ApiLevels
  public final static int VERSION_27__8_1 = Build.VERSION_CODES.O_MR1;
  public final static int VERSION_26__8_0 = Build.VERSION_CODES.O;
  public final static int VERSION_25__7_1 = Build.VERSION_CODES.N_MR1;  // 7.1, 7.1.1
  public final static int VERSION_24__7_0 = Build.VERSION_CODES.N;
  public final static int VERSION_23__6_0 = Build.VERSION_CODES.M;
  public final static int VERSION_22__5_1 = Build.VERSION_CODES.LOLLIPOP_MR1;
  public final static int VERSION_21__5_0 = Build.VERSION_CODES.LOLLIPOP;
  public final static int VERSION_19__4_4 = Build.VERSION_CODES.KITKAT;
  public final static int VERSION_18__4_3 = Build.VERSION_CODES.JELLY_BEAN_MR2;
  public final static int VERSION_17__4_2 = Build.VERSION_CODES.JELLY_BEAN_MR1; // 4.2, 4.2.2
  public final static int VERSION_16__4_1 = Build.VERSION_CODES.JELLY_BEAN; // 4.1, 4.1.1

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
  public final static String kAutoFocus = "kAutoFocus";
  public final static String kUseFlash  = "kUseFlash";
  public final static String kBarcode = "kBarcode";
  public final static String kFilepath = "kFilepath";

  // -- constents
  public final static int RC_BARCODE_CAPTURE = 9000;
  public final static int RC_HANDLE_GMS = 9001;
  public final static int RC_HANDLE_CAMERA_PERM = 2;


  // [DataFetch]
  // -- intent keys
  public final static String kApiKey = "kApiKey";
  public final static String kBaseUrl = "kBaseUrl";

  // -- http client (HttpsURLConnection)
  public final static String API_URL_BASE =
    "https://ch.oddb.org/de/mobile/api_search/ean/";

  public final static int HUC_READ_TIMEOUT = 9000;
  public final static int HUC_CONNECT_TIMEOUT = 6000;


  // [WebView]
  // -- intent keys
  public final static String kEan = "kEan";
  public final static String kReg = "kReg";
  public final static String kSeq = "kSeq";

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
  public final static HashMap<String, String> initData =
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
