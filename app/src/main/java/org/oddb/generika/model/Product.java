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

import android.util.Log;

import io.realm.annotations.PrimaryKey;
import io.realm.annotations.LinkingObjects;
import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmList;
import io.realm.RealmResults;

import org.json.JSONObject;
import org.json.JSONException;

import java.io.File;
import java.lang.IllegalAccessException;
import java.lang.IllegalArgumentException;
import java.lang.NoSuchMethodException;
import java.lang.SecurityException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.TimeZone;
import java.util.UUID;

import org.oddb.generika.util.Constant;


/**
 * Ths product class represents 2 type medical products, scanned drug and
 * prescribed medication.
 *
 * The prescribed medication belongs to the Receipt object.
 */
public class Product extends RealmObject implements Retryable {
  private final static String TAG = "Product";

  public static final String FIELD_ID = "id";

  @PrimaryKey
  private String id; // UUID

  @LinkingObjects("items")
  private final RealmResults<Data> source = null;

  private static final Pattern regPtrn = Pattern.compile(
    "^7680(\\d{5}).+");
  private static final Pattern packPtrn = Pattern.compile(
    "^7680\\d{5}(\\d{3}).+");

  private String ean;

  // parsed partial numbers from EAN13 (ean)
  private String reg;
  private String pack;

  // scannedAt (drug) / importedAt (medication)
  private String datetime;
  // barcode image (drug) / amk prescription file (medication)
  private String filepath;

  // expiry date (verfalldatum)
  private String expiresAt;

  private String name;

  // -- scanned product (drug)
  // (values from oddb api)
  private String seq;
  private String size;
  private String deduction;
  private String price;
  private String category;

  // -- prescribed product (medication)
  // (values from .amk file)
  private String atc;
  private String owner;
  private String comment;

  // -- static methods

  public static String generateId() {
    return UUID.randomUUID().toString();
  }

  public static String generateId(Realm realm) {
    String id;
    while (true) {
      id = generateId();
      Product product = realm.where(Product.class)
        .equalTo(FIELD_ID, id).findFirst();
      if (product == null) {
        break;
      }
    }
    return id;
  }

  private static HashMap<String, String> productkeyMap() {
    // property, importing key (.amk, api)
    HashMap<String, String> keyMap = new HashMap<String, String>();
    keyMap.put("ean", "eancode");
    keyMap.put("reg", "regnrs");
    keyMap.put("pack", "package");
    keyMap.put("name", "product_name");
    // (scanned drug)
    keyMap.put("seq", "seq");
    keyMap.put("size", "size");
    keyMap.put("deduction", "deduction");
    keyMap.put("price", "price");
    keyMap.put("category", "category");
    // (prescribed medication)
    keyMap.put("atc", "atccode");
    keyMap.put("owner", "owner");
    keyMap.put("comment", "comment");
    return keyMap;
  }

  // without id
  public static Product newInstanceFromJSON(JSONObject json) throws
    SecurityException, NoSuchMethodException, IllegalArgumentException,
    IllegalAccessException, InvocationTargetException {
    Product product = new Product();

    Class c = Product.class;
    Class[] parameterTypes = new Class[]{String.class};
    HashMap<String, String> keyMap = productkeyMap();
    for (String key: keyMap.keySet()) {
      String value;
      try {
        value = json.getString(keyMap.get(key));
      } catch (JSONException _e) {
        value = null;
      }
      if (value != null && value != "" && value != "null")  {
        String methodName = "set" +
          key.substring(0, 1).toUpperCase() + key.substring(1);
        Method method = c.getDeclaredMethod(methodName, parameterTypes);
        method.invoke(product, new Object[]{value});
      }
    }
    return product;
  }

  public static void withRetry(final int limit, WithRetry f) {
    Retryable.withRetry(limit, f);
  }

  // e.g. 20180223210923 in UTC (for saved value)
  public static String makeExpiresAt(Date date) {
    String expiresAt = "";
    if (date != null) {
      SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
      formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
      expiresAt = formatter.format(date);
    }
    return expiresAt;
  }

  // -- scanned product classes and methods

  public static class Barcode {
    String value;
    String filepath;

    String expiresAt; // GS1 DataMatrix only

    public Barcode() {}

    // See BarcodeExtractor
    public Barcode(HashMap<String, String> parseResult) {

      // e.g. 0768.... (GTIN 14) -> 768 (EAN 13)
      String barcodeValue = parseResult.get(Constant.GS1_DM_AI_GTIN);
      barcodeValue = barcodeValue.replaceAll("^0768", "768");
      this.value = barcodeValue;

      // e.g. 210600 -> 210601
      String expiryDate = parseResult.get(
        Constant.GS1_DM_AI_EXPIRY_DATE);
      expiryDate = expiryDate.replaceAll("00$", "01");

      if (expiryDate != null) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("yyMMdd");
        try {
          cal.setTime(formatter.parse(expiryDate));
          this.expiresAt = makeExpiresAt(cal.getTime());
        } catch (ParseException e) {
          // skip
          Log.d(TAG, "(Barcode) exception: " + e.getMessage());
        }
      }
      Log.d(TAG, "(Barcode) value: " + value);
      Log.d(TAG, "(Barcode) expiresAt: " + expiresAt);
    }

    public void setValue(String value_) {
      this.value = value_;
    }

