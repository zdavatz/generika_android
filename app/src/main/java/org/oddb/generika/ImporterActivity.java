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

import android.content.Context;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Base64;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.oddb.generika.BaseActivity;
import org.oddb.generika.data.DataManager;
import org.oddb.generika.model.Operator;
import org.oddb.generika.model.Patient;
import org.oddb.generika.model.Product;
import org.oddb.generika.model.Receipt;
import org.oddb.generika.network.ReceiptFileFetcher;
import org.oddb.generika.util.ConnectionStream;
import org.oddb.generika.util.Constant;
import org.oddb.generika.util.StreamReader;


public class ImporterActivity extends BaseActivity
  implements
    ActivityCompat.OnRequestPermissionsResultCallback,
    ReceiptFileFetcher.FetchTaskCallback<ReceiptFileFetcher.FetchResult> {
  private static final String TAG = "ImporterActivity";

  private static final int PERMISSION_REQUEST_CODE = 100;

  private Uri uri;

  // network (headless fragment)
  private boolean fetching = false;
  private ReceiptFileFetcher fetcher;

  private String[] permissions = new String[]{
    Manifest.permission.READ_EXTERNAL_STORAGE,
  };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    this.fetcher = buildReceiptFileFetcher();
    this.uri = getIntent().getData();

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
          int flags = 0;
          HashMap<String, String> extraMap = new HashMap<String, String>();
          openMainView(flags, extraMap);
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
    Log.d(TAG, "(doImport) uri: " + uri);

    int flags = 0;
    HashMap<String, String> extraMap = new HashMap<String, String>();
    try {
      String scheme = uri.getScheme();
      if (scheme.equals("https")) {
        // because network access cannot run in main thread.
        startFetching(uri);
      } else if (scheme.equals("file") || scheme.equals("content")) {
        String content = readFileFromUri(uri);
        Result result = importJSON(content);
        if (result != null) {
          extraMap = result.toExtraMap();
        }
        openMainView(flags, extraMap);
      }
    } catch (IOException e) {
      Log.d(TAG, "(doImport) exception: " + e.getMessage());
      Log.d(TAG, Log.getStackTraceString(e));

      // do nothing
      openMainView(flags, extraMap);
    }
  }

  private ReceiptFileFetcher buildReceiptFileFetcher() {
    FragmentManager fragmentManager = getSupportFragmentManager();
    Fragment fetcher_ = fragmentManager.findFragmentByTag(
      ReceiptFileFetcher.TAG);
    if (fetcher_ == null) {
      fetcher_ = ReceiptFileFetcher.getInstance(fragmentManager, this);
    }
    return (ReceiptFileFetcher)fetcher_;
  }

  public class Result {
    private String hashedKey = null;

    // IMPORT_FAILURE_{INVALID,DUPLICATED,UNSAVED,UNKNOWN}
    // IMPORT_SUCCESS
    private int status;
    private String message;

    public Result() {}

    public String getHashedKey() { return hashedKey; }
    public void setHashedKey(String value) { this.hashedKey = value; }

    public int getStatus() { return status; }
    public void setStatus(int value) { this.status = value; }

    public String getMessage() { return message; }
    public void setMessage(String value) { this.message = value; }

    public HashMap<String, String> toExtraMap() {
      HashMap<String, String> extraMap = new HashMap<String, String>();
      extraMap.put("status", String.valueOf(getStatus()));
      extraMap.put("hashedKey", getHashedKey());
      extraMap.put("message", getMessage());
      return extraMap;
    }
  }

  // Save incominng json file into app after some validations.
  private Result importJSON(String content) {
    Result result = new Result();

    try {
      // .amk (original file)
      Receipt.Amkfile amkfile = new Receipt.Amkfile(context, uri);
      String filename = amkfile.getOriginalName();
      Log.d(TAG, "(importJSON) filename: " + filename);

      JSONObject json = new JSONObject(content);
      String hashedKey = null;
      if (json != null) {
        hashedKey = json.getString("prescription_hash");
      }
      if (json == null ||
          hashedKey == null || hashedKey.equals("")) {
        // import error (required key/content is missing/invalid)
        result.setMessage(String.format(context.getString(
          R.string.message_import_failure_invalid), filename));
        result.setStatus(Constant.IMPORT_FAILURE_INVALID);
        return result;
      }

      DataManager dataManager = new DataManager(Constant.SOURCE_TYPE_AMKJSON);
      Receipt importedReceipt = dataManager.getReceiptByHashedKey(hashedKey);
      if (importedReceipt != null) {
        // duplicated
        result.setMessage(String.format(context.getString(
          R.string.message_import_failure_duplicated), filename));
        result.setStatus(Constant.IMPORT_FAILURE_DUPLICATED);
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

      // save original .amk file
      amkfile.setContent(content);
      String filepath = amkfile.getPath();
      Log.d(TAG, "(importJSON) .amk local filepath: " + filepath);

      boolean saved = amkfile.save();
      Log.d(TAG, "(importJSON) saved: " + saved);
      if (!saved) {
        // save error
        result.setMessage(String.format(context.getString(
          R.string.message_import_failure_unsaved), filename));
        result.setStatus(Constant.IMPORT_FAILURE_UNSAVED);
        return result;
      } else {
        result.setHashedKey(hashedKey);
      }

      Receipt receipt = new Receipt();
      receipt.setHashedKey(hashedKey);
      receipt.setPlaceDate(json.getString("place_date"));
      receipt.setFilepath(filepath);
      receipt.setFilename(filename);

      dataManager.addReceipt(receipt, operator, patient, medications);
      result.setMessage(String.format(
        context.getString(R.string.message_import_success), filename));
      result.setStatus(Constant.IMPORT_SUCCESS);
    } catch (Exception e) {
      Log.d(TAG, "(importJSON) exception: " + e.getMessage());
      Log.d(TAG, "(importJSON) " + Log.getStackTraceString(e));
      result.setMessage(context.getString(
        R.string.message_import_failure_unknown));
      result.setStatus(Constant.IMPORT_FAILURE_UNKNOWN);
    }
    return result;
  }

  private String decodeContent(String raw) {
    String content;

    try {
      // base64
      byte[] data = Base64.decode(raw, Base64.DEFAULT);
      content = new String(data, "UTF-8");
    } catch (UnsupportedEncodingException | IllegalArgumentException e) {
      Log.d(TAG, "(decodeContent) e: " + e.getMessage());
      content = null;
    }
    return content;
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

  private void openMainView(
    int additionalflags, HashMap<String, String> extraMap) {
    int flags =
      Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK |
      Intent.FLAG_ACTIVITY_TASK_ON_HOME | Intent.FLAG_ACTIVITY_NO_ANIMATION |
      additionalflags;

    Intent intent = new Intent(this, MainActivity.class);
    intent.setFlags(intent.getFlags() | flags);

    for (Map.Entry<String, String> entry: extraMap.entrySet()) {
      intent.putExtra(entry.getKey(), entry.getValue());
    }
    startActivity(intent);
    finish();
  }

  // -- ReceiptFileFetcher.FetchCallback

  private void startFetching(Uri uri_) {
    if (!fetching && fetcher != null) {
      fetcher.invokeFetch(uri);
      this.fetching = true;
    }
  }

  @Override
  public void onProgressUpdate(Integer...progress) {
    // pass
  }

  @Override
  public void updateFromFetch(ReceiptFileFetcher.FetchResult result) {
    String content = decodeContent(result.content);
    Log.d(TAG, "(updateFromFetch) content: " + content);

    HashMap<String, String> extraMap = new HashMap<String, String>();
    Result importResult = importJSON(content);
    if (importResult != null) {
      extraMap = importResult.toExtraMap();
    }
    int flags = Intent.FLAG_FROM_BACKGROUND;
    openMainView(flags, extraMap);
  }

  @Override
  public void finishFetching() {
    this.fetching = false;
    if (fetcher != null) {
      fetcher.cancelFetch();
    }
  }
}
