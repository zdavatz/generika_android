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

  // -- constents (Result Code)
  public final static int RC_BARCODE_CAPTURE = 9000;
  public final static int RC_HANDLE_GMS = 9001;
  public final static int RC_HANDLE_CAMERA_PERM = 2;

  public final static String GS1_DM_AI_GTIN = "01";
  public final static String GS1_DM_AI_BATCH_LOT = "10";
  public final static String GS1_DM_AI_PROD_DATE = "11";
  public final static String GS1_DM_AI_BEFORE_DATE = "15";
  public final static String GS1_DM_AI_EXPIRY_DATE = "17";
  public final static String GS1_DM_AI_SPECIAL_NUMBER = "21";
  public final static String GS1_DM_AI_AMOUNT = "30";

  // [Network]
  public final static String URL_HOST = "ch.oddb.org";
  public final static String URL_FLAVOR = "mobile";

  // [Network/DataFetch]
  // -- intent keys
  public final static String kApiKey = "kApiKey";
  public final static String kBaseUrl = "kBaseUrl";

  // -- api client
  public final static String API_URL_PATH =
    "https://" + URL_HOST + "/de/" + URL_FLAVOR + "/api_search/ean/";
  // (HttpsURLConnection)
  public final static int HUC_READ_TIMEOUT = 9000;
  public final static int HUC_CONNECT_TIMEOUT = 6000;
  public final static String HUC_USER_AGENT = "org.oddb.generikacc";

  // [Network/WebView]
  // -- intent keys
  public final static String kEan = "kEan";
  public final static String kReg = "kReg";
  public final static String kSeq = "kSeq";

  public final static String kEans = "kEans"; // for interaction link

  public final static int PAGE_ACTION_TOP = 1;
  public final static int PAGE_ACTION_SEARCH = 2;
  public final static int PAGE_ACTION_INTERACTIONS = 3;

  // -- web view client (WebViewClient)
  public final static String WEB_USER_AGENT = "org.oddb.generikacc";
  public final static String WEB_URL_HOST = URL_HOST;
  public final static String WEB_URL_PATH_COMPARE =
    "%s/" + URL_FLAVOR + "/compare/ean13/%s";
  public final static String WEB_URL_PATH_PATINFO =
    "%s/" + URL_FLAVOR + "/patinfo/reg/%s/seq/%s";
  public final static String WEB_URL_PATH_FACHINFO =
    "%s/" + URL_FLAVOR + "/fachinfo/reg/%s";
  public final static String WEB_URL_PATH_INTERACTION =
    "%s/" + URL_FLAVOR + "/home_interactions/%s";

  // [Product/Receipt ListView]
  // -- swipe on row
  public final static int SWIPE_DURATION_MIN = 600;
  public final static int SWIPE_DURATION_MAX = 1024;

  // -- place holder values
  // TODO: use translation file
  public final static HashMap<String, String> INIT_DATA =
    new HashMap<String, String>() {{
      put("ean", "GTIN (EAN-13)");
      put("name", "NAME");
      put("size", "SIZE");
      put("datetime", "SCANNED AT");
      put("price", "PRICE (CHF)");
      put("deduction", "DEDUCTION");
      put("category", "CATEGORY");
      put("expiresAt", "EXPIRES AT");
    }};

  // [Receipt Import]
  // status
  public final static int IMPORT_FAILURE_INVALID = 100;
  public final static int IMPORT_FAILURE_DUPLICATED = 110;
  public final static int IMPORT_FAILURE_UNSAVED = 120;
  public final static int IMPORT_FAILURE_UNKNOWN = 130;
  public final static int IMPORT_SUCCESS = 200;

  public final static int RC_FILE_PROVIDER = 8000;

  // [Receipt]
  // -- intent keys
  public final static String kHashedKey = "kHashedKey";

  // [Shared]
  // -- sourceType (value)
  public final static String SOURCE_TYPE_BARCODE = "barcode"; // product
  public final static String SOURCE_TYPE_AMKJSON = "amkjson"; // receipt
}
