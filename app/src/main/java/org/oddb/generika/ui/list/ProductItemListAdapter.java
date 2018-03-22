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
package org.oddb.generika.ui.list;

import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;

import io.realm.OrderedRealmCollection;

import java.util.Date;

import org.oddb.generika.model.ProductItem;


public interface ProductItemListAdapter extends ListAdapter {
  public static final String TAG = "ProductItemListAdapter";

  public interface ItemListener {
    abstract void onDelete(String productId);
    abstract void onExpiresAtChange(String productId, Date newDate);
  }

  abstract public void setCallback(ItemListener itemListener);

  abstract public void updateData(
    @Nullable OrderedRealmCollection<ProductItem> data);

  abstract public View getView(
    final int position, View convertView, ViewGroup parent);

  // -- utils

  /**
   * Findes the row (view) for target product item, then refreshes only it.
   *
   * @param ProdutItem productItem target product item instance
   * @param ListView listView
   * @return void
   */
  default public void refresh(ProductItem productItem, ListView listView) {
    // find it in visible range
    int startPosition = listView.getFirstVisiblePosition();
    ProductItem item;
    for (int i = startPosition,
             j = listView.getLastVisiblePosition(); i <= j; i++) {
      if (i < listView.getCount()) {
        try {
          item = (ProductItem)listView.getItemAtPosition(i);
        } catch (ArrayIndexOutOfBoundsException ignore) {
          Log.d(TAG, "(refresh) startPosition: " + startPosition);
          Log.d(TAG, "(refresh) i: " + i);
          Log.d(TAG, "(refresh) j: " + j);
          break;  // listView has already changed?
        }
        if (productItem.equals(item)) {
          Log.d(TAG, "(refresh) item.ean: " + productItem.getEan());
          // TODO: refactor view
          View view = listView.getChildAt(i - startPosition);
          // `getView()` is same as `listView.getAdapter().getView()`
          View row = getView(i, view, listView);
          row.setVisibility(View.VISIBLE);
          break;
        }
      }
    }
  }
}
