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
import java.util.Set;

import org.oddb.generika.BaseActivity;
import org.oddb.generika.data.DataManager;
import org.oddb.generika.model.Product;
import org.oddb.generika.model.Receipt;
import org.oddb.generika.network.ProductInfoFetcher;
import org.oddb.generika.ui.list.GenerikaListAdapter;
import org.oddb.generika.ui.list.ProductListAdapter;
import org.oddb.generika.ui.list.ReceiptListAdapter;
import org.oddb.generika.util.Constant;


public class MainActivity extends BaseActivity implements
  GenerikaListAdapter.ListItemListener,
  ProductInfoFetcher.FetchCallback<ProductInfoFetcher.FetchResult> {
  private static final String TAG = "Main";

  // view
  private DrawerLayout drawerLayout;
  private ActionBarDrawerToggle drawerToggle;
  private CharSequence title;
  private ListView listView;
  private ActionBar actionBar;
  private NavigationView navigationView;
  private FloatingActionButton actionButton;

  private EditText searchBox;

  private String sourceType;
  private DataManager dataManager;
  private GenerikaListAdapter listAdapter; // Product / Receipt

  // network (headless fragment)
  private boolean fetching = false;
  private ProductInfoFetcher fetcher;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    String sourceType_, title_;

    Intent intent = getIntent();
    Log.d(TAG, "(onCreate) intent: " + intent);
    Set<String> categories = intent.getCategories();
    // from import activity, or back from settings etc.
    if ((categories == null ||
         !categories.contains(Intent.CATEGORY_LAUNCHER)) &&
         !Intent.ACTION_MAIN.equals(intent.getAction())) {
      // TODO: set alert with extras
      Bundle extras = intent.getExtras();
      Log.d(TAG, "(onCreate) extras: " + extras);
      sourceType_ = Constant.SOURCE_TYPE_AMKJSON;
      title_ = context.getString(R.string.prescriptions);
    } else { // from launcher
      sourceType_ = Constant.SOURCE_TYPE_BARCODE;
      title_ = context.getString(R.string.drugs);
    }

    this.title = title_;
    this.dataManager = new DataManager(sourceType_);

    initViews(sourceType_);
    switchSource(sourceType_);
  }

  /**
   * Switches source and list adapter.
   *
   * sourceType, listAdapter and fetcher will be set.
   *
   * @param String sourceType amkjson/barcode
   * @return void
   */
  private void switchSource(String sourceType_) {
    Log.d(TAG, "(switchSource) sourceType: " + sourceType_);
    this.sourceType = sourceType_;

    dataManager.bindDataBySourceType(sourceType);
    initData();

    if (sourceType.equals(Constant.SOURCE_TYPE_BARCODE)) {
      this.listAdapter = new ProductListAdapter(dataManager.getProducts());
      this.fetcher = buildProductInfoFetcher(context);
    } else if (sourceType.equals(Constant.SOURCE_TYPE_AMKJSON)) {
      this.listAdapter = new ReceiptListAdapter(dataManager.getReceipts());
      this.fetcher = null;
    }

    // change list adapter
    listAdapter.setCallback(this);
    listAdapter.refreshAll();

    listView.setAdapter(listAdapter);

    if (sourceType.equals(Constant.SOURCE_TYPE_AMKJSON)) {
      searchBox.setHint(context.getString(R.string.receipt_search_box_hint));
      // TODO: set valid action for fab
      actionButton.setVisibility(View.GONE);
    } else {
      searchBox.setHint(context.getString(R.string.product_search_box_hint));
      actionButton.setVisibility(View.VISIBLE);
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    dataManager.release();
  }

  private ProductInfoFetcher buildProductInfoFetcher(Context context_) {
    FragmentManager fragmentManager = getSupportFragmentManager();
    Fragment fragment = fragmentManager.findFragmentByTag(
      ProductInfoFetcher.TAG);
    if (fragment == null) {
      // check search lang in preference
      SharedPreferences sharedPreferences = PreferenceManager
        .getDefaultSharedPreferences(context_);
      String searchLang = sharedPreferences.getString(
        Constant.kSearchLang, Constant.LANG_DE);
      String urlBase = String.format(
        Constant.API_URL_PATH, searchLang);
      fragment = ProductInfoFetcher.getInstance(fragmentManager, urlBase);
    }
    return (ProductInfoFetcher)fragment;
  }

  private void initData() {
    // TODO:
    if (sourceType.equals(Constant.SOURCE_TYPE_AMKJSON)) {
      return;
    }

    RealmList<Product> products = dataManager.getProductsList();
    if (products.size() == 0) {
      dataManager.preparePlaceholder();
    }

    // Check new product insertion via barcode reader
    products.removeAllChangeListeners();
    products.addChangeListener(
      new OrderedRealmCollectionChangeListener<RealmList<Product>>() {
      @Override
      public void onChange(
        RealmList<Product> items, OrderedCollectionChangeSet changeSet) {
        Log.d(TAG, "(addChangeListener) items.size: " + items.size());

        int insertions[] = changeSet.getInsertions();
        if (insertions != null && insertions.length == 1) {  // new scan
          int i = insertions[0];
          Log.d(TAG, "(addChangeListener) inserttion: " + i);
          Product product = (Product)items.get(i);
          if (product.getEan().equals(Constant.INIT_DATA.get("ean"))) {
            return; // do nothing for placeholder row
          }
          // invoke async api call
          startFetching(product);
        }
      }
    });
  }

  private void initViews(String sourceType_) {
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

    this.actionBar = getSupportActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setHomeButtonEnabled(true);

    this.searchBox = (EditText)findViewById(R.id.search_box);
    setupSearchBox();

    this.listView = (ListView)findViewById(R.id.list_view);

    // drawer navigation
    this.navigationView = (NavigationView)findViewById(
      R.id.navigation_view);
    navigationView.setNavigationItemSelectedListener(
      new NavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(MenuItem menuItem) {
          Log.d(TAG, "(onNavigationItemSelected) menuItem: " + menuItem);

          searchBox.setCursorVisible(false);
          searchBox.clearFocus();
          actionButton.setVisibility(View.VISIBLE);

          if (!menuItem.isChecked()) {
            String name = getResources().getResourceEntryName(
              menuItem.getItemId());
            Log.d(TAG, "(onNavigationItemSelected) name: " + name);
            String nextSourceType;
            if (name.contains("receipt")) {
              nextSourceType = Constant.SOURCE_TYPE_AMKJSON;
            } else {  // back to default
              nextSourceType = Constant.SOURCE_TYPE_BARCODE;
            }
            title = menuItem.getTitle(); // updated `title`
            switchSource(nextSourceType);
            menuItem.setChecked(true);
          }
          drawerLayout.closeDrawers();
          Toast.makeText(MainActivity.this, title, Toast.LENGTH_LONG).show();
          return true;
        }
    });

    navigationView.post(new Runnable() {
      @Override
      public void run() {
        Log.d(TAG, "(initViews) sourceType: " + sourceType_);
        if (sourceType_.equals(Constant.SOURCE_TYPE_AMKJSON)) {
          navigationView.setCheckedItem(R.id.navigation_item_receipts);
        } else {
          navigationView.setCheckedItem(R.id.navigation_item_products);
        }
      }
    });

    this.actionButton = (FloatingActionButton)findViewById(R.id.action_button);
    actionButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (sourceType.equals(Constant.SOURCE_TYPE_BARCODE)) {
          // product
          Intent intent = new Intent(
            MainActivity.this, BarcodeCaptureActivity.class);
          intent.putExtra(Constant.kAutoFocus, true);
          intent.putExtra(Constant.kUseFlash, true);
          startActivityForResult(intent, Constant.RC_BARCODE_CAPTURE);
        } else if (sourceType.equals(Constant.SOURCE_TYPE_AMKJSON)) {
          // receipt
          // TODO
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
          if (filterString.length() == 0) { // back to all items
            if (sourceType.equals(Constant.SOURCE_TYPE_AMKJSON)) {
              RealmResults<Receipt> receipts = dataManager.getReceipts();
              listAdapter.updateItems(receipts);
            } else { // barcode drugs
              RealmResults<Product> products = dataManager.getProducts();
              listAdapter.updateItems(products);
            }
          }
          return;
        }
        if (sourceType.equals(Constant.SOURCE_TYPE_AMKJSON)) {
          RealmResults<Receipt> receipts = dataManager
            .findReceiptsByProperties(filterString);
          listAdapter.updateItems(receipts);
        } else { // barcode drugs
          RealmResults<Product> products = dataManager
            .findProductsByProperties(filterString);
          listAdapter.updateItems(products);
        }
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
          actionButton.setVisibility(View.VISIBLE);
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
          actionButton.setVisibility(View.GONE);
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
            // use Product's Barcode
            Product.Barcode barcode_ = new Product.Barcode();
            barcode_.setValue(barcode.displayValue);
            barcode_.setFilepath(filepath);
            // save record into realm (next: changeset listener)
            dataManager.addProduct(barcode_);
          }
        } else {
          Log.d(TAG, "(onActivityResult) Barcode not found");
        }
      } else {
        Log.d(TAG, "(onActivityResult) status: " +
          CommonStatusCodes.getStatusCodeString(resultCode));
      }
    } else {
      Log.d(TAG, "(onActivityResult) requestCode: " + requestCode);
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  // -- GenerikaListAdapter.ListItemListener

  @Override
  public void onDelete(String id) {
    // id is primary key (itemTag)
    if (sourceType.equals(Constant.SOURCE_TYPE_AMKJSON)) {
      dataManager.deleteReceipt(id);
    } else {
      dataManager.deleteProduct(id);
    }
  }

  @Override
  public void onExpiresAtChange(String id, Date newDate) {
    Log.d(TAG, "(onExpiresAtChange) date: " + newDate);  // local time

    HashMap<String, String> properties = new HashMap<String, String>();
    properties.put("expiresAt", Product.makeExpiresAt(newDate));
    Log.d(TAG, "(onExpiresAtChange) expiresAt: " +
               Product.makeExpiresAt(newDate));
    dataManager.updateProduct(id, properties);
  }

  private void startFetching(Product product) {
    if (!fetching && fetcher != null) {
      fetcher.invokeFetch(product);
      this.fetching = true;
    }
  }

  // -- ProductInfoFetcher.FetchCallback

  @Override
  public void updateFromFetch(ProductInfoFetcher.FetchResult result) {
    Log.d(TAG, "(updateFromFetch) result: " + result);
    if (result == null) { return; }
    if (result.errorMessage != null) {
      alertDialog("", result.errorMessage);
    } else if (result.map != null) {
      final String id = result.id;
      Log.d(TAG, "(updateFromFetch) resut.id: " + id);

      final HashMap<String, String> properties = result.map;
      // NOTE:
      // realm is not transferred to background async task thread
      // it's not accecible.
      DataManager dataManager_ = new DataManager(Constant.SOURCE_TYPE_BARCODE);
      try {
        final Product product = dataManager_.getProductById(id);
        if (product == null) { return; }
        product.removeAllChangeListeners();
        // this listener is invoked after update transaction below
        product.addChangeListener(new RealmChangeListener<Product>() {
          @Override
          public void onChange(Product product_) {
            if (product_ == null || !product_.isValid()) { return; }
            // only once (remove self)
            product_.removeAllChangeListeners();
            if (product_.getName() != null && product_.getSize() != null) {
              // TODO: stop redraw all items on listview
              // redraw this row
              listAdapter.refresh(product_, listView);

              Log.d(TAG, "(updateFromFetch/onChange) product.name: " +
                    product_.getName());
              // notify result to user
              String title = context.getString(
                R.string.fetch_info_result_dialog_title);
              String message = product_.toMessage();
              alertDialog(title, message, product_);
            }
          }
        });
        // above onChange will be called
        dataManager_.updateProduct(product.getId(), properties);
      } finally {
        dataManager_.release();
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
    if (fetcher != null) {
      fetcher.cancelFetch();
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
    String title, String message, final Product product) {
    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle(title);
    builder.setMessage(message);
    builder.setCancelable(true);

    builder.setPositiveButton(
      context.getString(R.string.open),
      new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int _id) {
        openWebView(product);
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

  public void openWebView(Product product) {
    // WebView reads type and lang from shared preferences
    // So, just puts arguments here.
    Intent intent = new Intent(this, WebViewActivity.class);
    if (product != null) {
      intent.putExtra(Constant.kEan, product.getEan());
      intent.putExtra(Constant.kReg, product.getReg());
      intent.putExtra(Constant.kSeq, product.getSeq());
    }
    startActivity(intent);
    overridePendingTransition(R.anim.slide_leave,
                              R.anim.slide_enter);
  }
}