    public void setFilepath(String filepath_) {
      this.filepath = filepath_;
    }
  }

  // e.g. 20180223210923 in UTC (for saved value)
  public static String makeScannedAt(String filepath_) {
      String scannedAt = "";
      if (filepath_ != null) {
        // extract timestamp part from path
        File file = new File(filepath_);
        String filename = file.getName().toString();
        Log.d(TAG, "(makeScannedAt) filename: " + filename);
        if (filename.length() == 32) {
          // EAN:13 + -:1 + TIMESTAMP:14 + .:1 + EXT:3 = 32 (jpg)
          scannedAt = filename.substring(14, 28);
        }
      }
      // normally datetime_ is set from filename
      if (scannedAt.equals("")) {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        scannedAt = formatter.format(calendar.getTime());
      }
      return scannedAt;
  }

  // NOTE: it must be called in realm transaction & try/catch block
  public static Product insertNewBarcodeIntoSource(
    Realm realm,
    Product.Barcode barcode, Data data, boolean withUniqueCheck) {
    RealmList<Product> items = data.getItems();

    String id;
    if (withUniqueCheck) {
      id = generateId(realm);
    } else {
      id = generateId();
    }
    // id will be random generated UUID
    Product item = realm.createObject(Product.class, id);
    item.setEan(barcode.value);
    item.setFilepath(barcode.filepath);
    item.setDatetime(makeScannedAt(barcode.filepath));

    // GS1 DataMatrix
    Log.d(TAG, "(insertNewBarcodeIntoSource) expiresAt: " + barcode.expiresAt);
    if (barcode.expiresAt != null) {
      item.setExpiresAt(barcode.expiresAt);
    }

    // insert item to first on list as source
    items.add(0, item);
    return item;
  }

  // -- prescribed product classes and methods


  // -- instance methods

  public String getId() { return id; }
  public void setId(String id) { this.id = id; } // UUID

  public Data getSource() {
    if (source == null) { return null; }
    return source.where().findFirst();
  }

  public String getEan() { return ean; }
  public void setEan(String ean) { this.ean = ean; }

  public String getReg() {
    if (reg != null && !reg.equals("")) {
      return reg;
    } else if (ean != null) {
      Matcher m = regPtrn.matcher(ean);
      while (m.find()) {
        String s = m.group(1);
        return s;
      }
    }
    return null;
  }

  public void setReg(String reg) {
  // used only for receipt product (medication)
    this.reg = reg;
  }

  public String getPack() {
    if (pack != null && !pack.equals("")) {
      return pack;
    } else if (pack != null) {
      Matcher m = packPtrn.matcher(ean);
      while (m.find()) {
        String s = m.group(1);
        return s;
      }
    }
    return null;
  }

  public void setPack(String pack) {
    // used only for receipt product (medication)
    this.pack = pack;
  }

  public String getDatetime() { return datetime; }
  public void setDatetime(String datetime) { this.datetime = datetime; }

  public String getFilepath() { return filepath; }
  public void setFilepath(String filepath) { this.filepath = filepath; }

  public String getExpiresAt() { return expiresAt; }
  public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  // (values from oddb)
  public String getSeq() { return seq; }
  public void setSeq(String seq) { this.seq = seq; }

  public String getSize() { return size; }
  public void setSize(String size) { this.size = size; }

  public String getDeduction() { return deduction; }
  public void setDeduction(String deduction) { this.deduction = deduction; }

  public String getPrice() { return price; }
  public void setPrice(String price) { this.price = price; }

  public String getCategory() {
    // It seems that api response contains '&nbsp;' as white space.
    if (category != null) {
      return category.replace("&nbsp;", " ");
    }
    return category;
  }
  public void setCategory(String category) { this.category = category; }

  public String getAtc() { return atc; }
  public void setAtc(String value) { this.atc = value; }

  public String getOwner() { return owner; }
  public void setOwner(String value) { this.owner = value; }

  public String getComment() { return comment; }
  public void setComment(String value) { this.comment = value; }

  // NOTE: it must be called in realm transaction
  public void updateProperties(HashMap<String, String> properties) throws
    SecurityException, NoSuchMethodException, IllegalArgumentException,
    IllegalAccessException, InvocationTargetException {
    // TODO: validate format etc.
    String[] keys = {"seq", "name", "size", "deduction", "price", "category",
                     "expiresAt"};
    // this.getClass() fails :'(
    Class c = Product.class;
    Class[] parameterTypes = new Class[]{String.class};
    for (String key: keys) {
      String value = properties.get(key);
      if (value != null && value != "" && value != "null")  {
        String methodName = "set" +
          key.substring(0, 1).toUpperCase() + key.substring(1);
        Method method = c.getDeclaredMethod(methodName, parameterTypes);
        method.invoke(this, new Object[]{value});
      }
    }
    // extarct values from ean
    this.reg = getReg();
    this.pack = getPack();
  }

  // if this method returns `false`, `realm.cancelTransaction()` should be
  // called.
  public boolean delete() {
    boolean deleted = false;

    File barcode = null;
    if (filepath != null) {
      File file = new File(filepath);
      if (file.exists()) {
        barcode = file;
      }
    }
    try {
      deleteFromRealm();
      deleted = true;
    } catch (IllegalStateException e) {
      deleted = false;
    }
    if (barcode != null) {
      deleted = barcode.delete();
    }
    return deleted;
  }

  // converts as message to application user
  public String toMessage() {
    String unit = "CHF";
    String priceString = "";  // default empty string if price value is unknown
    if (price != null && !price.contains("null")) {
      priceString = String.format("%s: %s", unit, price);
    }
    String name = getName();
    if (name == null) { name = ""; }
    String size = getSize();
    if (size == null) { size = ""; }
    return String.format("%s,\n%s\n%s", name, size, priceString);
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) { return true; }
    if (!(other instanceof Product)) { return false; }

    Product item = (Product)other;
    return ean.equals(item.ean) &&
           datetime.toString().equals(item.datetime.toString());
  }
}
