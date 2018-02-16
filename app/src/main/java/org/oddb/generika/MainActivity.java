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
import android.content.DialogInterface;
import android.net.NetworkInfo;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmList;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.OrderedCollectionChangeSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.oddb.generika.model.Product;
import org.oddb.generika.model.ProductItem;
import org.oddb.generika.ui.list.ProductItemListAdapter;
import org.oddb.generika.network.ProductItemDataFetchFragment;


public class MainActivity extends AppCompatActivity implements
  AdapterView.OnItemClickListener,
  ProductItemListAdapter.DeleteListener,
  ProductItemDataFetchFragment.FetchCallback<
    ProductItemDataFetchFragment.FetchResult> {

  private static final int RC_BARCODE_CAPTURE = 9001;
  private static final String TAG = "BarcodeMain";

  // view
  private DrawerLayout mDrawerLayout;
  private ActionBarDrawerToggle mDrawerToggle;
  private CharSequence mTitle;
  private ListView mListView;

  // database
  private Realm realm;
  private Product product;  // container object
  private ProductItemListAdapter productItemListAdapter;

  // network (headless fragment)
  private boolean fetching = false;
  private ProductItemDataFetchFragment productItemDataFetcher;

  // as place holder values
  private static final HashMap<String, String> initData =
    new HashMap<String, String>() {{
      put("ean", "EAN 13");
      put("name", "Name");
      put("size", "Size");
      put("datetime", "Scanned At");
      put("price", "Price (CHF)");
      put("deduction", "Deduction (%)");
      put("category", "Category");
    }};

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // default: `scanned` product items
    this.realm = Realm.getDefaultInstance();
    this.product = realm.where(Product.class)
      .equalTo("sourceType", "scanned").findFirst();

    FragmentManager fragmentManager = getSupportFragmentManager();
    Fragment fragment = fragmentManager.findFragmentByTag(
      ProductItemDataFetchFragment.TAG);
    if (fragment == null) {
      // TODO: use constant utility
      fragment = ProductItemDataFetchFragment.getInstance(
        fragmentManager,
        "https://ch.oddb.org/de/mobile/api_search/ean/");
    }
    this.productItemDataFetcher = (ProductItemDataFetchFragment)fragment;

    initProductItems();
    initViews();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    realm.close();
  }

  private void initProductItems() {
    if (product == null) {
      return;
    }
    if (product.getItems().size() == 0) {
      realm.beginTransaction();
      ProductItem item = ProductItem.createWithEanIntoSource(
        realm, initData.get("ean"), product);
      item.setName(initData.get("name"));
      item.setSize(initData.get("size"));
      item.setDatetime(initData.get("datetime"));
      item.setPrice(initData.get("price"));
      item.setDeduction(initData.get("deduction"));
      item.setCategory(initData.get("category"));
      realm.commitTransaction();
    }

    // Check new product item insertion via barcode reader
    RealmList productItems = product.getItems();
    productItems.removeAllChangeListeners();
    productItems.addChangeListener(
      new OrderedRealmCollectionChangeListener<RealmList<ProductItem>>() {

      @Override
      public void onChange(
        RealmList<ProductItem> items,
        OrderedCollectionChangeSet changeSet) {
        Log.d(TAG, "(onActivityResult) items.size: " + items.size());

        int i[] = changeSet.getInsertions();
        if (i != null && i.length == 1) {  // new scan
          Log.d(TAG, "(onActivityResult) inserttions: " + i[0]);
          // get via location index
          ProductItem productItem = items.get(i[0]);
          // pass dummy object as container for id and ean
          ProductItem item = new ProductItem();
          item.setId(productItem.getId());
          item.setEan(productItem.getEan());
          startFetching(item);
        }
      }
    });

    this.productItemListAdapter = new ProductItemListAdapter(
      product.getItems());
    productItemListAdapter.setCallback(this);
  }

  private void initViews() {
    Context context = (Context)this;
    // default: medications
    this.mTitle = context.getString(R.string.medications);

    Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
    toolbar.setTitle(mTitle);
    setSupportActionBar(toolbar);

    this.mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
    this.mDrawerToggle = new ActionBarDrawerToggle(
      this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {

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

    this.mListView = (ListView)findViewById(R.id.list_view);
    mListView.setAdapter(productItemListAdapter);
    mListView.setOnItemClickListener(this);

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

    FloatingActionButton fab = (FloatingActionButton)findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View view) {
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

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
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
      // get result from barcode reader
      if (resultCode == CommonStatusCodes.SUCCESS) {
        if (data != null) {
          Barcode barcode = data.getParcelableExtra(
            BarcodeCaptureActivity.BarcodeObject);
          Log.d(TAG,
                "(onActivityResult) Barcode found: " + barcode.displayValue);
          if (barcode.displayValue.length() == 13) {
            // save record into realm (next: changeset listener)
            addProduct(barcode.displayValue);
          }
        } else {
          Log.d(TAG, "(onActivityResult) Barcode not found");
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

  private void addProduct(final String ean) {
    realm.executeTransaction(new Realm.Transaction() {

      @Override
      public void execute(Realm realm_) {
        ProductItem.createWithEanIntoSource(realm_, ean, product);
      }
    });
  }

  // -- AdapterView.OnItemClickListener

  @Override
  public void onItemClick(
    AdapterView<?> parent, View view, int position, long id) {

    ProductItem productItem = realm.where(ProductItem.class)
      .equalTo("id", id).findFirst();
    if (productItem == null) {  // unexpected
      return;
    }
    Intent intent = new Intent(this, WebViewActivity.class);
    // TODO: support de/fr, fachinfo/patinfo and preis vergleich
    intent.putExtra(WebViewActivity.Reg, productItem.getReg());
    startActivity(intent);

    // TODO: fix
    overridePendingTransition(R.anim.slide_leave,
                              R.anim.slide_enter);
  }

  // -- ProductItemListAdapter.DeleteListener

  @Override
  public void delete(long itemId) {
    // should check sourceType of Product?
    deleteProductItem(itemId);
  }

  private void deleteProductItem(long productItemId) {
    final long id = productItemId;
    realm.executeTransaction(new Realm.Transaction() {

      @Override
      public void execute(Realm realm_) {
        ProductItem productItem = realm_.where(ProductItem.class)
          .equalTo("id", id).findFirst();
        if (productItem != null) {
          productItem.deleteFromRealm();
        }
      }
    });
    //mListView.invalidateViews();
  }

  private void startFetching(ProductItem productItem) {
    if (!fetching && productItemDataFetcher != null) {
      productItemDataFetcher.invokeFetch(productItem);
      this.fetching = true;
    }
  }

  // -- ProductItemDataFetchFragment.FetchCallback

  @Override
  public void updateFromFetch(
    ProductItemDataFetchFragment.FetchResult result) {
    if (result == null) {
      return;
    }
    if (result.errorMessage != null) {
      alertDialog("", result.errorMessage);
    } else if (result.itemMap != null) {
      final long id = result.itemId;
      Log.d(TAG, "(updateFromFetch) resut.itemId: " + id);

      final HashMap<String, String> properties = result.itemMap;
      final ProductItem productItem = realm.where(ProductItem.class)
        .equalTo("id", id).findFirst();

      if (productItem == null) {
        return;
      }
      productItem.addChangeListener(new RealmChangeListener<ProductItem>() {

        @Override
        public void onChange(ProductItem productItem_) {
          if (productItem_ == null || !productItem_.isValid()) {
            return;
          }
          // only once (remove self)
          productItem_.removeAllChangeListeners();
          if (productItem_.getName() != null &&
              productItem_.getSize() != null) {
            Log.d(TAG,
              "(updateFromFetch) productItem.name: " + productItem_.getName());
            // notify result to user
            // TODO: replace with translated string
            String title = "Generika.cc sagt";
            String message = productItem_.toMessage();
            alertDialog(title, message);
          }
        }
      });
      realm.executeTransaction(new Realm.Transaction() {

        @Override
        public void execute(Realm realm_) {
          try { // update properties (map) from api fetch result
            if (productItem.isValid()) {
              productItem.updateProperties(properties);
            }
          } catch (Exception e) {
            Log.d(TAG, "(updateFromFetch) Update error: " + e.getMessage());
          }
        }
      });
    }
  }

  @Override
  public NetworkInfo getActiveNetworkInfo() {
    // It seems that this cast is not redundant :'(
    ConnectivityManager connectivityManager =
      (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo networkinfo = connectivityManager.getActiveNetworkInfo();
    return networkinfo;
  }

  @Override
  public void onProgressUpdate(int progressCode, int percentComplete) {
    // pass
  }

  @Override
  public void finishFetching() {
    this.fetching = false;
    if (productItemDataFetcher != null) {
      productItemDataFetcher.cancelFetch();
    }
  }

  private void alertDialog(String title, String message) {
    Context context = (Context)this;
    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle(title);
    builder.setMessage(message);
    builder.setCancelable(true);
    builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int _id) {
        dialog.cancel();
      }
    });
    AlertDialog alert = builder.create();
    alert.show();
  }
}
