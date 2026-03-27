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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import io.realm.RealmList;
import io.realm.RealmResults;

import org.oddb.generika.MainActivity;
import org.oddb.generika.R;
import org.oddb.generika.model.Operator;
import org.oddb.generika.model.Patient;
import org.oddb.generika.model.Product;
import org.oddb.generika.model.Receipt;
import org.oddb.generika.util.Formatter;


/**
 * RecyclerView adapter for Receipt list - replaces ReceiptListAdapter.
 * Uses ItemTouchHelper for swipe-to-delete instead of daimajia SwipeLayout.
 */
public class ReceiptRecyclerAdapter
    extends RecyclerView.Adapter<ReceiptRecyclerAdapter.ReceiptViewHolder>
    implements GenerikaListAdapter, SwipeToDeleteCallback.SwipeAdapter {

    private static final String TAG = "ReceiptRecyclerAdapter";

    private RealmResults<Receipt> receipts;
    private GenerikaListAdapter.ListItemListener itemListener;

    public ReceiptRecyclerAdapter(@Nullable RealmResults<Receipt> receipts) {
        this.receipts = receipts;
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
        this.receipts = (RealmResults<Receipt>) data;
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
        return receipts != null && receipts.isValid() ? receipts.size() : 0;
    }

    @Nullable
    public Receipt getItem(int position) {
        if (receipts != null && receipts.isValid() && position >= 0 && position < receipts.size()) {
            return receipts.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public ReceiptViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.activity_main_receipt_row, parent, false);
        return new ReceiptViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReceiptViewHolder holder, int position) {
        final Receipt item = getItem(position);
        if (item == null) return;

        final Context context = holder.itemView.getContext();

        Operator operator = null;
        Patient patient = null;
        RealmList<Product> medications = null;
        try { operator = item.getOperator(); } catch (Exception e) { /* null */ }
        try { patient = item.getPatient(); } catch (Exception e) { /* null */ }
        try { medications = item.getMedications(); } catch (Exception e) { /* null */ }

        // First line: patient name (most important)
        String patientName = "";
        if (patient != null) {
            String pGiven = patient.getGivenName();
            String pFamily = patient.getFamilyName();
            patientName = String.format("%s %s",
                pGiven != null && !pGiven.equals("null") ? pGiven : "",
                pFamily != null && !pFamily.equals("null") ? pFamily : "").trim();
        }
        if (patientName.isEmpty()) {
            String placeDate = item.getPlaceDate();
            patientName = (placeDate != null && !placeDate.equals("null")) ? placeDate : "";
        }
        Log.d(TAG, "(onBindViewHolder) pos=" + position + " patientName='" + patientName + "'");
        holder.placeDate.setText(patientName);

        // Second line: operator/doctor name
        String operatorName = "";
        if (operator != null) {
            String oGiven = operator.getGivenName();
            String oFamily = operator.getFamilyName();
            operatorName = String.format("%s %s",
                oGiven != null && !oGiven.equals("null") ? oGiven : "",
                oFamily != null && !oFamily.equals("null") ? oFamily : "").trim();
        }
        holder.operatorName.setText(operatorName);

        // filename (original filename)
        holder.filename.setText(item.getFilename());

        // phone
        holder.operatorPhone.setText(operator != null ? operator.getPhone() : "");

        // datetime (importedAt)
        holder.datetime.setText(
            Formatter.formatAsLocalDate(item.getDatetime(), "HH:mm dd.MM.YYYY"));

        // email
        holder.operatorEmail.setText(operator != null ? operator.getEmail() : "");

        // medications count
        int medicationsCount = 0;
        if (medications != null) {
            medicationsCount = medications.size();
        }
        holder.medicationsCount.setText(
            String.format("%d Medikamente", medicationsCount));

        // Click listener - tap to open receipt
        holder.itemView.setOnClickListener(v -> {
            String hashedKey = item.getHashedKey();
            if (hashedKey != null && !hashedKey.equals("")) {
                ((MainActivity) context).openReceipt(hashedKey);
            }
        });
    }

    /**
     * Called when item is swiped - triggers delete callback
     */
    @Override
    public void onItemSwiped(int position) {
        Receipt item = getItem(position);
        Log.d(TAG, "(onItemSwiped) position=" + position + " item=" + (item != null ? item.getId() : "null"));
        if (item != null && itemListener != null) {
            itemListener.onDelete(item.getId());
        }
        // Always update UI, even if item was already gone from Realm
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, getItemCount());
    }

    /**
     * Clean up when adapter is no longer needed
     */
    public void onDestroy() {
        // RealmResults auto-updates, no manual cleanup needed
    }

    // ViewHolder

    static class ReceiptViewHolder extends RecyclerView.ViewHolder {
        final TextView placeDate;
        final TextView operatorName;
        final TextView filename;
        final TextView operatorPhone;
        final TextView datetime;
        final TextView operatorEmail;
        final TextView medicationsCount;

        ReceiptViewHolder(@NonNull View itemView) {
            super(itemView);
            placeDate = itemView.findViewById(R.id.receipt_item_place_date);
            operatorName = itemView.findViewById(R.id.receipt_item_operator_name);
            filename = itemView.findViewById(R.id.receipt_item_filename);
            operatorPhone = itemView.findViewById(R.id.receipt_item_operator_phone);
            datetime = itemView.findViewById(R.id.receipt_item_datetime);
            operatorEmail = itemView.findViewById(R.id.receipt_item_operator_email);
            medicationsCount = itemView.findViewById(R.id.receipt_item_medications_count);
        }
    }
}
