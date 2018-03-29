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
  }
}
