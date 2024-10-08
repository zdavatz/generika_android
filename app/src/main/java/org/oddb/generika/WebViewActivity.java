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

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.preference.PreferenceManager;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.KeyEvent;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebChromeClient;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.Arrays;

import org.oddb.generika.util.Constant;


public class WebViewActivity extends BaseActivity {
  private static final String TAG = "WebViewActivity";

  private WebView webView;

  private ProgressBar progressBar;

  private SharedPreferences sharedPreferences;
  private int pageAction = Constant.PAGE_ACTION_TOP;

  private String searchType;
  private String searchLang;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // enable progress bar (see also initViews)
    getWindow().requestFeature(Window.FEATURE_PROGRESS);

    setContentView(R.layout.activity_web_view);

    // To fix a bug (locale will be back to device default) on WebView
    // >= Android N (7.0)
    if (Build.VERSION.SDK_INT >= Constant.VERSION_24__7_0) {
      enforceLocale();
    }

    getWindow().setFeatureInt(
      Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_ON);

    this.sharedPreferences = PreferenceManager
      .getDefaultSharedPreferences(context);

    loadSettings();
    initViews();

    if (webView != null && savedInstanceState == null) {
      String urlString = buildUrl();
      webView.loadUrl(urlString);
    }
  }

  @Override
  protected void onDestroy() {
    this.webView = null;

    super.onDestroy();
  }

  @Override
  protected void onResume() {
    super.onResume();
  }

  private void loadSettings() {
    Intent intent = getIntent();
    String ean = intent.getStringExtra(Constant.kEan);
    if (ean != null && !ean.equals("")) {
      this.searchType = sharedPreferences.getString(
        Constant.kSearchType, Constant.TYPE_PV);

      if (!ean.equals(Constant.INIT_DATA.get("ean"))) {
        this.pageAction = Constant.PAGE_ACTION_SEARCH;
      }
    }

    String[] uniqueEans = intent.getStringArrayExtra(Constant.kEans);
    if (uniqueEans != null) {
      this.pageAction = Constant.PAGE_ACTION_INTERACTIONS;
    }

    this.searchLang = sharedPreferences.getString(
      Constant.kSearchLang, Constant.LANG_DE);
  }

  private void initViews() {
    Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
    toolbar.setTitle(context.getString(R.string.oddb_org));
    if (pageAction == Constant.PAGE_ACTION_SEARCH ||
        pageAction == Constant.PAGE_ACTION_INTERACTIONS) {
      toolbar.setSubtitle(String.format(
          "%s - %s", searchLang, buildSearchTypeName()));
    }
    setSupportActionBar(toolbar);

    ActionBar actionBar = getSupportActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setDisplayShowHomeEnabled(true);

    this.webView = (WebView)findViewById(R.id.web_view);
    webView.setPadding(0, 0, 0, 0);
    webView.setScrollbarFadingEnabled(true);
    webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);

    WebSettings webSettings = webView.getSettings();
    webSettings.setJavaScriptEnabled(true);
    webSettings.setUserAgentString(Constant.WEB_USER_AGENT);

    // fix initial zoom-in (as zoom-out at start)
    webView.setInitialScale(1);
    webSettings.setUseWideViewPort(true);

    // allow (pinch) zoom
    webSettings.setBuiltInZoomControls(true);
    webSettings.setDisplayZoomControls(false);

    // lined progress bar
    this.progressBar = (ProgressBar)findViewById(
      R.id.web_view_progress_bar);
    webView.setWebChromeClient(new WebChromeClient() {
      public void onProgressChanged(WebView view, int progress) {
        if (progress < 100 &&
            progressBar.getVisibility() == ProgressBar.GONE) {
          progressBar.setVisibility(ProgressBar.VISIBLE);
        }
        // max: 100 (see `activity_web_view.xml`)
        if (Build.VERSION.SDK_INT >= Constant.VERSION_24__7_0) {
          progressBar.setProgress(progress, true);
        } else {
          progressBar.setProgress(progress);
        }
        if (progress == 100) {
          progressBar.setVisibility(ProgressBar.GONE);
        }
      }
    });
    webView.setWebViewClient(new WebViewClient() {
      public void onReceivedSslError(
        WebView view, int errorCode, String description, String failingUrl) {
        Toast.makeText(context, description, Toast.LENGTH_SHORT).show();
      }

      @Override
      public boolean shouldOverrideUrlLoading(WebView view, String url) {
        Log.d(TAG, "(shouldOverrideUrlLoading) url: " + url);
        view.loadUrl(url);
        return true;
      }
    });
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    // on tap `◁` (device back button)
    if (event.getAction() == KeyEvent.ACTION_DOWN) {
      if (keyCode == KeyEvent.KEYCODE_BACK) {
        if (webView.canGoBack()) {
          webView.goBack();
        } else {
          finishAfterTransition();
        }
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

    webView.saveState(outState);
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);

    webView.restoreState(savedInstanceState);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      finishAfterTransition();
    }
    return super.onOptionsItemSelected(item);
  }

  /**
   * Returns R.string.{pv|pi|fi}_fullname
   */
  private String buildSearchTypeName() {
    if (searchType == null) {
      return context.getString(R.string.interactions);
    }
    String[] s = {Constant.TYPE_PV, Constant.TYPE_PI, Constant.TYPE_FI};
    if (Arrays.asList(s).contains(searchType)) {
      try {
        String keyName = String.format("%s_fullname", searchType);
        String packageName = getPackageName();
        int resourceId = getResources().getIdentifier(
          keyName, "string", packageName);
        return getString(resourceId);
      } catch (Exception e) {
        Log.d(TAG, "(buildSearchTypeName) e: " + e.getMessage());
      }
      return "";
    } else {
      return "";
    }
  }

  /**
   * Builds url for generika.cc using search{Type|Lang} instance vars, like:
   *
   * https://generika.cc/{de|fr}/generika/{compare|patinfo|fachinfo}/<args>...
   */
  private String buildUrl() {
    Log.d(TAG, "(buildUrl) searchType: " + searchType);
    Log.d(TAG, "(buildUrl) searchLang: " + searchLang);

    String urlString = String.format("https://%s/", Constant.WEB_URL_HOST);

    Intent intent = getIntent();
    String ean = intent.getStringExtra(Constant.kEan);
    if (pageAction == Constant.PAGE_ACTION_TOP) {
      if (ean != null && ean.equals(Constant.INIT_DATA.get("ean"))) {
        // placeholder
        urlString += String.format("%s/%s/", searchLang, Constant.URL_FLAVOR);
      }
      Log.d(TAG, "(buildUrl) urlString: " + urlString);
      return urlString;
    } else if (pageAction == Constant.PAGE_ACTION_INTERACTIONS) {
      String[] uniqueEans = intent.getStringArrayExtra(Constant.kEans);
      if (uniqueEans == null) {
        Log.d(TAG, "(buildUrl) urlString: " + urlString);
        return urlString;
      }
      urlString += String.format(
        Constant.WEB_URL_PATH_INTERACTION,
        searchLang,
        TextUtils.join(",", uniqueEans));
      Log.d(TAG, "(buildUrl) urlString: " + urlString);
      return urlString;
    }

    String reg = intent.getStringExtra(Constant.kReg);
    String seq = intent.getStringExtra(Constant.kSeq);

    if (searchType.equals(Constant.TYPE_PV)) {  // preisvergleich
      if (ean != null && !ean.equals("")) {
        urlString += String.format(
          Constant.WEB_URL_PATH_COMPARE, searchLang, ean);
      }
    } else if (searchType.equals(Constant.TYPE_PI)) {  // patinfo

      if (reg != null && !reg.equals("") && seq != null && !seq.equals("")) {
        urlString += String.format(
          Constant.WEB_URL_PATH_PATINFO, searchLang, reg, seq);
      }
    } else if (searchType.equals(Constant.TYPE_FI)) {  // fachinfo
      if (reg != null && !reg.equals("")) {
        urlString += String.format(
          Constant.WEB_URL_PATH_FACHINFO, searchLang, reg);
      }
    }
    Log.d(TAG, "(buildUrl) urlString: " + urlString);
    return urlString;
  }
}
