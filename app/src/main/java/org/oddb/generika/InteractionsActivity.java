package org.oddb.generika;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

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
import org.oddb.generika.data.InteractionsDBManager;
import org.oddb.generika.data.InteractionsDBManager.DrugInfo;
import org.oddb.generika.data.InteractionsDBManager.InteractionResult;
import org.oddb.generika.model.AmikoDBRow;
import org.oddb.generika.util.Constant;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InteractionsActivity extends BaseActivity {
    private static final String TAG = "InteractionsActivity";
    private WebView webView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interactions);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(context.getString(R.string.interactions));
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

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
        controller.setAppearanceLightStatusBars(true);

        this.webView = findViewById(R.id.web_view);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(false);
        webSettings.setDefaultTextEncodingName("utf-8");

        String[] eans = getIntent().getStringArrayExtra(Constant.kEans);
        if (eans != null) {
            loadInteractions(eans);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finishAfterTransition();
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadInteractions(String[] eans) {
        new Thread(() -> {
            try {
                Log.d(TAG, "loadInteractions: " + eans.length + " EANs");

                // Resolve EANs to DrugInfo (with ATC codes) via AmikoDBManager
                AmikoDBManager amikoDB = AmikoDBManager.getInstance(this);
                List<DrugInfo> drugs = new ArrayList<>();
                Set<String> seenAtc = new HashSet<>();

                for (String ean : eans) {
                    if (ean == null || ean.isEmpty()) continue;
                    Log.d(TAG, "Looking up EAN: " + ean);
                    ArrayList<AmikoDBRow> rows = amikoDB.findWithGtin(ean, null);
                    Log.d(TAG, "  findWithGtin returned " + rows.size() + " rows");
                    if (!rows.isEmpty()) {
                        AmikoDBRow row = rows.get(0);
                        // atc field format: "N06AB06;Sertralin" — extract code before semicolon
                        String atc = row.atc;
                        Log.d(TAG, "  raw atc: " + atc + ", title: " + row.title);
                        if (atc != null && atc.contains(";")) {
                            atc = atc.substring(0, atc.indexOf(";")).trim();
                        }
                        String name = row.title;
                        if (name != null && name.contains(",")) {
                            name = name.substring(0, name.indexOf(",")).trim();
                        }
                        if (atc != null && !atc.isEmpty() && !seenAtc.contains(atc)) {
                            seenAtc.add(atc);
                            drugs.add(new DrugInfo(ean, name != null ? name : ean, atc));
                            Log.d(TAG, "  Added drug: " + name + " [" + atc + "]");
                        }
                    } else {
                        Log.w(TAG, "No AmikoDBRow found for EAN: " + ean);
                    }
                }

                Log.d(TAG, "Resolved " + drugs.size() + " drugs with ATC codes");

                // Look up interactions
                InteractionsDBManager interDB = InteractionsDBManager.getInstance(this);
                Log.d(TAG, "InteractionsDB exists: " + interDB.checkAllFilesExists());
                List<InteractionResult> results = interDB.getInteractions(drugs);
                Log.d(TAG, "Found " + results.size() + " interactions");

                // Build HTML
                String html = buildHtml(drugs, results);

                runOnUiThread(() -> {
                    webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading interactions", e);
                String errorHtml = "<html><body><h2>Error</h2><p>" + e.getMessage() + "</p></body></html>";
                runOnUiThread(() -> {
                    webView.loadDataWithBaseURL(null, errorHtml, "text/html", "utf-8", null);
                });
            }
        }).start();
    }

    private String buildHtml(List<DrugInfo> drugs, List<InteractionResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='utf-8'>");
        sb.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        sb.append("<style>");
        sb.append("body { font-family: -apple-system, sans-serif; font-size: 14px; margin: 8px; color: #333; }");
        sb.append("h2 { font-size: 16px; margin: 12px 0 8px 0; }");
        sb.append(".drug-list { background: #f0f0f0; padding: 8px 12px; border-radius: 6px; margin-bottom: 12px; }");
        sb.append(".drug-item { padding: 2px 0; }");
        sb.append(".interaction { border-radius: 6px; padding: 10px 12px; margin-bottom: 10px; }");
        sb.append(".interaction-header { font-weight: bold; font-size: 13px; margin-bottom: 6px; }");
        sb.append(".interaction-text { font-size: 12px; line-height: 1.4; }");
        sb.append(".no-results { text-align: center; color: #888; padding: 40px 20px; }");
        sb.append("</style></head><body>");

        // Drug list
        sb.append("<h2>").append(drugs.size()).append(" Medikamente</h2>");
        sb.append("<div class='drug-list'>");
        for (DrugInfo drug : drugs) {
            sb.append("<div class='drug-item'>").append(escapeHtml(drug.name));
            sb.append(" <small>[").append(escapeHtml(drug.atcCode)).append("]</small></div>");
        }
        sb.append("</div>");

        if (results.isEmpty()) {
            sb.append("<div class='no-results'>Keine Interaktionen gefunden.</div>");
        } else {
            sb.append("<h2>").append(results.size()).append(" Interaktionen</h2>");
            for (InteractionResult r : results) {
                sb.append("<div class='interaction' style='background-color: ").append(r.color).append(";'>");
                sb.append("<div class='interaction-header'>").append(escapeHtml(r.header)).append("</div>");
                sb.append("<div class='interaction-text'>").append(r.text).append("</div>");
                sb.append("</div>");
            }
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
