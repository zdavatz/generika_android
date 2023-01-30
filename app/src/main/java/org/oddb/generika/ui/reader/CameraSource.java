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
package org.oddb.generika.ui.reader;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Build;
import android.os.SystemClock;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.annotation.StringDef;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.google.android.gms.common.images.Size;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;

import java.io.IOException;
import java.lang.Thread.State;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@SuppressWarnings("deprecation")
public class CameraSource {

  @SuppressLint("InlinedApi")
  public static final int CAMERA_FACING_BACK = CameraInfo.CAMERA_FACING_BACK;
  @SuppressLint("InlinedApi")
  public static final int CAMERA_FACING_FRONT = CameraInfo.CAMERA_FACING_FRONT;

  private static final String TAG = "CameraSource";

  private static final int DUMMY_TEXTURE_NAME = 100;
  private static final float ASPECT_RATIO_TOLERANCE = 0.01f;

  @StringDef({
    Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE,
    Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO,
    Camera.Parameters.FOCUS_MODE_AUTO,
    Camera.Parameters.FOCUS_MODE_EDOF,
    Camera.Parameters.FOCUS_MODE_FIXED,
    Camera.Parameters.FOCUS_MODE_INFINITY,
    Camera.Parameters.FOCUS_MODE_MACRO
  })
  @Retention(RetentionPolicy.SOURCE)
  private @interface FocusMode {}

  @StringDef({
    Camera.Parameters.FLASH_MODE_ON,
    Camera.Parameters.FLASH_MODE_OFF,
    Camera.Parameters.FLASH_MODE_AUTO,
    Camera.Parameters.FLASH_MODE_RED_EYE,
    Camera.Parameters.FLASH_MODE_TORCH
  })
  @Retention(RetentionPolicy.SOURCE)
  private @interface FlashMode {}

  private Context context;

  private final Object cameraLock = new Object();

  private Camera camera;

  private int facing = CAMERA_FACING_BACK;
  private int rotation;

  private Size previewSize;

  private float requestedFps = 30.0f;
  private int requestedPreviewWidth = 1024;
  private int requestedPreviewHeight = 768;

  private String focusMode = null;
  private String flashMode = null;

  private SurfaceView dummySurfaceView;
  private SurfaceTexture dummySurfaceTexture;

  private Thread processingThread;
  private FrameProcessingRunnable frameProcessor;

  private Map<byte[], ByteBuffer> bytesToByteBuffer = new HashMap<>();

  public static class Builder {
    private final Detector<?> detector;
    private CameraSource cameraSource = new CameraSource();

    public Builder(Context context_, Detector<?> detector_) {
      if (context_ == null) {
        throw new IllegalArgumentException("No context supplied.");
      }
      if (detector_ == null) {
        throw new IllegalArgumentException("No detector supplied.");
      }

      this.detector = detector_;
      cameraSource.context = context_;
    }

    public Builder setRequestedFps(float fps) {
      if (fps <= 0) {
        throw new IllegalArgumentException("Invalid fps: " + fps);
      }
      cameraSource.requestedFps = fps;
      return this;
    }

    public Builder setFocusMode(@FocusMode String mode) {
      cameraSource.focusMode = mode;
      return this;
    }

    public Builder setFlashMode(@FlashMode String mode) {
      cameraSource.flashMode = mode;
      return this;
    }

    public Builder setRequestedPreviewSize(int width, int height) {
      final int MAX = 1000000;
      if ((width <= 0) || (width > MAX) || (height <= 0) || (height > MAX)) {
        throw new IllegalArgumentException(
          "Invalid preview size: " + width + "x" + height);
      }
      cameraSource.requestedPreviewWidth = width;
      cameraSource.requestedPreviewHeight = height;
      return this;
    }

    public Builder setFacing(int facing_) {
      if ((facing_ != CAMERA_FACING_BACK) &&
          (facing_ != CAMERA_FACING_FRONT)) {
        throw new IllegalArgumentException("Invalid camera: " + facing_);
      }
      cameraSource.facing = facing_;
      return this;
    }

    public CameraSource build() {
      cameraSource.frameProcessor =
        cameraSource.new FrameProcessingRunnable(detector);
      return cameraSource;
    }
  }

  public interface ShutterCallback {
    void onShutter();
  }

  public interface PictureCallback {
    // jpeg
    void onPictureTaken(byte[] data);
  }

  public interface AutoFocusCallback {
    void onAutoFocus(boolean success);
  }

