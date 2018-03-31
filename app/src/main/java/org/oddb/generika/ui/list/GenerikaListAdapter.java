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
import android.widget.ListAdapter;
import android.widget.ListView;

import io.realm.RealmModel;
import io.realm.RealmResults;

import java.util.Date;

import org.oddb.generika.model.Product;
import org.oddb.generika.model.Receipt;


public interface GenerikaListAdapter extends ListAdapter {

  public interface ListItemListener {
    abstract void onDelete(String itemId);
    abstract void onExpiresAtChange(String itemId, Date newDate);
  }

  abstract public void setCallback(ListItemListener callback);

  // TODO: Refactor
  // USE generics
  abstract public void updateItems(@Nullable Object data);
  default public void refresh(Product product, ListView listView) {};
  default public void refresh(Receipt receipt, ListView listView) {};
}
