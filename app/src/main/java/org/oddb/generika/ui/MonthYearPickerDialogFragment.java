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
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.NumberPicker;
import android.widget.TextView;

import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import java.util.Calendar;

import org.oddb.generika.R;


public class MonthYearPickerDialogFragment extends DialogFragment {
  private final static String TAG = "MonthYearPickerDialogFragment";

  private final static int M_MIN = 1;
  private final static int M_MAX = 12;

  // NOTE: same with iOS (01.2017 as minimum date)
  private final static int Y_MIN = 2017;
  private final static int Y_MAX_ADDITION = 20;

  private OnChangeListener listener;
  private String title;

  private Calendar cal;

  public static MonthYearPickerDialogFragment newInstance() {
    Calendar cal = Calendar.getInstance();

    return MonthYearPickerDialogFragment.newInstance(
      cal.get(Calendar.MONTH), cal.get(Calendar.YEAR));
  }

  public static MonthYearPickerDialogFragment newInstance(int m, int y) {
    Log.d(TAG, "(newInstance) month: " + m);
    Log.d(TAG, "(newInstance) year: " + y);

    MonthYearPickerDialogFragment f = new MonthYearPickerDialogFragment();

    int month;
    int year;
    if ((M_MIN <= m && m <= M_MAX) &&
        (Y_MIN <= y && y <= getMaxYear())) {
      month = m;
      year = y;
    } else {
      month = M_MIN;
      year = Y_MIN;
    }

    Bundle args = new Bundle();
    args.putInt("month", month);
    args.putInt("year", year);
    f.setArguments(args);

    return f;
  }

  public static int getMaxYear() {
    Calendar cal = Calendar.getInstance();
    return cal.get(Calendar.YEAR) + Y_MAX_ADDITION;
  }

  public interface OnChangeListener extends
    DatePickerDialog.OnDateSetListener {
    abstract void onCancel(DatePicker view);
  }

  public void setTitle(String text) {
    this.title = text;
  }

  public void setListener(OnChangeListener listener) {
    this.listener = listener;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    LayoutInflater inflater = getActivity().getLayoutInflater();
    View view = inflater.inflate(R.layout.month_year_picker_dialog, null);

    int month = getArguments().getInt("month", M_MIN);
    int year = getArguments().getInt("year", Y_MIN);

    Calendar cal = Calendar.getInstance();

    final NumberPicker monthPicker = (NumberPicker)view.findViewById(
      R.id.month_picker);
    monthPicker.setMinValue(M_MIN);
    monthPicker.setMaxValue(M_MAX);
    monthPicker.setValue(month);

    final NumberPicker yearPicker = (NumberPicker)view.findViewById(
      R.id.year_picker);
    yearPicker.setMinValue(Y_MIN);
    yearPicker.setMaxValue(getMaxYear());
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
    TextView titleText = (TextView)titleView.findViewById(
      R.id.month_year_picker_dialog_title);
    titleText.setText(title);
    builder.setCustomTitle(titleView);
    Dialog dialog = builder.create();
    dialog.setCancelable(false);
    // https://developer.android.com/reference/android/app/DialogFragment.html#setCancelable(boolean)
    setCancelable(false);  // fix to disallow cancel by device back button tap
    dialog.setCanceledOnTouchOutside(false);
    return dialog;
  }
}
