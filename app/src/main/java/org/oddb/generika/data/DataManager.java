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
package org.oddb.generika.data;

import android.util.Log;

import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;

import java.util.HashMap;

import org.oddb.generika.model.Data;
import org.oddb.generika.model.Product;
import org.oddb.generika.model.Receipt;
import org.oddb.generika.util.Constant;


public class DataManager {
  private static final String TAG = "DataManager";

  private Data data;
  private Realm realm;

  public DataManager(String sourceType) {
    this.realm = Realm.getDefaultInstance();

    bindDataBySourceType(sourceType);
  }

  public void release() {
    try {
      if (realm != null) {
        realm.close();
      }
    } finally {
      this.data = null;
      this.realm = null;
    }
  }

  /**
   * Binds/Re-binds data instance.
   *
   * This method should be called in initialization or after context switched.
   *
   * @param String sourceType "scanned" or "receipt"
   * @return void
   */
  public void bindDataBySourceType(String sourceType) {
    this.data = realm.where(Data.class)
      .equalTo("sourceType", sourceType).findFirst();
  }

  public void preparePlaceholder() {
    if (data == null) { return; }  // TODO: should raise exception

    Product.withRetry(2, new Product.WithRetry() {
      @Override
      public void execute(final int currentCount) {
        insertPlaceholder(currentCount == 1);
      }
    });
  }

  private  void insertPlaceholder(boolean withUniqueCheck) {
    if (data == null) { return; }  // TODO: should raise exception

    realm.beginTransaction();
    // placeholder
    Product.Barcode barcode = new Product.Barcode();
    barcode.setValue(Constant.INIT_DATA.get("ean"));

    // TODO: translation
    Product product = Product.insertNewBarcodeIntoSource(
      realm, barcode, data, withUniqueCheck);
    product.setName(Constant.INIT_DATA.get("name"));
    product.setSize(Constant.INIT_DATA.get("size"));
    product.setDatetime(Constant.INIT_DATA.get("datetime"));
    product.setPrice(Constant.INIT_DATA.get("price"));
    product.setDeduction(Constant.INIT_DATA.get("deduction"));
    product.setCategory(Constant.INIT_DATA.get("category"));
    product.setExpiresAt(Constant.INIT_DATA.get("expiresAt"));
    realm.commitTransaction();
  }

  // -- Product

  public Product getProductById(String id) {
    if (data == null) { return null; }  // TOD: should raise exception

    return data.getItems().where()
      .equalTo(Product.FIELD_ID, id).findFirst();
  }

  public RealmList<Product> getProducts() {
    //if (data == null) { return null; }  // TOD: should raise exception

    return data.getItems();
  }

  public RealmResults<Product> findProductsByNameOrEan(String query) {
    if (data == null) { return null; }  // TOD: should raise exception

    RealmResults<Product> products;
    realm.beginTransaction();
    // insensitive wors only for latin-1 chars
    products = data.getItems()
      .where()
      .contains("name", query, Case.INSENSITIVE)
      .or()
      .contains("ean", query)
      .findAll();
    realm.commitTransaction();
    return products;
  }

  public void addProduct(final Product.Barcode barcode) {
    if (data == null) { return; }  // TOD: should raise exception

    Product.withRetry(2, new Product.WithRetry() {
      @Override
      public void execute(final int currentCount) {
        Log.d(TAG, "(addProduct/execute) currentCount: " + currentCount);
        final Data data_ = data;
        realm.executeTransaction(new Realm.Transaction() {
          @Override
          public void execute(Realm realm_) {
            Product.insertNewBarcodeIntoSource(
              realm_, barcode, data_, (currentCount == 1));
          }
        });
      }
    });
  }

  public void updateProduct(String productId, final HashMap properties) {
    final String id = productId;

    realm.executeTransaction(new Realm.Transaction() {
      @Override
      public void execute(Realm _realm) {
        Product product = data.getItems().where().equalTo(
          "id", id).findFirst();
        if (product != null) {
          try {
            if (product.isValid()) {
              // TODO: create alert dialog for failure?
              product.updateProperties(properties);
            }
          } catch (Exception e) {
            Log.d(TAG, "(updateProductITem) Update error: " +
                  e.getMessage());
          }
        }
      }
    });
  }

  public void deleteProduct(String productId) {
    final String id = productId;

    realm.executeTransaction(new Realm.Transaction() {
      @Override
      public void execute(Realm _realm) {
        Product product = data.getItems().where().equalTo(
          "id", id).findFirst();
        if (product != null) {
          // TODO: create alert dialog for failure?
          product.delete();
        }
      }
    });
  }

  // -- Receipt

  public void addReceipt(final Receipt.Amkfile amkfile) {
    if (data == null) { return; }  // TOD: should raise exception

    Receipt.withRetry(2, new Receipt.WithRetry() {
      @Override
      public void execute(final int currentCount) {
        Log.d(TAG, "(addReceipt/execute) currentCount: " + currentCount);
        final Data data_ = data;
        realm.executeTransaction(new Realm.Transaction() {
          @Override
          public void execute(Realm realm_) {
            Receipt.insertNewAmkfileIntoSource(
              realm_, amkfile, data_, (currentCount == 1));
          }
        });
      }
    });
  }
}
