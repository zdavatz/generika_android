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

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.appbar.AppBarLayout;


public class SettingsActivity extends BaseActivity {
  private static final String TAG = "SettingsActivity";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_settings);
    initViews();
  }

  private void initViews() {
    Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
    toolbar.setTitle(context.getString(R.string.settings));
    setSupportActionBar(toolbar);

    ActionBar actionBar = getSupportActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setDisplayShowHomeEnabled(true);

      View coordinator = findViewById(R.id.coordinator);
      ViewCompat.setOnApplyWindowInsetsListener(coordinator, (v, insets) -> {
          Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
          // Remove the top padding from the root view so AppBarLayout can go to the top
          v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
          return insets;
      });

      // Handle top padding for the AppBarLayout specifically
      AppBarLayout appBarLayout = findViewById(R.id.app_bar_layout); // Ensure you have an ID in XML
      ViewCompat.setOnApplyWindowInsetsListener(appBarLayout, (v, insets) -> {
          Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
          v.setPadding(0, systemBars.top, 0, 0);
          return insets;
      });

      WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
      controller.setAppearanceLightStatusBars(true);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
  }

  @Override
  protected void onResume() {
    super.onResume();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      finishAfterTransition();
    }
    return super.onOptionsItemSelected(item);
  }
}
