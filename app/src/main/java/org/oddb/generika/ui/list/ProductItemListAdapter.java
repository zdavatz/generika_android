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

import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import io.realm.OrderedRealmCollection;
import io.realm.RealmBaseAdapter;

import java.io.File;

import org.oddb.generika.model.ProductItem;
import org.oddb.generika.R;


public class ProductItemListAdapter extends RealmBaseAdapter<ProductItem>
  implements ListAdapter {
  private static final String TAG = "ProductItemList";
  private DeleteListener listener;

  private static class ViewHolder {
    ImageView barcodeImage;

    TextView name;

    TextView size;
    TextView datetime;

    TextView price;
    TextView deduction;
    TextView category;

    TextView ean;
    // TODO: valdatum
    //TextView expiresAt;

    ImageView deleteButton;
  }

  public void setCallback(DeleteListener callback) {
    listener = callback;
  }

  public interface DeleteListener {
    void delete(long productId);
  }

  public ProductItemListAdapter(
    OrderedRealmCollection<ProductItem> realmResults) {
    super(realmResults);
  }

  @Override
  public long getItemId(int position) {
    return getItem(position).getId();
  }

  @Override
  public View getView(
    final int position, View convertView, ViewGroup parent) {

    final ProductItem item = (ProductItem)getItem(position);
    final long itemId = item.getId();

    ViewHolder viewHolder;
    if (convertView == null) {
      convertView = LayoutInflater.from(parent.getContext()).inflate(
        R.layout.activity_main_row, parent, false);
      // row for scanned product item
      viewHolder = new ViewHolder();
      viewHolder.barcodeImage = (ImageView)convertView.findViewById(
        R.id.scanned_product_item_barcode_image);
      viewHolder.name = (TextView)convertView.findViewById(
        R.id.scanned_product_item_name);
      viewHolder.size = (TextView)convertView.findViewById(
        R.id.scanned_product_item_size);
      viewHolder.datetime = (TextView)convertView.findViewById(
        R.id.scanned_product_item_datetime);
      viewHolder.price = (TextView)convertView.findViewById(
        R.id.scanned_product_item_price);
      viewHolder.deduction = (TextView)convertView.findViewById(
        R.id.scanned_product_item_deduction);
      viewHolder.category = (TextView)convertView.findViewById(
        R.id.scanned_product_item_category);
      viewHolder.ean = (TextView)convertView.findViewById(
        R.id.scanned_product_item_ean);
      // temporary delete button
      ImageView deleteButton = (ImageView)convertView.findViewById(
        R.id.scanned_product_item_delete_button);
      deleteButton.setTag(itemId);
      deleteButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          listener.delete((long) view.getTag());
        }
      });
      viewHolder.deleteButton = deleteButton;

      convertView.setTag(viewHolder);
    } else {
      viewHolder = (ViewHolder)convertView.getTag();
      // NOTE: ViewHolder will be in re-use, so image must be clear at here
      viewHolder.barcodeImage.setImageResource(0);
      viewHolder.deleteButton.setTag(itemId);
    }

    // barcode image
    String filepath = item.getFilepath();
    if (filepath != null) {
      File imageFile = new File(filepath);
      if (imageFile.exists()) {
        Log.d(TAG, "(getView) filepath: " + filepath);
        viewHolder.barcodeImage.setImageURI(Uri.fromFile(imageFile));
      }
    }
    // texts
    viewHolder.name.setText(item.getName());
    viewHolder.size.setText(item.getSize());
    viewHolder.datetime.setText(item.getDatetime());
    viewHolder.price.setText(item.getPrice());
    viewHolder.deduction.setText(item.getDeduction());
    viewHolder.category.setText(item.getCategory());
    viewHolder.ean.setText(item.getEan());
    return convertView;
  }

  public void refresh(ProductItem productItem, ListView listView) {
    // refresh only row for target item
    int startPosition = listView.getFirstVisiblePosition();
    for (int i = startPosition,
             j = listView.getLastVisiblePosition(); i <= j; i++) {
      if (productItem == listView.getItemAtPosition(i)) {
        Log.d(TAG, "(refresh) item.ean: " + productItem.getEan());
        View view = listView.getChildAt(i - startPosition);
        // `getView()` is `listView.getAdapter().getView()`
        View row = getView(i, view, listView);
        row.setVisibility(View.VISIBLE);
        break;
      }
    }
  }
}
