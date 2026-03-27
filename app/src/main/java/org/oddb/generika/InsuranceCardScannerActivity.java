/*
 *  Generika Android
 *  Copyright (C) 2026 ywesee GmbH
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

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ExperimentalGetImage;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class InsuranceCardScannerActivity extends AppCompatActivity {
  private static final String TAG = "InsuranceCardScanner";
  private static final int CAMERA_PERMISSION_REQUEST = 100;

  private PreviewView previewView;
  private View cardOutline;
  private TextView hintLabel;
  private Button cancelButton;

  private ExecutorService cameraExecutor;
  private TextRecognizer textRecognizer;
  private boolean captureFinished = false;

  private Map<String, String> bagToGLNMapping = new HashMap<>();
  private Map<String, String> bagToNameMapping = new HashMap<>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

    FrameLayout root = new FrameLayout(this);
    root.setBackgroundColor(Color.BLACK);

    previewView = new PreviewView(this);
    root.addView(previewView, new FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.MATCH_PARENT,
      FrameLayout.LayoutParams.MATCH_PARENT));

    // Card outline overlay
    cardOutline = new View(this) {
      @Override
      protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        float cardAspect = 85.6f / 53.98f;
        float cardX = getWidth() * 0.04f;
        float cardW = getWidth() - 2 * cardX;
        float cardH = cardW / cardAspect;
        float cardY = getHeight() / 2f - cardH / 2f;
        RectF rect = new RectF(cardX, cardY, cardX + cardW, cardY + cardH);
        canvas.drawRoundRect(rect, 10, 10, paint);
      }
    };
    root.addView(cardOutline, new FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.MATCH_PARENT,
      FrameLayout.LayoutParams.MATCH_PARENT));

    // Hint label
    hintLabel = new TextView(this);
    hintLabel.setText(R.string.insurance_card_hint);
    hintLabel.setTextColor(Color.WHITE);
    hintLabel.setTextSize(16);
    hintLabel.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
    FrameLayout.LayoutParams hintParams = new FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.MATCH_PARENT,
      FrameLayout.LayoutParams.WRAP_CONTENT);
    hintParams.bottomMargin = 80;
    hintParams.gravity = android.view.Gravity.BOTTOM;
    root.addView(hintLabel, hintParams);

    // Cancel button
    cancelButton = new Button(this);
    cancelButton.setText(R.string.cancel);
    cancelButton.setTextColor(Color.WHITE);
    cancelButton.setBackgroundColor(Color.TRANSPARENT);
    cancelButton.setOnClickListener(v -> {
      setResult(RESULT_CANCELED);
      finish();
    });
    FrameLayout.LayoutParams cancelParams = new FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.WRAP_CONTENT,
      FrameLayout.LayoutParams.WRAP_CONTENT);
    cancelParams.topMargin = 48;
    cancelParams.leftMargin = 16;
    cancelParams.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
    root.addView(cancelButton, cancelParams);

    setContentView(root);

    // Handle edge-to-edge insets
    ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
      Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
      v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
      return WindowInsetsCompat.CONSUMED;
    });

    loadMappings();
    textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    cameraExecutor = Executors.newSingleThreadExecutor();

    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        == PackageManager.PERMISSION_GRANTED) {
      startCamera();
    } else {
      ActivityCompat.requestPermissions(this,
        new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
    }
  }

  private void loadMappings() {
    bagToGLNMapping = loadJsonMapping("bag-to-insurance-gln.json");
    bagToNameMapping = loadJsonMapping("bag-to-insurance-name.json");
  }

  private Map<String, String> loadJsonMapping(String filename) {
    Map<String, String> map = new HashMap<>();
    try {
      InputStream is = getAssets().open(filename);
      byte[] buffer = new byte[is.available()];
      is.read(buffer);
      is.close();
      JSONObject json = new JSONObject(new String(buffer));
      Iterator<String> keys = json.keys();
      while (keys.hasNext()) {
        String key = keys.next();
        map.put(key, json.getString(key));
      }
    } catch (Exception e) {
      Log.e(TAG, "Error loading " + filename, e);
    }
    return map;
  }

  @Override
  public void onRequestPermissionsResult(int requestCode,
      @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == CAMERA_PERMISSION_REQUEST) {
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        startCamera();
      } else {
        setResult(RESULT_CANCELED);
        finish();
      }
    }
  }

  private void startCamera() {
    ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
      ProcessCameraProvider.getInstance(this);

    cameraProviderFuture.addListener(() -> {
      try {
        ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
          .setTargetResolution(new Size(1280, 720))
          .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
          .build();

        imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
      } catch (Exception e) {
        Log.e(TAG, "Camera setup failed", e);
      }
    }, ContextCompat.getMainExecutor(this));
  }

  @OptIn(markerClass = ExperimentalGetImage.class)
  private void analyzeImage(ImageProxy imageProxy) {
    if (captureFinished) {
      imageProxy.close();
      return;
    }

    android.media.Image mediaImage = imageProxy.getImage();
    if (mediaImage == null) {
      imageProxy.close();
      return;
    }

    InputImage inputImage = InputImage.fromMediaImage(
      mediaImage, imageProxy.getImageInfo().getRotationDegrees());

    textRecognizer.process(inputImage)
      .addOnSuccessListener(text -> {
        if (!captureFinished) {
          processTextResults(text);
        }
      })
      .addOnFailureListener(e -> Log.d(TAG, "OCR failed: " + e.getMessage()))
      .addOnCompleteListener(task -> imageProxy.close());
  }

  private void processTextResults(Text text) {
    List<String> allTexts = new ArrayList<>();
    for (Text.TextBlock block : text.getTextBlocks()) {
      for (Text.Line line : block.getLines()) {
        allTexts.add(line.getText().trim());
      }
    }

    if (allTexts.size() < 3) return;

    // Pattern matching for card fields
    String nameText = null;
    String cardNumber = null;
    String bagNumber = null;
    String ahvNumber = null;
    String dateSex = null;

    Pattern ahvPattern = Pattern.compile("\\d{3}\\.\\d{4}\\.\\d{4}\\.\\d{2}");
    Pattern dateSexPattern = Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4})[\\s.]+([MF])");
    String[] unwantedLabels = {"Name", "Vorname", "Cognome", "Karten",
      "Geburtsdatum", "Date de", "Data di", "Data da",
      "Carte", "Assicurato", "Versicherte"};

    for (String line : allTexts) {
      // Skip label text
      boolean isLabel = false;
      for (String label : unwantedLabels) {
        if (line.contains(label)) { isLabel = true; break; }
      }
      if (isLabel) continue;

      // Skip lines with unwanted characters
      if (line.matches(".*[/^&~!=:(%#_].*")) continue;

      // Card number: 19-20 digits
      if (cardNumber == null && (line.length() == 19 || line.length() == 20)
          && line.matches("\\d+")) {
        cardNumber = line;
        continue;
      }

      // BAG number: exactly 5 digits
      if (bagNumber == null && line.length() == 5 && line.matches("\\d+")) {
        bagNumber = line;
        continue;
      }

      // AHV number
      if (ahvNumber == null) {
        Matcher m = ahvPattern.matcher(line);
        if (m.find()) {
          ahvNumber = m.group();
          continue;
        }
      }

      // Date + Sex
      if (dateSex == null) {
        Matcher m = dateSexPattern.matcher(line);
        if (m.find()) {
          dateSex = line;
          continue;
        }
      }

      // Name: contains comma (Family, Given)
      if (nameText == null && line.contains(",")) {
        String[] parts = line.split(",", 2);
        if (parts.length >= 2 && !parts[1].trim().isEmpty()) {
          nameText = line;
          continue;
        }
      }
    }

    // Try to extract AHV from card number if not found directly
    if (ahvNumber == null && cardNumber != null && cardNumber.startsWith("8075")) {
      String digits = cardNumber.substring(2, cardNumber.length() - 1);
      if (digits.length() == 13) {
        ahvNumber = digits.substring(0, 3) + "." +
                    digits.substring(3, 7) + "." +
                    digits.substring(7, 11) + "." +
                    digits.substring(11, 13);
      }
    }

    // Validate: need at least name, card number, and date+sex
    if (nameText == null || cardNumber == null || dateSex == null) return;

    // Parse name
    String[] nameParts = nameText.split(",", 2);
    String familyName = nameParts[0].trim();
    String givenName = nameParts.length >= 2 ? nameParts[1].trim() : "";

    // Parse date and sex
    Matcher dateSexMatcher = dateSexPattern.matcher(dateSex);
    String dateString = "";
    String sexString = "";
    if (dateSexMatcher.find()) {
      dateString = dateSexMatcher.group(1);
      sexString = dateSexMatcher.group(2);
    }

    // Lookup insurance
    String insuranceGLN = "";
    String insuranceName = "";
    if (bagNumber != null) {
      String bagKey = String.valueOf(Integer.parseInt(bagNumber));
      insuranceGLN = bagToGLNMapping.getOrDefault(bagKey, "");
      insuranceName = bagToNameMapping.getOrDefault(bagKey, "");
    }

    captureFinished = true;

    Intent resultData = new Intent();
    resultData.putExtra("familyName", familyName);
    resultData.putExtra("givenName", givenName);
    resultData.putExtra("cardNumber", cardNumber);
    resultData.putExtra("dateString", dateString);
    resultData.putExtra("sexString", sexString);
    resultData.putExtra("bagNumber", bagNumber != null ? bagNumber : "");
    resultData.putExtra("ahvNumber", ahvNumber != null ? ahvNumber : "");
    resultData.putExtra("insuranceGLN", insuranceGLN);
    resultData.putExtra("insuranceName", insuranceName);

    runOnUiThread(() -> {
      setResult(RESULT_OK, resultData);
      finish();
    });
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (cameraExecutor != null) {
      cameraExecutor.shutdown();
    }
    if (textRecognizer != null) {
      textRecognizer.close();
    }
  }
}
