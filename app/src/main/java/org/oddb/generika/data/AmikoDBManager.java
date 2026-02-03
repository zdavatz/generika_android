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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AmikoDBManager extends SQLiteOpenHelper {
    private static String TAG = "AmikoDBManager";

    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "amiko_db_full_idx_pinfo_de.db";
    private static final String AMIKO_COLUMNS = "_id, title, auth, atc, substances, regnrs, atc_class, tindex_str, application_str, indications_str, customer_id, pack_info_str, add_info_str, ids_str, titles_str, content, style_str, packages, type";

    private SQLiteDatabase mDataBase;
    private String mAppDataDir;

    private final Context mContext;
    private static AmikoDBManager sInstance;

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
     * Copies file from local assets-folder to system folder (persistant storage),
     * from where it can be accessed and handled. This is done by transferring bytestream.
     * @param srcFile
     * @param dstPath
     */
    private void copyFileFromAssetsToPath(String srcFile, String dstPath) throws IOException {
        Log.d(TAG, "Copying file " + srcFile + " to " + dstPath);
        File dir = new File(dstPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // Open shipped database from assets folder
        InputStream mInput = mContext.getAssets().open(srcFile);
        OutputStream mOutput = new FileOutputStream(dstPath + srcFile);

        // Transfer bytes from input to output
        byte[] mBuffer = new byte[1024];
        int mLength;
        while ((mLength = mInput.read(mBuffer))>0) {
            mOutput.write(mBuffer, 0, mLength);
        }

        // Close streams
        mOutput.flush();
        mOutput.close();
        mInput.close();
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
        boolean shouldOverride = isBuildDateAfterLastUpdate(context) || !checkAllFilesExists();
        return shouldOverride;
    }

    /**
     * Creates a set of empty databases (if there are more than one) and rewrites them with own databases.
     */
    public void copyFilesFromNonPersistentFolder() throws Exception {
        boolean shouldOverride = shouldCopyFromPersistentFolder(mContext);
        if (shouldOverride) {
			/*
			this.getReadableDatabase();
			this.close();
			*/
            // Copy SQLite database from external storage
            try {
                copyFileFromAssetsToPath(DATABASE_NAME, mAppDataDir);
                Log.d(TAG, "createDataBase(): database created");
            } catch (IOException e) {
                throw new Exception("Error copying main database!" + e.getLocalizedMessage());
            }
        }

        if (isBuildDateAfterLastUpdate(mContext)) {
            Date apkBuildDate = new Date(BuildConfig.TIMESTAMP);
            SharedPreferences settings = mContext.getSharedPreferences("GENERIKA_PREFS_FILE", 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putLong("PREF_DB_UPDATE_DATE_DE", apkBuildDate.getTime());
            // Commit the edits!
            editor.commit();
        }
    }

    static private boolean checkFileExistsAtPath(String fileName, String path) {
        File dbFile = new File(path + fileName);
        return dbFile.exists();
    }
}
