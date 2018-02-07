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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.google.android.gms.vision.barcode.Barcode;

import org.oddb.generika.ui.reader.GraphicOverlay;


public class BarcodeGraphic extends GraphicOverlay.Graphic {
  private int mId;

  private static final int COLOR_CHOICES[] = {
    Color.BLUE,
    Color.CYAN,
    Color.GREEN
  };

  private static int mCurrentColorIndex = 0;

  private Paint mRectPaint;
  private Paint mTextPaint;
  private volatile Barcode mBarcode;

  BarcodeGraphic(GraphicOverlay overlay) {
    super(overlay);

    mCurrentColorIndex = (mCurrentColorIndex + 1) % COLOR_CHOICES.length;
    final int selectedColor = COLOR_CHOICES[mCurrentColorIndex];

    mRectPaint = new Paint();
    mRectPaint.setColor(selectedColor);
    mRectPaint.setStyle(Paint.Style.STROKE);
    mRectPaint.setStrokeWidth(4.0f);

    mTextPaint = new Paint();
    mTextPaint.setColor(selectedColor);
    mTextPaint.setTextSize(36.0f);
  }

  public int getId() {
    return mId;
  }

  public void setId(int id) {
    this.mId = id;
  }

  public Barcode getBarcode() {
    return mBarcode;
  }

  void updateItem(Barcode barcode) {
    mBarcode = barcode;
    postInvalidate();
  }

  @Override
  public void draw(Canvas canvas) {
    Barcode barcode = mBarcode;
    if (barcode == null) {
      return;
    }

    RectF rect = new RectF(barcode.getBoundingBox());
    rect.left = translateX(rect.left);
    rect.top = translateY(rect.top);
    rect.right = translateX(rect.right);
    rect.bottom = translateY(rect.bottom);
    canvas.drawRect(rect, mRectPaint);

    canvas.drawText(barcode.rawValue, rect.left, rect.bottom, mTextPaint);
  }
}
