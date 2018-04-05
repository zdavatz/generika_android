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
package org.oddb.generika.ui;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import org.oddb.generika.R;


/**
 * General message dialog for import/capture action results.
 */
public class MessageDialog extends DialogFragment {
  private final static String TAG = "MessageDialog";

  public static int TEXT_ID_NONE = -1;

  private OnChangeListener listener;

  private int positiveTextId = TEXT_ID_NONE;
  private int negativeTextId = TEXT_ID_NONE;

  public static MessageDialog newInstance(String title, String message) {
    Bundle extras = new Bundle();
    extras.putString("title", title);
    extras.putString("message", message);

    MessageDialog dialog = new MessageDialog();
    dialog.setArguments(extras);
    return dialog;
  }

  public interface OnChangeListener {
    abstract void onOk();
    abstract void onCancel();
  }

  public void setListener(OnChangeListener listener) {
    this.listener = listener;
  }

  public void setPositiveTextId(int textId) {
    this.positiveTextId = textId;
  }

  public void setNegativeTextId(int textId) {
    this.negativeTextId = textId;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Bundle extras = getArguments();
    String title = extras.getString("title", "");
    String message = extras.getString("message", "");

    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder
      .setTitle(title)
      .setMessage(message);
    
    if (negativeTextId != TEXT_ID_NONE) {
      builder.setNegativeButton(
        negativeTextId, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int id) {
          if (listener != null) {
            listener.onCancel();
          }
          MessageDialog.this.getDialog().cancel();
        }
      });
    }
    if (positiveTextId != TEXT_ID_NONE) {
      builder.setPositiveButton(
        positiveTextId, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int id) {
          if (listener != null) {
            listener.onOk();
          }
          MessageDialog.this.getDialog().cancel();
        }
      });
    }

    Dialog dialog = builder.create();
    dialog.setCancelable(false);
    // https://developer.android.com/reference/android/app/DialogFragment.html\
    // #setCancelable(boolean)
    setCancelable(false); // fix to disallow cancel by device back button tap
    dialog.setCanceledOnTouchOutside(false);
    return dialog;
  }
}
