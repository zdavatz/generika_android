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
import android.view.MenuItem;
import android.view.KeyEvent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.widget.TextView;

import org.oddb.generika.BaseActivity;
import org.oddb.generika.data.DataManager;
import org.oddb.generika.model.Receipt;
import org.oddb.generika.util.Constant;


public class ReceiptActivity extends BaseActivity {
  private static final String TAG = "RceiptActivity";

  private TextView textView;
  private DataManager dataManager;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_information);

    this.dataManager = new DataManager(Constant.SOURCE_TYPE_AMKJSON);

    if (savedInstanceState == null) {
      String hashedKey = getIntent().getStringExtra(Constant.kHashedKey);
      Receipt receipt = this.dataManager.getReceiptByHashedKey(hashedKey);
      Log.d(TAG, "(onCreate) receipt: " + receipt);
    }

    initViews();
  }

  private void initViews() {
    Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
    toolbar.setTitle(context.getString(R.string.receipt));
    toolbar.setSubtitle("");

    setSupportActionBar(toolbar);

    ActionBar actionBar = getSupportActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setDisplayShowHomeEnabled(true);

    this.textView = (TextView)findViewById(R.id.receipt_text);
  }

  @Override
  protected void onDestroy() {
    this.textView = null;

    super.onDestroy();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    // on tap `◁` (device back button)
    if (event.getAction() == KeyEvent.ACTION_DOWN) {
      if (keyCode == KeyEvent.KEYCODE_BACK) {
        finish();
        return true;
      }
    }
    return super.onKeyDown(keyCode, event);
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    // do nothing
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    // todo
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);

    // todo
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      finish();
    }
    return super.onOptionsItemSelected(item);
  }
}
