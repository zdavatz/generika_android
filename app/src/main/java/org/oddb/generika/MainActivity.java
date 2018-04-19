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

import android.app.ActivityOptions;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import android.support.v4.view.GravityCompat;

import android.support.v4.widget.DrawerLayout;

import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;

import android.support.v7.preference.PreferenceManager;

import com.google.android.gms.common.api.CommonStatusCodes;

import com.google.android.gms.vision.barcode.Barcode;

import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.RealmChangeListener;
import io.realm.RealmList;
import io.realm.RealmResults;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

import org.oddb.generika.BaseActivity;
import org.oddb.generika.barcode.BarcodeExtractor;
import org.oddb.generika.data.DataManager;
import org.oddb.generika.model.Product;
import org.oddb.generika.model.Receipt;
import org.oddb.generika.network.ProductInfoFetcher;
import org.oddb.generika.ui.MessageDialog;
import org.oddb.generika.ui.list.GenerikaListAdapter;
import org.oddb.generika.ui.list.ProductListAdapter;
import org.oddb.generika.ui.list.ReceiptListAdapter;
import org.oddb.generika.util.Constant;


public class MainActivity extends BaseActivity implements
  GenerikaListAdapter.ListItemListener,
  ProductInfoFetcher.FetchTaskCallback<ProductInfoFetcher.FetchResult> {
  private static final String TAG = "MainActivity";

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

    boolean importAction = false;
    Intent intent = getIntent();
    Log.d(TAG, "(onCreate) intent: " + intent);
    Set<String> categories = intent.getCategories();
    // from import activity, or back from settings etc.
    if ((categories == null ||
         !categories.contains(Intent.CATEGORY_LAUNCHER)) &&
         !Intent.ACTION_MAIN.equals(intent.getAction())) {
      this.sourceType = Constant.SOURCE_TYPE_AMKJSON;
      this.title = context.getString(R.string.amkfiles);
      importAction = true;
    } else { // from launcher
      this.sourceType = Constant.SOURCE_TYPE_BARCODE;
      this.title = context.getString(R.string.barcodes);
    }

    this.dataManager = new DataManager(this.sourceType);

    if (importAction) {
      Bundle extras = intent.getExtras();
      if (extras != null) {
        handleImportResult(extras);
      }
    }

    initViews();
    switchSource(this.sourceType);
  }

  private void handleImportResult(Bundle extras) {
    String message = extras.getString("message");
    Log.d(TAG, "(handleImportResult) message: " + message);
    if (message != null && !message.equals("")) {
      String status = extras.getString("status");
      Log.d(TAG, "(handleImportResult) status: " + status);
      int result = Constant.IMPORT_FAILURE_UNKNOWN;
      if (status != null) {
        result = Integer.parseInt(status);
      }
      if (result != Constant.IMPORT_SUCCESS) {
        alertDialog(context.getString(R.string.title_import_failure), message);
      } else {
        String hashedKey = extras.getString("hashedKey");

        alertDialog(
          context.getString(R.string.title_import_success),
          message,
          new MessageDialog.OnChangeListener() {
            @Override
            public void onOk() {
              if (hashedKey != null) { openReceipt(hashedKey); }
            }
            @Override
            public void onCancel() {
              // pass
            }
          });
      }
    }
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
      this.fetcher = buildProductInfoFetcher();
    } else if (sourceType.equals(Constant.SOURCE_TYPE_AMKJSON)) {
      this.listAdapter = new ReceiptListAdapter(dataManager.getReceipts());
      this.fetcher = null;
    }

    // change list adapter
    listAdapter.setCallback(this);
    listAdapter.refreshAll();

    listView.setAdapter(listAdapter);

    searchBox.setText("");
    if (sourceType.equals(Constant.SOURCE_TYPE_AMKJSON)) {
      searchBox.setHint(context.getString(R.string.receipt_search_box_hint));
    } else {
      searchBox.setHint(context.getString(R.string.product_search_box_hint));
    }
    actionButton.setVisibility(View.VISIBLE);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    dataManager.release();
  }

  private ProductInfoFetcher buildProductInfoFetcher() {
    FragmentManager fragmentManager = getSupportFragmentManager();
    Fragment fetcher_ = fragmentManager.findFragmentByTag(
      ProductInfoFetcher.TAG);
    if (fetcher_ == null) {
      // check search lang in preference
      SharedPreferences sharedPreferences = PreferenceManager
        .getDefaultSharedPreferences(context);
      String searchLang = sharedPreferences.getString(
        Constant.kSearchLang, Constant.LANG_DE);
      String urlBase = String.format(
        Constant.API_URL_PATH, searchLang);

      fetcher_ = ProductInfoFetcher.getInstance(
        fragmentManager, this, urlBase);
    }
    return (ProductInfoFetcher)fetcher_;
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
          String ean = product.getEan();
          if (ean.equals(Constant.INIT_DATA.get("ean"))) {
            return; // do nothing for placeholder row
          }
          if (ean.length() != 13) {
            // GTIN (via GS1 DataMatrix) is saved as EAN
            String errorMessage = String.format(
              context.getString(R.string.product_not_found_gtin), ean);
            alertDialog("", errorMessage);
            return;
          }
          // invoke async api call
          startFetching(product);
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
            // navigation_item_{products,receipts}
            if (name.equals("navigation_item_receipts")) {
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

    final String sourceType_ = this.sourceType;
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
        Intent intent;
        if (sourceType.equals(Constant.SOURCE_TYPE_BARCODE)) {
          // product (barcode capture)
          intent = new Intent(
            MainActivity.this, BarcodeCaptureActivity.class);
          intent.putExtra(Constant.kAutoFocus, true);
          intent.putExtra(Constant.kUseFlash, true);
          startActivityForResult(intent, Constant.RC_BARCODE_CAPTURE);
        } else if (sourceType.equals(Constant.SOURCE_TYPE_AMKJSON)) {
          // receipt (document provider)
          intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
          intent.addCategory(Intent.CATEGORY_OPENABLE);
          String[] mimeTypes = new String[]{
            "application/amk", "application/json", "application/octet-stream",
            "text/plain"};
          intent.setType(TextUtils.join("|", mimeTypes));
          intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
          startActivityForResult(intent, Constant.RC_FILE_PROVIDER);
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

    // NOTE:
    // this is a fix for cursor and button visibility in delay (
    // for device close button)
    //
    // ratio: assumed ratio for minimum keyboard height against the screen
    // delay: action will be invoked after the time
    final float ratio = (float)0.15;
    final long delay = 250;
    searchBox.getViewTreeObserver().addOnGlobalLayoutListener(
      new ViewTreeObserver.OnGlobalLayoutListener() {
        private boolean isKeyboardVisible() {
          View rootView = searchBox.getRootView();
          Rect r = new Rect();
          rootView.getWindowVisibleDisplayFrame(r);
          int rootViewHeight = rootView.getHeight();
          int keyboardHeight = rootViewHeight - r.bottom;
          return keyboardHeight > rootViewHeight * ratio;
        }

        @Override
        public void onGlobalLayout() {
          final Handler handler = new Handler();
          handler.postDelayed(new Runnable() {
            @Override
            public void run() {
              if (isKeyboardVisible()) {
                // Log.d(TAG, "(onGlobalLayout) keyboard: shown");
                if (actionButton.getVisibility() == View.VISIBLE &&
                    searchBox.hasFocus() &&
                    !searchBox.isCursorVisible()) {
                  // focused, but cursor and button visibility is not back
                  searchBox.setCursorVisible(true);
                  actionButton.setVisibility(View.GONE);
                }
              } else {
                // Log.d(TAG, "(onGlobalLayout) keyboard: hidden");
                if (actionButton.getVisibility() == View.GONE &&
                    searchBox.hasFocus() &&
                    searchBox.isCursorVisible()) {
                  // focus is remained, button is not back
                  searchBox.clearFocus();
                  actionButton.setVisibility(View.VISIBLE);
                }
              }
            }
          }, delay);
        }
      }
    );
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

    ActivityOptions options = ActivityOptions.makeBasic();
    Intent intent;
    switch (id) {
      case android.R.id.home:
        drawerLayout.openDrawer(GravityCompat.START);
        return true;
      case R.id.settings:
        intent = new Intent(this, SettingsActivity.class);
        startActivity(intent, options.toBundle());
        return true;
      case R.id.information:
        intent = new Intent(this, InformationActivity.class);
        startActivity(intent, options.toBundle());
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
          String value = barcode.displayValue;
          Log.d(TAG, "(onActivityResult) filepath: " + filepath);
          if (value == null || value.equals("")) {
            // TODO: remove file
            return;
          }
          int length = value.length();

          Product.Barcode barcode_ = null;
          if (length == 13) { // EAN 13
            Log.d(TAG, "(onActivityResult/EAN-13) barcode: " + value);
            barcode_ = new Product.Barcode();
            barcode_.setValue(value);
            barcode_.setFilepath(filepath);
          } else { // GS1 DataMatrix (GTIN)
            HashMap<String, String> result = BarcodeExtractor.extract(value);
            Log.d(TAG, "(onActivityResult/GS1 DataMatrix) result: " + result);
            String gtin = result.get(Constant.GS1_DM_AI_GTIN);
            if (result != null && gtin != null && gtin.length() == 14) {
              barcode_ = new Product.Barcode(result);
              barcode_.setFilepath(filepath);
            }
          }
          if (barcode_ != null) {
            dataManager.addProduct(barcode_);
          } else {
            File file = new File(filepath);
            if (file.exists()) {
              file.delete();
            }
            // wrong barcode is detected
            String errorMessage = String.format(
              context.getString(R.string.invalid_barcode_found), value);
            alertDialog("", "");
          }
        } else {
          Log.d(TAG, "(onActivityResult) Barcode not found");
        }
      } else {
        Log.d(TAG, "(onActivityResult) status: " +
          CommonStatusCodes.getStatusCodeString(resultCode));
      }
    } else if (requestCode == Constant.RC_FILE_PROVIDER) {
      if (resultCode == RESULT_OK) {
        Uri uri = null;
        if (data != null) {
          uri = data.getData();
          Log.d(TAG, "(onActivityResult) uri: " + uri);

          Intent intent = new Intent(this, ImporterActivity.class);
          intent.setData(uri);
          ActivityOptions options = ActivityOptions.makeBasic();
          startActivity(intent, options.toBundle());
        }
      } else {
        Log.d(TAG, "(onActivityResult) resultCode: " + resultCode);
      }
    } else {
      Log.d(TAG, "(onActivityResult) requestCode: " + requestCode);
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  private void alertDialog(String title, String message) {
    alertDialog(title, message, null);
  }

  // if the listener is given, dialog is going to have `ok` and `close` button
  // with callbacks. Otherwise, single button alert will be shown.
  private void alertDialog(
    String title, String message, MessageDialog.OnChangeListener listener) {
    int none = MessageDialog.TEXT_ID_NONE;
    int negativeTextId = none, positiveTextId = none;
    if (listener == null) { // "ok" (single) button
      positiveTextId = R.string.ok;
    } else { // "close" & "ok" button for import (receipt) or capture (barcode)
      negativeTextId = R.string.close;
      positiveTextId = R.string.open;
    }

    MessageDialog dialog = MessageDialog.newInstance(title, message);
    if (negativeTextId != none) { dialog.setNegativeTextId(negativeTextId); }
    if (positiveTextId != none) { dialog.setPositiveTextId(positiveTextId); }

    dialog.setListener(listener);

    FragmentTransaction tx = ((MainActivity)context)
      .getSupportFragmentManager().beginTransaction();
    tx.add(dialog, "MessageDialog");
    tx.commitAllowingStateLoss();
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
    ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(
      MainActivity.this);
    startActivity(intent, options.toBundle());
  }

  public void openReceipt(String hashedKey) {
    Intent intent = new Intent(this, ReceiptActivity.class);
    if (hashedKey != null) {
      intent.putExtra(Constant.kHashedKey, hashedKey);
    }
    ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(
      MainActivity.this);
    startActivity(intent, options.toBundle());
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

  // -- ProductInfoFetcher.FetchTaskCallback

  private void startFetching(Product product) {
    if (!fetching && fetcher != null) {
      fetcher.invokeFetch(product);
      this.fetching = true;
    }
  }

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
              // NOTE:
              // `updateFromFetch` will be called from `postOnExecute`, so this
              // `runOnUiThread` is not needed normally, but, it seems that
              // prevents an issue on first boot on Android 8.0? (not sure)
              ((MainActivity)context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                  listAdapter.refresh(product_, listView);
                  Log.d(TAG, "(updateFromFetch/onChange) product.name: " +
                        product_.getName());
                  // notify result to user
                  String title = context.getString(
                    R.string.fetch_info_result_dialog_title);
                  String message = product_.toMessage();
                  alertDialog(
                    title, message,
                    new MessageDialog.OnChangeListener() {
                      @Override
                      public void onOk() {
                        if (product_ != null) { openWebView(product_); }
                      }
                      @Override
                      public void onCancel() {
                        // pass
                      }
                    });
                }
              });
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
}
