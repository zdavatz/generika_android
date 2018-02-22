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

import java.io.IOException;

import org.oddb.generika.app.BaseActivity;
import org.oddb.generika.ui.reader.CameraSource;
import org.oddb.generika.ui.reader.CameraSourcePreview;
import org.oddb.generika.ui.reader.GraphicOverlay;


public final class BarcodeCaptureActivity extends BaseActivity implements
  BarcodeGraphicTracker.BarcodeUpdateListener {

  private static final int RC_HANDLE_GMS = 9001;
  private static final int RC_HANDLE_CAMERA_PERM = 2;

  private static final String TAG = "Barcode-Reader";

  public static final String AutoFocus = "AutoFocus";
  public static final String UseFlash  = "UseFlash";
  public static final String BarcodeObject = "Barcode";

  private CameraSource mCameraSource;
  private CameraSourcePreview mPreview;
  private GraphicOverlay<BarcodeGraphic> mGraphicOverlay;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    setContentView(R.layout.barcode_capture);

    mPreview = (CameraSourcePreview)findViewById(R.id.preview);
    mGraphicOverlay = (GraphicOverlay<BarcodeGraphic>)findViewById(
        R.id.graphicOverlay);

    // Options: from the main intent
    boolean autoFocus = getIntent().getBooleanExtra(AutoFocus, false);
    boolean useFlash = getIntent().getBooleanExtra(UseFlash, false);

    // Check for the camera permission
    int rc = ActivityCompat.checkSelfPermission(
      this, Manifest.permission.CAMERA);
    if (rc == PackageManager.PERMISSION_GRANTED) {
      createCameraSource(autoFocus, useFlash);
    } else {
      requestCameraPermission();
    }

    Snackbar.make(
      mGraphicOverlay,
      "Hold rear camera out over the barcode of package",
      Snackbar.LENGTH_LONG).show();
  }

  private void requestCameraPermission() {
    Log.w(TAG, "Camera permission is not granted. Requesting permission");

    final String[] permissions = new String[]{Manifest.permission.CAMERA};

    if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
      Manifest.permission.CAMERA)) {
      ActivityCompat.requestPermissions(
        this, permissions, RC_HANDLE_CAMERA_PERM);
      return;
    }

    final Activity thisActivity = this;

    View.OnClickListener listener = new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        ActivityCompat.requestPermissions(thisActivity, permissions,
          RC_HANDLE_CAMERA_PERM);
      }
    };

    findViewById(R.id.topLayout).setOnClickListener(listener);
    Snackbar.make(
      mGraphicOverlay,
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
    Context context = getApplicationContext();

    BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(
      context).build();
    BarcodeTrackerFactory barcodeFactory = new BarcodeTrackerFactory(
      mGraphicOverlay, this);
    barcodeDetector.setProcessor(
      new MultiProcessor.Builder<>(barcodeFactory).build());

    if (!barcodeDetector.isOperational()) {
      Log.w(TAG, "Detector dependencies are not yet available.");

      IntentFilter lowstorageFilter = new IntentFilter(
        Intent.ACTION_DEVICE_STORAGE_LOW);
      boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

      if (hasLowStorage) {
        Toast.makeText(
          this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
        Log.w(TAG, getString(R.string.low_storage_error));
      }
    }

    CameraSource.Builder builder = new CameraSource.Builder(
        getApplicationContext(), barcodeDetector)
          .setFacing(CameraSource.CAMERA_FACING_BACK)
          .setRequestedPreviewSize(1600, 1024)
          .setRequestedFps(15.0f);

    // auto focus
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      builder = builder.setFocusMode(
        autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null);
    }

    mCameraSource = builder
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
    if (mPreview != null) {
      mPreview.stop();
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (mPreview != null) {
      mPreview.release();
    }
  }

  @Override
  public void onRequestPermissionsResult(
    int requestCode,
    @NonNull String[] permissions,
    @NonNull int[] grantResults) {

    if (requestCode != RC_HANDLE_CAMERA_PERM) {
      Log.d(TAG, "Got unexpected permission result: " + requestCode);
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
      return;
    }

    if (grantResults.length != 0 &&
        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      Log.d(TAG, "Camera permission granted - initialize the camera source");
      // we have permission, so create the camerasource
      boolean autoFocus = getIntent().getBooleanExtra(AutoFocus,false);
      boolean useFlash = getIntent().getBooleanExtra(UseFlash, false);
      createCameraSource(autoFocus, useFlash);
      return;
    }

    Log.e(TAG,
      "Permission not granted: results len = " + grantResults.length +
      " Result code = " + (
        grantResults.length > 0 ? grantResults[0] : "(empty)"));

    DialogInterface.OnClickListener listener =
      new DialogInterface.OnClickListener() {

      public void onClick(DialogInterface dialog, int id) {
        finish();
      }
    };

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("Multitracker sample")
      .setMessage(R.string.no_camera_permission)
      .setPositiveButton(R.string.ok, listener)
      .show();
  }

  private void startCameraSource() throws SecurityException {
    int code = GoogleApiAvailability.getInstance()
      .isGooglePlayServicesAvailable(getApplicationContext());
    if (code != ConnectionResult.SUCCESS) {
      Dialog dlg = GoogleApiAvailability.getInstance().getErrorDialog(
        this, code, RC_HANDLE_GMS);
      dlg.show();
    }

    if (mCameraSource != null) {
      try {
        mPreview.start(mCameraSource, mGraphicOverlay);
      } catch (IOException e) {
        Log.e(TAG, "Unable to start camera source.", e);
        mCameraSource.release();
        mCameraSource = null;
      }
    }
  }

  private boolean onTap(float rawX, float rawY) {
    int[] location = new int[2];
    mGraphicOverlay.getLocationOnScreen(location);
    float x = (rawX - location[0]) / mGraphicOverlay.getWidthScaleFactor();
    float y = (rawY - location[1]) / mGraphicOverlay.getHeightScaleFactor();

    Barcode barcode = null;
    float bestDistance = Float.MAX_VALUE;
    for (BarcodeGraphic graphic : mGraphicOverlay.getGraphics()) {
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
    if (barcode != null) {
      onBarcodeDetected(barcode);
      return true;
    }
    return false;
  }

  @Override
  public void onBarcodeDetected(Barcode barcode) {
    // just rerutrn detected barcode to activity and finish
    Intent data = new Intent();
    data.putExtra(BarcodeObject, barcode);
    setResult(CommonStatusCodes.SUCCESS, data);
    finish();
  }
}
