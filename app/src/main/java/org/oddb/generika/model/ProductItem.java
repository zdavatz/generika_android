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

import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.icu.util.TimeZone;
import android.util.Log;

import io.realm.annotations.PrimaryKey;
import io.realm.annotations.LinkingObjects;
import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmList;
import io.realm.RealmResults;

import java.io.File;
import java.lang.String;
import java.lang.IllegalAccessException;
import java.lang.IllegalArgumentException;
import java.lang.NoSuchMethodException;
import java.lang.SecurityException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


public class ProductItem extends RealmObject {
  private final static String TAG = "ProductItem";

  public static final String FIELD_ID = "id";

  private static AtomicInteger INTEGER_COUNTER = new AtomicInteger(0);

  @PrimaryKey
  private long id;

  @LinkingObjects("items")
  private final RealmResults<Product> source = null;

  private String ean;

  private static final Pattern regPtrn = Pattern.compile(
    "^7680(\\d{5}).+");
  private static final Pattern packPtrn = Pattern.compile(
    "^7680\\d{5}(\\d{3}).+");

  // parsed partial numbers from EAN13 (ean)
  private String reg;
  private String pack;

  // scanned_at/imported_at
  private String datetime;
  // barcode image/amk prescription
  private String filepath;
  // valdatum/expiration
  private String expiresAt; // TODO

  // -- scanner product item (scanner medications)
  // (values from oddb)
  private String seq;
  private String name;
  private String size;
  private String deduction;
  private String price;
  private String category;

  // TODO: receipt (amk)
  // -- receipt product item (receipt medications)
  // (values from receipt)
  // * prescription_hash
  // * title
  // * comment
  // * atc
  // * owner

  private static int increment() {
    return INTEGER_COUNTER.getAndIncrement();
  }

  public long getId() { return id; }
  public void setId(long id) { this.id = id; }

  public RealmResults<Product> getSource() { return source; }

  public String getEan() { return ean; }
  public void setEan(String ean) { this.ean = ean; }

  // no setter (readonly)
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

  // no setter (readonly)
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

  public String getDatetime() { return datetime; }

  // return formatted local datetime string
  public String getLocalDatetimeAs(String formatString) {
    String datetimeString = null;
    try {
      // TODO:
      // TimeZote.getDefaultZone() and Calendar.getInstance().getTimeZone()
      // both will return wrong timezone in Android 6.1 (>= 7.0 OK) :'(

      // from UTC
      SimpleDateFormat inFormatter = new SimpleDateFormat("yyyyMMddHHmmss");
      inFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));

      // localtime
      SimpleDateFormat outFormatter = new SimpleDateFormat(formatString);
      outFormatter.setTimeZone(Calendar.getInstance().getTimeZone());

      datetimeString = outFormatter.format(inFormatter.parse(datetime));
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      return datetimeString;
    }
  }

  public void setDatetime(String datetime) { this.datetime = datetime; }

  public String getFilepath() { return filepath; }
  public void setFilepath(String filepath) { this.filepath = filepath; }

  // (values from oddb)
  public String getSeq() { return seq; }
  public void setSeq(String seq) { this.seq = seq; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

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

  public String getCountString() {
    return Long.toString(id);
  }

  public void updateProperties(HashMap<String, String> properties) throws
    SecurityException, NoSuchMethodException, IllegalArgumentException,
    IllegalAccessException, InvocationTargetException {
    // TODO: validate format etc.
    String[] keys = {"seq", "name", "size", "deduction", "price", "category"};
    // this.getClass() fails :'(
    Class c = ProductItem.class;
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

  public static class Barcode {
    String value;
    String filepath;

    public void setValue(String value_) {
      this.value = value_;
    }
    public void setFilepath(String filepath_) {
      this.filepath = filepath_;
    }
  }

  // e.g. 20180223210923 in UTC
  public static String makeScannedAt(String filepath_) {
      String scannedAt = "";
      if (filepath_ != null) {
        // extract timestapm part from path
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

  // must be called in transaction
  public static ProductItem createFromBarcodeIntoSource(
    Realm realm, ProductItem.Barcode barcode, Product product) {
    RealmList<ProductItem> items = product.getItems();

    ProductItem item = realm.createObject(ProductItem.class, increment());
    item.setEan(barcode.value);
    item.setFilepath(barcode.filepath);
    item.setDatetime(makeScannedAt(barcode.filepath));

    items.add(item);
    return item;
  }
}
