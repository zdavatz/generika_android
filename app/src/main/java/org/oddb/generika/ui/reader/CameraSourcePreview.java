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
import android.content.Context;
import android.content.res.Configuration;
import androidx.annotation.RequiresPermission;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import com.google.android.gms.common.images.Size;

import java.io.IOException;


public class CameraSourcePreview extends ViewGroup {
  private static final String TAG = "CameraSourcePreview";

  private Context context;
  private SurfaceView surfaceView;
  private boolean startRequested;
  private boolean surfaceAvailable;
  private CameraSource cameraSource;

  private GraphicOverlay overlay;

  public CameraSourcePreview(Context context_, AttributeSet attrs) {
    super(context_, attrs);
    this.context = context_;
    this.startRequested = false;
    this.surfaceAvailable = false;

    this.surfaceView = new SurfaceView(context_);
    surfaceView.getHolder().addCallback(new SurfaceCallback());
    addView(surfaceView);
  }

  @RequiresPermission(Manifest.permission.CAMERA)
  public void start(CameraSource cameraSource_) throws
    IOException, SecurityException {

    if (cameraSource_ == null) {
      stop();
    }

    this.cameraSource = cameraSource_;

    if (cameraSource_ != null) {
      this.startRequested = true;
      startIfReady();
    }
  }

  @RequiresPermission(Manifest.permission.CAMERA)
  public void start(CameraSource cameraSource_, GraphicOverlay overlay_) throws
    IOException, SecurityException {

    this.overlay = overlay_;
    start(cameraSource_);
  }

  public void stop() {
    if (cameraSource != null) {
      cameraSource.stop();
    }
  }

  public void release() {
    if (cameraSource != null) {
      cameraSource.release();
      cameraSource = null;
    }
  }

  @RequiresPermission(Manifest.permission.CAMERA)
  private void startIfReady() throws IOException, SecurityException {
    if (startRequested && surfaceAvailable) {
      cameraSource.start(surfaceView.getHolder());
      if (overlay != null) {
        Size size = cameraSource.getPreviewSize();
        int min = Math.min(size.getWidth(), size.getHeight());
        int max = Math.max(size.getWidth(), size.getHeight());

        if (isPortraitMode()) {
          overlay.setCameraInfo(min, max, cameraSource.getCameraFacing());
        } else {
          overlay.setCameraInfo(max, min, cameraSource.getCameraFacing());
        }
        overlay.clear();
      }
      this.startRequested = false;
    }
  }

  private class SurfaceCallback implements SurfaceHolder.Callback {
    @Override
    public void surfaceCreated(SurfaceHolder _surface) {
      surfaceAvailable = true;
      try {
        startIfReady();
      } catch (SecurityException se) {
        Log.e(TAG,"Do not have permission to start the camera", se);
      } catch (IOException e) {
        Log.e(TAG, "Could not start camera source.", e);
      }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder _surface) {
      surfaceAvailable = false;
    }

    @Override
    public void surfaceChanged(
      SurfaceHolder _holder, int _format, int _width, int _height) {
      // pass
    }
  }

  @Override
  protected void onLayout(
    boolean changed, int left, int top, int right, int bottom) {

    int width = 320;
    int height = 240;
    if (cameraSource != null) {
      Size size = cameraSource.getPreviewSize();
      if (size != null) {
        width = size.getWidth();
        height = size.getHeight();
      }
    }

    if (isPortraitMode()) {
      int tmp = width;
      width = height;
      height = tmp;
    }

    final int layoutWidth = right - left;
    final int layoutHeight = bottom - top;

    // fix landscape mode and aspect ratio
    int childWidth;
    int childHeight;
    int childOffsetX = 0;
    int childOffsetY = 0;
    float widthRatio = (float)layoutWidth / (float)width;
    float heightRatio = (float)layoutHeight / (float)height;

    if (widthRatio > heightRatio) {
      childWidth = layoutWidth;
      childHeight = (int)((float)height * widthRatio);
      childOffsetY = (childHeight - layoutHeight) / 2;
    } else {
      childWidth = (int)((float)width * heightRatio);
      childHeight = layoutHeight;
      childOffsetX = (childWidth - layoutWidth) / 2;
    }

    for (int i = 0; i < getChildCount(); ++i) {
      getChildAt(i).layout(
        -1 * childOffsetX,
        -1 * childOffsetY,
        childWidth - childOffsetX,
        childHeight - childOffsetY);
    }

    try {
      startIfReady();
    } catch (SecurityException se) {
      Log.e(TAG,"Do not have permission to start the camera", se);
    } catch (IOException e) {
      Log.e(TAG, "Could not start camera source.", e);
    }
  }

  private boolean isPortraitMode() {
    int orientation = context.getResources().getConfiguration().orientation;
    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
      return false;
    }
    if (orientation == Configuration.ORIENTATION_PORTRAIT) {
      return true;
    }

    Log.d(TAG, "isPortraitMode returning false by default");
    return false;
  }
}
