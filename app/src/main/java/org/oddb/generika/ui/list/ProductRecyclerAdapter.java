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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;

import io.realm.RealmResults;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.oddb.generika.MainActivity;
import org.oddb.generika.R;
import org.oddb.generika.model.Product;
import org.oddb.generika.ui.MonthYearPickerDialog;
import org.oddb.generika.util.Constant;
import org.oddb.generika.util.Formatter;


/**
 * RecyclerView adapter for Product list - replaces ProductListAdapter.
 * Uses ItemTouchHelper for swipe-to-delete instead of daimajia SwipeLayout.
 */
public class ProductRecyclerAdapter 
    extends RecyclerView.Adapter<ProductRecyclerAdapter.ProductViewHolder>
    implements GenerikaListAdapter, SwipeToDeleteCallback.SwipeAdapter {

    private static final String TAG = "ProductRecyclerAdapter";

    private RealmResults<Product> products;
    private GenerikaListAdapter.ListItemListener itemListener;

    private final Pattern deduction10 = Pattern.compile("\\A\\s*10\\s*%\\z");
    private final Pattern deduction20 = Pattern.compile("\\A\\s*20\\s*%\\z");

    protected final String datetimeFormat = "HH:mm dd.MM.YYYY";
    protected final String expiresAtFormat = "MM.yyyy";

    public ProductRecyclerAdapter(@Nullable RealmResults<Product> products) {
        this.products = products;
    }

    // GenerikaListAdapter interface implementation

    @Override
    public void setCallback(GenerikaListAdapter.ListItemListener callback) {
        this.itemListener = callback;
    }

    @Override
    public void refreshAll() {
        notifyDataSetChanged();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void updateItems(@Nullable Object data) {
        this.products = (RealmResults<Product>) data;
        notifyDataSetChanged();
    }

    /**
     * For RecyclerView, we refresh by position instead of finding in visible range
     */
    public void refreshItem(int position) {
        notifyItemChanged(position);
    }

    // RecyclerView.Adapter implementation

    @Override
    public int getItemCount() {
        return products != null && products.isValid() ? products.size() : 0;
    }

    @Nullable
    public Product getItem(int position) {
        if (products != null && products.isValid() && position >= 0 && position < products.size()) {
            return products.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.activity_main_product_row, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        final Product item = getItem(position);
        if (item == null) return;

        final Context context = holder.itemView.getContext();
        final Resources resources = context.getResources();

        // Barcode image
        String filepath = item.getFilepath();
        Glide.with(context)
            .asBitmap()
            .load(filepath)
            .placeholder(new ColorDrawable(resources.getColor(R.color.lightGray)))
            .centerCrop()
            .into(new BitmapImageViewTarget(holder.barcodeImage) {
                @Override
                protected void setResource(Bitmap resource) {
                    RoundedBitmapDrawable image = RoundedBitmapDrawableFactory.create(
                        resources, resource);
                    image.setCornerRadius(5);
                    holder.barcodeImage.setImageDrawable(image);
                }
            });

        // Name
        holder.name.setText(item.getName());

        // Size
        holder.size.setText(item.getSize());

        // Datetime
        String datetime = item.getDatetime();
        if (datetime != null && !datetime.equals(Constant.INIT_DATA.get("datetime"))) {
            datetime = Formatter.formatAsLocalDate(datetime, datetimeFormat);
        }
        holder.datetime.setText(datetime);

        // Price
        holder.price.setText(item.getPrice());

        // Deduction
        String deductionValue = item.getDeduction();
        holder.deduction.setText(deductionValue);
        holder.deduction.setTextColor(composeDeductionTextColor(deductionValue, context));

        // Category
        holder.category.setText(item.getCategory());

        // EAN
        holder.ean.setText(item.getEan());

        // ExpiresAt
        String expiresAt = item.getExpiresAt();
        if (expiresAt != null && !expiresAt.equals(Constant.INIT_DATA.get("expiresAt"))) {
            expiresAt = Formatter.formatAsLocalDate(expiresAt, expiresAtFormat);
        }
        holder.expiresAt.setText(expiresAt);
        holder.expiresAt.setTextColor(composeExpiresAtTextColor(expiresAt, context));

        // Click listener - tap to display product
        holder.itemView.setOnClickListener(v -> {
            if ((item.getEan() == null) || 
                (item.getEan().equals(Constant.INIT_DATA.get("ean")))) {
                // placeholder row
                ((MainActivity) context).displayProduct(null);
            } else {
                ((MainActivity) context).displayProduct(item);
            }
        });

        // Long click listener - show date picker dialog
        holder.itemView.setOnLongClickListener(v -> {
            showMonthYearPickerDialog(item, context);
            return true;
        });
    }

    /**
     * Called when item is swiped - triggers delete callback
     */
    @Override
    public void onItemSwiped(int position) {
        Product item = getItem(position);
        if (item != null && itemListener != null) {
            itemListener.onDelete(item.getId());
        }
    }

    private void showMonthYearPickerDialog(Product item, Context context) {
        // Extract month and year
        String expiresAt = Formatter.formatAsLocalDate(
            item.getExpiresAt(), expiresAtFormat);
        String[] dateFields = {"", ""};
        if (expiresAt != null && expiresAt.contains(".")) {
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
        dialog.setListener(new MonthYearPickerDialog.OnChangeListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int _dayOfMonth) {
                if (itemListener != null) {
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.YEAR, year);
                    cal.set(Calendar.MONTH, month - 1); // fix for picker's index
                    cal.set(Calendar.DAY_OF_MONTH, 1);
                    itemListener.onExpiresAtChange(item.getId(), cal.getTime());
                }
            }

            @Override
            public void onCancel(DatePicker view) {
                // Nothing to do
            }
        });
        dialog.show(((MainActivity) context).getSupportFragmentManager(), "MonthYearPickerDialog");
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

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        Date now = cal.getTime();

        if (value == null || value.equals("")) { return color; }
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("dd." + expiresAtFormat);
            Date expiresAt = formatter.parse("01." + value);
            if (expiresAt.before(now) || expiresAt.equals(now)) {
                color = ContextCompat.getColor(context, R.color.colorAccent);
            } else {
                color = ContextCompat.getColor(context, R.color.colorPrimary);
            }
        } catch (ParseException e) {
            // pass (default color)
        }
        return color;
    }

    /**
     * Clean up when adapter is no longer needed
     */
    public void onDestroy() {
        // RealmResults auto-updates, no manual cleanup needed
    }

    // ViewHolder

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        final ImageView barcodeImage;
        final TextView name;
        final TextView size;
        final TextView datetime;
        final TextView price;
        final TextView deduction;
        final TextView category;
        final TextView ean;
        final TextView expiresAt;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            barcodeImage = itemView.findViewById(R.id.product_item_barcode_image);
            name = itemView.findViewById(R.id.product_item_name);
            size = itemView.findViewById(R.id.product_item_size);
            datetime = itemView.findViewById(R.id.product_item_datetime);
            price = itemView.findViewById(R.id.product_item_price);
            deduction = itemView.findViewById(R.id.product_item_deduction);
            category = itemView.findViewById(R.id.product_item_category);
            ean = itemView.findViewById(R.id.product_item_ean);
            expiresAt = itemView.findViewById(R.id.product_item_expires_at);
        }
    }
}
