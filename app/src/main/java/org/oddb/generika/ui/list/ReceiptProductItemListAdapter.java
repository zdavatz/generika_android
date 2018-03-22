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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;
import io.realm.RealmList;

import org.oddb.generika.R;
import org.oddb.generika.model.ProductItem;
import org.oddb.generika.ui.list.BaseProductItemListAdapter;


public class ReceiptProductItemListAdapter extends BaseProductItemListAdapter {
  private static final String TAG = "ReceiptProductItemListAdapter";

  public ReceiptProductItemListAdapter(RealmList<ProductItem> realmList) {
    super(realmList);
  }

  private static class ViewHolder {
    TextView name;

    // TODO

    ImageView deleteButton;
  }

  @Override
  public View getView(final int position, View convertView, ViewGroup parent) {
    View view = convertView;
    if (view == null) {
      view = super.getView(position, convertView, parent);
      itemManager.initialize(view, position);
    } else {
      itemManager.updateConvertView(view, position);
    }

    final ViewGroup parentView = parent;

    final SwipeLayout layout = (SwipeLayout)view.findViewById(
      getSwipeLayoutResourceId(position));
    layout.close();
    layout.setShowMode(SwipeLayout.ShowMode.LayDown);

    ProductItem productItem = (ProductItem)getItem(position);
    final SwipeRow row = new SwipeRow(layout);
    row.setProductItem(productItem);

    // handle other touch events
    row.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View view, MotionEvent event) {
        // TODO
        return false;
      }
    });

    fillValues(position, view, parent);
    return view;
  }

  @Override
  protected View generateView(int position, ViewGroup parent) {
    return LayoutInflater.from(parent.getContext()).inflate(
        R.layout.activity_main_row, parent, false);
  }

  private void fillValues(int position, View convertView, ViewGroup parent) {
    View view = convertView;
    Context context = (Context)parent.getContext();

    final ProductItem item = (ProductItem)getItem(position);
    final String itemId = item.getId();

    // row for receipt product item
    ViewHolder viewHolder = new ViewHolder();
    // name
    viewHolder.name = (TextView)view.findViewById(
      R.id.scanned_product_item_name);
    viewHolder.name.setText(item.getName());

    // delete button
    ImageView deleteButton = (ImageView)view.findViewById(
      R.id.scanned_product_item_delete_button);
    deleteButton.setTag(itemId);
    deleteButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (itemListener != null) {
          itemListener.onDelete((String)view.getTag());
        }
      }
    });
    viewHolder.deleteButton = deleteButton;
    viewHolder.deleteButton.setTag(itemId);

    view.setTag(viewHolder);
  }
}
