package org.oddb.generika.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.*;
import android.util.Log;

import org.oddb.generika.BuildConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InteractionsDBManager extends SQLiteOpenHelper {
    private static String TAG = "InteractionsDBManager";

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "interactions.db";
    private static final String DB_URL = "http://pillbox.oddb.org/interactions.db";
    private static final String PREF_KEY = "PREF_INTERACTIONS_DB_UPDATE_DATE";

    private SQLiteDatabase mDataBase;
    private String mAppDataDir;

    private final Context mContext;
    private static InteractionsDBManager sInstance;

    // Cached ATC class keywords
    private Map<String, List<String>> atcKeywords;

    // Severity ratings
    public static final String[] SEVERITY_LABELS = {
        "Keine Einstufung",
        "Vorsichtsmassnahmen",
        "Kombination vermeiden",
        "Kontraindiziert"
    };
    public static final String[] SEVERITY_COLORS = {
        "#caff70",  // green
        "#ffec8b",  // yellow
        "#ff82ab",  // pink
        "#ff6a6a"   // red
    };

    public interface DownloadCallback {
        void onProgress(int percent);
        void onComplete();
        void onError(Exception e);
    }

    public static synchronized InteractionsDBManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new InteractionsDBManager(context.getApplicationContext());
        }
        return sInstance;
    }

    private InteractionsDBManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.mContext = context;
        mAppDataDir = context.getApplicationInfo().dataDir + "/databases/";
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

    public boolean openDataBase() throws SQLException {
        if (mDataBase != null && mDataBase.isOpen()) return true;
        String mPath = mAppDataDir + DATABASE_NAME;
        File db_path = new File(mPath);
        if (!db_path.exists())
            return false;
        try {
            Log.d(TAG, "opening database " + mPath + "...");
            mDataBase = SQLiteDatabase.openDatabase(mPath, null, SQLiteDatabase.OPEN_READONLY);
        } catch (SQLException sqle) {
            Log.e(TAG, "open >> " + mPath + " / " + sqle.toString());
            throw sqle;
        }
        return mDataBase != null;
    }

    public void reloadDatabase() {
        if (mDataBase != null && mDataBase.isOpen()) {
            mDataBase.close();
            mDataBase = null;
        }
        atcKeywords = null;
        openDataBase();
    }

    public boolean checkAllFilesExists() {
        File dbFile = new File(mAppDataDir + DATABASE_NAME);
        return dbFile.exists();
    }

    public void downloadDatabaseIfNeeded(DownloadCallback callback) {
        if (checkAllFilesExists()) {
            // File exists — no need to re-download on every launch
            SharedPreferences settings = mContext.getSharedPreferences("GENERIKA_PREFS_FILE", 0);
            long lastUpdate = settings.getLong(PREF_KEY, 0);
            if (lastUpdate == 0) {
                // File exists but never recorded - set timestamp
                SharedPreferences.Editor editor = settings.edit();
                editor.putLong(PREF_KEY, System.currentTimeMillis());
                editor.apply();
            }
            if (callback != null) callback.onComplete();
            return;
        }

        new Thread(() -> {
            try {
                downloadDatabase(callback);
                SharedPreferences settings = mContext.getSharedPreferences("GENERIKA_PREFS_FILE", 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putLong(PREF_KEY, new Date().getTime());
                editor.commit();
                reloadDatabase();
                if (callback != null) callback.onComplete();
            } catch (Exception e) {
                Log.e(TAG, "Error downloading interactions database", e);
                if (callback != null) callback.onError(e);
            }
        }).start();
    }

    public void forceDownload(DownloadCallback callback) {
        new Thread(() -> {
            try {
                downloadDatabase(callback);
                SharedPreferences settings = mContext.getSharedPreferences("GENERIKA_PREFS_FILE", 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putLong(PREF_KEY, new Date().getTime());
                editor.commit();
                reloadDatabase();
                if (callback != null) callback.onComplete();
            } catch (Exception e) {
                Log.e(TAG, "Error downloading interactions database", e);
                if (callback != null) callback.onError(e);
            }
        }).start();
    }

    private void downloadDatabase(DownloadCallback callback) throws IOException {
        Log.d(TAG, "Downloading interactions database from " + DB_URL);

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

        if (finalFile.exists()) {
            finalFile.delete();
        }
        tempFile.renameTo(finalFile);

        Log.d(TAG, "Interactions database download complete");
    }

    // --- Interaction lookup methods ---

    private boolean ephaTableExists() {
        if (mDataBase == null && !openDataBase()) return false;
        Cursor cursor = mDataBase.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='epha_interactions'",
            null);
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    /**
     * Load ATC class keywords from class_keywords table.
     */
    private Map<String, List<String>> getAtcKeywords() {
        if (atcKeywords != null) return atcKeywords;
        atcKeywords = new HashMap<>();
        if (mDataBase == null && !openDataBase()) return atcKeywords;

        Cursor cursor = mDataBase.rawQuery(
            "SELECT atc_prefix, keyword FROM class_keywords", null);
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                String prefix = cursor.getString(0);
                String kw = cursor.getString(1);
                if (!atcKeywords.containsKey(prefix)) {
                    atcKeywords.put(prefix, new ArrayList<>());
                }
                atcKeywords.get(prefix).add(kw);
                cursor.moveToNext();
            }
        }
        cursor.close();
        return atcKeywords;
    }

    /**
     * Get substance names for an ATC code from the drugs table.
     */
    private List<String> substancesForAtc(String atcCode) {
        List<String> substances = new ArrayList<>();
        if (mDataBase == null && !openDataBase()) return substances;
        if (atcCode == null) return substances;

        Cursor cursor = mDataBase.rawQuery(
            "SELECT DISTINCT active_substances FROM drugs WHERE atc_code = ?",
            new String[]{atcCode});
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                String subs = cursor.getString(0);
                if (subs != null) {
                    for (String s : subs.split(", ")) {
                        String trimmed = s.trim();
                        if (!trimmed.isEmpty() && !substances.contains(trimmed)) {
                            substances.add(trimmed);
                        }
                    }
                }
                cursor.moveToNext();
            }
        }
        cursor.close();
        return substances;
    }

    /**
     * Get interactions_text for a drug by ATC code.
     */
    private String interactionsTextForAtc(String atcCode) {
        if (mDataBase == null && !openDataBase()) return null;
        if (atcCode == null) return null;

        Cursor cursor = mDataBase.rawQuery(
            "SELECT interactions_text FROM drugs WHERE atc_code = ? AND length(interactions_text) > 0 LIMIT 1",
            new String[]{atcCode});
        String text = null;
        if (cursor.moveToFirst()) {
            text = cursor.getString(0);
        }
        cursor.close();
        return text;
    }

    /**
     * Find EPha curated interaction between two ATC codes.
     */
    private Map<String, String> findEphaInteraction(String atc1, String atc2) {
        if (mDataBase == null && !openDataBase()) return null;
        if (!ephaTableExists()) return null;

        Cursor cursor = mDataBase.rawQuery(
            "SELECT * FROM epha_interactions WHERE atc1 = ? AND atc2 = ? LIMIT 1",
            new String[]{atc1, atc2});
        Map<String, String> row = null;
        if (cursor.moveToFirst()) {
            row = new HashMap<>();
            for (int i = 0; i < cursor.getColumnCount(); i++) {
                row.put(cursor.getColumnName(i), cursor.getString(i));
            }
        }
        cursor.close();
        return row;
    }

    /**
     * Score severity of a text context based on clinical language.
     */
    private int scoreSeverity(String text) {
        String t = text.toLowerCase();
        if (t.matches(".*(?:kontraindiziert|nicht anwenden|darf nicht|kontraindikation).*"))
            return 3;
        if (t.matches(".*(?:gefährlich|schwerwiegend|lebensbedroh|erhöhtes.*blutungsrisiko).*"))
            return 2;
        if (t.matches(".*(?:vorsicht|warnung|überwach|achten|kontrolle|erhöht.*risiko|verstärkt).*"))
            return 1;
        return 0;
    }

    /**
     * Extract context sentence around a keyword match in text.
     */
    private String extractContext(String text, String keyword) {
        String kwLower = keyword.toLowerCase();
        String txtLower = text.toLowerCase();
        String bestSentence = null;
        int bestScore = -1;
        int offset = 0;

        while (true) {
            int idx = txtLower.indexOf(kwLower, offset);
            if (idx < 0) break;

            // Find start of sentence
            int startPos = 0;
            for (int i = idx - 1; i >= 0; i--) {
                char c = text.charAt(i);
                if (c == '.' || c == ':') {
                    startPos = i + 1;
                    break;
                }
            }

            // Find end of sentence
            int endPos = text.length();
            int searchFrom = idx + keyword.length();
            for (int i = searchFrom; i < text.length(); i++) {
                if (text.charAt(i) == '.') {
                    endPos = i + 1;
                    break;
                }
            }

            String sentence = text.substring(startPos, Math.min(endPos, text.length())).trim();
            if (sentence.length() > 500) {
                sentence = sentence.substring(0, 500);
            }

            int sev = scoreSeverity(sentence);
            if (sev > bestScore) {
                bestScore = sev;
                bestSentence = sentence;
            }

            offset = idx + keyword.length();
        }

        return bestSentence;
    }

    /**
     * Compute class-level severity score for one direction.
     */
    private int classSeverityForDirection(String myAtcCode, String otherAtcCode) {
        String fiText = interactionsTextForAtc(myAtcCode);
        if (fiText == null) return 0;
        int best = 0;
        Map<String, List<String>> keywords = getAtcKeywords();
        for (Map.Entry<String, List<String>> entry : keywords.entrySet()) {
            String prefix = entry.getKey();
            if (!otherAtcCode.startsWith(prefix)) continue;
            for (String kw : entry.getValue()) {
                if (!fiText.toLowerCase().contains(kw.toLowerCase())) continue;
                String context = extractContext(fiText, kw);
                if (context == null) continue;
                int sev = scoreSeverity(context);
                if (sev > best) best = sev;
            }
        }
        return best;
    }

    /**
     * Represents one interaction result for display.
     */
    public static class InteractionResult {
        public String header;
        public int severity;
        public String color;
        public String text;

        public InteractionResult(String header, int severity, String color, String text) {
            this.header = header;
            this.severity = severity;
            this.color = color;
            this.text = text;
        }
    }

    /**
     * Drug info holder for interaction lookups.
     */
    public static class DrugInfo {
        public String ean;
        public String name;
        public String atcCode;

        public DrugInfo(String ean, String name, String atcCode) {
            this.ean = ean;
            this.name = name;
            this.atcCode = atcCode;
        }
    }

    /**
     * Get all interactions for a list of drugs.
     * Implements 3-tier lookup: EPha curated -> substance-level -> class-level.
     */
    public List<InteractionResult> getInteractions(List<DrugInfo> drugs) {
        List<InteractionResult> allResults = new ArrayList<>();
        if (mDataBase == null && !openDataBase()) {
            Log.e(TAG, "getInteractions: could not open database");
            return allResults;
        }
        if (drugs == null || drugs.size() < 2) {
            Log.d(TAG, "getInteractions: need at least 2 drugs, got " + (drugs != null ? drugs.size() : 0));
            return allResults;
        }

        Log.d(TAG, "getInteractions: checking " + drugs.size() + " drugs");
        for (DrugInfo d : drugs) {
            Log.d(TAG, "  drug: " + d.name + " [" + d.atcCode + "] ean=" + d.ean);
        }

        // Pairwise comparison
        for (int i = 0; i < drugs.size(); i++) {
            DrugInfo drugA = drugs.get(i);
            if (drugA.atcCode == null || drugA.atcCode.isEmpty()) continue;

            for (int j = i + 1; j < drugs.size(); j++) {
                DrugInfo drugB = drugs.get(j);
                if (drugB.atcCode == null || drugB.atcCode.isEmpty()) continue;

                Log.d(TAG, "Checking pair: " + drugA.atcCode + " <-> " + drugB.atcCode);
                List<InteractionResult> pairResults = getInteractionsForPair(drugA, drugB);
                Log.d(TAG, "  pair results: " + pairResults.size());
                allResults.addAll(pairResults);
            }
        }

        Log.d(TAG, "getInteractions: total results = " + allResults.size());
        // Sort by severity descending
        allResults.sort((a, b) -> b.severity - a.severity);
        return allResults;
    }

    private List<InteractionResult> getInteractionsForPair(DrugInfo drugA, DrugInfo drugB) {
        List<InteractionResult> results = new ArrayList<>();

        // Tier 1: EPha curated ATC-to-ATC (check both directions)
        Map<String, String> ephaRow = findEphaInteraction(drugA.atcCode, drugB.atcCode);
        if (ephaRow == null) {
            ephaRow = findEphaInteraction(drugB.atcCode, drugA.atcCode);
            if (ephaRow != null) {
                // Swap so drugA matches atc1
                DrugInfo tmp = drugA;
                drugA = drugB;
                drugB = tmp;
            }
        }

        if (ephaRow != null) {
            results.add(buildEphaResult(ephaRow, drugA, drugB));
            return results; // EPha match found, no need for lower tiers
        }

        // Tier 2: Substance-level interactions (both directions)
        List<InteractionResult> substanceResults = findSubstanceInteractions(drugA, drugB);
        results.addAll(substanceResults);
        List<InteractionResult> substanceResultsReverse = findSubstanceInteractions(drugB, drugA);
        results.addAll(substanceResultsReverse);

        // Tier 3: Class-level interactions (both directions)
        List<InteractionResult> classResults = findClassInteractions(drugA, drugB);
        results.addAll(classResults);
        List<InteractionResult> classResultsReverse = findClassInteractions(drugB, drugA);
        results.addAll(classResultsReverse);

        return results;
    }

    private InteractionResult buildEphaResult(Map<String, String> ephaRow, DrugInfo drugA, DrugInfo drugB) {
        String severityStr = ephaRow.get("severity_score");
        int severity = 0;
        try { severity = Integer.parseInt(severityStr); } catch (Exception e) {}
        String riskLabel = ephaRow.get("risk_label");
        String effect = ephaRow.get("effect");
        String mechanism = ephaRow.get("mechanism");
        String measures = ephaRow.get("measures");

        String header = drugA.name + " [" + drugA.atcCode + "] \u2194 " +
                         drugB.name + " [" + drugB.atcCode + "]";

        StringBuilder sb = new StringBuilder();
        sb.append("<span style='background-color: #dde8f0; padding: 1px 6px; font-size: 11px; border-radius: 3px;'>Quelle: EPha.ch</span><br>");
        sb.append(severity).append(": ").append(riskLabel);
        if (effect != null && !effect.isEmpty()) {
            sb.append("<br><b>").append(effect).append("</b>");
        }
        if (mechanism != null && !mechanism.isEmpty()) {
            sb.append("<br>").append(mechanism);
        }
        if (measures != null && !measures.isEmpty()) {
            sb.append("<br><i>Massnahmen: ").append(measures).append("</i>");
        }

        String color = severity >= 0 && severity < SEVERITY_COLORS.length ?
            SEVERITY_COLORS[severity] : SEVERITY_COLORS[0];

        return new InteractionResult(header, severity, color, sb.toString());
    }

    private List<InteractionResult> findSubstanceInteractions(DrugInfo drugA, DrugInfo drugB) {
        List<InteractionResult> results = new ArrayList<>();
        List<String> subsA = substancesForAtc(drugA.atcCode);
        List<String> subsB = substancesForAtc(drugB.atcCode);

        if (subsA.isEmpty() || subsB.isEmpty()) return results;

        for (String subA : subsA) {
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT drug_brand, drug_substance, interacting_substance, interacting_brands, ");
            sqlBuilder.append("description, severity_score, severity_label ");
            sqlBuilder.append("FROM interactions WHERE LOWER(drug_substance) = LOWER(?) AND (");
            List<String> args = new ArrayList<>();
            args.add(subA);
            for (int i = 0; i < subsB.size(); i++) {
                if (i > 0) sqlBuilder.append(" OR ");
                sqlBuilder.append("LOWER(interacting_substance) = LOWER(?)");
                args.add(subsB.get(i));
            }
            sqlBuilder.append(")");

            Cursor cursor = mDataBase.rawQuery(sqlBuilder.toString(), args.toArray(new String[0]));
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    int severity = cursor.getInt(5);
                    String drugSubstance = cursor.getString(1);
                    String interactingSub = cursor.getString(2);
                    String interactingBrands = cursor.getString(3);
                    String description = cursor.getString(4);

                    String header = drugSubstance + " (" + drugA.name + ") => " + interactingSub;
                    if (interactingBrands != null && !interactingBrands.isEmpty()) {
                        String[] brands = interactingBrands.split(",");
                        StringBuilder brandStr = new StringBuilder();
                        for (int b = 0; b < Math.min(3, brands.length); b++) {
                            if (b > 0) brandStr.append(", ");
                            brandStr.append(brands[b].trim());
                        }
                        header += " (" + brandStr.toString() + ")";
                    }

                    StringBuilder sb = new StringBuilder();
                    sb.append("<span style='background-color: #d5ecd5; padding: 1px 6px; font-size: 11px; border-radius: 3px;'>Quelle: Swissmedic FI</span><br>");
                    sb.append(severity).append(": ").append(
                        severity >= 0 && severity < SEVERITY_LABELS.length ?
                        SEVERITY_LABELS[severity] : SEVERITY_LABELS[0]);
                    if (description != null && !description.isEmpty()) {
                        sb.append("<br>").append(description);
                    }

                    String color = severity >= 0 && severity < SEVERITY_COLORS.length ?
                        SEVERITY_COLORS[severity] : SEVERITY_COLORS[0];
                    results.add(new InteractionResult(header, severity, color, sb.toString()));

                    cursor.moveToNext();
                }
            }
            cursor.close();
        }
        return results;
    }

    private List<InteractionResult> findClassInteractions(DrugInfo myDrug, DrugInfo otherDrug) {
        List<InteractionResult> results = new ArrayList<>();
        String fiText = interactionsTextForAtc(myDrug.atcCode);
        if (fiText == null) return results;

        int reverseSeverity = classSeverityForDirection(otherDrug.atcCode, myDrug.atcCode);

        Map<String, List<String>> keywords = getAtcKeywords();
        for (Map.Entry<String, List<String>> entry : keywords.entrySet()) {
            String prefix = entry.getKey();
            if (!otherDrug.atcCode.startsWith(prefix)) continue;

            for (String kw : entry.getValue()) {
                if (!fiText.toLowerCase().contains(kw.toLowerCase())) continue;
                String context = extractContext(fiText, kw);
                if (context == null) continue;
                int severity = scoreSeverity(context);

                String hint = "";
                if (reverseSeverity > severity) {
                    hint = "<br><span style='background-color: #ffec8b; padding: 2px 6px; font-size: 11px;'>" +
                           "Gegenrichtung hat höhere Einstufung</span>";
                }

                String header = myDrug.name + " [" + myDrug.atcCode + "] \u2194 " +
                                otherDrug.name + " [" + otherDrug.atcCode + "]";

                StringBuilder sb = new StringBuilder();
                sb.append("<span style='background-color: #d5ecd5; padding: 1px 6px; font-size: 11px; border-radius: 3px;'>Quelle: Swissmedic FI</span><br>");
                sb.append(severity).append(": ").append(
                    severity >= 0 && severity < SEVERITY_LABELS.length ?
                    SEVERITY_LABELS[severity] : SEVERITY_LABELS[0]);
                sb.append("<br>ATC-Klasse<br><i>Keyword \u00AB").append(kw);
                sb.append("\u00BB gefunden in Fachinformation von ").append(myDrug.name);
                sb.append("</i><br>").append(context).append(hint);

                String color = severity >= 0 && severity < SEVERITY_COLORS.length ?
                    SEVERITY_COLORS[severity] : SEVERITY_COLORS[0];
                results.add(new InteractionResult(header, severity, color, sb.toString()));

                break; // one match per ATC prefix
            }
        }
        return results;
    }

    /**
     * Returns a map of table names to row counts for display.
     */
    public Map<String, Integer> getTableStats() {
        Map<String, Integer> stats = new HashMap<>();
        if (mDataBase == null && !openDataBase()) return stats;

        String[] tables = {"epha_interactions", "interactions", "drugs", "class_keywords", "cyp_rules"};
        for (String table : tables) {
            try {
                Cursor cursor = mDataBase.rawQuery("SELECT COUNT(*) FROM " + table, null);
                if (cursor.moveToFirst()) {
                    stats.put(table, cursor.getInt(0));
                }
                cursor.close();
            } catch (Exception e) {
                // table may not exist
                Log.d(TAG, "Table " + table + " not found: " + e.getMessage());
            }
        }
        return stats;
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
