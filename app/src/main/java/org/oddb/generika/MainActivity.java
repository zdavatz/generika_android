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
import android.content.SharedPreferences;
import android.content.Intent;
import android.content.DialogInterface;
import android.os.Bundle;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import io.realm.RealmChangeListener;
import io.realm.RealmList;
import io.realm.RealmResults;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.OrderedCollectionChangeSet;

import java.util.Date;
import java.util.HashMap;

import org.oddb.generika.BaseActivity;
import org.oddb.generika.data.ProductItemManager;
import org.oddb.generika.model.ProductItem;
import org.oddb.generika.network.ProductItemDataFetchFragment;
import org.oddb.generika.ui.list.ProductItemListAdapter;
import org.oddb.generika.ui.list.ScannedProductItemListAdapter;
import org.oddb.generika.ui.list.ReceiptProductItemListAdapter;
import org.oddb.generika.util.Constant;


public class MainActivity extends BaseActivity implements
  ProductItemListAdapter.ItemListener,
  ProductItemDataFetchFragment.FetchCallback<
    ProductItemDataFetchFragment.FetchResult> {
  private static final String TAG = "Main";

  private static final String SOURCE_TYPE_SCANNED = "scanned"; // medications
  private static final String SOURCE_TYPE_RECEIPT = "receipt"; // prescriptions

  // view
  private DrawerLayout drawerLayout;
  private ActionBarDrawerToggle drawerToggle;
  private CharSequence title;
  private ListView listView;
  private FloatingActionButton fab;

  private EditText searchBox;

  private String sourceType;
  private ProductItemManager itemManager;  // data manager
  private ProductItemListAdapter listAdapter; // scanned/receipt

  // network (headless fragment)
  private boolean fetching = false;
  private ProductItemDataFetchFragment productItemDataFetcher;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_main);

    // default: medications (scanned product items)
    this.itemManager = new ProductItemManager(SOURCE_TYPE_SCANNED);
    this.title = context.getString(R.string.medications);

    initViews();

    switchProduct("medications"); // medications/prescriptions
  }

  /**
   * Switches product and list adapter
   *
   * @param String productName prescriptions/medications
   * @return void
   */
  private void switchProduct(String productName) {
    String sourceType_ = SOURCE_TYPE_SCANNED;
    if (productName != null && productName.equals("prescriptions")) {
      sourceType_ = SOURCE_TYPE_RECEIPT;
    }
    this.sourceType = sourceType_;

    itemManager.bindProductBySourceType(sourceType);
    initProductItems();

    if (sourceType.equals(SOURCE_TYPE_SCANNED)) {
      this.listAdapter = new ScannedProductItemListAdapter(
        itemManager.getProductItems());
      this.productItemDataFetcher = buildProductItemDataFetchFragment(context);
    } else if (sourceType.equals(SOURCE_TYPE_RECEIPT)) {
      this.listAdapter = new ReceiptProductItemListAdapter(
        itemManager.getProductItems());
      this.productItemDataFetcher = null;
    }

    // change list adapter
    listAdapter.setCallback(this);
    listView.setAdapter(listAdapter);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    itemManager.release();
  }

  private ProductItemDataFetchFragment buildProductItemDataFetchFragment(
      Context context_) {
    FragmentManager fragmentManager = getSupportFragmentManager();
    Fragment fragment = fragmentManager.findFragmentByTag(
      ProductItemDataFetchFragment.TAG);
    if (fragment == null) {
      // check search lang in preference
      SharedPreferences sharedPreferences = PreferenceManager
        .getDefaultSharedPreferences(context_);
      String searchLang = sharedPreferences.getString(
        Constant.kSearchLang, Constant.LANG_DE);
      String urlBase = String.format(
        Constant.API_URL_PATH, searchLang);
      fragment = ProductItemDataFetchFragment.getInstance(
        fragmentManager, urlBase);
    }
    return (ProductItemDataFetchFragment)fragment;
  }

  private void initProductItems() {
    // TODO:
    if (sourceType.equals(SOURCE_TYPE_RECEIPT)) {
      return;
    }

    if (itemManager.getProductItems().size() == 0) {
      itemManager.preparePlaceholder();
    }

    // Check new product item insertion via barcode reader
    RealmList productItems = itemManager.getProductItems();
    productItems.removeAllChangeListeners();
    productItems.addChangeListener(
      new OrderedRealmCollectionChangeListener<RealmList<ProductItem>>() {
      @Override
      public void onChange(
        RealmList<ProductItem> items, OrderedCollectionChangeSet changeSet) {
        Log.d(TAG, "(addChangeListener) items.size: " + items.size());

        int insertions[] = changeSet.getInsertions();
        if (insertions != null && insertions.length == 1) {  // new scan
          int i = insertions[0];
          Log.d(TAG, "(addChangeListener) inserttion: " + i);
          ProductItem productItem = items.get(i);
          if (productItem.getEan().equals(Constant.INIT_DATA.get("ean"))) {
            return; // do nothing for placeholder row
          }
          // pass dummy object as container for id and ean
          ProductItem item = new ProductItem();
          item.setId(productItem.getId());
          item.setEan(productItem.getEan());
          // invoke async api call
          startFetching(item);
        }
      }
    });
  }

  private void initViews() {
    Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
    toolbar.setTitle(title);
    setSupportActionBar(toolbar);

    this.drawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
    this.drawerToggle = new ActionBarDrawerToggle(
      this, drawerLayout,
      R.string.open, R.string.close) {
      public void onDrawerOpened(View view) {
        super.onDrawerOpened(view);
        getSupportActionBar().setTitle(title);

        invalidateOptionsMenu(); // onPrepareOptionsMenu
      }

      public void onDrawerClosed(View view) {
        super.onDrawerClosed(view);
        getSupportActionBar().setTitle(title);

        invalidateOptionsMenu();  // onPrepareOptionsMenu
      }
    };
    drawerToggle.syncState();
    drawerLayout.addDrawerListener(drawerToggle);

    ActionBar actionBar = getSupportActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setHomeButtonEnabled(true);

    this.searchBox = (EditText)findViewById(R.id.search_box);
    setupSearchBox();

    this.listView = (ListView)findViewById(R.id.list_view);

    // drawer navigation (products)
    NavigationView navigationView = (NavigationView)findViewById(
      R.id.navigation_view);
    navigationView.setNavigationItemSelectedListener(
      new NavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(MenuItem menuItem) {
          Log.d(TAG, "(onNavigationItemSelected) menuItem: " + menuItem);

          searchBox.setCursorVisible(false);
          searchBox.clearFocus();
          fab.setVisibility(View.VISIBLE);

          if (!menuItem.isChecked()) {
            String name = getResources().getResourceEntryName(
              menuItem.getItemId());
            Log.d(TAG, "(onNavigationItemSelected) name: " + name);
            String productName;
            if (name.contains("prescriptions")) {
              productName = "prescriptions";
            } else {  // default (scanned product items)
              productName = "medications";
            }
            title = menuItem.getTitle();  // update `title`
            switchProduct(productName);
            menuItem.setChecked(true);
          }
          drawerLayout.closeDrawers();
          Toast.makeText(MainActivity.this, title, Toast.LENGTH_LONG).show();
          return true;
        }
    });

    this.fab = (FloatingActionButton)findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (sourceType.equals(SOURCE_TYPE_SCANNED)) { // medications
          Intent intent = new Intent(
            MainActivity.this, BarcodeCaptureActivity.class);
          intent.putExtra(Constant.kAutoFocus, true);
          intent.putExtra(Constant.kUseFlash, true);
          startActivityForResult(intent, Constant.RC_BARCODE_CAPTURE);
        } else if (sourceType.equals(SOURCE_TYPE_RECEIPT)) {  // prescriptions
          // TODO: prescriptions
        }
      }
    });
  }

  private void setupSearchBox() {
    // filter
    searchBox.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(
        CharSequence s, int start, int count, int after) {
        // pass
      }

      @Override
      public void onTextChanged(
        CharSequence s, int start, int before, int count) {
        String filterString = s.toString().toLowerCase();
        Log.d(TAG, "(onTextChanged) filterString: " + filterString);
        // minimum 3 chars
        if (filterString.length() < 3) {
          if (filterString.length() == 0) {  // back to all items
            listAdapter.updateData(itemManager.getProductItems());
          }
          return;
        }
        RealmResults<ProductItem> data = itemManager
          .findProductItemsByNameOrEan(filterString);
        // NOTE:
        // This `updateData()` method invokes `notifyDataSetChanged()`, after
        // data set. See also below.
        //
        // https://github.com/realm/realm-android-adapters/blob/\
        //   bd22599bbbac33e0f3840e5832a99954dcb128c1/adapters/src/main/java\
        //   /io/realm/RealmBaseAdapter.java#L135
        listAdapter.updateData(data);
      }

      @Override
      public void afterTextChanged(Editable s) {
        // pass
      }
    });

    // hide cursor/clear focus on press enter or done
    searchBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
      @Override
      public boolean onEditorAction(
        TextView view, int actionId, KeyEvent event) {
        if ((event != null &&
             (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && // 66
             (event.getAction() == KeyEvent.ACTION_DOWN)) || // 0
            (actionId == EditorInfo.IME_ACTION_DONE)) { // 6
          searchBox.setCursorVisible(false);
          searchBox.clearFocus();
          fab.setVisibility(View.VISIBLE);
          return true;
        }
        return false;
      }
    });

    // show/hide software keyboard
    searchBox.setOnFocusChangeListener(new View.OnFocusChangeListener() {
      @Override
      public void onFocusChange(View view, boolean focused) {
        InputMethodManager keyboard = (InputMethodManager)(context)
          .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (focused) {
          fab.setVisibility(View.GONE);
          searchBox.setCursorVisible(true);
          keyboard.showSoftInput(searchBox, 0);
        } else {
          keyboard.hideSoftInputFromWindow(searchBox.getWindowToken(), 0);
        }
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

    Intent intent;
    switch (id) {
      case android.R.id.home:
        drawerLayout.openDrawer(GravityCompat.START);
        return true;
      case R.id.settings:
        intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
        return true;
      case R.id.information:
        intent = new Intent(this, InformationActivity.class);
        startActivity(intent);
        return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onActivityResult(
    int requestCode, int resultCode, Intent data) {

    if (requestCode == Constant.RC_BARCODE_CAPTURE) {
      // get result from barcode reader
      if (resultCode == CommonStatusCodes.SUCCESS) {
        if (data != null) {
          Barcode barcode = data.getParcelableExtra(Constant.kBarcode);
          String filepath = data.getStringExtra(Constant.kFilepath);
          Log.d(TAG, "(onActivityResult) filepath: " + filepath);

          if (barcode.displayValue.length() == 13) {
            // use ProductItem's Barcode
            ProductItem.Barcode barcode_ = new ProductItem.Barcode();
            barcode_.setValue(barcode.displayValue);
            barcode_.setFilepath(filepath);
            // save record into realm (next: changeset listener)
            itemManager.addProductItem(barcode_);
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

  // -- ProductItemListAdapter.ItemListener

  @Override
  public void onDelete(String productItemId) {
    // productItem is primary key (itemTag)
    itemManager.deleteProductItem(productItemId);

    // should check sourceType of Product at here?
  }

  @Override
  public void onExpiresAtChange(String productItemId, Date newDate) {
    Log.d(TAG, "(onExpiresAtChange) date: " + newDate);  // local time

    HashMap<String, String> properties = new HashMap<String, String>();
    properties.put("expiresAt", ProductItem.makeExpiresAt(newDate));
    Log.d(TAG, "(onExpiresAtChange) expiresAt: " +
               ProductItem.makeExpiresAt(newDate));
    itemManager.updateProductItem(productItemId, properties);
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
    Log.d(TAG, "(updateFromFetch) result: " + result);
    if (result == null) { return; }
    if (result.errorMessage != null) {
      alertDialog("", result.errorMessage);
    } else if (result.itemMap != null) {
      final String id = result.itemId;
      Log.d(TAG, "(updateFromFetch) resut.itemId: " + id);

      final HashMap<String, String> properties = result.itemMap;
      // NOTE:
      // realm is not transferred to background async task thread
      // it's not accecible.
      ProductItemManager itemManager_ = new ProductItemManager(
        SOURCE_TYPE_SCANNED);
      try {
        final ProductItem productItem = itemManager_.getProductItemById(id);
        if (productItem == null) { return; }
        productItem.removeAllChangeListeners();
        // this listener is invoked after update transaction below
        productItem.addChangeListener(new RealmChangeListener<ProductItem>() {
          @Override
          public void onChange(ProductItem productItem_) {
            if (productItem_ == null || !productItem_.isValid()) { return; }
            // only once (remove self)
            productItem_.removeAllChangeListeners();
            if (productItem_.getName() != null &&
                productItem_.getSize() != null) {
              // TODO: stop redraw all items on listview
              // redraw this row
              listAdapter.refresh(productItem_, listView);

              Log.d(TAG, "(updateFromFetch/onChange) productItem.name: " +
                    productItem_.getName());
              // notify result to user
              // TODO: replace with translated string
              String title = "Generika.cc sagt";
              String message = productItem_.toMessage();
              alertDialog(title, message, productItem_);
            }
          }
        });
        // above onChange will be called
        itemManager_.updateProductItem(productItem.getId(), properties);
      } finally {
        itemManager_.release();
      }
    }
  }

  @Override
  public NetworkInfo getActiveNetworkInfo() {
    // it seems that this cast is not redundant :'(
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
    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle(title);
    builder.setMessage(message);
    builder.setCancelable(true);

    builder.setPositiveButton(
      context.getString(R.string.ok),
      new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int _id) {
        dialog.cancel();
      }
    });
    AlertDialog alert = builder.create();
    alert.show();
  }

  private void alertDialog(
    String title, String message, final ProductItem productItem_) {
    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle(title);
    builder.setMessage(message);
    builder.setCancelable(true);

    builder.setPositiveButton(
      context.getString(R.string.open),
      new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int _id) {
        openWebView(productItem_);
        dialog.cancel();
      }
    });
    builder.setNegativeButton(
      context.getString(R.string.close),
      new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int _id) {
        dialog.cancel();
      }
    });
    AlertDialog alert = builder.create();
    alert.show();
  }

  public void openWebView(ProductItem productItem) {
    // WebView reads type and lang from shared preferences
    // So, just puts arguments here.
    Intent intent = new Intent(this, WebViewActivity.class);
    if (productItem != null) {
      intent.putExtra(Constant.kEan, productItem.getEan());
      intent.putExtra(Constant.kReg, productItem.getReg());
      intent.putExtra(Constant.kSeq, productItem.getSeq());
    }
    startActivity(intent);
    overridePendingTransition(R.anim.slide_leave,
                              R.anim.slide_enter);
  }
}