  public interface AutoFocusMoveCallback {
    void onAutoFocusMoving(boolean start);
  }

  public void release() {
    synchronized (cameraLock) {
      stop();
      frameProcessor.release();
    }
  }

  @RequiresPermission(Manifest.permission.CAMERA)
  public CameraSource start() throws IOException {
    synchronized (cameraLock) {
      if (camera != null) {
        return this;
      }

      this.camera = createCamera();

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
        dummySurfaceTexture = new SurfaceTexture(DUMMY_TEXTURE_NAME);
        camera.setPreviewTexture(dummySurfaceTexture);
      } else {
        dummySurfaceView = new SurfaceView(context);
        camera.setPreviewDisplay(dummySurfaceView.getHolder());
      }
      camera.startPreview();

      processingThread = new Thread(frameProcessor);
      frameProcessor.setActive(true);
      processingThread.start();
    }
    return this;
  }

  @RequiresPermission(Manifest.permission.CAMERA)
  public CameraSource start(SurfaceHolder surfaceHolder) throws IOException {
    synchronized (cameraLock) {
      if (camera != null) {
        return this;
      }

      this.camera = createCamera();
      camera.setPreviewDisplay(surfaceHolder);
      camera.startPreview();

      processingThread = new Thread(frameProcessor);
      frameProcessor.setActive(true);
      processingThread.start();
    }
    return this;
  }

  public void stop() {
    synchronized (cameraLock) {
      frameProcessor.setActive(false);
      if (processingThread != null) {
        try {
          // Wait for the thread to complete
          processingThread.join();
        } catch (InterruptedException e) {
          Log.d(TAG, "Frame processing thread interrupted on release.");
        }
        processingThread = null;
      }
      bytesToByteBuffer.clear();

      if (camera != null) {
        camera.stopPreview();
        camera.setPreviewCallbackWithBuffer(null);
        try {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            camera.setPreviewTexture(null);
          } else {
            camera.setPreviewDisplay(null);
          }
        } catch (Exception e) {
          Log.e(TAG, "Failed to clear camera preview: " + e);
        }
        camera.release();
        this.camera = null;
      }
    }
  }

  public Size getPreviewSize() {
    return previewSize;
  }

  public int getCameraFacing() {
    return facing;
  }

  public int doZoom(float scale) {
    synchronized (cameraLock) {
      if (camera == null) {
        return 0;
      }
      int currentZoom = 0;
      int maxZoom;
      Camera.Parameters parameters = camera.getParameters();
      if (!parameters.isZoomSupported()) {
        Log.w(TAG, "Zoom is not supported on this device");
        return currentZoom;
      }
      maxZoom = parameters.getMaxZoom();

      currentZoom = parameters.getZoom() + 1;
      float newZoom;
      if (scale > 1) {
        newZoom = currentZoom + scale * (maxZoom / 10);
      } else {
        newZoom = currentZoom * scale;
      }
      currentZoom = Math.round(newZoom) - 1;
      if (currentZoom < 0) {
        currentZoom = 0;
      } else if (currentZoom > maxZoom) {
        currentZoom = maxZoom;
      }
      parameters.setZoom(currentZoom);
      camera.setParameters(parameters);
      return currentZoom;
    }
  }

  public void takePicture(ShutterCallback shutter, PictureCallback jpeg) {
    synchronized (cameraLock) {
      if (camera != null) {
        PictureStartCallback startCallback = new PictureStartCallback();
        startCallback.delegate = shutter;
        PictureDoneCallback doneCallback = new PictureDoneCallback();
        doneCallback.delegate = jpeg;
        camera.takePicture(startCallback, null, null, doneCallback);
      }
    }
  }

  @Nullable
  @FocusMode
  public String getFocusMode() {
    return focusMode;
  }

  public boolean setFocusMode(@FocusMode String mode) {
    synchronized (cameraLock) {
      if (camera != null && mode != null) {
        Camera.Parameters parameters = camera.getParameters();
        if (parameters.getSupportedFocusModes().contains(mode)) {
          parameters.setFocusMode(mode);
          camera.setParameters(parameters);
          this.focusMode = mode;
          return true;
        }
      }
      return false;
    }
  }

  @Nullable
  @FlashMode
  public String getFlashMode() {
    return flashMode;
  }

  public boolean setFlashMode(@FlashMode String mode) {
    synchronized (cameraLock) {
      if (camera != null && mode != null) {
        Camera.Parameters parameters = camera.getParameters();
        if (parameters.getSupportedFlashModes().contains(mode)) {
          parameters.setFlashMode(mode);
          camera.setParameters(parameters);
          this.flashMode = mode;
          return true;
        }
      }

      return false;
    }
  }

  public void autoFocus(@Nullable AutoFocusCallback cb) {
    synchronized (cameraLock) {
      if (camera != null) {
        CameraAutoFocusCallback autoFocusCallback = null;
        if (cb != null) {
          autoFocusCallback = new CameraAutoFocusCallback();
          autoFocusCallback.delegate = cb;
        }
        camera.autoFocus(autoFocusCallback);
      }
    }
  }

  public void cancelAutoFocus() {
    synchronized (cameraLock) {
      if (camera != null) {
        camera.cancelAutoFocus();
      }
    }
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  public boolean setAutoFocusMoveCallback(@Nullable AutoFocusMoveCallback cb) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
      return false;
    }

    synchronized (cameraLock) {
      if (camera != null) {
        CameraAutoFocusMoveCallback autoFocusMoveCallback = null;
        if (cb != null) {
          autoFocusMoveCallback = new CameraAutoFocusMoveCallback();
          autoFocusMoveCallback.delegate = cb;
        }
        camera.setAutoFocusMoveCallback(autoFocusMoveCallback);
      }
    }
    return true;
  }

  private CameraSource() {
    // pass
  }

  private class PictureStartCallback implements Camera.ShutterCallback {
    private ShutterCallback delegate;

    @Override
    public void onShutter() {
      if (delegate != null) {
        delegate.onShutter();
      }
    }
  }

  private class PictureDoneCallback implements Camera.PictureCallback {
    private PictureCallback delegate;

    @Override
    public void onPictureTaken(byte[] data, Camera camera_) {
      if (delegate != null) {
        delegate.onPictureTaken(data);
      }
      synchronized (cameraLock) {
        if (camera_ != null) {
          camera_.startPreview();
        }
      }
    }
  }

  private class CameraAutoFocusCallback implements Camera.AutoFocusCallback {
    private AutoFocusCallback delegate;

    @Override
    public void onAutoFocus(boolean success, Camera _camera) {
      if (delegate != null) {
        delegate.onAutoFocus(success);
      }
    }
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  private class CameraAutoFocusMoveCallback implements
    Camera.AutoFocusMoveCallback {

    private AutoFocusMoveCallback delegate;

    @Override
    public void onAutoFocusMoving(boolean start, Camera _camera) {
      if (delegate != null) {
        delegate.onAutoFocusMoving(start);
      }
    }
  }

  @SuppressLint("InlinedApi")
  private Camera createCamera() {
    int requestedCameraId = getIdForRequestedCamera(facing);
    if (requestedCameraId == -1) {
      throw new RuntimeException("Could not find requested camera.");
    }
    Camera camera_ = Camera.open(requestedCameraId);

    SizePair sizePair = selectSizePair(
      camera_, requestedPreviewWidth, requestedPreviewHeight);
    if (sizePair == null) {
      throw new RuntimeException("Could not find suitable preview size.");
    }
    Size pictureSize = sizePair.pictureSize();
    this.previewSize = sizePair.previewSize();

    int[] previewFpsRange = selectPreviewFpsRange(camera_, requestedFps);
    if (previewFpsRange == null) {
      throw new RuntimeException(
        "Could not find suitable preview frames per second range.");
    }

    Camera.Parameters parameters = camera_.getParameters();

    if (pictureSize != null) {
      parameters.setPictureSize(
        pictureSize.getWidth(), pictureSize.getHeight());
    }

    parameters.setPreviewSize(
      previewSize.getWidth(), previewSize.getHeight());
    parameters.setPreviewFpsRange(
      previewFpsRange[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
      previewFpsRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
    parameters.setPreviewFormat(ImageFormat.NV21);

    setRotation(camera_, parameters, requestedCameraId);
    if (focusMode != null) {
      if (parameters.getSupportedFocusModes().contains(
          focusMode)) {
        parameters.setFocusMode(focusMode);
      } else {
        Log.i(TAG,
          "Camera focus mode: " + focusMode +
          " is not supported on this device.");
      }
    }
    focusMode = parameters.getFocusMode();

    if (flashMode != null) {
      if (parameters.getSupportedFlashModes() != null) {
        if (parameters.getSupportedFlashModes().contains(
            flashMode)) {
          parameters.setFlashMode(flashMode);
        } else {
          Log.i(TAG,
            "Camera flash mode: " + flashMode +
            " is not supported on this device.");
        }
      }
    }

    flashMode = parameters.getFlashMode();
    camera_.setParameters(parameters);

    camera_.setPreviewCallbackWithBuffer(new CameraPreviewCallback());
    camera_.addCallbackBuffer(createPreviewBuffer(previewSize));
    camera_.addCallbackBuffer(createPreviewBuffer(previewSize));
    camera_.addCallbackBuffer(createPreviewBuffer(previewSize));
    camera_.addCallbackBuffer(createPreviewBuffer(previewSize));
    return camera_;
  }

  private static int getIdForRequestedCamera(int facing_) {
    CameraInfo cameraInfo = new CameraInfo();
    for (int i = 0; i < Camera.getNumberOfCameras(); ++i) {
      Camera.getCameraInfo(i, cameraInfo);
      if (cameraInfo.facing == facing_) {
        return i;
      }
    }
    return -1;
  }

  private static SizePair selectSizePair(
    Camera camera_, int desiredWidth, int desiredHeight) {

    List<SizePair> validPreviewSizes = generateValidPreviewSizeList(camera_);

    SizePair selectedPair = null;
    int minDiff = Integer.MAX_VALUE;
    for (SizePair sizePair : validPreviewSizes) {
      Size size = sizePair.previewSize();
      int diff = Math.abs(size.getWidth() - desiredWidth) +
          Math.abs(size.getHeight() - desiredHeight);
      if (diff < minDiff) {
        selectedPair = sizePair;
        minDiff = diff;
      }
    }
    return selectedPair;
  }

  private static class SizePair {
    private Size preview;
    private Size picture;

    public SizePair(android.hardware.Camera.Size previewSize,
                    android.hardware.Camera.Size pictureSize) {
      preview = new Size(previewSize.width, previewSize.height);
      if (pictureSize != null) {
        picture = new Size(pictureSize.width, pictureSize.height);
      }
    }

    public Size previewSize() {
      return preview;
    }

    @SuppressWarnings("unused")
    public Size pictureSize() {
      return picture;
    }
  }

  private static List<SizePair> generateValidPreviewSizeList(Camera camera_) {
    Camera.Parameters parameters = camera_.getParameters();
    List<android.hardware.Camera.Size> supportedPreviewSizes =
      parameters.getSupportedPreviewSizes();
    List<android.hardware.Camera.Size> supportedPictureSizes =
      parameters.getSupportedPictureSizes();
    List<SizePair> validPreviewSizes = new ArrayList<>();

    for (android.hardware.Camera.Size previewSize_ : supportedPreviewSizes) {
      float previewAspectRatio =
        (float) previewSize_.width / (float) previewSize_.height;

      for (android.hardware.Camera.Size pictureSize : supportedPictureSizes) {
        float pictureAspectRatio =
          (float) pictureSize.width / (float) pictureSize.height;
        if (Math.abs(previewAspectRatio - pictureAspectRatio) <
            ASPECT_RATIO_TOLERANCE) {
          validPreviewSizes.add(new SizePair(previewSize_, pictureSize));
          break;
        }
      }
    }

    if (validPreviewSizes.size() == 0) {
      Log.w(TAG,
        "No preview sizes have a corresponding same-aspect-ratio size");
      for (android.hardware.Camera.Size previewSize_ : supportedPreviewSizes) {
        validPreviewSizes.add(new SizePair(previewSize_, null));
      }
    }
    return validPreviewSizes;
  }

  private int[] selectPreviewFpsRange(
      Camera camera_, float desiredPreviewFps) {
    int desiredPreviewFpsScaled = (int) (desiredPreviewFps * 1000.0f);

    int[] selectedFpsRange = null;
    int minDiff = Integer.MAX_VALUE;
    List<int[]> previewFpsRangeList = camera_.getParameters()
      .getSupportedPreviewFpsRange();

    for (int[] range : previewFpsRangeList) {
      int deltaMin = desiredPreviewFpsScaled -
        range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX];
      int deltaMax = desiredPreviewFpsScaled -
        range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX];
      int diff = Math.abs(deltaMin) + Math.abs(deltaMax);
      if (diff < minDiff) {
        selectedFpsRange = range;
        minDiff = diff;
      }
    }
    return selectedFpsRange;
  }

  private void setRotation(
    Camera camera_, Camera.Parameters parameters, int cameraId) {

    WindowManager windowManager =
      (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    int degrees = 0;
    int rotation = windowManager.getDefaultDisplay().getRotation();
    switch (rotation) {
      case Surface.ROTATION_0:
        degrees = 0;
        break;
      case Surface.ROTATION_90:
        degrees = 90;
        break;
      case Surface.ROTATION_180:
        degrees = 180;
        break;
      case Surface.ROTATION_270:
        degrees = 270;
        break;
      default:
        Log.e(TAG, "Bad rotation value: " + rotation);
    }

    CameraInfo cameraInfo = new CameraInfo();
    Camera.getCameraInfo(cameraId, cameraInfo);

    int angle;
    int displayAngle;
    if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
      angle = (cameraInfo.orientation + degrees) % 360;
      displayAngle = (360 - angle) % 360;
    } else {
      angle = (cameraInfo.orientation - degrees + 360) % 360;
      displayAngle = angle;
    }

    this.rotation = angle / 90;

    camera_.setDisplayOrientation(displayAngle);
    parameters.setRotation(angle);
  }

  private byte[] createPreviewBuffer(Size previewSize) {
    int bitsPerPixel = ImageFormat.getBitsPerPixel(ImageFormat.NV21);
    long sizeInBits = previewSize.getHeight() *
      previewSize.getWidth() * bitsPerPixel;
    int bufferSize = (int) Math.ceil(sizeInBits / 8.0d) + 1;

    byte[] byteArray = new byte[bufferSize];
    ByteBuffer buffer = ByteBuffer.wrap(byteArray);
    if (!buffer.hasArray() || (buffer.array() != byteArray)) {
      throw new IllegalStateException(
        "Failed to create valid buffer for camera source.");
    }

    bytesToByteBuffer.put(byteArray, buffer);
    return byteArray;
  }

  private class CameraPreviewCallback implements Camera.PreviewCallback {
    @Override
    public void onPreviewFrame(byte[] data, Camera camera_) {
      frameProcessor.setNextFrame(data, camera_);
    }
  }

  private class FrameProcessingRunnable implements Runnable {
    private Detector<?> detector;
    private long startTimeMillis = SystemClock.elapsedRealtime();

    private final Object lock = new Object();
    private boolean active = true;

    private long pendingTimeMillis;
    private int pendingFrameId = 0;
    private ByteBuffer pendingFrameData;

    FrameProcessingRunnable(Detector<?> detector_) {
      this.detector = detector_;
    }

    @SuppressLint("Assert")
    void release() {
      assert (processingThread == null || processingThread.getState() == State.TERMINATED);
      detector.release();
      this.detector = null;
    }

    void setActive(boolean active_) {
      synchronized (lock) {
        this.active = active_;
        this.lock.notifyAll();
      }
    }

    void setNextFrame(byte[] data, Camera camera_) {
      synchronized (lock) {
        if (pendingFrameData != null) {
          camera_.addCallbackBuffer(pendingFrameData.array());
          pendingFrameData = null;
        }

        if (!bytesToByteBuffer.containsKey(data)) {
          Log.d(TAG,
            "Skipping frame. Could not find ByteBuffer associated with the " +
            "image data from the camera.");
          return;
        }

        pendingTimeMillis = SystemClock.elapsedRealtime() - startTimeMillis;
        pendingFrameId++;
        pendingFrameData = bytesToByteBuffer.get(data);
        lock.notifyAll();
      }
    }

    @Override
    public void run() {
      Frame outputFrame;
      ByteBuffer data;

      while (true) {
        synchronized (lock) {
          while (active && (pendingFrameData == null)) {
            try {
              lock.wait();
            } catch (InterruptedException e) {
              Log.d(TAG, "Frame processing loop terminated.", e);
              return;
            }
          }
          if (!active) {
            return;
          }
          // skip if bytebuffer is empty
          if (pendingFrameData.slice().remaining() < 1) {
            return;
          }
          int width = previewSize.getWidth();
          int height = previewSize.getHeight();

          // create a frame to pass barcode detection
          outputFrame = new Frame.Builder()
            .setImageData(
              pendingFrameData, width, height, ImageFormat.NV21)
            .setId(pendingFrameId)
            .setTimestampMillis(pendingTimeMillis)
            .setRotation(rotation)
            .build();

          data = pendingFrameData;
          pendingFrameData = null;
        }

        try {
          detector.receiveFrame(outputFrame);
        } catch (Throwable t) {
          Log.e(TAG, "Exception thrown from receiver.", t);
        } finally {
          camera.addCallbackBuffer(data.array());
        }
      }
    }
  }
}
