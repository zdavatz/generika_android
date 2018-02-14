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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import io.realm.OrderedRealmCollection;
import io.realm.RealmBaseAdapter;

import org.oddb.generika.model.ProductItem;
import org.oddb.generika.R;


public class ProductItemListAdapter extends RealmBaseAdapter<ProductItem>
  implements ListAdapter {

    // TODO
    private DeleteListener mListener;

    private static class ViewHolder {
      TextView title;
      TextView description;
      ImageView deleteButton;
    }

    public void setCallback(DeleteListener callback) {
      mListener = callback;
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

      ViewHolder viewHolder;
      if (convertView == null) {
        convertView = LayoutInflater.from(parent.getContext()).inflate(
          R.layout.activity_main_row, parent, false);

        viewHolder = new ViewHolder();
        viewHolder.title = (TextView)convertView.findViewById(
          R.id.title);
        viewHolder.description = (TextView)convertView.findViewById(
          R.id.description);

        ImageView deleteButton = (ImageView)convertView.findViewById(
          R.id.deleteButton);
        deleteButton.setTag(getItemId(position));
        deleteButton.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            mListener.delete((long) view.getTag());
          }
        });
        viewHolder.deleteButton = deleteButton;

        convertView.setTag(viewHolder);
      } else {
        viewHolder = (ViewHolder)convertView.getTag();

        viewHolder.deleteButton.setTag(getItemId(position));
      }

      // TODO: (for now) set values as ean
      viewHolder.title.setText(getItem(position).getEan());
      viewHolder.description.setText(getItem(position).getEan());
      return convertView;
    }
}
