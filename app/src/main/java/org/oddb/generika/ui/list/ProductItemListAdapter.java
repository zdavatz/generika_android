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
import io.realm.RealmList;
import io.realm.RealmBaseAdapter;
import com.daimajia.swipe.SwipeLayout;
import com.daimajia.swipe.implments.SwipeItemAdapterMangerImpl;
import com.daimajia.swipe.interfaces.SwipeAdapterInterface;
import com.daimajia.swipe.interfaces.SwipeItemMangerInterface;
import com.daimajia.swipe.util.Attributes;

import java.io.File;
import java.util.List;

import org.oddb.generika.model.Product;
import org.oddb.generika.model.ProductItem;
import org.oddb.generika.R;


public class ProductItemListAdapter extends RealmBaseAdapter<ProductItem>
  implements
    ListAdapter,
    SwipeItemMangerInterface, SwipeAdapterInterface {
  private static final String TAG = "ProductItemList";
  private DeleteListener listener;
  private SwipeItemAdapterMangerImpl itemManager;

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
    void delete(String productId);
  }

  public ProductItemListAdapter(RealmList<ProductItem> realmList) {
    super((OrderedRealmCollection<ProductItem>)realmList);

    // swipe
    this.itemManager = new SwipeItemAdapterMangerImpl(this);
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

  @Override
  public View getView(
    final int position, View convertView, ViewGroup parent) {

    View view = convertView;
    if (view == null) {
      view = generateView(position, parent);
      itemManager.initialize(view, position);
    } else {
      itemManager.updateConvertView(view, position);
    }

    SwipeLayout swipeLayout = (SwipeLayout)view.findViewById(
      R.id.scanned_product_item_row);
    swipeLayout.close();
    swipeLayout.setShowMode(SwipeLayout.ShowMode.LayDown);

    fillValues(position, view);

    return view;
  }

  public View generateView(int position, ViewGroup parent) {
    return LayoutInflater.from(parent.getContext()).inflate(
        R.layout.activity_main_row, parent, false);
  }

  public void fillValues(int position, View convertView) {
    View view = convertView;

    final ProductItem item = (ProductItem)getItem(position);
    final String itemId = item.getId();

    // row for scanned product item
    ViewHolder viewHolder = new ViewHolder();

    // barcode image
    viewHolder.barcodeImage = (ImageView)view.findViewById(
      R.id.scanned_product_item_barcode_image);
    String filepath = item.getFilepath();
    if (filepath != null) {
      File imageFile = new File(filepath);
      if (imageFile.exists()) {
        Log.d(TAG, "(getView) filepath: " + filepath);
        viewHolder.barcodeImage.setImageResource(0);
        viewHolder.barcodeImage.setImageURI(Uri.fromFile(imageFile));
      }
    }

    // name
    viewHolder.name = (TextView)view.findViewById(
      R.id.scanned_product_item_name);
    viewHolder.name.setText(item.getName());

    // size
    viewHolder.size = (TextView)view.findViewById(
      R.id.scanned_product_item_size);
    viewHolder.size.setText(item.getSize());
    // datetime
    viewHolder.datetime = (TextView)view.findViewById(
      R.id.scanned_product_item_datetime);
    viewHolder.datetime.setText(item.getLocalDatetimeAs("HH:mm dd.MM.YYYY"));

    // price
    viewHolder.price = (TextView)view.findViewById(
      R.id.scanned_product_item_price);
    viewHolder.price.setText(item.getPrice());
    // deduction
    viewHolder.deduction = (TextView)view.findViewById(
      R.id.scanned_product_item_deduction);
    viewHolder.deduction.setText(item.getDeduction());
    // category
    viewHolder.category = (TextView)view.findViewById(
      R.id.scanned_product_item_category);
    viewHolder.category.setText(item.getCategory());

    // ean
    viewHolder.ean = (TextView)view.findViewById(
      R.id.scanned_product_item_ean);
    viewHolder.ean.setText(item.getEan());

    // temporary delete button
    ImageView deleteButton = (ImageView)view.findViewById(
      R.id.scanned_product_item_delete_button);
    deleteButton.setTag(itemId);
    deleteButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        listener.delete((String)view.getTag());
      }
    });
    viewHolder.deleteButton = deleteButton;
    viewHolder.deleteButton.setTag(itemId);

    view.setTag(viewHolder);
  }

  // -- SwipeLayout

  @Override
  public int getSwipeLayoutResourceId(int _position) {
    return R.id.scanned_product_item_row;
  }

  @Override
  public void openItem(int position) {
    itemManager.openItem(position);
  }

  @Override
  public void closeItem(int position) {
    itemManager.closeItem(position);
  }

  @Override
  public void closeAllExcept(SwipeLayout layout) {
    itemManager.closeAllExcept(layout);
  }

  @Override
  public void closeAllItems() {
    itemManager.closeAllItems();
  }

  @Override
  public List<Integer> getOpenItems() {
    return itemManager.getOpenItems();
  }

  @Override
  public List<SwipeLayout> getOpenLayouts() {
    return itemManager.getOpenLayouts();
  }

  @Override
  public void removeShownLayouts(SwipeLayout layout) {
    itemManager.removeShownLayouts(layout);
  }

  @Override
  public boolean isOpen(int position) {
    return itemManager.isOpen(position);
  }

  @Override
  public Attributes.Mode getMode() {
    return itemManager.getMode();
  }

  @Override
  public void setMode(Attributes.Mode mode) {
    itemManager.setMode(mode);
  }

  // -- utils

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
