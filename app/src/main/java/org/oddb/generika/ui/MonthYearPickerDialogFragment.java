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

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.NumberPicker;
import android.widget.TextView;

import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import org.oddb.generika.R;


public class MonthYearPickerDialogFragment extends DialogFragment {
  public interface OnChangeListener extends
    DatePickerDialog.OnDateSetListener {
    abstract void onCancel(DatePicker view);
  }

  private int kYEAR_MIN = 2017;
  private int kYEAR_ADDITION = 20;

  private OnChangeListener listener;
  private String dialogTitleText;

  public MonthYearPickerDialogFragment(String text) {
    this.dialogTitleText = text;
  }

  public void setListener(OnChangeListener listener) {
    this.listener = listener;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    LayoutInflater inflater = getActivity().getLayoutInflater();
    final Calendar cal = Calendar.getInstance();
    View view = inflater.inflate(R.layout.month_year_picker_dialog, null);

    final NumberPicker monthPicker = (NumberPicker)view.findViewById(
      R.id.month_picker);
    monthPicker.setMinValue(1);
    monthPicker.setMaxValue(12);
    monthPicker.setValue(cal.get(Calendar.MONTH));

    final NumberPicker yearPicker = (NumberPicker)view.findViewById(
      R.id.year_picker);
    int year = cal.get(Calendar.YEAR);
    // same with iOS (01.2017 as minimum date)
    yearPicker.setMinValue(kYEAR_MIN);
    yearPicker.setMaxValue(year + kYEAR_ADDITION);
    yearPicker.setValue(year);

    builder.setView(view)
      .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int id) {
          if (listener != null) {
            listener.onDateSet(
              null, yearPicker.getValue(), monthPicker.getValue(), 0);
          }
        }
      })
      .setNegativeButton(
        R.string.cancel, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int id) {
          if (listener != null) {
            listener.onCancel(null);
          }
          MonthYearPickerDialogFragment.this.getDialog().cancel();
        }
      });

    View titleView = inflater.inflate(
      R.layout.month_year_picker_dialog_title, null);
    TextView title = (TextView)titleView.findViewById(
      R.id.month_year_picker_dialog_title);
    title.setText(this.dialogTitleText);
    builder.setCustomTitle(titleView);
    Dialog dialog = builder.create();
    dialog.setCancelable(false);
    // https://developer.android.com/reference/android/app/DialogFragment.html#setCancelable(boolean)
    setCancelable(false);  // fix to disallow cancel by device back button tap
    dialog.setCanceledOnTouchOutside(false);
    return dialog;
  }

  public void onDateSet(DatePicker view, int year, int month) {

  }
}
