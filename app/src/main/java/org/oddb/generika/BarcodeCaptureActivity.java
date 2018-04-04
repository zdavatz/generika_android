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
/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.oddb.generika;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.CommonStatusCodes;

import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.oddb.generika.BaseActivity;
import org.oddb.generika.barcode.BarcodeGraphic;
import org.oddb.generika.barcode.BarcodeGraphicTracker;
import org.oddb.generika.barcode.BarcodeImageCapturingDetector;
import org.oddb.generika.barcode.BarcodeTrackerFactory;
import org.oddb.generika.model.Product;
import org.oddb.generika.ui.reader.CameraSource;
import org.oddb.generika.ui.reader.CameraSourcePreview;
import org.oddb.generika.ui.reader.GraphicOverlay;
import org.oddb.generika.util.Constant;


public final class BarcodeCaptureActivity extends BaseActivity implements
  BarcodeGraphicTracker.BarcodeUpdateListener,
  BarcodeImageCapturingDetector.BarcodeImageCaptureListener {
  private static final String TAG = "BarcodeCaptureActivity";

  private CameraSource cameraSource;
  private CameraSourcePreview preview;
  private GraphicOverlay<BarcodeGraphic> graphicOverlay;

  private final Object captureLock = new Object();
  private boolean captured = false;
  private String filepath;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.barcode_capture);

    this.preview = (CameraSourcePreview)findViewById(R.id.preview);
    this.graphicOverlay = (GraphicOverlay<BarcodeGraphic>)findViewById(
      R.id.graphicOverlay);

    // Options: from the main intent
    boolean autoFocus = getIntent().getBooleanExtra(
      Constant.kAutoFocus, false);
    boolean useFlash = getIntent().getBooleanExtra(
      Constant.kUseFlash, false);

    // Check for the camera permission
    int rc = ActivityCompat.checkSelfPermission(
      this, Manifest.permission.CAMERA);
    if (rc == PackageManager.PERMISSION_GRANTED) {
      createCameraSource(autoFocus, useFlash);
    } else {
      requestCameraPermission();
    }

    String description = context.getString(
      R.string.barcode_reader_hover_text);
    Snackbar.make(graphicOverlay, description, Snackbar.LENGTH_LONG).show();
  }

  private void requestCameraPermission() {
    Log.w(TAG, "(requestCameraPermission) Camera permission is not granted.");

    final String[] permissions = new String[]{Manifest.permission.CAMERA};

    if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
      Manifest.permission.CAMERA)) {
      ActivityCompat.requestPermissions(
        this, permissions, Constant.RC_HANDLE_CAMERA_PERM);
      return;
    }

    final Activity thisActivity = this;

    View.OnClickListener listener = new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        ActivityCompat.requestPermissions(thisActivity, permissions,
          Constant.RC_HANDLE_CAMERA_PERM);
      }
    };

    findViewById(R.id.topLayout).setOnClickListener(listener);
    Snackbar.make(
      graphicOverlay,
      R.string.permission_camera_rationale, Snackbar.LENGTH_INDEFINITE)
        .setAction(R.string.ok, listener)
        .show();
  }

  @Override
  public boolean onTouchEvent(MotionEvent e) {
    return super.onTouchEvent(e);
  }

  @SuppressLint("InlinedApi")
  private void createCameraSource(boolean autoFocus, boolean useFlash) {
    // TODO: Enable support DATA_MATRIX and QR_CODE
    BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(context)
      .setBarcodeFormats(Barcode.EAN_13)
      .build();
    // wrap barcode detector to capture image
    BarcodeImageCapturingDetector detector = new BarcodeImageCapturingDetector(
      context, barcodeDetector);
    detector.setOnBarcodeImageCaptureListener(this);

    BarcodeTrackerFactory barcodeFactory = new BarcodeTrackerFactory(
      graphicOverlay, this);
    detector.setProcessor(
      new MultiProcessor.Builder<>(barcodeFactory).build());

    if (!detector.isOperational()) {
      Log.w(TAG, "(createCameraSource) not operational:" +
            "Detector dependencies are not yet available.");

      IntentFilter lowstorageFilter = new IntentFilter(
        Intent.ACTION_DEVICE_STORAGE_LOW);
      boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

      if (hasLowStorage) {
        Toast.makeText(
          this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
        Log.w(TAG, getString(R.string.low_storage_error));
      }
    }

    CameraSource.Builder builder = new CameraSource.Builder(context, detector)
      .setFacing(CameraSource.CAMERA_FACING_BACK)
      .setRequestedPreviewSize(1600, 1024)
      .setRequestedFps(15.0f);

    // auto focus
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      builder = builder.setFocusMode(
        autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null);
    }
    // auto flash
    this.cameraSource = builder
      .setFlashMode( // Enable only auto mode flash
        useFlash ? Camera.Parameters.FLASH_MODE_AUTO : null)
      .build();
  }

  @Override
  protected void onResume() {
    super.onResume();
    startCameraSource();
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (preview != null) {
      preview.stop();
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (preview != null) {
      preview.release();
    }
  }

  @Override
  public void onRequestPermissionsResult(
    int requestCode,
    @NonNull String[] permissions,
    @NonNull int[] grantResults) {

    if (requestCode != Constant.RC_HANDLE_CAMERA_PERM) {
      Log.d(TAG, "(onRequestPermissionsResult) requestCode: " + requestCode);
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
      return;
    }

    if (grantResults.length != 0 &&
        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      Log.d(TAG, "(onRequestPermissionsResult) Camera permission granted");
      // we have permission, so create the camerasource
      boolean autoFocus = getIntent().getBooleanExtra(
        Constant.kAutoFocus, false);
      boolean useFlash = getIntent().getBooleanExtra(
        Constant.kUseFlash, false);
      createCameraSource(autoFocus, useFlash);
      return;
    }

    Log.e(TAG,
      "(onRequestPermissionsResult) results len: " + grantResults.length +
      " result code: " + (
        grantResults.length > 0 ? grantResults[0] : "(empty)"));

    DialogInterface.OnClickListener listener =
      new DialogInterface.OnClickListener() {

      public void onClick(DialogInterface dialog, int id) {
        finish();
      }
    };

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    // TODO
    builder.setTitle(context.getString(R.string.app_name))
      .setMessage(R.string.no_camera_permission)
      .setPositiveButton(R.string.ok, listener)
      .show();
  }

  private void startCameraSource() throws SecurityException {
    int code = GoogleApiAvailability.getInstance()
      .isGooglePlayServicesAvailable(context);
    if (code != ConnectionResult.SUCCESS) {
      Dialog dlg = GoogleApiAvailability.getInstance().getErrorDialog(
        this, code, Constant.RC_HANDLE_GMS);
      dlg.show();
    }

    if (cameraSource != null) {
      try {
        preview.start(cameraSource, graphicOverlay);
      } catch (IOException e) {
        Log.e(TAG, "(startCameraSource) message: " + e.getMessage());
        cameraSource.release();
        this.cameraSource = null;
      }
    }
  }

  private boolean onTap(float rawX, float rawY) {
    int[] location = new int[2];
    graphicOverlay.getLocationOnScreen(location);
    float x = (rawX - location[0]) / graphicOverlay.getWidthScaleFactor();
    float y = (rawY - location[1]) / graphicOverlay.getHeightScaleFactor();

    Barcode barcode = null;
    float bestDistance = Float.MAX_VALUE;
    for (BarcodeGraphic graphic : graphicOverlay.getGraphics()) {
      Barcode detected = graphic.getBarcode();
      if (detected.getBoundingBox().contains((int) x, (int) y)) {
        // captured
        barcode = detected;
        break;
      }
      float dx = x - detected.getBoundingBox().centerX();
      float dy = y - detected.getBoundingBox().centerY();
      float distance = (dx * dx) + (dy * dy);
      if (distance < bestDistance) {
        barcode = detected;
        bestDistance = distance;
      }
    }
    Log.d(TAG, "(onTap) barcode: " + barcode);
    if (barcode != null) {
      onBarcodeDetected(barcode);
      return true;
    }
    return false;
  }

  @Override
  public void onBarcodeDetected(Barcode barcode) {
    Log.d(TAG, "(onBarcodeDetected) barcode: " + barcode.displayValue);

    // wait filepath in async task invoked from another callback
    synchronized (captureLock) {
      while (filepath == null) {
        try {
          captureLock.wait();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      Log.d(TAG, "(onBarcodeDetected) filepath: " + filepath);
      // just rerutrn detected barcode and image path to activity, and finish
      Intent data = new Intent();
      data.putExtra(Constant.kBarcode, barcode);
      data.putExtra(Constant.kFilepath, filepath);
      setResult(CommonStatusCodes.SUCCESS, data);
      finish();
      return;
    }
  }

  private static class CapturedData {
    public File files;
    public String barcodeValue;
    public Bitmap bitmap;
  }

  private class SaveBarcodeImageTask extends AsyncTask<
    CapturedData, Void, String> {
    private final static String TAG = "SaveBarcodeImageTask";

    @Override
    protected String doInBackground(CapturedData... params) {
      CapturedData data = (CapturedData)params[0];
      File barcodes = new File(data.files, "barcodes");

      // e.g. 7680529860526-20180223210923.jpg
      String dateString = Product.makeScannedAt(null);
      String filename = String.format(
        "%s-%s.jpg", data.barcodeValue, dateString);
      Log.d(TAG, "(doInBackground) filename: " + filename);

      String path = null;
      OutputStream out = null;
      try {
        barcodes.mkdir();
        File file = new File(barcodes, filename);
        path = file.getAbsolutePath();
        out = new BufferedOutputStream(
          new FileOutputStream(path, true));
        data.bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        try {
          if (out != null) {
            out.close();
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
        return path;
      }
    }

    @Override
    protected void onPostExecute(String path) {  // called on main ui thread
      Log.d(TAG, "(onPostExecute) fullpath: " + path);

      synchronized (captureLock) {
        filepath = path;
        captured = true;

        captureLock.notifyAll();
      }
    }
  }

  @Override
  public void onBarcodeImageCaptured(String barcodeValue, Bitmap bitmap) {
    if (bitmap == null) { return; }
    // build parameter
    CapturedData capturedData = new CapturedData();
    capturedData.files = context.getFilesDir();
    capturedData.barcodeValue = barcodeValue;
    capturedData.bitmap = bitmap;

    SaveBarcodeImageTask task = new SaveBarcodeImageTask();
    task.execute(capturedData);
  }
}
