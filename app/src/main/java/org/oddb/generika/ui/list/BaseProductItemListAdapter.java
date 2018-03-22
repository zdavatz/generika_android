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
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.daimajia.swipe.SwipeLayout;
import com.daimajia.swipe.implments.SwipeItemAdapterMangerImpl;
import com.daimajia.swipe.interfaces.SwipeAdapterInterface;
import com.daimajia.swipe.interfaces.SwipeItemMangerInterface;
import com.daimajia.swipe.util.Attributes;

import io.realm.OrderedRealmCollection;
import io.realm.RealmList;
import io.realm.RealmResults;
import io.realm.RealmBaseAdapter;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.oddb.generika.R;
import org.oddb.generika.model.ProductItem;
import org.oddb.generika.util.Constant;


abstract public class BaseProductItemListAdapter
  extends RealmBaseAdapter<ProductItem>
  implements
    ProductItemListAdapter,
    SwipeItemMangerInterface,
    SwipeAdapterInterface {
  private static final String TAG = "BaseProductItemListAdapter";

  protected ProductItemListAdapter.ItemListener itemListener;
  protected SwipeItemAdapterMangerImpl itemManager;

  protected final String expiresAtFormat = "MM.yyyy";

  public BaseProductItemListAdapter(RealmList<ProductItem> realmList) {
    super((OrderedRealmCollection<ProductItem>)realmList);

    // swipe
    this.itemManager = new SwipeItemAdapterMangerImpl(this);
  }

  // -- ProductItemListAdapter: ListAdapter
  
  @Override
  public void updateData(@Nullable OrderedRealmCollection<ProductItem> data) {
    super.updateData(data);

    // pass
  }

  @Override
  public void setCallback(ProductItemListAdapter.ItemListener callback) {
    // list
    this.itemListener = callback;
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

  // -- SwipeLayout

  // swipe layout wrapper holds custom touch status fields
  protected class SwipeRow {
    // touch directions
    public final static int kRighttoLeft = -1;
    public final static int kNone= 0;
    public final static int kLefttoRight = 1;

    // duration as range of touch event for swipe action
    public final HashMap<String, Integer> kSwipeDurationThreshold =
      new HashMap<String, Integer>() {{ // will be checked with uptimeMillis
        put("min", Constant.SWIPE_DURATION_MIN);
        put("max", Constant.SWIPE_DURATION_MAX);
      }};

    // custom fields
    private int touchDown = 0;
    private int touchDirection = kNone;
    private long touchStartedAt = 0;
    private boolean hasDialog = false;

    public ProductItem item;
    public SwipeLayout layout;

    SwipeRow(SwipeLayout layout) {
      this.layout = layout;
    }

    // delegate to layout
    public void setOnTouchListener(View.OnTouchListener listener) {
      this.layout.setOnTouchListener(listener);
    }
    public String getState() { return this.layout.getOpenStatus().toString(); }
    public String getEdge() { return this.layout.getDragEdge().toString(); }
    public void open(boolean smooth) { this.layout.open(smooth);}
    public void close(boolean smooth) { this.layout.close(smooth); }

    // accessor methods
    public void setTouchDown(int v) { this.touchDown = v; }
    public int getTouchDown() { return this.touchDown; }

    public void setTouchDirection(int v) { this.touchDirection = v; }
    public int getTouchDirection() { return this.touchDirection; }

    public void setTouchStartedAt(long v) { this.touchStartedAt = v; }
    public long getTouchStartedAt() { return this.touchStartedAt; }

    public void setHasDialog(boolean v) { this.hasDialog = v; }
    public boolean hasDialog() { return this.hasDialog; }

    public void setProductItem(ProductItem item) { this.item = item; }
    public ProductItem getProductItem() { return this.item; }
  }

  @Override
  public View getView(final int position, View convertView, ViewGroup parent) {
    View view = convertView;
    if (view == null) {
      view = generateView(position, parent);
    }
    return view;
  }

  abstract protected View generateView(final int position, ViewGroup parent);

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
}
