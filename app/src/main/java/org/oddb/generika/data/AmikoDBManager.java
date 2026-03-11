package org.oddb.generika.data;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.*;
import android.util.Log;

import org.oddb.generika.BuildConfig;
import org.oddb.generika.model.AmikoDBPackage;
import org.oddb.generika.model.AmikoDBPriceComparison;
import org.oddb.generika.model.AmikoDBRow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AmikoDBManager extends SQLiteOpenHelper {
    private static String TAG = "AmikoDBManager";

    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "amiko_db_full_idx_pinfo_de.db";
    private static final String AMIKO_COLUMNS = "_id, title, auth, atc, substances, regnrs, atc_class, tindex_str, application_str, indications_str, customer_id, pack_info_str, add_info_str, ids_str, titles_str, content, style_str, packages, type";
    private static final String DB_URL = "http://pillbox.oddb.org/amiko_db_full_idx_pinfo_de.db";

    private SQLiteDatabase mDataBase;
    private String mAppDataDir;

    private final Context mContext;
    private static AmikoDBManager sInstance;

    public interface DownloadCallback {
        void onProgress(int percent);
        void onComplete();
        void onError(Exception e);
    }

    public static synchronized AmikoDBManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new AmikoDBManager(context.getApplicationContext());
        }
        return sInstance;
    }

    private AmikoDBManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.mContext = context;
        mAppDataDir = context.getApplicationInfo().dataDir + "/databases/";
        try {
            copyFilesFromNonPersistentFolder();
        } catch (Exception e) {
            Log.e(TAG, "Cannot initialise database", e);
        }
    }
    public void onCreate(SQLiteDatabase db) {
        db.disableWriteAheadLogging();
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    /**
     * Opens SQLite database in read-only mode
     * @return true if operation is successful, false otherwise
     * @throws SQLException
     */
    public boolean openDataBase() throws SQLException {
        if (mDataBase != null && mDataBase.isOpen()) return true;
        String mPath = mAppDataDir + DATABASE_NAME;
        File db_path = new File(mPath);
        if (!db_path.exists())
            return false;
        try {
            Log.d(TAG, "opening database " + mPath + "...");
            mDataBase = SQLiteDatabase.openDatabase(mPath, null, SQLiteDatabase.OPEN_READONLY);
        } catch (SQLException sqle ) {
            Log.e(TAG, "open >> " + mPath + " / " + sqle.toString());
            throw sqle;
        }
        return mDataBase != null;
    }

    /**
     * Closes and reopens the database to pick up a newly downloaded file.
     */
    public void reloadDatabase() {
        if (mDataBase != null && mDataBase.isOpen()) {
            mDataBase.close();
            mDataBase = null;
        }
        openDataBase();
    }

    public ArrayList<AmikoDBRow> findWithRegnr(String regnr, String type) {
        ArrayList<AmikoDBRow> results = new ArrayList<>();
        if (mDataBase == null && !this.openDataBase()) {
            return results;
        }
        String sql = "SELECT " + AMIKO_COLUMNS + " FROM amikodb WHERE regnrs LIKE ? ";
        ArrayList<String> args = new ArrayList<String>();
        args.add("%" + regnr + "%");
        if (type != null && !type.isEmpty()) {
            sql += " AND type = ?";
            args.add(type);
        }
        Cursor cursor = mDataBase.rawQuery(sql, args.toArray(new String[0]));
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                results.add(new AmikoDBRow(cursor));
                cursor.moveToNext();
            }
        }
        // Make sure to close the cursor
        cursor.close();
        return results;
    }

    public ArrayList<AmikoDBRow> findWithGtin(String gtin, String type) {
        if (gtin == null || gtin.length() != 13) {
            return new ArrayList<>();
        }
        String regnr = gtin.substring(4, 9);
        return findWithRegnr(regnr, type);
    }

    public List<AmikoDBRow> findWithATC(String atc) {
        List<AmikoDBRow> results = new ArrayList<>();
        if (mDataBase == null && !this.openDataBase()) {
            return results;
        }
        String sql = "SELECT " + AMIKO_COLUMNS + " FROM amikodb WHERE atc = ?";
        Cursor cursor = mDataBase.rawQuery(sql, new String[]{atc});
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                results.add(new AmikoDBRow(cursor));
                cursor.moveToNext();
            }
        }
        cursor.close();
        return results;
    }

    /**
     * Downloads database from server if needed
     */
    public void downloadDatabaseIfNeeded(DownloadCallback callback) {
        boolean shouldDownload = shouldCopyFromPersistentFolder(mContext);
        if (!shouldDownload) {
            if (callback != null) callback.onComplete();
            return;
        }

        new Thread(() -> {
            try {
                downloadDatabase(callback);
                
                // Update timestamp
                Date downloadDate = new Date();
                SharedPreferences settings = mContext.getSharedPreferences("GENERIKA_PREFS_FILE", 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putLong("PREF_DB_UPDATE_DATE_DE", downloadDate.getTime());
                editor.commit();
                
                // Reload the database connection to use the new file
                reloadDatabase();
                
                if (callback != null) callback.onComplete();
            } catch (Exception e) {
                Log.e(TAG, "Error downloading database", e);
                if (callback != null) callback.onError(e);
            }
        }).start();
    }

    /**
     * Force download database (even if it already exists)
     */
    public void forceDownload(DownloadCallback callback) {
        new Thread(() -> {
            try {
                downloadDatabase(callback);
                
                Date downloadDate = new Date();
                SharedPreferences settings = mContext.getSharedPreferences("GENERIKA_PREFS_FILE", 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putLong("PREF_DB_UPDATE_DATE_DE", downloadDate.getTime());
                editor.commit();
                
                // Reload the database connection to use the new file
                reloadDatabase();
                
                if (callback != null) callback.onComplete();
            } catch (Exception e) {
                Log.e(TAG, "Error downloading database", e);
                if (callback != null) callback.onError(e);
            }
        }).start();
    }

    private void downloadDatabase(DownloadCallback callback) throws IOException {
        Log.d(TAG, "Downloading database from " + DB_URL);
        
        File dir = new File(mAppDataDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        File tempFile = new File(mAppDataDir + DATABASE_NAME + ".tmp");
        File finalFile = new File(mAppDataDir + DATABASE_NAME);
        
        URL url = new URL(DB_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.connect();
        
        long totalSize = connection.getContentLength();
        long downloadedSize = 0;
        
        InputStream input = connection.getInputStream();
        OutputStream output = new FileOutputStream(tempFile);
        
        byte[] buffer = new byte[8192];
        int bytesRead;
        int lastProgress = 0;
        
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
            downloadedSize += bytesRead;
            
            if (callback != null && totalSize > 0) {
                int progress = (int) (downloadedSize * 100 / totalSize);
                if (progress != lastProgress) {
                    lastProgress = progress;
                    callback.onProgress(progress);
                }
            }
        }
        
        output.flush();
        output.close();
        input.close();
        connection.disconnect();
        
        // Rename temp file to final file
        if (finalFile.exists()) {
            finalFile.delete();
        }
        tempFile.renameTo(finalFile);
        
        Log.d(TAG, "Database download complete");
    }

    public boolean checkAllFilesExists() {
        return checkFileExistsAtPath(DATABASE_NAME, mAppDataDir);
    }

    static public boolean isBuildDateAfterLastUpdate(Context context) {
        SharedPreferences settings = context.getSharedPreferences("GENERIKA_PREFS_FILE", 0);
        long timeMillisSince1970 = settings.getLong("PREF_DB_UPDATE_DATE_DE", 0);
        Date lastUpdate = new Date(timeMillisSince1970);
        Date apkBuildDate = new Date(BuildConfig.TIMESTAMP);
        return apkBuildDate.after(lastUpdate);
    }

    public boolean shouldCopyFromPersistentFolder(Context context) {
        // First check if file exists - if not, we definitely need to download
        if (!checkAllFilesExists()) {
            return true;
        }
        
        // File exists - now check if we need to update it
        SharedPreferences settings = context.getSharedPreferences("GENERIKA_PREFS_FILE", 0);
        long timeMillisSince1970 = settings.getLong("PREF_DB_UPDATE_DATE_DE", 0);
        
        // If we've never recorded a download (timeMillisSince1970 == 0), 
        // but the file exists, it means we already have it - don't re-download
        if (timeMillisSince1970 == 0) {
            // Set the current time so we don't keep checking this
            SharedPreferences.Editor editor = settings.edit();
            editor.putLong("PREF_DB_UPDATE_DATE_DE", System.currentTimeMillis());
            editor.apply();
            return false;
        }
        
        // Compare build date with last update
        Date lastUpdate = new Date(timeMillisSince1970);
        Date apkBuildDate = new Date(BuildConfig.TIMESTAMP);
        return apkBuildDate.after(lastUpdate);
    }

    /**
     * Check if database exists (download happens via downloadDatabaseIfNeeded)
     */
    public void copyFilesFromNonPersistentFolder() throws Exception {
        if (!checkAllFilesExists()) {
            Log.d(TAG, "Database not found, will need to download");
        }
    }

    static private boolean checkFileExistsAtPath(String fileName, String path) {
        File dbFile = new File(path + fileName);
        return dbFile.exists();
    }

    /**
     * Returns row count of the amikodb table, or -1 if unavailable.
     */
    public int getRowCount() {
        if (mDataBase == null && !this.openDataBase()) return -1;
        try {
            Cursor cursor = mDataBase.rawQuery("SELECT COUNT(*) FROM amikodb", null);
            int count = 0;
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
            return count;
        } catch (Exception e) {
            Log.e(TAG, "getRowCount error", e);
            return -1;
        }
    }

    /**
     * Returns the file size of the database in bytes, or -1 if unavailable.
     */
    public long getFileSizeBytes() {
        File dbFile = new File(mAppDataDir + DATABASE_NAME);
        if (dbFile.exists()) {
            return dbFile.length();
        }
        return -1;
    }
}
