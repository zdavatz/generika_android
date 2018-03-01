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
  private int id;

  private static final int COLOR_CHOICES[] = {
    // Generika uses only green (primary dark color) ;)
    Color.GREEN
  };

  private static int currentColorIndex = 0;

  private Paint rectPaint;
  private Paint textPaint;
  private volatile Barcode barcode;

  BarcodeGraphic(GraphicOverlay overlay) {
    super(overlay);

    // TODO: Change color for barcode type (barcode, qrcode etc.)
    this.currentColorIndex = (currentColorIndex + 1) % COLOR_CHOICES.length;
    final int selectedColor = COLOR_CHOICES[currentColorIndex];

    this.rectPaint = new Paint();
    rectPaint.setColor(selectedColor);
    rectPaint.setStyle(Paint.Style.STROKE);
    rectPaint.setStrokeWidth(4.0f);

    this.textPaint = new Paint();
    textPaint.setColor(selectedColor);
    textPaint.setTextSize(36.0f);
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public Barcode getBarcode() {
    return barcode;
  }

  void updateItem(Barcode barcode_) {
    this.barcode = barcode_;
    postInvalidate();
  }

  @Override
  public void draw(Canvas canvas) {
    Barcode barcode_ = barcode;
    if (barcode_ == null) {
      return;
    }
    RectF rect = new RectF(barcode_.getBoundingBox());
    rect.left = translateX(rect.left);
    rect.top = translateY(rect.top);
    rect.right = translateX(rect.right);
    rect.bottom = translateY(rect.bottom);
    canvas.drawRect(rect, rectPaint);
    canvas.drawText(barcode_.rawValue, rect.left, rect.bottom, textPaint);
  }
}
