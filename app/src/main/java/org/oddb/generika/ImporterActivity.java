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

import android.content.pm.PackageManager;
import android.content.Intent;
import android.Manifest;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.oddb.generika.BaseActivity;
import org.oddb.generika.util.Constant;
import org.oddb.generika.util.ConnectionStream;
import org.oddb.generika.util.StreamReader;


public class ImporterActivity extends BaseActivity
  implements ActivityCompat.OnRequestPermissionsResultCallback {
  private static final String TAG = "ImporterActivity";

  private static final int PERMISSION_REQUEST_CODE = 100;

  private String[] permissions = new String[]{
    Manifest.permission.READ_EXTERNAL_STORAGE,
  };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (Build.VERSION.SDK_INT >= Constant.VERSION_23__6_0) {
      if (checkPermissions()) {
        doImport();
      }
    } else {
      doImport();
    }
  }

  @Override
  public void onRequestPermissionsResult(
    int requestCode, String permissions[], int[] grantResults) {
    switch (requestCode) {
      case PERMISSION_REQUEST_CODE:
        if (grantResults.length > 0 &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          doImport();
        } else {
          // cancel because it is denied
          // TODO: close or just start MainActivity
        }
        break;
    }
    return;
  }

  private boolean checkPermissions() {
    int result;
    List<String> notAllowed = new ArrayList<>();
    for (String permission: permissions) {
      result = ContextCompat.checkSelfPermission(this, permission);
      if (result != PackageManager.PERMISSION_GRANTED) {
        notAllowed.add(permission);
      }
    }
    if (!notAllowed.isEmpty()) {
      ActivityCompat.requestPermissions(this, notAllowed.toArray(
        new String[notAllowed.size()]), PERMISSION_REQUEST_CODE);
      return false;
    }
    return true;
  }

  private void doImport() {
    // e.g.
    // * https://dl.grauwoelfchen.net/data/RZ_2018-01-13T183946.amk
    // * file:///storage/emulated/0/Download/RZ_2017-12-20T141403.amk
    // * content://com.android.chrome.FileProvider/downloads/RZ_2018-01-13T183946.amk
    Uri uri = getIntent().getData();
    String url = uri.toString().toLowerCase();

    String result = null;
    int flags = 0;
    HashMap<String, String> extras = new HashMap<String, String>();
    try {
      Log.d(TAG, "(doImport) uri: " + uri.toString());
      if (url.startsWith("https:")) {
        // because network access cannot run in main thread.
        FetchTask fetchTask = new FetchTask();
        fetchTask.execute(uri);
      } else if (url.startsWith("file:") || url.startsWith("content:")) {
        result = readFileFromUri(uri);
        if (result != null) {
          // TODO: prepare extras to intent for alert
          importJSON(result);
        }

        openMainView(flags, extras);
      }
    } catch (IOException e) {
      Log.d(TAG, "(doImport) exception: " + e.getMessage());
      e.printStackTrace();
    }
    // do nothing
    openMainView(flags, extras);
  }

  private void openMainView(
    int additionalflags, HashMap<String, String> extras) {
    int flags =
      Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK |
      Intent.FLAG_ACTIVITY_TASK_ON_HOME | Intent.FLAG_ACTIVITY_NO_ANIMATION |
      additionalflags;

    Intent intent = new Intent(this, MainActivity.class);
    intent.setFlags(intent.getFlags() | flags);

    for (Map.Entry<String, String> entry: extras.entrySet()) {
      intent.putExtra(entry.getKey(), entry.getValue());
    }
    startActivity(intent);
    finish();
  }

  private void importJSON(String content) {
    Log.d(TAG, "(importJSON) content: " + content);
    try {
      HashMap<String, String> rMap = new HashMap<String, String>();
      JSONObject json = new JSONObject(content);

      rMap.put("hashedKey", json.getString("prescription_hash"));
      rMap.put("placeDate", json.getString("place_date"));

      // TODO: key mapping in model
      // operator (Receipt.Operator)
      HashMap<String, String> oMap = new HashMap<String, String>();
      JSONObject operator = json.getJSONObject("operator");
      oMap.put("givenName", operator.getString("given_name"));
      oMap.put("familyName", operator.getString("family_name"));
      oMap.put("title", operator.getString("title"));
      oMap.put("email", operator.getString("email_address"));
      oMap.put("phone", operator.getString("phone_number"));
      oMap.put("address", operator.getString("postal_address"));
      oMap.put("city", operator.getString("family_name"));
      oMap.put("zipcode", operator.getString("zip_code"));
      oMap.put("country", operator.getString("country"));
      oMap.put("signature", operator.getString("signature"));

      // patient (Receipt.Patient)
      HashMap<String, String> pMap = new HashMap<String, String>();
      JSONObject patient = json.getJSONObject("patient");
      pMap.put("identifier", patient.getString("patient_id"));
      pMap.put("givenName", patient.getString("given_name"));
      pMap.put("familyName", patient.getString("family_name"));
      pMap.put("birthDate", patient.getString("birth_date"));
      pMap.put("gender", patient.getString("gender"));
      pMap.put("height", patient.getString("height_cm"));
      pMap.put("weight", patient.getString("weight_kg"));
      pMap.put("email", patient.getString("email_address"));
      pMap.put("phone", patient.getString("phone_number"));
      pMap.put("address", patient.getString("postal_address"));
      pMap.put("city", patient.getString("city"));
      pMap.put("zipcode", patient.getString("zip_code"));
      pMap.put("country", patient.getString("country"));

      // medications (Product)
      JSONArray medications = json.getJSONArray("medications");
      for (int i = 0; i < medications.length(); i++) {
        HashMap<String, String> mMap = new HashMap<String, String>();
        JSONObject medication = medications.getJSONObject(i);
        mMap.put("ean", medication.getString("eancode"));
        mMap.put("reg", medication.getString("regnrs"));
        mMap.put("pack", medication.getString("package"));
        mMap.put("name", medication.getString("product_name"));
        mMap.put("atc", medication.getString("atccode"));
        mMap.put("owner", medication.getString("owner"));
        mMap.put("comment", medication.getString("comment"));

      }
    } catch (Exception e) {
      Log.d(TAG, "(importJSON) exception: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private String decodeContent(String raw) {
    String content;

    byte[] data = Base64.decode(raw, Base64.DEFAULT);
    try {
      // base64
      content = new String(data, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      Log.d(TAG, "(decodeContent) e: " + e.getMessage());
      e.printStackTrace();
      content = null;
    }
    return content;
  }

  private class FetchTask extends
    AsyncTask<Uri, Integer, String> {

    @Override
    protected String doInBackground(Uri... uris) {
      String result = null;

      if (!isCancelled() && uris != null && uris.length > 0) {
        Uri uri = uris[0];
        try {
          result = fetchFileFromUri(uri);
        } catch (IOException e) {
          Log.d(TAG, "(doInBackground) e: " + e.getMessage());
          e.printStackTrace();
        }
      }
      return result;
    }

    @Override
    protected void onPostExecute(String result) {
      if (result != null) {
        // TODO: prepare extras to intent for alert
        importJSON(result);
      }
      int flags = Intent.FLAG_FROM_BACKGROUND;
      HashMap<String, String> extras = new HashMap<String, String>();
      openMainView(flags, extras);
    }
  }

  /// https
  private String fetchFileFromUri(Uri uri) throws IOException {
    Log.d(TAG, "(fetchFileFromUri) uri: " + uri);
    String raw;

    ConnectionStream stream = null;
    try {
      stream = new ConnectionStream();
      stream.setSource(uri.toString());

      StreamReader reader = new StreamReader();
      reader.setMaxReadLength(1200);
      reader.setStream(stream.derive());
      raw = reader.read();
    } finally {
      if (stream != null) {
        stream.close();
      }
    }
    return decodeContent(raw);
  }

  /// file
  private String readFileFromUri(Uri uri) throws FileNotFoundException {
    Log.d(TAG, "(readFileFromUri) uri: " + uri);

    StreamReader reader = new StreamReader();
    reader.setStream(getContentResolver().openInputStream(uri));
    String raw = reader.read();
    return decodeContent(raw);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
  }
}
