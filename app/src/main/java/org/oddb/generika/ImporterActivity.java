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
import org.oddb.generika.data.DataManager;
import org.oddb.generika.model.Receipt;
import org.oddb.generika.model.Operator;
import org.oddb.generika.model.Patient;
import org.oddb.generika.model.Product;
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
    // * https://<domain>/data/<file>.amk
    // * file:///storage/emulated/0/Download/<file>.amk
    // * content://com.android.chrome.FileProvider/downloads/<file>.amk
    Uri uri = getIntent().getData();
    String url = uri.toString().toLowerCase();

    int flags = 0;
    HashMap<String, String> extras = new HashMap<String, String>();
    try {
      Log.d(TAG, "(doImport) uri: " + uri.toString());
      if (url.startsWith("https:")) {
        // because network access cannot run in main thread.
        FetchTask fetchTask = new FetchTask();
        fetchTask.execute(uri);
      } else if (url.startsWith("file:") || url.startsWith("content:")) {
        String content = readFileFromUri(uri);
        if (content != null && !content.equals("")) {
          Result result = importJSON(content);
          extras.put("filename", result.getFilename());
          extras.put("message", result.getMessage());
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

  public class Result {
    private String filename = null;
    private String message = null;

    public Result() {}

    public String getFilename() { return filename; }
    public void setFilename(String filename_) { this.filename = filename_; }

    public String getMessage() { return message; }
    public void setMessage(String message_) { this.message = message_; }
  }

  // save incominng json file into app after some validations
  private Result importJSON(String content) {
    Log.d(TAG, "(importJSON) content: " + content);

    Result result = new Result();
    try {
      JSONObject json = new JSONObject(content);

      String hashedKey = json.getString("prescription_hash");
      if (hashedKey == null || hashedKey.equals("")) {
        // import error (required)
        result.setMessage("TODO");
        return result;
      }

      DataManager dataManager = new DataManager(Constant.SOURCE_TYPE_AMKJSON);
      Receipt importedReceipt = dataManager.getReceiptByHashedKey(hashedKey);
      if (importedReceipt != null) {
        // duplicated
        result.setMessage("TODO");
        return result;
      }

      // operator
      Operator operator = Operator.newInstanceFromJSON(
        json.getJSONObject("operator"));

      // patient
      Patient patient = Patient.newInstanceFromJSON(
        json.getJSONObject("patient"));

      // medications
      JSONArray medicationArray = json.getJSONArray("medications");
      int count = medicationArray.length();
      Product[] medications = new Product[count];
      for (int i = 0; i < count; i++) {
        JSONObject medication = medicationArray.getJSONObject(i);
        medications[i] = Product.newInstanceFromJSON(medication);
      }

      // .amk
      Receipt.Amkfile amkfile = new Receipt.Amkfile();
      amkfile.setPrescriptionHash(hashedKey);
      amkfile.setContent(content);

      // TODO: save file in background
      String filepath = ".amk";
      amkfile.setFilepath(filepath);
      Log.d(TAG, "(importJSON) .amk filepath: " + filepath);

      if (filepath == null || filepath.equals("")) {
        // save error
        result.setMessage("TODO");
        return result;
      }

      Receipt receipt = new Receipt();
      receipt.setHashedKey(hashedKey);
      receipt.setPlaceDate(json.getString("place_date"));
      receipt.setFilepath(filepath);

      dataManager.addReceipt(receipt, operator, patient, medications);
      // TODO
      result.setFilename(".amk");
    } catch (Exception e) {
      // TODO
      Log.d(TAG, "(importJSON) exception: " + e.getMessage());
      e.printStackTrace();
      result.setMessage(e.getMessage());
    }
    return result;
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
    protected void onPostExecute(String content) {
      Result result = null;
      if (content != null && !content.equals("")) {
        result = importJSON(content);
      }
      HashMap<String, String> extras = new HashMap<String, String>();
      int flags = Intent.FLAG_FROM_BACKGROUND;
      if (result != null) {
        extras.put("filename", result.getFilename());
        extras.put("message", result.getMessage());
      }
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
