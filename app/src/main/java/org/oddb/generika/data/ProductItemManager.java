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

import org.oddb.generika.model.Product;
import org.oddb.generika.model.ProductItem;
import org.oddb.generika.util.Constant;


public class ProductItemManager {
  private static final String TAG = "ProductItemManager";

  private Product product;
  private Realm realm;

  public ProductItemManager(String sourceType) {
    this.realm = Realm.getDefaultInstance();

    bindProductBySourceType(sourceType);
  }

  public void release() {
    try {
      if (realm != null) {
        realm.close();
      }
    } finally {
      this.product = null;
      this.realm = null;
    }
  }

  /**
   * Binds/Re-binds product instance.
   *
   * This method should be called in initialization or after product switched.
   *
   * @param String sourceType "scanned" or "receipt"
   * @return void
   */
  public void bindProductBySourceType(String sourceType) {
    this.product = realm.where(Product.class)
      .equalTo("sourceType", sourceType).findFirst();
  }

  public ProductItem getProductItemById(String id) {
      return realm.where(ProductItem.class)
        .equalTo(ProductItem.FIELD_ID, id).findFirst();
  }

  public RealmList<ProductItem> getProductItems() {
    if (product == null) { return null; }  // TOD: should raise exception

    return product.getItems();
  }

  public RealmResults<ProductItem> findProductItemsByNameOrEan(String query) {
    if (product == null) { return null; }  // TOD: should raise exception

    RealmResults<ProductItem> data;
    realm.beginTransaction();
    // insensitive wors only for latin-1 chars
    data = product.getItems()
      .where()
      .contains("name", query, Case.INSENSITIVE)
      .or()
      .contains("ean", query)
      .findAll();
    realm.commitTransaction();
    return data;
  }

  public void preparePlaceholder() {
    if (product == null) { return; }  // TODO: should raise exception

    ProductItem.withRetry(2, new ProductItem.WithRetry() {
      @Override
      public void execute(final int currentCount) {
        insertPlaceholder(currentCount == 1);
      }
    });
  }

  private  void insertPlaceholder(boolean withUniqueCheck) {
    if (product == null) { return; }  // TODO: should raise exception

    realm.beginTransaction();
    // placeholder
    ProductItem.Barcode barcode = new ProductItem.Barcode();
    barcode.setValue(Constant.INIT_DATA.get("ean"));

    // TODO: translation
    ProductItem item = ProductItem.insertNewBarcodeItemIntoSource(
      realm, barcode, product, withUniqueCheck);
    item.setName(Constant.INIT_DATA.get("name"));
    item.setSize(Constant.INIT_DATA.get("size"));
    item.setDatetime(Constant.INIT_DATA.get("datetime"));
    item.setPrice(Constant.INIT_DATA.get("price"));
    item.setDeduction(Constant.INIT_DATA.get("deduction"));
    item.setCategory(Constant.INIT_DATA.get("category"));
    item.setExpiresAt(Constant.INIT_DATA.get("expiresAt"));
    realm.commitTransaction();
  }

  public void addProductItem(final ProductItem.Barcode barcode) {
    if (product == null) { return; }  // TOD: should raise exception

    ProductItem.withRetry(2, new ProductItem.WithRetry() {
      @Override
      public void execute(final int currentCount) {
        Log.d(TAG, "(addProductItem/execute) currentCount: " + currentCount);
        final Product product_ = product;
        realm.executeTransaction(new Realm.Transaction() {
          @Override
          public void execute(Realm realm_) {
            ProductItem.insertNewBarcodeItemIntoSource(
              realm_, barcode, product_, (currentCount == 1));
          }
        });
      }
    });
  }

  public void updateProductItem(
    String productItemId, final HashMap properties) {
    final String id = productItemId;

    realm.executeTransaction(new Realm.Transaction() {
      @Override
      public void execute(Realm realm_) {
        ProductItem productItem = realm_.where(ProductItem.class)
          .equalTo("id", id).findFirst();
        if (productItem != null) {
          try {
            if (productItem.isValid()) {
              // TODO: create alert dialog for failure?
              productItem.updateProperties(properties);
            }
          } catch (Exception e) {
            Log.d(TAG, "(updateProductITem) Update error: " +
                  e.getMessage());
          }
        }
      }
    });
  }

  public void deleteProductItem(String productItemId) {
    final String id = productItemId;

    realm.executeTransaction(new Realm.Transaction() {
      @Override
      public void execute(Realm realm_) {
        ProductItem productItem = realm_.where(ProductItem.class)
          .equalTo("id", id).findFirst();
        if (productItem != null) {
          // TODO: create alert dialog for failure?
          productItem.delete();
        }
      }
    });
  }
}
