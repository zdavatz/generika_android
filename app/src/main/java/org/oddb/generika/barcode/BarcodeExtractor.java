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
package org.oddb.generika.barcode;

import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.oddb.generika.util.Constant;


// An extractor utility for detected barcode raw value.
//
// NOTE:
//
// # Format
//
// - FNC1 is placed a top of the data
// - <FNC1> is `]d2` (ISO/IEC 15424)
//
// <FUNC1>(01)GTIN(17)EXPIRY(10)BATCH/LOT
//
// e.g.
//   01034531200000111719112510ABCD1234
//   (01)03453120000011(17)191125(10)ABCD1234
//
// e.g.
//   01095011010209171719050810ABCD12342110
//   (01)09501101020917(17)190508(10)ABCD1234(21)10
//
// # GS1 Application Identifiers (AI) and Formats
//
// There are >= 130 AIs. Following codes are mainly used in medi packs.
//
// 01: GTIN                (N2 + N14)     fixed -> 16
// 11: Production date     (N6) -> YYMMDD fixed -> 8
// 15: Best before date    (N6) -> YYMMDD fixed -> 8
// 17: Expiry date         (N6)           fixed -> 8
// 10: Batch or lot number (N2 + Nx..20)
// 21: Special number      (N2 + Nx..20)
// 30: Amount              (N2 + Nx..8)
//
// # AI Order
//
// Mostly, the order of AIs will be following formats. But it seems
// that it's just a guide.
//
// ```
// FUNC1 + GTIN + Fixed length infor +
//   Variable length info + FNC1 + Variable length info ...
// ```
//
// # References
//
// - http://www.gs1.se/en/our-standards/Capture/gs1-datamatrix/
// - https://www.gs1.org/barcodes/2d
// - http://xchange.gs1.org/sites/faq/Pages/ \
//   what-is-the-function-1-character-fnc1-and-what-is-it-used-for.aspx
//
public class BarcodeExtractor extends Object {
  private static final String TAG = "BarcodeExtractor";

  private static int minLengthOfAI = 2;
  private static int maxLengthOfAI = 4;

  // https://en.wikipedia.org/wiki/List_of_Unicode_characters
  private static List<Integer> numericCodes = Arrays.asList(
    48, 49, 50, 51, 52, 53, 54, 55, 56, 57); // 0-9
 
  public final static HashMap<String, HashMap<String, Integer>> AI_DATA =
    new HashMap<String, HashMap<String, Integer>>() {{
      // GTIN (01)
      put(Constant.GS1_DM_AI_GTIN, new HashMap<String, Integer>() {{
          put("codeLength", 2); put("maxLength", 14); }});
      // Batch or lot number (10)
      put(Constant.GS1_DM_AI_BATCH_LOT, new HashMap<String, Integer>() {{
        put("codeLength", 2); put("maxLength", 20); }});
      // Production date (11)
      put(Constant.GS1_DM_AI_PROD_DATE, new HashMap<String, Integer>() {{
        put("codeLength", 2); put("maxLength",  6); }});
      // Best before date (15)
      put(Constant.GS1_DM_AI_BEFORE_DATE, new HashMap<String, Integer>() {{
        put("codeLength", 2); put("maxLength",  6); }});
      // Expiry date (17)
      put(Constant.GS1_DM_AI_EXPIRY_DATE, new HashMap<String, Integer>() {{
          put("codeLength", 2); put("maxLength",  6); }});
      // Special number (21)
      put(Constant.GS1_DM_AI_SPECIAL_NUMBER, new HashMap<String, Integer>() {{
        put("codeLength", 2); put("maxLength", 20); }});
      // Amount (30)
      put(Constant.GS1_DM_AI_AMOUNT, new HashMap<String, Integer>() {{
        put("codeLength", 2); put("maxLength",  6); }});
    }};

  private static class AI {
    private String code;
    private int codeLength;
    private int maxLength;

    private AI(String codeValue, HashMap<String, Integer> data) {
      if (data == null) {
        return;
      }
      this.code = codeValue;
      this.codeLength = data.get("codeLength");
      this.maxLength = data.get("maxLength");
      return;
    }
  }

  public static HashMap<String, String> extract(String rawValue) {
    HashMap<String, String> result = new HashMap<String, String>();

    if (rawValue == null || rawValue.equals("")) {
      return result;
    }

    Log.d(TAG, "(extract) rawValue: " + rawValue);

    // detect FNC1
    char fnc1 = rawValue.charAt(0);
    Log.d(TAG, "(extract) code[0]: " + (int)fnc1);

    String[] values;
    if (numericCodes.contains((int)fnc1)) {
      // FNC1 is not suitable for separation of code
      values = new String[]{rawValue};
    } else {
      // omit first FNC1 (GS1 DataMatrix has always FNC1 as first char)
      String input = rawValue.substring(1);
      // FNC1 might be appeared multiple times in code
      values = input.split(fnc1 + "");
    }

    try {
      for (int i = 0; i < values.length; i++) {
        String value = values[i];
        Log.d(TAG, String.format("(extract) i: %d, value: %s", i, value));
        int index = 0;

        while (index < value.length()) {
          AI ai = findAI(value, index);
          if (ai == null) {
            throw new Exception("AI Not Found");
          } else {
            index += ai.codeLength;
            if (result.get(ai.code) != null) {
              continue; // don't overwrite
            }
            String text = getValue(ai, value, index);
            result.put(ai.code, text);
            index += text.length();
          }
        }
      }
    } catch (Exception e) {
      Log.d(TAG, "(findAI) exception: " + e.getMessage());
    }
    Log.d(TAG, "(findAI) result: " + result);
    return result;
  }

  private static AI findAI(String value, int index) {
    AI ai = null;

    for (int i = minLengthOfAI; i <= maxLengthOfAI; i++) {
      String code = value.substring(index, index + i);
      HashMap<String, Integer> data = AI_DATA.get(code);
      if (data != null) {
        ai = new AI(code, data);
        break;
      }
    }
    return ai;
  }

  private static String getValue(AI ai, String value, int index) {
    int length = Math.min(ai.maxLength, value.length() - index);
    return value.substring(index, index + length);
  }
}
