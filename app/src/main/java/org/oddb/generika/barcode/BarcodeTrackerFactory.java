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
package org.oddb.generika.barcode;

import android.content.Context;

import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;

import org.oddb.generika.ui.reader.GraphicOverlay;

import org.oddb.generika.barcode.BarcodeGraphic;
import org.oddb.generika.barcode.BarcodeGraphicTracker;


public class BarcodeTrackerFactory implements MultiProcessor.Factory<Barcode> {
  private GraphicOverlay<BarcodeGraphic> graphicOverlay;
  private Context context;

  public BarcodeTrackerFactory(
    GraphicOverlay<BarcodeGraphic> graphicOverlay_, Context context_) {
    this.graphicOverlay = graphicOverlay_;
    this.context = context_;
  }

  @Override
  public Tracker<Barcode> create(Barcode barcode) {
    BarcodeGraphic graphic = new BarcodeGraphic(graphicOverlay);
    return new BarcodeGraphicTracker(graphicOverlay, graphic, context);
  }
}
