/*
 *  Generika Android
 *  Copyright (C) 2024-2026 ywesee GmbH
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

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmList;

import org.oddb.generika.data.AmikoDBManager;
import org.oddb.generika.data.DataManager;
import org.oddb.generika.model.AmikoDBRow;
import org.oddb.generika.model.AmikoDBPackage;
import org.oddb.generika.model.Operator;
import org.oddb.generika.model.Patient;
import org.oddb.generika.model.Product;
import org.oddb.generika.model.Receipt;
import org.oddb.generika.util.Constant;


public class KostengutspracheActivity extends BaseActivity {
  private static final String TAG = "KostengutspracheAct";

  private static final int REQUEST_INSURANCE_SCAN = 2001;
  private static final int REQUEST_PRESCRIPTION_SCAN = 2002;

  private Receipt receipt;
  private Realm realm;

  // Patient fields
  private EditText patientNameField;
  private EditText patientFirstNameField;
  private EditText patientBirthDateField;
  private RadioGroup genderGroup;
  private RadioButton genderFemale;
  private RadioButton genderMale;
  private EditText patientStreetField;
  private EditText patientZipCityField;
  private EditText patientAhvField;

  // Insurance fields
  private EditText insurerNameField;
  private EditText insurerNumberField;

  // Diagnosis
  private RadioGroup diagnosisGroup;
  private RadioButton diagnosisCrohn;
  private RadioButton diagnosisColitis;

  // Medication
  private EditText medicationText;

  // Physician fields
  private EditText physicianNameField;
  private EditText physicianFirstNameField;
  private EditText physicianZsrField;
  private EditText physicianHospitalField;
  private EditText physicianDepartmentField;

  // Date
  private EditText dateField;

  private ActivityResultLauncher<Intent> insuranceScanLauncher;
  private ActivityResultLauncher<Intent> prescriptionScanLauncher;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_kostengutsprache);

    realm = Realm.getDefaultInstance();

    DataManager dataManager = new DataManager(Constant.SOURCE_TYPE_AMKJSON);
    String hashedKey = getIntent().getStringExtra(Constant.kHashedKey);
    if (hashedKey != null) {
      this.receipt = dataManager.getReceiptByHashedKey(hashedKey);
    }

    initViews();
    registerLaunchers();
    prefillFromReceipt();
  }

  private void registerLaunchers() {
    insuranceScanLauncher = registerForActivityResult(
      new ActivityResultContracts.StartActivityForResult(),
      result -> {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
          applyInsuranceCardResult(result.getData());
        }
      });

    prescriptionScanLauncher = registerForActivityResult(
      new ActivityResultContracts.StartActivityForResult(),
      result -> {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
          applyPrescriptionScanResult(result.getData());
        }
      });
  }

  private void initViews() {
    Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
    toolbar.setTitle(getString(R.string.kostengutsprache));
    setSupportActionBar(toolbar);

    ActionBar actionBar = getSupportActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setDisplayShowHomeEnabled(true);

    // Patient
    patientNameField = findViewById(R.id.kg_patient_name);
    patientFirstNameField = findViewById(R.id.kg_patient_first_name);
    patientBirthDateField = findViewById(R.id.kg_patient_birth_date);
    genderGroup = findViewById(R.id.kg_gender_group);
    genderFemale = findViewById(R.id.kg_gender_female);
    genderMale = findViewById(R.id.kg_gender_male);
    patientStreetField = findViewById(R.id.kg_patient_street);
    patientZipCityField = findViewById(R.id.kg_patient_zip_city);
    patientAhvField = findViewById(R.id.kg_patient_ahv);

    // Insurance
    insurerNameField = findViewById(R.id.kg_insurer_name);
    insurerNumberField = findViewById(R.id.kg_insurer_number);

    // Diagnosis
    diagnosisGroup = findViewById(R.id.kg_diagnosis_group);
    diagnosisCrohn = findViewById(R.id.kg_diagnosis_crohn);
    diagnosisColitis = findViewById(R.id.kg_diagnosis_colitis);

    // Medication
    medicationText = findViewById(R.id.kg_medication_text);

    // Physician
    physicianNameField = findViewById(R.id.kg_physician_name);
    physicianFirstNameField = findViewById(R.id.kg_physician_first_name);
    physicianZsrField = findViewById(R.id.kg_physician_zsr);
    physicianHospitalField = findViewById(R.id.kg_physician_hospital);
    physicianDepartmentField = findViewById(R.id.kg_physician_department);

    // Date
    dateField = findViewById(R.id.kg_date);

    // Buttons
    ImageButton scanPrescriptionBtn = findViewById(R.id.kg_scan_prescription_btn);
    scanPrescriptionBtn.setOnClickListener(v -> {
      Intent intent = new Intent(this, PrescriptionScannerActivity.class);
      prescriptionScanLauncher.launch(intent);
    });

    ImageButton scanInsuranceBtn = findViewById(R.id.kg_scan_insurance_btn);
    scanInsuranceBtn.setOnClickListener(v -> {
      Intent intent = new Intent(this, InsuranceCardScannerActivity.class);
      insuranceScanLauncher.launch(intent);
    });
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    menu.add(0, 1, 0, getString(R.string.kg_pdf_email))
      .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      saveFormToReceipt();
      finishAfterTransition();
      return true;
    }
    if (item.getItemId() == 1) {
      generateAndSharePDF();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onBackPressed() {
    saveFormToReceipt();
    super.onBackPressed();
  }

  private void prefillFromReceipt() {
    if (receipt == null) return;

    Patient patient = receipt.getPatient();
    if (patient != null) {
      setTextIfNotEmpty(patientNameField, patient.getFamilyName());
      setTextIfNotEmpty(patientFirstNameField, patient.getGivenName());
      setTextIfNotEmpty(patientBirthDateField, patient.getBirthDate());

      String gender = patient.getGender();
      if (gender != null && !gender.isEmpty()) {
        String g = gender.toLowerCase();
        if (g.equals("f") || g.equals("w") || g.equals("woman") || g.equals("2")) {
          genderFemale.setChecked(true);
        } else if (g.equals("m") || g.equals("man") || g.equals("1")) {
          genderMale.setChecked(true);
        }
      }

      setTextIfNotEmpty(patientStreetField, patient.getAddress());
      String zip = patient.getZipcode();
      String city = patient.getCity();
      if ((zip != null && !zip.isEmpty()) || (city != null && !city.isEmpty())) {
        patientZipCityField.setText(
          ((zip != null ? zip : "") + " " + (city != null ? city : "")).trim());
      }
      setTextIfNotEmpty(patientAhvField, patient.getAhvNumber());
      setTextIfNotEmpty(insurerNameField, patient.getInsurerName());
      setTextIfNotEmpty(insurerNumberField, patient.getHealthCardNumber());
    }

    Operator operator = receipt.getOperator();
    if (operator != null) {
      setTextIfNotEmpty(physicianNameField, operator.getFamilyName());
      setTextIfNotEmpty(physicianFirstNameField, operator.getGivenName());
      setTextIfNotEmpty(physicianZsrField, operator.getZsrNumber());
    }

    // Medications
    RealmList<Product> medications = receipt.getMedications();
    if (medications != null) {
      StringBuilder medText = new StringBuilder();
      for (Product product : medications) {
        String name = product.getName();
        String pack = product.getPack();
        String ean = product.getEan();

        if (pack != null && !pack.isEmpty()) {
          name = pack;
        } else if ((name == null || name.isEmpty()) && ean != null && !ean.isEmpty()) {
          String dbName = lookupMedNameByGTIN(ean);
          if (!dbName.isEmpty()) {
            name = dbName;
          }
        }

        if (name == null || name.isEmpty()) {
          name = (ean != null && !ean.isEmpty()) ? ean : "?";
        }

        if (medText.length() > 0) medText.append("\n");
        medText.append(name);

        String comment = product.getComment();
        if (comment != null && !comment.isEmpty()) {
          medText.append(" \u2013 ").append(comment);
        }
      }
      medicationText.setText(medText.toString());
    }

    // Diagnosis
    String diagnosis = receipt.getDiagnosis();
    if (diagnosis != null) {
      if (diagnosis.equals("crohn")) {
        diagnosisCrohn.setChecked(true);
      } else if (diagnosis.equals("colitis")) {
        diagnosisColitis.setChecked(true);
      }
    }

    // Date
    SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    dateField.setText(fmt.format(new Date()));
  }

  private String lookupMedNameByGTIN(String gtin) {
    if (gtin == null || gtin.length() != 13) return "";
    AmikoDBManager amikoDb = AmikoDBManager.getInstance(this);
    if (amikoDb == null) return "";
    java.util.ArrayList<AmikoDBRow> rows = amikoDb.findWithGtin(gtin, "");
    if (rows == null || rows.isEmpty()) return "";
    AmikoDBRow row = rows.get(0);
    // Parse packages string to find matching GTIN
    if (row.packages != null && !row.packages.isEmpty()) {
      String[] pkgStrings = row.packages.split("\n");
      for (String pkgStr : pkgStrings) {
        if (pkgStr.contains("|")) {
          try {
            AmikoDBPackage pkg = new AmikoDBPackage(pkgStr, row);
            if (gtin.equals(pkg.gtin)) {
              return (pkg.name != null && !pkg.name.isEmpty()) ? pkg.name : row.title;
            }
          } catch (Exception e) {
            // skip malformed package string
          }
        }
      }
    }
    return row.title != null ? row.title : "";
  }

  private void saveFormToReceipt() {
    if (receipt == null) return;

    realm.executeTransaction(r -> {
      // Patient
      Patient patient = receipt.getPatient();
      if (patient == null) return;

      patient.setFamilyName(getText(patientNameField));
      patient.setGivenName(getText(patientFirstNameField));
      patient.setBirthDate(getText(patientBirthDateField));

      if (genderFemale.isChecked()) {
        patient.setGender("F");
      } else if (genderMale.isChecked()) {
        patient.setGender("M");
      }

      patient.setAddress(getText(patientStreetField));
      String zipCity = getText(patientZipCityField);
      String[] parts = zipCity.split(" ", 2);
      if (parts.length >= 1) patient.setZipcode(parts[0]);
      if (parts.length >= 2) patient.setCity(parts[1]);

      patient.setAhvNumber(getText(patientAhvField));
      patient.setInsurerName(getText(insurerNameField));
      patient.setHealthCardNumber(getText(insurerNumberField));

      // Operator
      Operator operator = receipt.getOperator();
      if (operator != null) {
        operator.setFamilyName(getText(physicianNameField));
        operator.setGivenName(getText(physicianFirstNameField));
        operator.setZsrNumber(getText(physicianZsrField));
      }

      // Diagnosis
      if (diagnosisCrohn.isChecked()) {
        receipt.setDiagnosis("crohn");
      } else if (diagnosisColitis.isChecked()) {
        receipt.setDiagnosis("colitis");
      }
    });
  }

  private void applyInsuranceCardResult(Intent data) {
    String cardNumber = data.getStringExtra("cardNumber");
    String insuranceName = data.getStringExtra("insuranceName");
    String familyName = data.getStringExtra("familyName");
    String givenName = data.getStringExtra("givenName");
    String dateString = data.getStringExtra("dateString");
    String sexString = data.getStringExtra("sexString");
    String ahvNumber = data.getStringExtra("ahvNumber");

    if (cardNumber != null)
      insurerNumberField.setText(cardNumber);
    if (insuranceName != null && !insuranceName.isEmpty())
      insurerNameField.setText(insuranceName);

    if (isEmpty(patientNameField) && familyName != null)
      patientNameField.setText(familyName);
    if (isEmpty(patientFirstNameField) && givenName != null)
      patientFirstNameField.setText(givenName);
    if (isEmpty(patientBirthDateField) && dateString != null)
      patientBirthDateField.setText(dateString);

    if (genderGroup.getCheckedRadioButtonId() == -1) {
      if ("F".equals(sexString)) genderFemale.setChecked(true);
      else if ("M".equals(sexString)) genderMale.setChecked(true);
    }

    if (isEmpty(patientAhvField) && ahvNumber != null && !ahvNumber.isEmpty())
      patientAhvField.setText(ahvNumber);
  }

  private void applyPrescriptionScanResult(Intent data) {
    // AHV number
    String ahvNumber = data.getStringExtra("ahvNumber");
    if (isEmpty(patientAhvField) && ahvNumber != null && !ahvNumber.isEmpty())
      patientAhvField.setText(ahvNumber);

    // Patient address
    String street = data.getStringExtra("patientStreet");
    if (isEmpty(patientStreetField) && street != null && !street.isEmpty())
      patientStreetField.setText(street);

    String zip = data.getStringExtra("patientZip");
    String city = data.getStringExtra("patientCity");
    if (isEmpty(patientZipCityField) && (zip != null || city != null)) {
      String zipCity = ((zip != null ? zip : "") + " " + (city != null ? city : "")).trim();
      if (!zipCity.isEmpty()) patientZipCityField.setText(zipCity);
    }

    // Physician
    String physicianName = data.getStringExtra("physicianFullName");
    if (physicianName != null && !physicianName.isEmpty()) {
      String fullName = physicianName;
      String[] titlePatterns = {"Prof.", "PD", "Dr.", "med.", "Dr"};
      for (String pattern : titlePatterns) {
        fullName = fullName.replace(pattern, "").trim();
      }
      fullName = fullName.replaceAll("\\s+", " ").trim();
      String[] nameParts = fullName.split(" ");
      if (nameParts.length >= 2) {
        if (isEmpty(physicianFirstNameField)) {
          StringBuilder firstName = new StringBuilder();
          for (int i = 0; i < nameParts.length - 1; i++) {
            if (firstName.length() > 0) firstName.append(" ");
            firstName.append(nameParts[i]);
          }
          physicianFirstNameField.setText(firstName.toString());
        }
        if (isEmpty(physicianNameField))
          physicianNameField.setText(nameParts[nameParts.length - 1]);
      }
    }

    String zsrNumber = data.getStringExtra("zsrNumber");
    if (isEmpty(physicianZsrField) && zsrNumber != null && !zsrNumber.isEmpty())
      physicianZsrField.setText(zsrNumber);

    String hospital = data.getStringExtra("hospitalName");
    if (isEmpty(physicianHospitalField) && hospital != null && !hospital.isEmpty())
      physicianHospitalField.setText(hospital);

    String department = data.getStringExtra("departmentName");
    if (isEmpty(physicianDepartmentField) && department != null && !department.isEmpty())
      physicianDepartmentField.setText(department);

    // Medications
    String medications = data.getStringExtra("medications");
    if (medications != null && !medications.isEmpty()) {
      String currentText = medicationText.getText().toString().trim();
      if (currentText.isEmpty()) {
        medicationText.setText(medications);
      }
    }

    // Prescription date
    String prescriptionDate = data.getStringExtra("prescriptionDate");
    if (prescriptionDate != null && !prescriptionDate.isEmpty()) {
      String[] parts = prescriptionDate.split("\\.");
      if (parts.length == 3) {
        dateField.setText(parts[2] + "-" + parts[1] + "-" + parts[0]);
      }
    }
  }

  // -- PDF Generation --

  private void generateAndSharePDF() {
    PdfDocument document = new PdfDocument();
    PdfDocument.PageInfo pageInfo =
      new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4
    PdfDocument.Page page = document.startPage(pageInfo);
    Canvas canvas = page.getCanvas();

    float margin = 40;
    float w = pageInfo.getPageWidth() - 2 * margin;
    float y = margin;

    // Title
    Paint titlePaint = new Paint();
    titlePaint.setTextSize(16);
    titlePaint.setFakeBoldText(true);
    titlePaint.setColor(Color.BLACK);
    canvas.drawText(getString(R.string.kostengutsprache_pdf_title), margin, y + 16, titlePaint);
    y += 22;

    Paint subtitlePaint = new Paint();
    subtitlePaint.setTextSize(12);
    subtitlePaint.setColor(Color.DKGRAY);
    canvas.drawText(getString(R.string.kostengutsprache_ibd), margin, y + 12, subtitlePaint);
    y += 28;

    // Patient
    y = drawSectionHeader(canvas, getString(R.string.kg_patient_section), margin, y, w);
    y = drawRow(canvas, getString(R.string.kg_name) + ":", getText(patientNameField), margin, y, w);
    y = drawRow(canvas, getString(R.string.kg_first_name) + ":", getText(patientFirstNameField), margin, y, w);
    y = drawRow(canvas, getString(R.string.kg_birth_date) + ":", getText(patientBirthDateField), margin, y, w);
    String genderText = genderFemale.isChecked() ? "weiblich" :
                         genderMale.isChecked() ? "m\u00E4nnlich" : "";
    y = drawRow(canvas, "Geschlecht:", genderText, margin, y, w);
    y = drawRow(canvas, getString(R.string.kg_street) + ":", getText(patientStreetField), margin, y, w);
    y = drawRow(canvas, getString(R.string.kg_zip_city) + ":", getText(patientZipCityField), margin, y, w);
    y = drawRow(canvas, getString(R.string.kg_ahv_number) + ":", getText(patientAhvField), margin, y, w);
    y += 10;

    // Insurance
    y = drawSectionHeader(canvas, getString(R.string.kg_insurance_section), margin, y, w);
    y = drawRow(canvas, getString(R.string.kg_insurer_name) + ":", getText(insurerNameField), margin, y, w);
    y = drawRow(canvas, getString(R.string.kg_insurer_number) + ":", getText(insurerNumberField), margin, y, w);
    y += 10;

    // Diagnosis
    y = drawSectionHeader(canvas, getString(R.string.kg_diagnosis_section), margin, y, w);
    String diagnosis = diagnosisCrohn.isChecked() ? "Morbus Crohn" :
                        diagnosisColitis.isChecked() ? "Colitis ulcerosa" : "";
    y = drawRow(canvas, "Diagnose:", diagnosis, margin, y, w);
    y += 10;

    // Medication
    y = drawSectionHeader(canvas, getString(R.string.kg_medication_section), margin, y, w);
    y = drawMultiline(canvas, medicationText.getText().toString(), margin, y, w);
    y += 10;

    // Physician
    y = drawSectionHeader(canvas, "Behandelnder " + getString(R.string.kg_physician_section), margin, y, w);
    y = drawRow(canvas, getString(R.string.kg_name) + ":", getText(physicianNameField), margin, y, w);
    y = drawRow(canvas, getString(R.string.kg_first_name) + ":", getText(physicianFirstNameField), margin, y, w);
    y = drawRow(canvas, getString(R.string.kg_zsr_number) + ":", getText(physicianZsrField), margin, y, w);
    y = drawRow(canvas, getString(R.string.kg_hospital) + ":", getText(physicianHospitalField), margin, y, w);
    y = drawRow(canvas, getString(R.string.kg_department) + ":", getText(physicianDepartmentField), margin, y, w);
    y += 10;

    y = drawRow(canvas, getString(R.string.kg_date) + ":", getText(dateField), margin, y, w);

    document.finishPage(page);

    // Save to file and share
    try {
      File cacheDir = new File(getCacheDir(), "pdfs");
      cacheDir.mkdirs();
      File pdfFile = new File(cacheDir, "Kostengutsprache_KVV71.pdf");
      FileOutputStream fos = new FileOutputStream(pdfFile);
      document.writeTo(fos);
      fos.close();
      document.close();

      Uri uri = FileProvider.getUriForFile(this,
        getPackageName() + ".fileprovider", pdfFile);

      Intent shareIntent = new Intent(Intent.ACTION_SEND);
      shareIntent.setType("application/pdf");
      shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
      String patientFullName = (getText(patientFirstNameField) + " " +
        getText(patientNameField)).trim();
      shareIntent.putExtra(Intent.EXTRA_SUBJECT,
        "Kostengutsprache KVV 71 \u2013 " + patientFullName);
      shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      startActivity(Intent.createChooser(shareIntent, getString(R.string.kg_pdf_email)));
    } catch (IOException e) {
      Log.e(TAG, "Error generating PDF", e);
      Toast.makeText(this, "Error generating PDF", Toast.LENGTH_SHORT).show();
    }
  }

  private float drawSectionHeader(Canvas canvas, String text, float x, float y, float w) {
    Paint headerPaint = new Paint();
    headerPaint.setTextSize(11);
    headerPaint.setFakeBoldText(true);
    headerPaint.setColor(Color.DKGRAY);
    canvas.drawText(text, x, y + 11, headerPaint);

    Paint linePaint = new Paint();
    linePaint.setColor(Color.GRAY);
    linePaint.setStrokeWidth(0.5f);
    float lineY = y + 15;
    canvas.drawLine(x, lineY, x + w, lineY, linePaint);
    return lineY + 4;
  }

  private float drawRow(Canvas canvas, String label, String value,
                         float x, float y, float w) {
    float labelW = 130;
    Paint labelPaint = new Paint();
    labelPaint.setTextSize(10);
    labelPaint.setColor(Color.DKGRAY);

    Paint valuePaint = new Paint();
    valuePaint.setTextSize(10);
    valuePaint.setColor(Color.BLACK);

    canvas.drawText(label, x, y + 10, labelPaint);
    canvas.drawText(value != null ? value : "", x + labelW, y + 10, valuePaint);
    return y + 14;
  }

  private float drawMultiline(Canvas canvas, String text, float x, float y, float w) {
    Paint paint = new Paint();
    paint.setTextSize(10);
    paint.setColor(Color.BLACK);

    if (text == null || text.isEmpty()) return y;
    String[] lines = text.split("\n");
    for (String line : lines) {
      canvas.drawText(line, x, y + 10, paint);
      y += 14;
    }
    return y;
  }

  private String getText(EditText field) {
    return field.getText().toString().trim();
  }

  private boolean isEmpty(EditText field) {
    return field.getText().toString().trim().isEmpty();
  }

  private void setTextIfNotEmpty(EditText field, String value) {
    if (value != null && !value.isEmpty() && !value.equals("null")) {
      field.setText(value);
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (realm != null) {
      realm.close();
    }
  }
}
