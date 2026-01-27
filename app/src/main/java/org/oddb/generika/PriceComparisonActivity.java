package org.oddb.generika;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.oddb.generika.data.AmikoDBManager;
import org.oddb.generika.model.AmikoDBPriceComparison;

import java.util.ArrayList;
import java.util.List;

public class PriceComparisonActivity extends AppCompatActivity {
    public static final String EXTRA_GTIN = "gtin";

    private ListView listView;
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

        listView = findViewById(R.id.comparison_list);

        String gtin = getIntent().getStringExtra(EXTRA_GTIN);
        if (gtin != null) {
            AmikoDBManager manager = AmikoDBManager.getInstance(this);
            comparisons = AmikoDBPriceComparison.comparePrice(manager, gtin);
        }

        if (comparisons == null) {
            comparisons = new ArrayList<>();
        }

        ComparisonAdapter adapter = new ComparisonAdapter(comparisons);
        listView.setAdapter(adapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class ComparisonAdapter extends BaseAdapter {
        private List<RowData> flattenedData;

        public ComparisonAdapter(List<AmikoDBPriceComparison> data) {
            flattenedData = new ArrayList<>();
            for (AmikoDBPriceComparison comparison : data) {
                flattenedData.add(new RowData("", ""));
                flattenedData.add(new RowData("Präparat", comparison.package_.name));
                flattenedData.add(new RowData("Zulassungsinhaber", comparison.package_.parent.auth));
                flattenedData.add(new RowData("Packungsgrösse", comparison.package_.dosage + " " + comparison.package_.units));
                flattenedData.add(new RowData("PP", comparison.package_.pp));
                flattenedData.add(new RowData("% (Preisunterschied in Prozent)", String.valueOf((int) Math.floor(comparison.priceDifferenceInPercentage))));
                flattenedData.add(new RowData("SB", comparison.package_.selbstbehalt()));
            }
        }

        @Override
        public int getCount() {
            return flattenedData.size();
        }

        @Override
        public Object getItem(int position) {
            return flattenedData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_price_comparison_row, parent, false);
            }

            RowData row = flattenedData.get(position);
            TextView label = convertView.findViewById(R.id.label);
            TextView value = convertView.findViewById(R.id.value);

            label.setText(row.label);
            value.setText(row.value);

            if (position % 7 == 0) {
                convertView.setBackgroundColor(0xFFEEEEEE);
            } else {
                convertView.setBackgroundColor(0x00000000);
            }

            return convertView;
        }
    }

    private static class RowData {
        String label;
        String value;

        RowData(String label, String value) {
            this.label = label;
            this.value = value != null ? value : "";
        }
    }
}
