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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements
  AdapterView.OnItemClickListener {

  private DrawerLayout mDrawerLayout;
  private ActionBarDrawerToggle mDrawerToggle;
  private CharSequence mTitle;

  private static final int RC_BARCODE_CAPTURE = 9001;
  private static final String TAG = "BarcodeMain";

  private static final List<Item> list = new ArrayList<Item>() {
    {
      // dummy
      add(new Item(R.string.dummy_title0, R.string.dummy_description0));
      add(new Item(R.string.dummy_title1, R.string.dummy_description1));
      add(new Item(R.string.dummy_title2, R.string.dummy_description2));
      add(new Item(R.string.dummy_title3, R.string.dummy_description3));
      add(new Item(R.string.dummy_title4, R.string.dummy_description4));
      add(new Item(R.string.dummy_title5, R.string.dummy_description5));
      add(new Item(R.string.dummy_title6, R.string.dummy_description6));
      add(new Item(R.string.dummy_title7, R.string.dummy_description7));
      add(new Item(R.string.dummy_title8, R.string.dummy_description8));
      add(new Item(R.string.dummy_title9, R.string.dummy_description9));
      add(new Item(R.string.dummy_title0, R.string.dummy_description0));
      add(new Item(R.string.dummy_title1, R.string.dummy_description1));
      add(new Item(R.string.dummy_title2, R.string.dummy_description2));
      add(new Item(R.string.dummy_title3, R.string.dummy_description3));
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    initViews();
  }

  private void initViews() {
    Context context = (Context)this;

    // default: medications
    mTitle = context.getString(R.string.medications);

    Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
    toolbar.setTitle(mTitle);
    setSupportActionBar(toolbar);

    mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
    mDrawerToggle = new ActionBarDrawerToggle(
      this,
      mDrawerLayout,
      R.string.drawer_open,
      R.string.drawer_close
    ) {
      public void onDrawerOpened(View view) {
        super.onDrawerOpened(view);
        getSupportActionBar().setTitle(mTitle);
        invalidateOptionsMenu(); // onPrepareOptionsMenu
      }

      public void onDrawerClosed(View view) {
        super.onDrawerClosed(view);
        // TODO: update title
        getSupportActionBar().setTitle(mTitle);
        invalidateOptionsMenu();  // onPrepareOptionsMenu
      }
    };
    mDrawerToggle.syncState();
    mDrawerLayout.addDrawerListener(mDrawerToggle);

    ActionBar actionBar = getSupportActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setHomeButtonEnabled(true);

    NavigationView navigationView = (NavigationView)findViewById(
      R.id.navigation_view);
    navigationView.setNavigationItemSelectedListener(
      new NavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(MenuItem menuItem) {
          menuItem.setChecked(true);
          mDrawerLayout.closeDrawers();
          Toast.makeText(
            MainActivity.this, menuItem.getTitle(), Toast.LENGTH_LONG).show();
          return true;
        }
    });

    ListView listView = (ListView)findViewById(R.id.list_view);
    listView.setOnItemClickListener(this);
    listView.setAdapter(new ListAdapter());

    FloatingActionButton fab = (FloatingActionButton)findViewById(
      R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Snackbar.make(
          view, "Scanner view is comming!", Snackbar.LENGTH_LONG
        ).setAction("Action", null).show();

        Intent intent = new Intent(
          MainActivity.this, BarcodeCaptureActivity.class);
        intent.putExtra(BarcodeCaptureActivity.AutoFocus, true);
        intent.putExtra(BarcodeCaptureActivity.UseFlash, true);
        startActivityForResult(intent, RC_BARCODE_CAPTURE);
      }
    });
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    // TODO: set options menu by selected item (drawer)
    return super.onPrepareOptionsMenu(menu);
  }

  private static class ListAdapter extends BaseAdapter {

    @Override
    public int getCount() {
      return list.size();
    }

    @Override
    public Item getItem(int position) {
      return list.get(position);
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      Context context = parent.getContext();
      Item item = list.get(position);

      if (convertView == null) {
        LayoutInflater inflater = LayoutInflater.from(context);
        convertView = inflater.inflate(
          R.layout.activity_main_row, parent, false);
      }

      TextView titleView = (TextView)convertView.findViewById(
        R.id.title);
      TextView descriptionView = (TextView)convertView.findViewById(
        R.id.description);
      titleView.setText(context.getString(item.title));
      descriptionView.setText(context.getString(item.description));
      return convertView;
    }
  }

  @Override
  public void onItemClick(
      AdapterView<?> parent, View view, int position, long id) {
    Item item = list.get(position);

    Intent intent = new Intent(this, WebViewActivity.class);
    startActivity(intent);
    overridePendingTransition(R.anim.slide_leave,
                              R.anim.slide_enter);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it exists.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    switch (id) {
      case android.R.id.home:
        mDrawerLayout.openDrawer(GravityCompat.START);
        return true;
      case R.id.action_settings:
        return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onActivityResult(
    int requestCode, int resultCode, Intent data) {

    if (requestCode == RC_BARCODE_CAPTURE) {
      if (resultCode == CommonStatusCodes.SUCCESS) {
        if (data != null) {
          Barcode barcode = data.getParcelableExtra(
            BarcodeCaptureActivity.BarcodeObject);
          Log.d(TAG, "Barcode found: " + barcode.displayValue);
        } else {
          Log.d(TAG, "Barcode not found");
        }
      } else {
        Log.d(
          TAG,
          String.format(
            getString(R.string.barcode_error),
            CommonStatusCodes.getStatusCodeString(resultCode))
        );
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  private static class Item {
    @StringRes
    int title;

    @StringRes
    int description;

    public Item(int title, int description) {
      this.title = title;
      this.description = description;
    }
  }
}
