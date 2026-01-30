package org.oddb.generika;

import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.appbar.AppBarLayout;

import org.oddb.generika.data.AmikoDBManager;
import org.oddb.generika.model.AmikoDBRow;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class PatinfoActivity extends BaseActivity {
    private static final String TAG = "PatinfoActivity";
    public static final String EXTRA_GTIN = "gtin";
    private WebView webView;

    private AmikoDBRow amikoDBRow;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patinfo);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setupEdgeToEdge();

        webView = findViewById(R.id.web_view);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());

        String gtin = getIntent().getStringExtra(EXTRA_GTIN);
        if (gtin != null) {
            AmikoDBManager dbManager = AmikoDBManager.getInstance(this);
            ArrayList<AmikoDBRow> rows = dbManager.findWithGtin(gtin, "FI");
            if (!rows.isEmpty()) {
                this.amikoDBRow = rows.get(0);
                webView.loadDataWithBaseURL("about:blank", getHTML(), "text/html", "UTF-8", null);
            }
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            if (this.amikoDBRow != null) {
                actionBar.setTitle(this.amikoDBRow.title);
            }
        }
    }

    private void setupEdgeToEdge() {
        View coordinator = findViewById(R.id.coordinator);
        ViewCompat.setOnApplyWindowInsetsListener(coordinator, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        AppBarLayout appBarLayout = findViewById(R.id.app_bar_layout);
        ViewCompat.setOnApplyWindowInsetsListener(appBarLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(true);
        }
    }

    private String getHTML() {
        if (amikoDBRow == null) return "";

        String colorSchemeFilename = "color-scheme-light.css";

        String colorCss = readAssetFile(colorSchemeFilename);
        String amikoCss = readAssetFile("amiko_stylesheet.css");

        String scalingMeta = "<meta name=\"viewport\" content=\"initial-scale=1.0\" />";
        String charsetMeta = "<meta charset=\"utf-8\" />";
        String colorSchemeMeta = "<meta name=\"supported-color-schemes\" content=\"light dark\" />";

        String headHtml = String.format("<head>%s\n%s\n%s\n<style type=\"text/css\">%s</style>\n<style type=\"text/css\">%s</style>\n</head>",
                charsetMeta,
                colorSchemeMeta,
                scalingMeta,
                colorCss,
                amikoCss);

        return amikoDBRow.content.replace("<head></head>", headHtml);
    }

    private String readAssetFile(String fileName) {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = getAssets().open(fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading asset file: " + fileName, e);
        }
        return sb.toString();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (amikoDBRow != null) {
            String[] titles = amikoDBRow.chapterTitles();
            if (titles != null && titles.length > 0) {
                SubMenu subMenu = menu.addSubMenu(Menu.NONE, 1000, Menu.NONE, "Chapters");
                subMenu.getItem().setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                for (int i = 0; i < titles.length; i++) {
                    subMenu.add(1, i, Menu.NONE, titles[i]);
                }
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getGroupId() == 1) {
            int index = item.getItemId();
            String[] ids = amikoDBRow.chapterIds();
            if (ids != null && index >= 0 && index < ids.length) {
                webView.evaluateJavascript("document.getElementById('"+ ids[index] + "').scrollIntoView(true);", null);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
