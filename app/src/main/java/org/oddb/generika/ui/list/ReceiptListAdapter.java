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
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;
import com.daimajia.swipe.SwipeLayout;
import com.daimajia.swipe.implments.SwipeItemAdapterMangerImpl;
import com.daimajia.swipe.interfaces.SwipeAdapterInterface;
import com.daimajia.swipe.interfaces.SwipeItemMangerInterface;
import com.daimajia.swipe.util.Attributes;

import io.realm.OrderedRealmCollection;
import io.realm.RealmList;
import io.realm.RealmBaseAdapter;

import java.util.HashMap;
import java.util.List;

import org.oddb.generika.R;
import org.oddb.generika.model.Receipt;
import org.oddb.generika.util.Constant;


public class ReceiptListAdapter extends RealmBaseAdapter<Receipt>
  implements SwipeItemMangerInterface, SwipeAdapterInterface {
  private static final String TAG = "ReceiptListAdapter";

  protected ListItemListener itemListener;
  protected SwipeItemAdapterMangerImpl itemManager;

  public ReceiptListAdapter(RealmList<Receipt> realmList) {
    super((OrderedRealmCollection<Receipt>)realmList);

    // swipe
    this.itemManager = new SwipeItemAdapterMangerImpl(this);
  }

  public interface ListItemListener {
    abstract void onDelete(String itemId);
  }

  public void setCallback(ListItemListener callback) {
    this.itemListener = callback;
  }

  @Override
  public void updateData(@Nullable OrderedRealmCollection<Receipt> data) {
    super.updateData(data);
    // pass
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
    TextView name;

    // TODO

    ImageView deleteButton;
  }

  // -- swipe action

  // swipe layout wrapper holds custom touch status fields
  public class SwipeRow {
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

    public Receipt item;
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

    // receipt as item
    public void setItem(Receipt item) { this.item = item; }
    public Receipt getItem() { return this.item; }
  }

  @Override
  public int getSwipeLayoutResourceId(int _position) {
    return R.id.receipt_item_row;
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

  // -- view

  @Override
  public View getView(final int position, View convertView, ViewGroup parent) {
    View view = convertView;
    if (view == null) {
      view = generateView(position, parent);
      itemManager.initialize(view, position);
    } else {
      itemManager.updateConvertView(view, position);
    }

    final ViewGroup parentView = parent;

    final SwipeLayout layout = (SwipeLayout)view.findViewById(
      getSwipeLayoutResourceId(position));
    layout.close();
    layout.setShowMode(SwipeLayout.ShowMode.LayDown);

    Receipt item = (Receipt)getItem(position);
    final SwipeRow row = new SwipeRow(layout);
    row.setItem(item);

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

  protected View generateView(int position, ViewGroup parent) {
    return LayoutInflater.from(parent.getContext()).inflate(
        R.layout.activity_main_receipt_row, parent, false);
  }

  private void fillValues(int position, View convertView, ViewGroup parent) {
    View view = convertView;
    Context context = (Context)parent.getContext();

    final Receipt item = (Receipt)getItem(position);
    final String itemId = item.getId();

    // row for receipt
    ViewHolder viewHolder = new ViewHolder();
    // name
    viewHolder.name = (TextView)view.findViewById(
      R.id.receipt_item_name);
    viewHolder.name.setText(item.getName());

    // delete button
    ImageView deleteButton = (ImageView)view.findViewById(
      R.id.receipt_item_delete_button);
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
