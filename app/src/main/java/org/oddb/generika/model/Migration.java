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

import io.realm.DynamicRealm;
import io.realm.DynamicRealmObject;
import io.realm.FieldAttribute;
import io.realm.RealmMigration;
import io.realm.RealmObjectSchema;
import io.realm.RealmSchema;


public class Migration implements RealmMigration {
  public static final String TAG = "Migration";

  @Override
  public void migrate(
    final DynamicRealm realm, long oldVersion, long newVersion) {
    Log.d(TAG, "(migrate) oldVersion: " + oldVersion);
    Log.d(TAG, "(migrate) newVersion: " + newVersion);

    RealmSchema schema = realm.getSchema();

    /**
     * // Version 0 - 1 v1.0.25 -> v1.0.26 (03.2018)
     *
     * - RENAME class Product -> class Data
     * - CHANGE sourceType values (scanned -> barcode, receipt -> amkjson)
     * - ADD new a class Receipt with fields
     *   (hashedKey, placeDate, givenName, familyName, medications)
     * - ADD new an array property `files` into Data
     */
    if (oldVersion == 0) {
      Log.d(TAG, "(migrate) oldVersion: " + oldVersion);

      schema.rename("Product", "Data");
      schema.rename("ProductItem", "Product");

      schema.get("Data")
        .transform(new RealmObjectSchema.Function() {
          @Override
          public void apply(DynamicRealmObject obj) {
            if (obj.getString("sourceType").equals("scanned")) {
              obj.setString("sourceType", "barcode");
            } else if (obj.getString("sourceType").equals("receipt")) {
              obj.setString("sourceType", "amkjson");
            }
          }
        });

      RealmObjectSchema productSchema = schema.get("Product");

      RealmObjectSchema receiptSchema = schema.create("Receipt")
        .addField("id", String.class, FieldAttribute.PRIMARY_KEY)
        .addField("hashedKey", String.class, FieldAttribute.REQUIRED)
        .addField("placeDate", String.class)
        .addField("givenName", String.class)
        .addField("familyName", String.class)
        .addRealmListField("medications", productSchema)
        .transform(new RealmObjectSchema.Function() {
          @Override
          public void apply(DynamicRealmObject obj) {
            // pass (no new data)
          }
        });

      schema.get("Data")
        .addRealmListField("files", receiptSchema)
        .transform(new RealmObjectSchema.Function() {
          @Override
          public void apply(DynamicRealmObject obj) {
            // pass (no new data)
          }
        });

      oldVersion++;
    }

    /**
     * // Version 1 - 2 v1.0.27 -> v1.0.28 (03.2018)
     *
     * - ADD new fields into Product (atc, owner, comment)
     * - ADD new a class Operator
     * - ADD new a class Patient
     * - DELETE fields from Receipt (givenName, familyName)
     * - ADD new fields into Receipt (datetime, filpath, filename)
     * - SET relations (receipt -> operator, receipt -> patient)
     */
    if (oldVersion == 1) {
      Log.d(TAG, "(migrate) oldVersion: " + oldVersion);

      RealmObjectSchema _productSchema = schema.get("Product")
        .addField("atc", String.class)
        .addField("owner", String.class)
        .addField("comment", String.class);

      RealmObjectSchema operatorSchema = schema.create("Operator")
        .addField("id", String.class, FieldAttribute.PRIMARY_KEY)
        .addField("givenName", String.class)
        .addField("familyName", String.class)
        .addField("title", String.class)
        .addField("email", String.class)
        .addField("phone", String.class)
        .addField("address", String.class)
        .addField("city", String.class)
        .addField("zipcode", String.class)
        .addField("country", String.class)
        .addField("signature", String.class);

      RealmObjectSchema patientSchema = schema.create("Patient")
        .addField("id", String.class, FieldAttribute.PRIMARY_KEY)
        .addField("identifier", String.class)
        .addField("givenName", String.class)
        .addField("familyName", String.class)
        .addField("birthDate", String.class)
        .addField("gender", String.class)
        .addField("weight", String.class)
        .addField("height", String.class)
        .addField("email", String.class)
        .addField("phone", String.class)
        .addField("address", String.class)
        .addField("city", String.class)
        .addField("zipcode", String.class)
        .addField("country", String.class);

      // relations
      RealmObjectSchema receiptSchema = schema.get("Receipt")
        .removeField("givenName")
        .removeField("familyName")
        .addField("datetime", String.class)
        .addField("filepath", String.class)
        .addField("filename", String.class)
        .addRealmListField("operator", operatorSchema)
        .addRealmListField("patient", patientSchema)
        .transform(new RealmObjectSchema.Function() {
          @Override
          public void apply(DynamicRealmObject obj) {
            // pass (no new data)
          }
        });

      oldVersion++;
    }
  }
}
