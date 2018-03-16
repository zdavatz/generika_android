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

import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;
import com.daimajia.swipe.implments.SwipeItemAdapterMangerImpl;
import com.daimajia.swipe.interfaces.SwipeAdapterInterface;
import com.daimajia.swipe.interfaces.SwipeItemMangerInterface;
import com.daimajia.swipe.util.Attributes;

import io.realm.OrderedRealmCollection;
import io.realm.RealmList;
import io.realm.RealmBaseAdapter;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.oddb.generika.MainActivity;
import org.oddb.generika.R;
import org.oddb.generika.model.Product;
import org.oddb.generika.model.ProductItem;
import org.oddb.generika.ui.MonthYearPickerDialogFragment;


public class ProductItemListAdapter extends RealmBaseAdapter<ProductItem>
  implements
    ListAdapter,
    SwipeItemMangerInterface,
    SwipeAdapterInterface {
  private static final String TAG = "ProductItemList";
  private DeleteListener listener;
  private SwipeItemAdapterMangerImpl itemManager;

  Pattern deduction10 = Pattern.compile("\\A\\s*10\\s*%\\z");
  Pattern deduction20 = Pattern.compile("\\A\\s*20\\s*%\\z");

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

  // swipe layout wrapper holds custom touch status fields
  private class SwipeRow {
    // touch directions
    public final static int kRighttoLeft = -1;
    public final static int kNone= 0;
    public final static int kLefttoRight = 1;

    // duration as range of touch event for swipe action
    public final HashMap<String, Integer> kSwipeDurationThreshold =
      new HashMap<String, Integer>() {{ // will be checked with uptimeMillis
        put("min", 600);
        put("max", 1024);
      }};

    // custom fields
    private int touchDown = 0;
    private int touchDirection = kNone;
    private long touchStartedAt = 0;
    private boolean hasDialog = false;

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

    final ViewGroup parentView = parent;
    final ProductItemListAdapter adapter = this;

    final SwipeLayout layout = (SwipeLayout)view.findViewById(
      getSwipeLayoutResourceId(position));
    layout.close();
    layout.setShowMode(SwipeLayout.ShowMode.LayDown);

    final SwipeRow row = new SwipeRow(layout);
    // handle other touch events
    row.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View view, MotionEvent event) {
        long startedAt = row.getTouchStartedAt();
        long duration = event.getEventTime() - startedAt;
        int direction = row.getTouchDirection();

        switch (event.getActionMasked()) {
          case MotionEvent.ACTION_UP:
            Log.d(TAG, "(onTouch/up) startedAt: " + startedAt);
            Log.d(TAG, "(onTouch/up) direction: " + direction);
            row.setTouchDirection(SwipeRow.kNone);
            // fix consistency. This means down has been started when swipe
            // layout state is fixed as "close", not "open".
            if (row.getTouchDown() != 1) { return false; }
            row.setTouchDown(0);
            if (row.getState().equals("Close")) {
              // it's tap (not swipe). up/down event must be fire both in
              // closed row
              if (duration > row.kSwipeDurationThreshold.get("max")) {
                if (row.hasDialog()) { return false; }
                Log.d(TAG, "(onTouch/up) long press, duration: " + duration);
                row.setHasDialog(true);
                // dialog does not exist yet on ACTION_MOVE
                Context context = parentView.getContext();
                showMonthYearPickerDialog(row ,context);
                return true;
              }
              Log.d(TAG, "(onTouch/up) short tap, duration: " + duration);
              // short (single) tap
              ProductItem productItem = (ProductItem)getItem(position);
              if ((productItem == null || productItem.getEan() == null) ||
                  (productItem.getEan().equals("EAN 13"))) { // placeholder
                return false;  // unexpected
              }
              // TODO: re-consider it might be not good (usage: MainActivity)
              ((MainActivity)parentView.getContext()).openWebView(
                productItem);
              return true;
            } else { // swipe state "middle"
              // smooth swipe assistance, use this instead of swipelistener
              if (duration >= row.kSwipeDurationThreshold.get("min") &&
                  duration <= row.kSwipeDurationThreshold.get("max")) {
                // don't use `toggle`, because it works oppositely here.
                // just support user's intention.
                if (direction == SwipeRow.kRighttoLeft) {
                  row.open(true);
                } else if (direction == SwipeRow.kLefttoRight) {
                  row.close(true);
                }
                return true;
              }
            }
            return false;
          case MotionEvent.ACTION_DOWN:
            String state = row.getState();
            if (state.equals("Close")) {
              row.setTouchDown(1); // swipe/click start on closed row
              row.setTouchStartedAt(event.getEventTime());
              row.setTouchDirection(SwipeRow.kRighttoLeft);
            } else if (state.equals("Open")) {
              row.setTouchDown(0);
              row.setTouchStartedAt(event.getEventTime());
              row.setTouchDirection(SwipeRow.kLefttoRight);
            }
            Log.d(TAG, "(onTouch/down) startedAt: " + row.getTouchStartedAt());
            Log.d(TAG, "(onTouch/down) direction: " + row.getTouchDirection());
            return false;
          case MotionEvent.ACTION_MOVE:
            // NOTE:
            // `ACTION_MOVE` is invoked often while keeping touch, because it
            // is not possible to keep touching same place by finger. But on
            // some devices this event is invoked very often or not invoked at
            // all. See below:
            //
            // - https://developer.android.com/reference/android/view/\
            //     MotionEvent.html
            if (row.getEdge().equals("Right") &&
                row.getState().equals("Close")) {
              // onLongClickListener responds, too quickly. So just
              // do it here or action up (depends on the type of device).
              if (duration > row.kSwipeDurationThreshold.get("max") &&
                  !row.hasDialog()) {
                Log.d(TAG, "(onTouch/move) long press, duration: " + duration);
                row.setHasDialog(true);
                Context context = parentView.getContext();
                showMonthYearPickerDialog(row ,context);
                return true;
              }
            }
            return false;
          default:
            Log.d(TAG, "action: " + event.getActionMasked());
            row.setTouchDown(0);
            row.setTouchStartedAt(0);
            row.setTouchDirection(SwipeRow.kNone);
            return false;
        }
      }
    });

    fillValues(position, view, parent);
    return view;
  }

  private void showMonthYearPickerDialog(SwipeRow row, Context context) {
    MonthYearPickerDialogFragment fragment =
      new MonthYearPickerDialogFragment(
        context.getString(R.string.expiry_date));
    fragment.setListener(
      new MonthYearPickerDialogFragment.OnChangeListener() {
      @Override
      public void onDateSet(
        DatePicker view, int year, int month, int _dayOfMonth) {
        row.setHasDialog(false);
        // TODO
      }
      @Override
      public void onCancel(DatePicker view) {
        row.setHasDialog(false);
      }
    });
    fragment.show(((MainActivity)context)
      .getSupportFragmentManager(),
      "MonthYearPickerDialogFragment");
  }

  public View generateView(int position, ViewGroup parent) {
    return LayoutInflater.from(parent.getContext()).inflate(
        R.layout.activity_main_row, parent, false);
  }

  public void fillValues(int position, View convertView, ViewGroup parent) {
    View view = convertView;
    Context context = (Context)parent.getContext();

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
    String deductionValue = item.getDeduction();
    viewHolder.deduction.setText(deductionValue);
    int deductionColor = ContextCompat.getColor(context, R.color.textColor);
    if (deductionValue != null && !deductionValue.equals("")) {
      Matcher m10 = deduction10.matcher(deductionValue);
      if (m10.find()) {
        deductionColor = ContextCompat.getColor(context, R.color.colorPrimary);
      } else {
        Matcher m20 = deduction20.matcher(deductionValue);
        if (m20.find()) {
          deductionColor = ContextCompat.getColor(
            context, R.color.colorAccent);
        } else {
        }
      }
    }
    viewHolder.deduction.setTextColor(deductionColor);
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
          View view = listView.getChildAt(i - startPosition);
          // `getView()` is `listView.getAdapter().getView()`
          View row = getView(i, view, listView);
          row.setVisibility(View.VISIBLE);
          break;
        }
      }
    }
  }
}
