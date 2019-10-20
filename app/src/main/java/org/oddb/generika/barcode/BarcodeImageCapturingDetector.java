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
package org.oddb.generika.barcode;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import androidx.annotation.UiThread;
import android.util.Log;
import android.util.SparseArray;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;

import java.lang.Long;
import java.nio.ByteBuffer;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;


public class BarcodeImageCapturingDetector extends Detector<Barcode> {
  private static final String TAG = "BarcodeImageCapturingDetector";

  private BarcodeDetector detector;

  private BarcodeImageCaptureListener barcodeImageCaptureListener;

  // yuvimage to bitmap conversion
  private RenderScript rs;
  private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;

  public void setOnBarcodeImageCaptureListener(Activity activity) {
    this.barcodeImageCaptureListener = (BarcodeImageCaptureListener)activity;
  }

  public interface BarcodeImageCaptureListener {
    @UiThread
    void onBarcodeImageCaptured(String barcodeValue, Bitmap bitmap);
  }

  public BarcodeImageCapturingDetector(
    Context context, BarcodeDetector detector) {
    this.detector = detector;

    this.rs = RenderScript.create(context);
    this.yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(
      rs, Element.U8_4(rs));
  }

  @Override
  public SparseArray<Barcode> detect(Frame frame) {
    SparseArray<Barcode> barcodes = detector.detect(frame);

    if (barcodes != null && barcodes.size() > 0) {
      // get only first one
      Barcode barcode = barcodes.valueAt(0);
      if (barcode != null) {
        Frame.Metadata metadata = frame.getMetadata();
        Log.d(TAG, "(detect) duration: " +
          Long.toString(metadata.getTimestampMillis()));

        // get colored bitmap image
        ByteBuffer byteBuffer = frame.getGrayscaleImageData();
        Bitmap bitmap = buildBitmap(byteBuffer,
            metadata.getWidth(), metadata.getHeight());

        if (barcodeImageCaptureListener != null) { // barcode + captured bitmap
          barcodeImageCaptureListener.onBarcodeImageCaptured(
            barcode.displayValue, bitmap);
        }
      }
    }
    return barcodes;
  }

  private Bitmap buildBitmap(ByteBuffer data, int width, int height) {
    Log.d(TAG, "(buildBitmap) data size: " + data.slice().remaining());
    Bitmap bitmap = null;

    int remaining = data.slice().remaining();
    if (remaining == 0) {
      return bitmap;
    }
    byte[] bytes = new byte[remaining];
    data.get(bytes, 0, bytes.length);
    if (bytes.length < 1) {
      return bitmap;
    }

    try {
      Allocation in, out;
      Type.Builder yuvType, rgbaType;

      yuvType = new Type.Builder(rs, Element.U8(rs)).setX(bytes.length);
      in = Allocation.createTyped(
        rs, yuvType.create(), Allocation.USAGE_SCRIPT);

      rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs))
        .setX(width)
        .setY(height);
      out = Allocation.createTyped(
        rs, rgbaType.create(), Allocation.USAGE_SCRIPT);

      in.copyFrom(bytes);
      yuvToRgbIntrinsic.setInput(in);
      yuvToRgbIntrinsic.forEach(out);

      bitmap = Bitmap.createBitmap(
        width, height, Bitmap.Config.ARGB_8888);
      out.copyTo(bitmap);
      // rotate
      Matrix matrix = new Matrix();
      matrix.postRotate(90);
      Bitmap s = Bitmap.createScaledBitmap(bitmap, width, height, true);
      bitmap = Bitmap.createBitmap(
        s, 0, 0, s.getWidth(), s.getHeight(), matrix, true);
    } catch (Exception e) {
      e.printStackTrace();
      Log.d(TAG, "(buildBitmap) error: " + e.getMessage());
    } finally {
      return bitmap;
    }
  }
}
