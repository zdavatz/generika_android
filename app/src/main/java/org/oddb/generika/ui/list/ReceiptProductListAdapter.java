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

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import io.realm.OrderedRealmCollection;
import io.realm.RealmBaseAdapter;
import io.realm.RealmResults;

import org.oddb.generika.R;
import org.oddb.generika.model.Product;


public class ReceiptProductListAdapter extends RealmBaseAdapter<Product> {
  private static final String TAG = "ReceiptProductListAdapter";

  public ReceiptProductListAdapter(RealmResults<Product> products) {

    super((OrderedRealmCollection<Product>)products);
  }

  @Override
  public boolean hasStableIds() { // default: false
    return false;
  }

  @Override
  public long getItemId(int position) {
    // NOTE:
    // this is same the getItemId() of current RealmBaseAdapter class.
    // Our primary key is String UUID. It's guranteed uniqueness in DB through
    // use of retry block, but `hashCode` is not enough to provide uniqueness
    // here :'(
    //return getItem(position).getId().hashCode();
    // TODO: think another good way
    return position;
  }

  private static class ViewHolder {
    TextView pack;
    TextView ean;
    TextView comment;
  }

  @Override
  public View getView(final int position, View convertView, ViewGroup parent) {
    View view = convertView;
    if (view == null) {
      view = generateView(position, parent);
    }
    fillValues(position, view, parent);
    view.setClickable(false);
    return view;
  }

  protected View generateView(int position, ViewGroup parent) {
    return LayoutInflater.from(parent.getContext()).inflate(
        R.layout.activity_receipt_product_row, parent, false);
  }

  private void fillValues(int position, View convertView, ViewGroup parent) {
    View view = convertView;
    Product item = (Product)getItem(position);

    // row for a receipt product (medication)
    ViewHolder viewHolder = new ViewHolder();
    // pack
    viewHolder.pack = (TextView)view.findViewById(
      R.id.receipt_product_item_pack);
    viewHolder.pack.setText(item.getPack());
    // ean
    viewHolder.ean = (TextView)view.findViewById(
      R.id.receipt_product_item_ean);
    viewHolder.ean.setText(item.getEan());
    // comment
    viewHolder.comment = (TextView)view.findViewById(
      R.id.receipt_product_item_comment);
    viewHolder.comment.setText(item.getComment());

    view.setTag(viewHolder);
  }
}
