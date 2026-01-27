package org.oddb.generika;

import android.content.res.Configuration;
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

    private void populateTable(List<AmikoDBPriceComparison> data) {
        TableLayout table = findViewById(R.id.comparison_table);
        table.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        if (isLandscape) {
            addHeaderRowLand(table, inflater);
            int rowCount = 0;
            for (AmikoDBPriceComparison comparison : data) {
                addRowLand(table, inflater, comparison, rowCount++);
            }
        } else {
            int rowCount = 0;
            for (AmikoDBPriceComparison comparison : data) {
                addRowPortrait(table, inflater, "", "", rowCount++);
                addRowPortrait(table, inflater, "Präparat", comparison.package_.name, rowCount++);
                addRowPortrait(table, inflater, "Zulassungsinhaber", comparison.package_.parent.auth, rowCount++);
                addRowPortrait(table, inflater, "Packungsgrösse", comparison.package_.dosage + " " + comparison.package_.units, rowCount++);
                addRowPortrait(table, inflater, "PP", comparison.package_.pp, rowCount++);
                addRowPortrait(table, inflater, "% (Preisunterschied in Prozent)", String.valueOf((int) Math.floor(comparison.priceDifferenceInPercentage)), rowCount++);
                addRowPortrait(table, inflater, "SB", comparison.package_.selbstbehalt(), rowCount++);
            }
        }
    }

    private void addHeaderRowLand(TableLayout table, LayoutInflater inflater) {
        View rowView = inflater.inflate(R.layout.item_price_comparison_row_land, table, false);
        ((TextView) rowView.findViewById(R.id.name)).setText("Präparat");
        ((TextView) rowView.findViewById(R.id.auth)).setText("Inhaber");
        ((TextView) rowView.findViewById(R.id.size)).setText("Grösse");
        ((TextView) rowView.findViewById(R.id.pp)).setText("PP");
        ((TextView) rowView.findViewById(R.id.diff)).setText("%");
        ((TextView) rowView.findViewById(R.id.sb)).setText("SB");

        // Make headers bold
        int[] ids = {R.id.name, R.id.auth, R.id.size, R.id.pp, R.id.diff, R.id.sb};
        for (int id : ids) {
            ((TextView) rowView.findViewById(id)).setTypeface(null, android.graphics.Typeface.BOLD);
        }

        rowView.setBackgroundColor(0xFFDDDDDD);
        table.addView(rowView);
    }

    private void addRowLand(TableLayout table, LayoutInflater inflater, AmikoDBPriceComparison comparison, int position) {
        View rowView = inflater.inflate(R.layout.item_price_comparison_row_land, table, false);
        ((TextView) rowView.findViewById(R.id.name)).setText(comparison.package_.name);
        ((TextView) rowView.findViewById(R.id.auth)).setText(comparison.package_.parent.auth);
        ((TextView) rowView.findViewById(R.id.size)).setText(comparison.package_.dosage + " " + comparison.package_.units);
        ((TextView) rowView.findViewById(R.id.pp)).setText(comparison.package_.pp);
        ((TextView) rowView.findViewById(R.id.diff)).setText(String.valueOf((int) Math.floor(comparison.priceDifferenceInPercentage)));
        ((TextView) rowView.findViewById(R.id.sb)).setText(comparison.package_.selbstbehalt());

        if (position % 2 == 1) {
            rowView.setBackgroundColor(0xFFEEEEEE);
        }
        table.addView(rowView);
    }

    private void addRowPortrait(TableLayout table, LayoutInflater inflater, String labelText, String valueText, int position) {
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
