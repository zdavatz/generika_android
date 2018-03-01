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

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import com.google.android.gms.vision.CameraSource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;


public class GraphicOverlay<T extends GraphicOverlay.Graphic> extends View {
  private final Object lock = new Object();
  private int previewWidth;
  private float widthScaleFactor = 1.0f;
  private int previewHeight;
  private float heightScaleFactor = 1.0f;
  private int facing = CameraSource.CAMERA_FACING_BACK;
  private Set<T> graphics = new HashSet<>();

  public static abstract class Graphic {
    private GraphicOverlay overlay;

    public Graphic(GraphicOverlay overlay_) {
      this.overlay = overlay_;
    }

    public abstract void draw(Canvas canvas);

    public float scaleX(float horizontal) {
      return horizontal * overlay.widthScaleFactor;
    }

    public float scaleY(float vertical) {
      return vertical * overlay.heightScaleFactor;
    }

    public float translateX(float x) {
      if (overlay.facing == CameraSource.CAMERA_FACING_FRONT) {
        return overlay.getWidth() - scaleX(x);
      } else {
        return scaleX(x);
      }
    }

    public float translateY(float y) {
      return scaleY(y);
    }

    public void postInvalidate() {
      overlay.postInvalidate();
    }
  }

  public GraphicOverlay(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public void clear() {
    synchronized (lock) {
      graphics.clear();
    }
    postInvalidate();
  }

  public void add(T graphic) {
    synchronized (lock) {
      graphics.add(graphic);
    }
    postInvalidate();
  }

  public void remove(T graphic) {
    synchronized (lock) {
      graphics.remove(graphic);
    }
    postInvalidate();
  }

  public List<T> getGraphics() {
    synchronized (lock) {
      return new Vector(graphics);
    }
  }

  public float getWidthScaleFactor() {
    return widthScaleFactor;
  }

  public float getHeightScaleFactor() {
    return heightScaleFactor;
  }

  public void setCameraInfo(
    int previewWidth_, int previewHeight_, int facing_) {
    synchronized (lock) {
      this.previewWidth = previewWidth_;
      this.previewHeight = previewHeight_;
      facing = facing_;
    }
    postInvalidate();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    synchronized (lock) {
      if ((previewWidth != 0) && (previewHeight != 0)) {
        widthScaleFactor =
          (float) canvas.getWidth() / (float) previewWidth;
        heightScaleFactor =
          (float) canvas.getHeight() / (float) previewHeight;
      }

      for (Graphic graphic : graphics) {
        graphic.draw(canvas);
      }
    }
  }
}
