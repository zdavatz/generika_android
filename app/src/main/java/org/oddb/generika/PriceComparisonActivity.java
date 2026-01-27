package org.oddb.generika;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.appbar.AppBarLayout;

import org.oddb.generika.data.AmikoDBManager;
import org.oddb.generika.model.AmikoDBPriceComparison;

import java.util.ArrayList;
import java.util.List;

public class PriceComparisonActivity extends AppCompatActivity {
    public static final String EXTRA_GTIN = "gtin";

    private List<AmikoDBPriceComparison> comparisons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_price_comparison);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.price_comparison);
        }

        String gtin = getIntent().getStringExtra(EXTRA_GTIN);
        if (gtin != null) {
            AmikoDBManager manager = AmikoDBManager.getInstance(this);
            comparisons = AmikoDBPriceComparison.comparePrice(manager, gtin);
        }

        if (comparisons == null) {
            comparisons = new ArrayList<>();
        }

        populateTable(comparisons);

        View coordinator = findViewById(R.id.coordinator);
        ViewCompat.setOnApplyWindowInsetsListener(coordinator, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Remove the top padding from the root view so AppBarLayout can go to the top
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Handle top padding for the AppBarLayout specifically
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

    private void populateTable(List<AmikoDBPriceComparison> data) {
        TableLayout table = findViewById(R.id.comparison_table);
        table.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        int rowCount = 0;
        for (AmikoDBPriceComparison comparison : data) {
            addRow(table, inflater, "", "", rowCount++);
            addRow(table, inflater, "Präparat", comparison.package_.name, rowCount++);
            addRow(table, inflater, "Zulassungsinhaber", comparison.package_.parent.auth, rowCount++);
            addRow(table, inflater, "Packungsgrösse", comparison.package_.dosage + " " + comparison.package_.units, rowCount++);
            addRow(table, inflater, "PP", comparison.package_.pp, rowCount++);
            addRow(table, inflater, "% (Preisunterschied in Prozent)", String.valueOf((int) Math.floor(comparison.priceDifferenceInPercentage)), rowCount++);
            addRow(table, inflater, "SB", comparison.package_.selbstbehalt(), rowCount++);
        }
    }

    private void addRow(TableLayout table, LayoutInflater inflater, String labelText, String valueText, int position) {
        View rowView = inflater.inflate(R.layout.item_price_comparison_row, table, false);
        TextView label = rowView.findViewById(R.id.label);
        TextView value = rowView.findViewById(R.id.value);

        label.setText(labelText);
        value.setText(valueText);

        if (position % 7 == 0) {
            rowView.setBackgroundColor(0xFFEEEEEE);
        } else {
            rowView.setBackgroundColor(0x00000000);
        }
        table.addView(rowView);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
