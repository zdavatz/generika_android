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
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;
import io.realm.RealmList;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.oddb.generika.MainActivity;
import org.oddb.generika.R;
import org.oddb.generika.model.ProductItem;
import org.oddb.generika.ui.MonthYearPickerDialogFragment;
import org.oddb.generika.ui.list.BaseProductItemListAdapter;
import org.oddb.generika.util.Constant;


public class ScannedProductItemListAdapter extends BaseProductItemListAdapter {
  private static final String TAG = "ScannedProductItemListAdapter";

  private final Pattern deduction10 = Pattern.compile("\\A\\s*10\\s*%\\z");
  private final Pattern deduction20 = Pattern.compile("\\A\\s*20\\s*%\\z");

  public ScannedProductItemListAdapter(RealmList<ProductItem> realmList) {
    super(realmList);
  }

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
              ProductItem item = row.getProductItem();
              if ((item == null || item.getEan() == null) ||
                  (item.getEan().equals(Constant.INIT_DATA.get("ean")))) {
                // placeholder row
                ((MainActivity)parentView.getContext()).openWebView(null);
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
    // extract month and year
    String expiresAt = ProductItem.getLocalDateAs(
      row.item.getExpiresAt(), expiresAtFormat);
    String[] dateFields = {"", ""};
    if (expiresAt.contains(".")) {
      dateFields = expiresAt.split("\\.", 2);
    }
    if (dateFields.length != 2) { return; }
    int month, year;
    MonthYearPickerDialogFragment fragment;
    try {
      month = Integer.parseInt(dateFields[0]);
      year = Integer.parseInt(dateFields[1]);
      fragment = MonthYearPickerDialogFragment.newInstance(month, year);
    } catch (NumberFormatException e) {
      fragment = MonthYearPickerDialogFragment.newInstance();
    }
    fragment.setTitle(context.getString(R.string.expiry_date));
    fragment.setListener(
      new MonthYearPickerDialogFragment.OnChangeListener() {
      @Override
      public void onDateSet(
        DatePicker view, int year, int month, int _dayOfMonth) {
        row.setHasDialog(false);
        ProductItem item = row.getProductItem();
        if (itemListener != null && item != null) {
          Calendar cal = Calendar.getInstance();  // local time zone
          cal.set(Calendar.YEAR, year);
          cal.set(Calendar.MONTH, month - 1);
          cal.set(Calendar.DAY_OF_MONTH, 1);
          itemListener.onExpiresAtChange(item.getId(), cal.getTime());
        }
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
    viewHolder.datetime.setText(
      ProductItem.getLocalDateAs(item.getDatetime(), "HH:mm dd.MM.YYYY"));

    // price
    viewHolder.price = (TextView)view.findViewById(
      R.id.scanned_product_item_price);
    viewHolder.price.setText(item.getPrice());
    // deduction
    viewHolder.deduction = (TextView)view.findViewById(
      R.id.scanned_product_item_deduction);
    String deductionValue = item.getDeduction();
    viewHolder.deduction.setText(deductionValue);
    viewHolder.deduction.setTextColor(
      composeDeductionTextColor(deductionValue, context));
    // category
    viewHolder.category = (TextView)view.findViewById(
      R.id.scanned_product_item_category);
    viewHolder.category.setText(item.getCategory());

    // ean
    viewHolder.ean = (TextView)view.findViewById(
      R.id.scanned_product_item_ean);
    viewHolder.ean.setText(item.getEan());
    // expiresAt
    String expiresAtValue = ProductItem.getLocalDateAs(
      item.getExpiresAt(), expiresAtFormat);
    viewHolder.expiresAt = (TextView)view.findViewById(
      R.id.scanned_product_item_expires_at);
    viewHolder.expiresAt.setText(expiresAtValue);
    viewHolder.expiresAt.setTextColor(
      composeExpiresAtTextColor(expiresAtValue, context));

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
      Log.d(TAG, "(composeDeductionTextColor) now: " + now);
      Log.d(TAG, "(composeDeductionTextColor) expiresAt: " + expiresAt);
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
