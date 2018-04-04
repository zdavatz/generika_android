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
package org.oddb.generika.model;

import android.content.Context;
import android.util.Log;

import io.realm.annotations.PrimaryKey;
import io.realm.annotations.LinkingObjects;
import io.realm.annotations.Required;
import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmList;
import io.realm.RealmResults;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.UUID;


public class Receipt extends RealmObject implements Retryable {
  private final static String TAG = "Receipt";

  public static final String FIELD_ID = "id";

  @PrimaryKey
  private String id; // UUID

  @LinkingObjects("files")
  private final RealmResults<Data> source = null;

  @Required
  private String hashedKey; // prescription_hash

  private String placeDate; // place_date

  private RealmList<Operator> operator;
  private RealmList<Patient> patient;
  private RealmList<Product> medications;

  private String datetime; // importedAt
  private String filepath; // file path of `.amk`
  private String filename; // original filename

  // -- static methods

  public static String generateId() {
    return UUID.randomUUID().toString();
  }

  public static String generateId(Realm realm) {
    String id;
    while (true) {
      id = generateId();
      Receipt receipt = realm.where(Receipt.class)
        .equalTo(FIELD_ID, id).findFirst();
      if (receipt == null) {
        break;
      }
    }
    return id;
  }

  public static void withRetry(final int limit, WithRetry f) {
    Retryable.withRetry(limit, f);
  }

  public static class Amkfile {
    private Context context;
    private String url;

    private String originalName;

    private File file;
    private String content;

    public Amkfile(Context context, String url) {
      this.context = context;
      this.url = url;

      try {
        this.originalName = URLDecoder.decode(
          new File(url).getName(), "UTF-8");
      } catch (UnsupportedEncodingException e) {
        Log.d(TAG, "(Amkfile) exception: " + e.getMessage());
        e.printStackTrace();
        this.originalName = "";
      }
      // local
      // e.g. /path/to/org.oddb.generika/files/amkfiles/RZ-20180404053819.amk
      String filename = String.format(
        "%s-%s.amk", "RZ", Receipt.makeImportedAt(null));
      File directory = new File(
        context.getFilesDir() + File.separator + "amkfiles");
      directory.mkdirs();
      this.file = new File(directory, filename);
    }

    public void setContent(String value) {
      this.content = value;
    }

    public String getPath() {
      if (file != null) {
        return file.getAbsolutePath();
      }
      return null;
    }

    public String getOriginalName() {
      return originalName;
    }

    public boolean save() {
      boolean result = false;
      FileOutputStream outputStream = null;

      try {
        outputStream = new FileOutputStream(file);
        outputStream.write(content.getBytes());
        outputStream.close();
        result = true;
      } catch (Exception e) {
        Log.d(TAG, "(Amkfile.save) exception: " + e.getMessage());
        e.printStackTrace();
        result = false;
      } finally {
        if (outputStream != null) {
          try {
            outputStream.close();
          } catch (IOException e) {
            Log.d(TAG, "(Amkfile.save) exception: " + e.getMessage());
            e.printStackTrace();
          }
        }
        return result;
      }
    }
  }

  // e.g. 20180223210923 in UTC (for saved value)
  public static String makeImportedAt(String filepath_) {
    String importedAt = "";
    if (filepath_ != null) {
      // extract timestamp part from path
      File file = new File(filepath_);
      String filename = file.getName().toString();
      Log.d(TAG, "(makeImportedAt) filename: " + filename);
      if (filename.length() == 32) {
        // RZ:2 + -:1 + TIMESTAMP:14 + .:1 + EXT:3 = 21 (amk)
        importedAt = filename.substring(3, 17);
      }
    }
    // normally datetime_ is set from filename
    if (importedAt.equals("")) {
      Calendar calendar = Calendar.getInstance();
      SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
      formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
      importedAt = formatter.format(calendar.getTime());
    }
    return importedAt;
  }

