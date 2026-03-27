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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PrescriptionScannerActivity extends AppCompatActivity {
  private static final String TAG = "PrescriptionScanner";
  private static final int CAMERA_PERMISSION_REQUEST = 101;

  private PreviewView previewView;
  private TextView statusLabel;
  private TextView qrCheckmark;
  private ImageButton captureButton;

  private ExecutorService cameraExecutor;
  private BarcodeScanner barcodeScanner;
  private TextRecognizer textRecognizer;
  private ImageCapture imageCapture;

  private boolean qrFound = false;
  private String qrPayload = null;

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

    // A4 frame guide
    View frameGuide = new View(this) {
      @Override
      protected void onDraw(android.graphics.Canvas canvas) {
        super.onDraw(canvas);
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColor(Color.argb(128, 255, 255, 255));
        paint.setStyle(android.graphics.Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        float w = getWidth() * 0.85f;
        float h = w * 297f / 210f;
        float x = (getWidth() - w) / 2f;
        float y = (getHeight() - h) / 2f - 10;
        RectF rect = new RectF(x, y, x + w, y + h);
        canvas.drawRoundRect(rect, 8, 8, paint);
      }
    };
    root.addView(frameGuide, new FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.MATCH_PARENT,
      FrameLayout.LayoutParams.MATCH_PARENT));

    // Cancel button
    Button cancelBtn = new Button(this);
    cancelBtn.setText(R.string.cancel);
    cancelBtn.setTextColor(Color.WHITE);
    cancelBtn.setBackgroundColor(Color.TRANSPARENT);
    cancelBtn.setTextSize(18);
    cancelBtn.setOnClickListener(v -> {
      setResult(RESULT_CANCELED);
      finish();
    });
    FrameLayout.LayoutParams cancelParams = new FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.WRAP_CONTENT,
      FrameLayout.LayoutParams.WRAP_CONTENT);
    cancelParams.topMargin = 48;
    cancelParams.leftMargin = 16;
    cancelParams.gravity = Gravity.TOP | Gravity.START;
    root.addView(cancelBtn, cancelParams);

    // Status label
    statusLabel = new TextView(this);
    statusLabel.setText(R.string.prescription_hint);
    statusLabel.setTextColor(Color.WHITE);
    statusLabel.setTextSize(16);
    statusLabel.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
    FrameLayout.LayoutParams statusParams = new FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.WRAP_CONTENT,
      FrameLayout.LayoutParams.WRAP_CONTENT);
    statusParams.topMargin = 52;
    statusParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
    root.addView(statusLabel, statusParams);

    // QR checkmark
    qrCheckmark = new TextView(this);
    qrCheckmark.setText("\u2705");
    qrCheckmark.setTextSize(24);
    qrCheckmark.setVisibility(View.GONE);
    FrameLayout.LayoutParams checkParams = new FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.WRAP_CONTENT,
      FrameLayout.LayoutParams.WRAP_CONTENT);
    checkParams.topMargin = 48;
    checkParams.rightMargin = 16;
    checkParams.gravity = Gravity.TOP | Gravity.END;
    root.addView(qrCheckmark, checkParams);

    // Capture button (circle) - use dp
    float density = getResources().getDisplayMetrics().density;
    int btnSize = (int)(70 * density);
    captureButton = new ImageButton(this);
    android.graphics.drawable.GradientDrawable circle =
      new android.graphics.drawable.GradientDrawable();
    circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);
    circle.setColor(Color.WHITE);
    captureButton.setBackground(circle);
    captureButton.setOnClickListener(v -> captureTapped());
    FrameLayout.LayoutParams captureParams = new FrameLayout.LayoutParams(btnSize, btnSize);
    captureParams.bottomMargin = (int)(40 * density);
    captureParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
    root.addView(captureButton, captureParams);

    setContentView(root);

    // Handle edge-to-edge insets
    ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
      Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
      v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
      return WindowInsetsCompat.CONSUMED;
    });

    BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
      .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
      .build();
    barcodeScanner = BarcodeScanning.getClient(options);
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

        imageCapture = new ImageCapture.Builder()
          .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
          .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
          .setTargetResolution(new Size(1280, 720))
          .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
          .build();

        imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeForQR);

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector,
          preview, imageCapture, imageAnalysis);
      } catch (Exception e) {
        Log.e(TAG, "Camera setup failed", e);
      }
    }, ContextCompat.getMainExecutor(this));
  }

  @OptIn(markerClass = ExperimentalGetImage.class)
  private void analyzeForQR(ImageProxy imageProxy) {
    if (qrFound) {
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

    barcodeScanner.process(inputImage)
      .addOnSuccessListener(barcodes -> {
        for (Barcode barcode : barcodes) {
          String payload = barcode.getRawValue();
          if (payload != null &&
              (payload.startsWith("CHMED16A") || payload.contains("eprescription.hin.ch"))) {
            qrFound = true;
            qrPayload = payload;
            runOnUiThread(() -> {
              qrCheckmark.setVisibility(View.VISIBLE);
              statusLabel.setText(R.string.prescription_qr_found);
            });
            break;
          }
        }
      })
      .addOnCompleteListener(task -> imageProxy.close());
  }

  private void captureTapped() {
    captureButton.setEnabled(false);
    statusLabel.setText(R.string.prescription_processing);

    // Save to temp file, then process - more reliable than in-memory capture
    File photoFile = new File(getCacheDir(), "prescription_scan.jpg");
    ImageCapture.OutputFileOptions outputOptions =
      new ImageCapture.OutputFileOptions.Builder(photoFile).build();

    imageCapture.takePicture(outputOptions, cameraExecutor,
      new ImageCapture.OnImageSavedCallback() {
        @Override
        public void onImageSaved(@NonNull ImageCapture.OutputFileResults results) {
          Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
          if (bitmap == null) {
            runOnUiThread(() -> {
              statusLabel.setText(R.string.prescription_capture_error);
              captureButton.setEnabled(true);
            });
            return;
          }

          InputImage inputImage = InputImage.fromBitmap(bitmap, 0);

          // Try QR detection on photo if not found during live preview
          if (qrPayload == null) {
            barcodeScanner.process(inputImage)
              .addOnSuccessListener(barcodes -> {
                for (Barcode barcode : barcodes) {
                  String payload = barcode.getRawValue();
                  if (payload != null &&
                      (payload.startsWith("CHMED16A") ||
                       payload.contains("eprescription.hin.ch"))) {
                    qrPayload = payload;
                    break;
                  }
                }
              });
          }

          // OCR on the captured photo
          textRecognizer.process(inputImage)
            .addOnSuccessListener(text -> {
              List<String> lines = new ArrayList<>();
              for (Text.TextBlock block : text.getTextBlocks()) {
                for (Text.Line line : block.getLines()) {
                  lines.add(line.getText().trim());
                }
              }
              processOCRResults(lines);
              photoFile.delete();
            })
            .addOnFailureListener(e -> {
              Log.e(TAG, "OCR failed", e);
              photoFile.delete();
              runOnUiThread(() -> {
                statusLabel.setText(R.string.prescription_capture_error);
                captureButton.setEnabled(true);
              });
            });
        }

        @Override
        public void onError(@NonNull ImageCaptureException exception) {
          Log.e(TAG, "Photo capture failed", exception);
          runOnUiThread(() -> {
            statusLabel.setText(R.string.prescription_capture_error);
            captureButton.setEnabled(true);
          });
        }
      });
  }

  private void processOCRResults(List<String> lines) {
    Intent resultData = new Intent();

    String ahvNumber = "";
    String zsrNumber = "";
    String patientStreet = "";
    String patientZip = "";
    String patientCity = "";
    String patientPhone = "";
    String prescriptionDate = "";
    String hospitalName = "";
    String departmentName = "";
    String physicianFullName = "";

    List<String> medNames = new ArrayList<>();
    List<String> dosages = new ArrayList<>();

    Pattern ahvPattern = Pattern.compile("\\d{3}\\.\\d{4}\\.\\d{4}\\.\\d{2}");
    Pattern zsrPattern = Pattern.compile("ZSR[\\-\\s]*Nr\\.?:?\\s*([A-Z]\\d{4,6})");
    Pattern zsrExtract = Pattern.compile("[A-Z]\\d{4,6}");
    Pattern phonePattern = Pattern.compile("Tel\\.?:?\\s*([\\d\\s\\+]+)");
    Pattern datePattern = Pattern.compile("\\d{2}\\.\\d{2}\\.\\d{4}");
    Pattern addressPattern = Pattern.compile("^(CH-)?\\d{4}\\s+\\S");
    Pattern plzPattern = Pattern.compile("^\\d{4}");
    Pattern physicianPattern = Pattern.compile("(Prof\\.?|PD|Dr\\.?)\\s");
    Pattern dosageFormPattern = Pattern.compile(
      "(Filmtabl|Tabl|Kaps|Drag|Supp|Inf|Inj|Sirup|Tropfen|Salbe|Gel|Creme|" +
      "L\u00F6sung|Susp|Amp|Retard|Depot)");
    Pattern dosageInstrPattern = Pattern.compile("^\\d+-\\d+-\\d+");

    for (String line : lines) {
      String trimmed = line.trim();
      String lower = trimmed.toLowerCase();

      // AHV number
      if (ahvNumber.isEmpty()) {
        Matcher m = ahvPattern.matcher(trimmed);
        if (m.find()) { ahvNumber = m.group(); }
      }

      // ZSR number
      if (zsrNumber.isEmpty()) {
        Matcher m = zsrPattern.matcher(trimmed);
        if (m.find()) {
          Matcher m2 = zsrExtract.matcher(m.group());
          if (m2.find()) zsrNumber = m2.group();
        }
      }

      // Phone number
      if (patientPhone.isEmpty()) {
        Matcher m = phonePattern.matcher(trimmed);
        if (m.find()) {
          String phone = m.group(1).trim();
          if (phone.length() >= 10) patientPhone = phone;
        }
      }

      // Prescription date
      if (prescriptionDate.isEmpty() && lower.contains("rezept")) {
        Matcher m = datePattern.matcher(trimmed);
        if (m.find()) prescriptionDate = m.group();
      }

      // Patient address
      if (patientStreet.isEmpty() && trimmed.contains(",")) {
        int commaIdx = trimmed.indexOf(',');
        String afterComma = trimmed.substring(commaIdx + 1).trim();
        if (addressPattern.matcher(afterComma).find()) {
          String street = trimmed.substring(0, commaIdx).trim();
          if (street.matches(".*\\d.*") && !street.contains("AHV") &&
              !street.contains("PID") && !street.contains("FID")) {
            patientStreet = street;
            String cleaned = afterComma.replace("CH-", "");
            Matcher plzMatcher = plzPattern.matcher(cleaned);
            if (plzMatcher.find()) {
              patientZip = plzMatcher.group();
              String cityPart = cleaned.substring(4).trim();
              int cityComma = cityPart.indexOf(',');
              patientCity = (cityComma >= 0) ?
                cityPart.substring(0, cityComma).trim() : cityPart;
            }
          }
        }
      }

      // Hospital/Clinic
      if (hospitalName.isEmpty()) {
        if (lower.contains("spital") || lower.contains("klinik") ||
            lower.contains("praxis") || lower.contains("universit")) {
          if (trimmed.length() > 5 && !lower.contains("strasse") &&
              !lower.contains("str.")) {
            hospitalName = trimmed;
          }
        }
      }

      // Department
      if (departmentName.isEmpty()) {
        if (lower.contains("gastroenterologie") || lower.contains("hepatologie") ||
            lower.contains("abteilung") || lower.contains("innere medizin")) {
          if (!lower.contains("chefarzt")) {
            departmentName = trimmed;
          }
        }
      }

      // Physician name
      if (physicianFullName.isEmpty()) {
        if (physicianPattern.matcher(trimmed).find() &&
            !lower.contains("chefarzt") && !lower.contains("abteilung")) {
          physicianFullName = trimmed;
        }
      }

      // Medication detection
      if (dosageFormPattern.matcher(trimmed).find()) {
        if (!lower.contains("aus medizinischen") && !lower.contains("substituieren") &&
            !lower.contains("medikamentenname") && !lower.contains("wirkstoff")) {
          String medName = trimmed.replaceAll("^\\d+\\s*OP\\s*", "").trim();
          if (!medName.isEmpty()) medNames.add(medName);
        }
      }

      // Dosage/instruction
      if (lower.contains("bedarf") || lower.contains("x/d") ||
          lower.matches(".*max\\s+\\d.*") || lower.contains("t\u00E4glich") ||
          dosageInstrPattern.matcher(lower).find()) {
        if (!lower.contains("medikamentenname") && !lower.contains("rezeptierung")) {
          dosages.add(trimmed);
        }
      }
    }

    // Build medication text
    StringBuilder medText = new StringBuilder();
    for (int i = 0; i < medNames.size(); i++) {
      if (medText.length() > 0) medText.append("\n");
      medText.append(medNames.get(i));
      if (i < dosages.size() && !dosages.get(i).isEmpty()) {
        medText.append(" \u2013 ").append(dosages.get(i));
      }
    }

    resultData.putExtra("ahvNumber", ahvNumber);
    resultData.putExtra("zsrNumber", zsrNumber);
    resultData.putExtra("patientStreet", patientStreet);
    resultData.putExtra("patientZip", patientZip);
    resultData.putExtra("patientCity", patientCity);
    resultData.putExtra("patientPhone", patientPhone);
    resultData.putExtra("prescriptionDate", prescriptionDate);
    resultData.putExtra("hospitalName", hospitalName);
    resultData.putExtra("departmentName", departmentName);
    resultData.putExtra("physicianFullName", physicianFullName);
    resultData.putExtra("medications", medText.toString());
    resultData.putExtra("qrPayload", qrPayload != null ? qrPayload : "");

    runOnUiThread(() -> {
      setResult(RESULT_OK, resultData);
      finish();
    });
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (cameraExecutor != null) cameraExecutor.shutdown();
    if (barcodeScanner != null) barcodeScanner.close();
    if (textRecognizer != null) textRecognizer.close();
  }
}
