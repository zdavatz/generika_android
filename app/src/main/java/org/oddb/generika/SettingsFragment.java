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

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.appcompat.app.AlertDialog;
import android.widget.Toast;
import android.util.Log;

import java.lang.Boolean;
import java.lang.Object;
import java.util.Locale;

import org.oddb.generika.preference.AppListPreference;
import org.oddb.generika.data.AmikoDBManager;
import org.oddb.generika.data.InteractionsDBManager;
import org.oddb.generika.util.AppLocale;
import org.oddb.generika.util.Constant;


public class SettingsFragment extends PreferenceFragmentCompat {
  private static final String TAG = "SettingsFragment";

  private SharedPreferences sharedPreferences;

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String _s) {
    // load settings from xml file
    addPreferencesFromResource(R.xml.user_settings);

    Context context = getContext();
    this.sharedPreferences = PreferenceManager
      .getDefaultSharedPreferences(context);

    initLocales();
    initDatabasePreference();
  }

  private void initLocales() {
    Preference useSystemLocale = findPreference(Constant.kAppUseSystemLocale);
    Preference appLocale = findPreference(Constant.kAppLocale);

    Boolean checked = sharedPreferences.getBoolean(
      Constant.kAppUseSystemLocale, false);
    Log.d(TAG, "(initLocaleStates) kAppUseSystemLocale checked: " + checked);
    if (checked) {
      Locale systemLocale = Resources.getSystem()
        .getConfiguration().locale;
      updateAppLocaleValue(systemLocale.getLanguage());

      appLocale.setEnabled(false);
    }

    // callback (app locale)
    appLocale.setOnPreferenceChangeListener(
      new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(
          Preference _preference, Object newValue) {
          Locale newLocale = new Locale(newValue.toString());
          changeCurrentLocale(newLocale);
          return true;
        }
      }
    );

    // callback (use system locale check)
    useSystemLocale.setOnPreferenceChangeListener(
      new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(
          Preference _preference, Object newValue) {

          Preference appLocale_ = findPreference(Constant.kAppLocale);
          if ((Boolean)newValue) {
            Locale systemLocale = Resources.getSystem()
              .getConfiguration().locale;

            updateAppLocaleValue(systemLocale.getLanguage());
            appLocale_.setEnabled(false);

            // all activity extends BaseActivity (has currentLocale)
            if (systemLocale != (
                (SettingsActivity)getActivity()).currentLocale) {
              changeCurrentLocale(systemLocale);
            }
          } else {
            appLocale_.setEnabled(true);
          }
          return true;
        }
    });
  }

  /**
   * Update application locale based on system locale (language) value.
   * `de` is our default.
   */
  private void updateAppLocaleValue(String language) {
    Log.d(TAG, "(initLocaleStates) language: " + language);

    AppListPreference appLocale = (AppListPreference)findPreference(
      Constant.kAppLocale);
    switch (language) {
      case Constant.LANG_DE: case Constant.LANG_FR: case Constant.LANG_EN:
        appLocale.setValue(language);
        break;
      default:
        appLocale.setValue(Constant.LANG_DE);
        break;
    }
  }

  private void initDatabasePreference() {
    Preference downloadDb = findPreference("download_database");
    if (downloadDb != null) {
      downloadDb.setOnPreferenceClickListener(preference -> {
        showDatabaseDownloadConfirmation();
        return true;
      });
    }
    updateDatabaseInfo();
  }

  private void updateDatabaseInfo() {
    // Pharmaceutical DB stats
    Preference dbInfo = findPreference("database_info");
    if (dbInfo != null) {
      new Thread(() -> {
        AmikoDBManager dbManager = AmikoDBManager.getInstance(getContext());
        if (dbManager.checkAllFilesExists()) {
          int rowCount = dbManager.getRowCount();
          long fileSize = dbManager.getFileSizeBytes();
          String sizeMB = String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
          String summary;
          if (rowCount >= 0) {
            summary = String.format("%,d drugs | %s", rowCount, sizeMB);
          } else {
            summary = "Installed | " + sizeMB;
          }
          final String s = summary;
          if (getActivity() != null) {
            getActivity().runOnUiThread(() -> dbInfo.setSummary(s));
          }
        } else {
          if (getActivity() != null) {
            getActivity().runOnUiThread(() -> dbInfo.setSummary("Not installed"));
          }
        }
      }).start();
    }

    // Interactions DB stats
    Preference interInfo = findPreference("interactions_database_info");
    if (interInfo != null) {
      new Thread(() -> {
        InteractionsDBManager interDB = InteractionsDBManager.getInstance(getContext());
        if (interDB.checkAllFilesExists()) {
          java.util.Map<String, Integer> stats = interDB.getTableStats();
          long fileSize = interDB.getFileSizeBytes();
          String sizeMB = String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
          StringBuilder sb = new StringBuilder();
          if (!stats.isEmpty()) {
            Integer epha = stats.get("epha_interactions");
            Integer subst = stats.get("interactions");
            Integer drugs = stats.get("drugs");
            if (epha != null) sb.append(String.format("%,d EPha", epha));
            if (subst != null) {
              if (sb.length() > 0) sb.append(" | ");
              sb.append(String.format("%,d substance", subst));
            }
            if (drugs != null) {
              if (sb.length() > 0) sb.append(" | ");
              sb.append(String.format("%,d drugs", drugs));
            }
            sb.append(" | ").append(sizeMB);
          } else {
            sb.append("Installed | ").append(sizeMB);
          }
          final String s = sb.toString();
          if (getActivity() != null) {
            getActivity().runOnUiThread(() -> interInfo.setSummary(s));
          }
        } else {
          if (getActivity() != null) {
            getActivity().runOnUiThread(() -> interInfo.setSummary("Not installed"));
          }
        }
      }).start();
    }
  }

  private void showDatabaseDownloadConfirmation() {
    new AlertDialog.Builder(getContext())
      .setTitle("Update Databases")
      .setMessage("This will download the pharmaceutical database (~600MB) and the interactions database. Continue?")
      .setPositiveButton("Download", (dialog, which) -> startAllDatabaseDownloads())
      .setNegativeButton("Cancel", null)
      .show();
  }

  private void startAllDatabaseDownloads() {
    ProgressDialog progressDialog = new ProgressDialog(getContext());
    progressDialog.setTitle(getString(R.string.app_name));
    progressDialog.setMessage("Downloading pharmaceutical database...");
    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    progressDialog.setMax(100);
    progressDialog.setCancelable(false);
    progressDialog.show();

    AmikoDBManager dbManager = AmikoDBManager.getInstance(getContext());
    dbManager.forceDownload(new AmikoDBManager.DownloadCallback() {
      @Override
      public void onProgress(int percent) {
        getActivity().runOnUiThread(() -> {
          progressDialog.setProgress(percent);
          progressDialog.setMessage(String.format(
            "Downloading pharmaceutical database...\n%d%%", percent));
        });
      }

      @Override
      public void onComplete() {
        // Now download interactions DB
        getActivity().runOnUiThread(() -> {
          progressDialog.setProgress(0);
          progressDialog.setMessage("Downloading interactions database...");
        });

        InteractionsDBManager interDB = InteractionsDBManager.getInstance(getContext());
        interDB.forceDownload(new InteractionsDBManager.DownloadCallback() {
          @Override
          public void onProgress(int percent) {
            getActivity().runOnUiThread(() -> {
              progressDialog.setProgress(percent);
              progressDialog.setMessage(String.format(
                "Downloading interactions database...\n%d%%", percent));
            });
          }

          @Override
          public void onComplete() {
            getActivity().runOnUiThread(() -> {
              progressDialog.dismiss();
              Toast.makeText(getContext(), "All databases updated", Toast.LENGTH_SHORT).show();
              updateDatabaseInfo();
            });
          }

          @Override
          public void onError(Exception e) {
            getActivity().runOnUiThread(() -> {
              progressDialog.dismiss();
              Log.e(TAG, "Interactions database download error", e);
              new AlertDialog.Builder(getContext())
                .setTitle("Download Error")
                .setMessage("Failed to download interactions database: " + e.getMessage())
                .setPositiveButton("OK", null)
                .show();
              updateDatabaseInfo(); // still update amiko stats
            });
          }
        });
      }

      @Override
      public void onError(Exception e) {
        getActivity().runOnUiThread(() -> {
          progressDialog.dismiss();
          Log.e(TAG, "Database download error", e);
          new AlertDialog.Builder(getContext())
            .setTitle("Download Error")
            .setMessage("Failed to download pharmaceutical database: " + e.getMessage())
            .setPositiveButton("OK", null)
            .show();
        });
      }
    });
  }

  /**
   * Change current (resource) locale.
   * SettingsActivity will be refreshed.
   */
  private void changeCurrentLocale(Locale newLocale) {
    AppLocale.setLocale(getContext(), newLocale);

    // refresh
    ((SettingsActivity)getActivity()).finish();
    Intent intent = new Intent(getContext(), SettingsActivity.class); 
    startActivity(intent); 
  }
}
