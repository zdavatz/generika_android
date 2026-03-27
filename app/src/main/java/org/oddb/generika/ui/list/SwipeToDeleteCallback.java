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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

/**
 * ItemTouchHelper.Callback for swipe-to-delete functionality.
 * Replaces the old daimajia SwipeLayout library.
 * 
 * Usage:
 *   ItemTouchHelper itemTouchHelper = new ItemTouchHelper(
 *       new SwipeToDeleteCallback(context, adapter));
 *   itemTouchHelper.attachToRecyclerView(recyclerView);
 */
public class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {

    public interface SwipeAdapter {
        void onItemSwiped(int position);
    }

    private final ColorDrawable background;
    private final Drawable deleteIcon;
    private final int intrinsicWidth;
    private final int intrinsicHeight;
    private final Paint clearPaint;
    private final int backgroundColor;
    private final SwipeAdapter adapter;

    public SwipeToDeleteCallback(Context context, ProductRecyclerAdapter adapter) {
        this(context, (SwipeAdapter) adapter);
    }

    public SwipeToDeleteCallback(Context context, ReceiptRecyclerAdapter adapter) {
        this(context, (SwipeAdapter) adapter);
    }

    private SwipeToDeleteCallback(Context context, SwipeAdapter adapter) {
        super(0, ItemTouchHelper.LEFT);
        
        this.adapter = adapter;
        this.backgroundColor = Color.parseColor("#505050"); // darkGray
        this.background = new ColorDrawable();
        this.deleteIcon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_delete);
        
        if (deleteIcon != null) {
            // Tint the icon white for visibility
            deleteIcon.setTint(Color.WHITE);
            this.intrinsicWidth = deleteIcon.getIntrinsicWidth();
            this.intrinsicHeight = deleteIcon.getIntrinsicHeight();
        } else {
            this.intrinsicWidth = 0;
            this.intrinsicHeight = 0;
        }
        
        this.clearPaint = new Paint();
        this.clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        // We don't support drag & drop reordering
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();
        Log.d("SwipeToDelete", "onSwiped position=" + position + " direction=" + direction);
        if (position != RecyclerView.NO_POSITION) {
            adapter.onItemSwiped(position);
        }
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                            int actionState, boolean isCurrentlyActive) {
        
        View itemView = viewHolder.itemView;
        int itemHeight = itemView.getBottom() - itemView.getTop();
        boolean isCanceled = dX == 0f && !isCurrentlyActive;

        if (isCanceled) {
            clearCanvas(c, itemView.getRight() + dX, (float) itemView.getTop(),
                    (float) itemView.getRight(), (float) itemView.getBottom());
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, false);
            return;
        }

        // Draw the delete background
        background.setColor(backgroundColor);
        background.setBounds(
                itemView.getRight() + (int) dX,
                itemView.getTop(),
                itemView.getRight(),
                itemView.getBottom()
        );
        background.draw(c);

        // Calculate position of delete icon
        if (deleteIcon != null) {
            int deleteIconTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
            int deleteIconMargin = (itemHeight - intrinsicHeight) / 2;
            int deleteIconLeft = itemView.getRight() - deleteIconMargin - intrinsicWidth;
            int deleteIconRight = itemView.getRight() - deleteIconMargin;
            int deleteIconBottom = deleteIconTop + intrinsicHeight;

            // Draw the delete icon
            deleteIcon.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom);
            deleteIcon.draw(c);
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    private void clearCanvas(Canvas c, float left, float top, float right, float bottom) {
        c.drawRect(left, top, right, bottom, clearPaint);
    }

    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
        return 0.4f; // Require 40% swipe to trigger delete (similar to original)
    }
}
