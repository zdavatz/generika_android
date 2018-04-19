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
package org.oddb.generika;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import io.realm.RealmList;
import io.realm.RealmResults;
import io.realm.Sort;

import org.oddb.generika.BaseActivity;
import org.oddb.generika.GlideApp;
import org.oddb.generika.data.DataManager;
import org.oddb.generika.model.Operator;
import org.oddb.generika.model.Patient;
import org.oddb.generika.model.Product;
import org.oddb.generika.model.Receipt;
import org.oddb.generika.ui.list.ReceiptProductListAdapter;
import org.oddb.generika.util.Constant;


public class ReceiptActivity extends BaseActivity {
  private static final String TAG = "ReceiptActivity";

  private Receipt receipt;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_receipt);

    if (receipt == null || savedInstanceState == null) {
      DataManager dataManager = new DataManager(Constant.SOURCE_TYPE_AMKJSON);
      String hashedKey = getIntent().getStringExtra(Constant.kHashedKey);
      this.receipt = dataManager.getReceiptByHashedKey(hashedKey);
    }
    Log.d(TAG, "(onCreate) placeDate: " + receipt.getPlaceDate());

    initViews();

    // set initial position
    ScrollView scrollView = (ScrollView)findViewById(R.id.receipt_view);
    scrollView.post(new Runnable() {
      public void run() {
        scrollView.scrollTo(0, 0);
      }
    });
  }

  private void initViews() {
    Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
    toolbar.setTitle(context.getString(R.string.receipt));
    toolbar.setSubtitle(receipt.getFilename());

    setSupportActionBar(toolbar);

    ActionBar actionBar = getSupportActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setDisplayShowHomeEnabled(true);

    TextView placeDate = (TextView)findViewById(R.id.receipt_view_place_date);
    placeDate.setText(stringify(receipt.getPlaceDate()));

    // operator
    TextView operatorTitle = (TextView)findViewById(
      R.id.receipt_view_operator_section_title);
    operatorTitle.setText(
      context.getString(R.string.receipt_operator_section_title));
    Operator operator = receipt.getOperator();
    if (operator != null) {
      fillOperatorFields(operator);
    }

    // patient
    TextView patientTitle = (TextView)findViewById(
      R.id.receipt_view_patient_section_title);
    patientTitle.setText(
      context.getString(R.string.receipt_patient_section_title));
    Patient patient = receipt.getPatient();
    if (patient != null) {
      fillPatientFields(patient);
    }

    // products (medications)
    RealmList<Product> productList = receipt.getMedications();
    TextView productsTitle = (TextView)findViewById(
      R.id.receipt_view_products_section_title);
    productsTitle.setText(String.format(
      context.getString(R.string.receipt_products_section_title),
      productList.size()));

    RealmResults<Product> products = null;
    if (productList != null) { // list -> results
      products = productList.where().sort(
        new String[]{"pack", "ean"},
        new Sort[]{Sort.ASCENDING, Sort.ASCENDING}
      ).findAll();
    }
    ReceiptProductListAdapter listAdapter = new ReceiptProductListAdapter(
      products);
    ListView listView = (ListView)findViewById(
      R.id.receipt_view_products_list);
    listView.setAdapter(listAdapter);

    setListViewHeight(listView);
  }

  private void fillOperatorFields(Operator operator) {
    // name
    TextView name = (TextView)findViewById(
      R.id.receipt_view_operator_name);
    String fullname = stringify(
      operator.getGivenName(), operator.getFamilyName());
    String title = stringify(operator.getTitle());
    name.setText(stringify(title, fullname));

    // address
    TextView address = (TextView)findViewById(
      R.id.receipt_view_operator_address);
    address.setText(stringify(operator.getAddress()));

    // region: city and country
    TextView region = (TextView)findViewById(
      R.id.receipt_view_operator_region);
    region.setText(stringify(operator.getCity(), operator.getCountry()));

    // phone
    TextView phone = (TextView)findViewById(
      R.id.receipt_view_operator_phone);
    phone.setText(stringify(operator.getPhone()));

    // email
    TextView email = (TextView)findViewById(
      R.id.receipt_view_operator_email);
    email.setText(stringify(operator.getEmail()));

    // signature
    ImageView signature = (ImageView)findViewById(
      R.id.receipt_view_operator_signature);
    byte[] bytes = Base64.decode(operator.getSignature(), Base64.DEFAULT);
    GlideApp.with(context)
      .load(bytes)
      .into(signature);
  }

  private void fillPatientFields(Patient patient) {
    // name
    TextView name = (TextView)findViewById(
      R.id.receipt_view_patient_name);
    name.setText(stringify(patient.getGivenName(), patient.getFamilyName()));

    // personal info: weight(kg),height(cm), gender and birth_date
    TextView info = (TextView)findViewById(
      R.id.receipt_view_patient_personal_info);
    StringBuilder builder = new StringBuilder();
    String weight = stringify(patient.getWeight());
    int parts = 0;
    if (!weight.equals("")) {
      builder.append(String.format("%skg", weight));
      parts += 1;
    }
    String height = stringify(patient.getHeight());
    if (!height.equals("")) {
      if (parts > 0) { builder.append("/"); }
      builder.append(String.format("%scm", height));
      parts += 1;
    }
    String gender = stringify(patient.getGenderSign());
    if (!gender.equals("")) {
      if (parts > 0) { builder.append(" "); }
      builder.append(gender);
      parts += 1;
    }
    String birthDate = stringify(patient.getBirthDate());
    if (!birthDate.equals("")) {
      if (parts > 0) { builder.append(" "); }
      builder.append(birthDate);
      parts += 1;
    }
    if (parts > 0) {
      info.setText(builder.toString());
    }

    // address
    TextView address = (TextView)findViewById(
      R.id.receipt_view_patient_address);
    address.setText(stringify(patient.getAddress()));

    // region: city and country
    TextView region = (TextView)findViewById(
      R.id.receipt_view_patient_region);
    region.setText(stringify(patient.getCity(), patient.getCountry()));

    // phone
    TextView phone = (TextView)findViewById(
      R.id.receipt_view_patient_phone);
    phone.setText(stringify(patient.getPhone()));

    // email
    TextView email = (TextView)findViewById(
      R.id.receipt_view_patient_email);
    email.setText(stringify(patient.getEmail()));
  }

  private String stringify(String value) {
    if (value == null || value.equals("null")) {
      return "";
    }
    return value;
  }

  private String stringify(String valueA, String valueB) {
    return String.format("%s %s", stringify(valueA), stringify(valueB)).trim();
  }

  // fix row height dynamically
  private void setListViewHeight(ListView listView) {
    ListAdapter listAdapter = listView.getAdapter();
    if (listAdapter == null) { return; }

    int total = 0;
    int count = listAdapter.getCount();
    // fixed width value is needed for height measurement
    float width = listView.getResources().getDisplayMetrics().widthPixels;
    width -= listView.getPaddingLeft() + listView.getPaddingRight();
    Log.d(TAG, "(setListViewHeight) width: " + width);

    for (int i = 0; i < count; i++) {
      View item = listAdapter.getView(i, null, listView);
      item.measure(
        MeasureSpec.makeMeasureSpec((int)width, MeasureSpec.AT_MOST),
        MeasureSpec.UNSPECIFIED);
      int height = item.getMeasuredHeight();
      Log.d(TAG, "(setListViewHeight) height: " + height);
      total += height;
    }
    total += listView.getPaddingTop() + listView.getPaddingBottom();
    Log.d(TAG, "(setListViewHeight) total: " + total);

    ViewGroup.LayoutParams params = listView.getLayoutParams();
    params.height = total + (listView.getDividerHeight() * (count - 1));
    listView.setLayoutParams(params);
    listView.setScrollContainer(false);
    listView.setEnabled(false);
    listView.requestLayout();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
  }

  @Override
  protected void onResume() {
    super.onResume();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    // on tap `◁` (device back button)
    if (event.getAction() == KeyEvent.ACTION_DOWN) {
      if (keyCode == KeyEvent.KEYCODE_BACK) {
        finishAfterTransition();
        return true;
      }
    }
    return super.onKeyDown(keyCode, event);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    ScrollView scrollView = (ScrollView)findViewById(R.id.receipt_view);
    int x = scrollView.getScrollX();
    int y = scrollView.getScrollY();
    Log.d(TAG, "(onSaveInstanceState) scroll x: " + x);
    Log.d(TAG, "(onSaveInstanceState) scroll y: " + y);
    outState.putIntArray("kScrollPosition", new int[]{x, y});
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);

    final int[] position = savedInstanceState.getIntArray("kScrollPosition");
    if (position != null) {
      ScrollView scrollView = (ScrollView)findViewById(R.id.receipt_view);
      scrollView.post(new Runnable() {
        public void run() {
          Log.d(TAG, "(onRestoreInstanceState) position.0: " + position[0]);
          Log.d(TAG, "(onRestoreInstanceState) position.1: " + position[1]);
          scrollView.scrollTo(position[0], position[1]);
        }
      });
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      finishAfterTransition();
    }
    return super.onOptionsItemSelected(item);
  }
}
