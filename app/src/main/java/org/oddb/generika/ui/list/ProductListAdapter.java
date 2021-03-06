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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.ImageView;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;
import com.daimajia.swipe.implments.SwipeItemAdapterMangerImpl;
import com.daimajia.swipe.interfaces.SwipeAdapterInterface;
import com.daimajia.swipe.interfaces.SwipeItemMangerInterface;
import com.daimajia.swipe.util.Attributes;
import com.bumptech.glide.request.target.BitmapImageViewTarget;

import io.realm.OrderedRealmCollection;
import io.realm.RealmResults;
import io.realm.RealmBaseAdapter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.oddb.generika.MainActivity;
import org.oddb.generika.GlideApp;
import org.oddb.generika.R;
import org.oddb.generika.model.Product;
import org.oddb.generika.ui.MonthYearPickerDialog;
import org.oddb.generika.util.Formatter;
import org.oddb.generika.util.Constant;


public class ProductListAdapter extends RealmBaseAdapter<Product>
  implements
    GenerikaListAdapter,
    SwipeItemMangerInterface, SwipeAdapterInterface {
  private static final String TAG = "ProductListAdapter";

  protected GenerikaListAdapter.ListItemListener itemListener;
  protected SwipeItemAdapterMangerImpl itemManager;

  private final Pattern deduction10 = Pattern.compile("\\A\\s*10\\s*%\\z");
  private final Pattern deduction20 = Pattern.compile("\\A\\s*20\\s*%\\z");

  protected final String datetimeFormat = "HH:mm dd.MM.YYYY";
  protected final String expiresAtFormat = "MM.yyyy";

  public ProductListAdapter(RealmResults<Product> products) {
    super((OrderedRealmCollection<Product>)products);

    // swipe
    this.itemManager = new SwipeItemAdapterMangerImpl(this);
  }

  // GenerikaListAdapter

  @Override
  public void setCallback(GenerikaListAdapter.ListItemListener callback) {
    this.itemListener = callback;
  }

  @Override
  public void refreshAll() {
    this.notifyDataSetChanged();
  }

  @Override
  public void updateItems(@Nullable Object data) {
    // NOTE:
    // This `updateData()` method invokes `notifyDataSetChanged()`, after
    // data set. See also below.
    //
    // https://github.com/realm/realm-android-adapters/blob/\
    //   bd22599bbbac33e0f3840e5832a99954dcb128c1/adapters/src/main/java\
    //   /io/realm/RealmBaseAdapter.java#L135
    this.updateData((OrderedRealmCollection<Product>)data);

    // pass
  }

  /**
   * Findes the row (view) for product list item, then refreshes only it.
   *
   * @param Produt product target product item instance
   * @param ListView listView
   * @return void
   */
  @Override
  public void refresh(Product product, ListView listView) {
    // find it in visible range
    int startPosition = listView.getFirstVisiblePosition();
    Product item;
    for (int i = startPosition,
             j = listView.getLastVisiblePosition(); i <= j; i++) {
      if (i < listView.getCount()) {
        try {
          item = (Product)listView.getItemAtPosition(i);
        } catch (ArrayIndexOutOfBoundsException ignore) {
          Log.d(TAG, "(refresh) startPosition: " + startPosition);
          break;  // listView has already changed?
        }
        if (product.equals(item)) {
          Log.d(TAG, "(refresh) item.ean: " + item.getEan());
          View view = listView.getChildAt(i - startPosition);
          // `getView()` is same as `listView.getAdapter().getView()`
          View row = getView(i, view, listView);
          row.setVisibility(View.VISIBLE);
          break;
        }
      }
    }
  }

  // --

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

    public Product item;
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

    // product as item
    public void setItem(Product item) { this.item = item; }
    public Product getItem() { return this.item; }
  }

  @Override
  public int getSwipeLayoutResourceId(int _position) {
    return R.id.product_item_row;
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

  private static class ViewHolder {
    ImageView barcodeImage;

    TextView name;

    TextView size;
    TextView datetime;

    TextView price;
    TextView deduction;
    TextView category;

    TextView ean;
    TextView expiresAt;

    ImageView deleteButton;
  }

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

    Product item = (Product)getItem(position);
    final SwipeRow row = new SwipeRow(layout);
    row.setItem(item);

    // handle other touch events
    row.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View view, MotionEvent event) {
        // TODO: refactor view
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
              // TODO: re-consider it might be not good (usage: MainActivity)
              Product item = (Product)row.getItem();
              if ((item == null || item.getEan() == null) ||
                  (item.getEan().equals(Constant.INIT_DATA.get("ean")))) {
                // placeholder row
                Product dummy = null;
                ((MainActivity)parentView.getContext()).openWebView(dummy);
              } else {
                ((MainActivity)parentView.getContext()).openWebView(item);
              }
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
            Log.d(TAG, "(onTouch/move) action: " + event.getActionMasked());
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
    // extract month and year
    String expiresAt = Formatter.formatAsLocalDate(
      row.item.getExpiresAt(), expiresAtFormat);
    String[] dateFields = {"", ""};
    if (expiresAt.contains(".")) {
      dateFields = expiresAt.split("\\.", 2);
    }
    if (dateFields.length != 2) { return; }
    int month, year;
    MonthYearPickerDialog dialog;
    try {
      month = Integer.parseInt(dateFields[0]);
      year = Integer.parseInt(dateFields[1]);
      dialog = MonthYearPickerDialog.newInstance(month, year);
    } catch (NumberFormatException e) {
      dialog = MonthYearPickerDialog.newInstance();
    }
    dialog.setTitle(context.getString(R.string.expiry_date));
    dialog.setListener(
      new MonthYearPickerDialog.OnChangeListener() {
      @Override
      public void onDateSet(
        DatePicker view, int year, int month, int _dayOfMonth) {
        row.setHasDialog(false);
        Product item = row.getItem();
        if (itemListener != null && item != null) {
          Calendar cal = Calendar.getInstance();  // local time zone
          cal.set(Calendar.YEAR, year);
          cal.set(Calendar.MONTH, month - 1); // fix for picker's index
          cal.set(Calendar.DAY_OF_MONTH, 1);
          itemListener.onExpiresAtChange(item.getId(), cal.getTime());
        }
      }
      @Override
      public void onCancel(DatePicker view) {
        row.setHasDialog(false);
      }
    });
    dialog.show(((MainActivity)context)
      .getSupportFragmentManager(), "MonthYearPickerDialog");
  }

  protected View generateView(int position, ViewGroup parent) {
    return LayoutInflater.from(parent.getContext()).inflate(
        R.layout.activity_main_product_row, parent, false);
  }

  private void fillValues(int position, View convertView, ViewGroup parent) {
    View view = convertView;
    Context context = (Context)parent.getContext();

    final Product item = (Product)getItem(position);
    final String itemId = item.getId();

    // row for product
    ViewHolder viewHolder = new ViewHolder();

    // barcode image
    viewHolder.barcodeImage = (ImageView)view.findViewById(
      R.id.product_item_barcode_image);
    String filepath = item.getFilepath();
    // Log.d(TAG, "(getView) filepath: " + filepath);

    final Resources resources = context.getResources();

    GlideApp.with(context)
      .asBitmap()
      .load(filepath)
      .placeholder(new ColorDrawable(resources.getColor(R.color.lightGray)))
      .centerCrop()
      .into(new BitmapImageViewTarget(viewHolder.barcodeImage) {
        @Override
        protected void setResource(Bitmap resource) {
          RoundedBitmapDrawable image = RoundedBitmapDrawableFactory.create(
            resources, resource);
          image.setCornerRadius(5); // slightly
          viewHolder.barcodeImage.setImageDrawable(image);
        }
      });

    // name
    viewHolder.name = (TextView)view.findViewById(
      R.id.product_item_name);
    viewHolder.name.setText(item.getName());

    // size
    viewHolder.size = (TextView)view.findViewById(
      R.id.product_item_size);
    viewHolder.size.setText(item.getSize());
    // datetime
    String datetime = item.getDatetime();
    if (datetime != null &&
        !datetime.equals(Constant.INIT_DATA.get("datetime"))) {
      datetime = Formatter.formatAsLocalDate(
        datetime, datetimeFormat);
    }
    viewHolder.datetime = (TextView)view.findViewById(
      R.id.product_item_datetime);
    viewHolder.datetime.setText(datetime);

    // price
    viewHolder.price = (TextView)view.findViewById(
      R.id.product_item_price);
    viewHolder.price.setText(item.getPrice());
    // deduction
    viewHolder.deduction = (TextView)view.findViewById(
      R.id.product_item_deduction);
    String deductionValue = item.getDeduction();
    viewHolder.deduction.setText(deductionValue);
    viewHolder.deduction.setTextColor(
      composeDeductionTextColor(deductionValue, context));
    // category
    viewHolder.category = (TextView)view.findViewById(
      R.id.product_item_category);
    viewHolder.category.setText(item.getCategory());

    // ean
    viewHolder.ean = (TextView)view.findViewById(
      R.id.product_item_ean);
    viewHolder.ean.setText(item.getEan());
    // expiresAt
    String expiresAt = item.getExpiresAt();
    if (expiresAt != null &&
        !expiresAt.equals(Constant.INIT_DATA.get("expiresAt"))) {
      expiresAt = Formatter.formatAsLocalDate(
        expiresAt, expiresAtFormat);
    }
    viewHolder.expiresAt = (TextView)view.findViewById(
      R.id.product_item_expires_at);
    viewHolder.expiresAt.setText(expiresAt);
    viewHolder.expiresAt.setTextColor(
      composeExpiresAtTextColor(expiresAt, context));

    // delete button
    ImageView deleteButton = (ImageView)view.findViewById(
      R.id.product_item_delete_button);
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

  private int composeDeductionTextColor(String value, Context context) {
    int color = ContextCompat.getColor(context, R.color.textColor);

    if (value == null || value.equals("")) { return color; }
    Matcher m10 = deduction10.matcher(value);
    if (m10.find()) {
      color = ContextCompat.getColor(context, R.color.colorPrimary);
    } else {
      Matcher m20 = deduction20.matcher(value);
      if (m20.find()) {
        color = ContextCompat.getColor(context, R.color.colorAccent);
      }
    }
    return color;
  }

  private int composeExpiresAtTextColor(String value, Context context) {
    int color = ContextCompat.getColor(context, R.color.textColor);

    Calendar cal = Calendar.getInstance();  // local time zone
    cal.set(Calendar.DAY_OF_MONTH, 1);
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    Date now = cal.getTime();

    if (value == null || value.equals("")) { return color; }
    try {
      // assumes as first day of the month
      SimpleDateFormat formatter = new SimpleDateFormat(
        "dd." + expiresAtFormat);
      Date expiresAt = formatter.parse("01." + value);
      // Log.d(TAG, "(composeDeductionTextColor) now: " + now);
      // Log.d(TAG, "(composeDeductionTextColor) expiresAt: " + expiresAt);
      if (expiresAt.before(now) || expiresAt.equals(now)) {
        color = ContextCompat.getColor(context, R.color.colorAccent);
      } else {
        color = ContextCompat.getColor(context, R.color.colorPrimary);
      }
    } catch(ParseException e) {
      // pass (default color)
    }
    return color;
  }
}
