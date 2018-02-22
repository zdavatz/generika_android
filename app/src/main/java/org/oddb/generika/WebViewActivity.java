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

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.KeyEvent;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebChromeClient;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.oddb.generika.app.BaseActivity;


public class WebViewActivity extends BaseActivity {

  // TODO: enable these values for patinfo
  public static final String Reg = "reg";
  public static final String Seq = "seq";
  public static final String Pack = "pack";

  private WebView webView;

  private Activity activity;
  private ProgressBar progressBar;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // enable progress bar (see also initViews)
    getWindow().requestFeature(Window.FEATURE_PROGRESS);

    setContentView(R.layout.activity_web_view);

    getWindow().setFeatureInt(
      Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_ON);

    initViews();

    // TODO: patinfo
    String urlString = "https://i.ch.oddb.org/";
    String reg = getIntent().getStringExtra(Reg);
    if (reg != null && reg != "") {
      urlString += "/de/mobile/fachinfo/reg/" + reg;
    }

    if (webView != null && savedInstanceState == null) {
      webView.loadUrl(urlString);
    }
  }

  @Override
  protected void onDestroy() {
    this.webView = null;

    super.onDestroy();
  }

  private void initViews() {
    Context context = (Context)this;
    this.activity = this;

    Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
    toolbar.setTitle(context.getString(R.string.oddb_org));
    toolbar.setSubtitle("https://i.ch.oddb.org/...");
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
        progressBar.setProgress(progress);
        if (progress < 100 &&
            progressBar.getVisibility() == ProgressBar.GONE) {
          progressBar.setVisibility(ProgressBar.VISIBLE);

        }
        if (progress == 100) {
          progressBar.setVisibility(ProgressBar.GONE);
        }
      }
    });
    webView.setWebViewClient(new WebViewClient() {
      public void onReceivedSslError(
        WebView view, int errorCode, String description, String failingUrl) {
        Toast.makeText(activity, description, Toast.LENGTH_SHORT).show();
      }

      @Override
      public boolean shouldOverrideUrlLoading(WebView view, String url) {
        view.loadUrl(url);
        return true;
      }
    });
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    // on tap `◁` (device back button)
    if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
      webView.goBack();
      return true;
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
      finish();
    }
    return super.onOptionsItemSelected(item);
  }
}
