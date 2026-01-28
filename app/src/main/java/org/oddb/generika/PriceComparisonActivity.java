package org.oddb.generika;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PriceComparisonActivity extends AppCompatActivity {
    public static final String EXTRA_GTIN = "gtin";

    private List<AmikoDBPriceComparison> comparisons;

    private enum SortField {
        NAME, OWNER, SIZE, PRICE, DIFF, SB
    }
    private SortField currentSortField = SortField.DIFF;
    private boolean isAsc = true;

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

        sortAndReload();

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_price_comparison, menu);
        return true;
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
                addRowPortrait(table, inflater, "% (Preisunterschied in Prozent)", String.format("%.0f%%", comparison.priceDifferenceInPercentage), rowCount++);
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
        ((TextView) rowView.findViewById(R.id.diff)).setText(String.format("%.0f%%", comparison.priceDifferenceInPercentage));
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
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        SortField field = null;
        if (id == R.id.sort_name) field = SortField.NAME;
        else if (id == R.id.sort_owner) field = SortField.OWNER;
        else if (id == R.id.sort_size) field = SortField.SIZE;
        else if (id == R.id.sort_price) field = SortField.PRICE;
        else if (id == R.id.sort_diff) field = SortField.DIFF;
        else if (id == R.id.sort_sb) field = SortField.SB;

        if (field != null) {
            if (currentSortField == field) {
                isAsc = !isAsc;
            } else {
                currentSortField = field;
                isAsc = true;
            }
            sortAndReload();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void sortAndReload() {
        Comparator<AmikoDBPriceComparison> comparator;
        switch (currentSortField) {
            case NAME:
                comparator = (c1, c2) -> c1.package_.name.compareToIgnoreCase(c2.package_.name);
                break;
            case OWNER:
                comparator = (c1, c2) -> c1.package_.parent.auth.compareToIgnoreCase(c2.package_.parent.auth);
                break;
            case SIZE:
                comparator = (c1, c2) -> Double.compare(parseSafe(c1.package_.dosage), parseSafe(c2.package_.dosage));
                break;
            case PRICE:
                comparator = (c1, c2) -> Double.compare(parsePrice(c1.package_.pp), parsePrice(c2.package_.pp));
                break;
            case DIFF:
                comparator = Comparator.comparingDouble(c -> c.priceDifferenceInPercentage);
                break;
            case SB:
                comparator = (c1, c2) -> Double.compare(parsePercentage(c1.package_.selbstbehalt()), parsePercentage(c2.package_.selbstbehalt()));
                break;
            default:
                return;
        }

        if (!isAsc) {
            comparator = comparator.reversed();
        }

        Collections.sort(comparisons, comparator);
        populateTable(comparisons);
    }

    private double parseSafe(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0;
        }
    }

    private double parsePrice(String price) {
        if (price == null) return 0;
        try {
            return Double.parseDouble(price.replace("CHF ", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private double parsePercentage(String percentageStr) {
        if (percentageStr == null) return 0;
        try {
            return Double.parseDouble(percentageStr.replace("%", ""));
        } catch (Exception e) {
            return 0;
        }
    }
}