  // NOTE: it must be called in realm transaction & try/catch block
  public static Receipt insertNewReceiptIntoSource(
    Realm realm,
    Receipt receipt, Operator operator, Patient patient, Product[] medications,
    Data data, boolean withUniqueCheck) {
    try {
      RealmList<Receipt> files = data.getFiles();

      String id;
      if (withUniqueCheck) {
        id = generateId(realm);
      } else {
        id = generateId();
      }

      // id will be random generated UUID
      Receipt file = realm.createObject(Receipt.class, id);
      String filepath_ = receipt.getFilepath();
      file.setHashedKey(receipt.getHashedKey());
      file.setPlaceDate(receipt.getPlaceDate());
      file.setFilepath(filepath_);
      file.setFilename(receipt.getFilename());
      file.setDatetime(makeImportedAt(filepath_));

      // each objects below must have id and it must be managed object
      operator.setId(operator.generateId(realm));
      final Operator managedOperator = realm.copyToRealm(operator);
      file.setOperator(managedOperator);

      patient.setId(patient.generateId(realm));
      final Patient managedPatient = realm.copyToRealm(patient);
      file.setPatient(managedPatient);

      RealmList<Product> medicationList = file.getMedications();
      for (int i = 0; i < medications.length; i++)  {
        Product product = medications[i];
        product.setId(product.generateId(realm));
        final Product managedProduct = realm.copyToRealm(product);
        medicationList.add(0, managedProduct);
      }

      // insert (receipt, operator, patient, products) to first
      // on list as source
      files.add(0, file);
      return file;
    } catch (Exception e) {
      Log.d(TAG, "(insertNewReceiptIntoSource) exception: " + e.getMessage());
      Log.d(TAG, Log.getStackTraceString(e));
      return null;
    }
  }

  // -- instance methods

  public Operator getOperator() {
    if (operator != null) {
      // raises if this object is not managed yet
      return operator.where().findFirst();
    } else {
      return null;
    }
  }
  public void setOperator(Operator operator_) {
    if (operator == null) {
      // copyable list
      this.operator = new RealmList<Operator>();
    }
    operator.add(0, operator_);
  }

  public Patient getPatient() {
    if (patient != null) {
      // raises if this object is not managed yet
      return patient.where().findFirst();
    } else {
      return null;
    }
  }
  public void setPatient(Patient patient_) {
    if (patient == null) {
      this.patient = new RealmList<Patient>();
    }
    patient.add(0, patient_);
  }

  public RealmList<Product> getMedications() { return medications; }
  // no setter for medications

  public String getId() { return id; }
  public void setId(String value) { this.id = value; } // UUID

  public Data getSource() {
    if (source == null) { return null; }
    return source.where().findFirst();
  }

  public String getHashedKey() { return hashedKey; }
  public void setHashedKey(String hashedKey_) { this.hashedKey = hashedKey_; }

  public String getPlaceDate() { return placeDate; }
  public void setPlaceDate(String placeDate_) { this.placeDate = placeDate_; }

  public String getDatetime() { return datetime; }
  public void setDatetime(String value) { this.datetime = value; }

  public String getFilepath() { return filepath; }
  public void setFilepath(String value) { this.filepath = value; }

  public String getFilename() { return filename; }
  public void setFilename(String value) { this.filename = value; }

  // TODO
  public String getIssuedDate() { return ""; } // (via place_date)
  public String getIssuedPlace() { return ""; } // (via place_date)

  // if this method returns `false`, `realm.cancelTransaction()` should be
  // called.
  public boolean delete() {
    boolean deleted = false;

    File amkfile = null;
    if (filepath != null) {
      File file = new File(filepath);
      if (file.exists()) {
        amkfile = file;
      }
    }
    try {
      deleteFromRealm();
      deleted = true;
    } catch (IllegalStateException e) {
      deleted = false;
    }
    if (amkfile != null) {
      deleted = amkfile.delete();
    }
    return deleted;
  }
}
