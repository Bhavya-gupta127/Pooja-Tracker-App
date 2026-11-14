package com.example.poojatracker;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.YouTubePlayerTracker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MyDBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "PoojaDB";
    private static final int DATABASE_VERSION = 1;
    //table poojas
    private static final String TABLE_POOJA = "poojas";
    private static final String KEY_ID = "id";
    private static final String KEY_TITLE = "title";
    private static final String KEY_DESC = "description";
    private static final String KEY_URL = "contentUrl";
    private static final String KEY_STATUS = "status";
    //table days
    private static final String TABLE_DAY = "days";
    private static final String KEY_DAY = "day";


    Map<String, Integer> daysOfWeek = new HashMap<>();

    // Add entries to the map


    public MyDBHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        daysOfWeek.put("Monday", 0);
        daysOfWeek.put("Tuesday", 1);
        daysOfWeek.put("Wednesday", 2);
        daysOfWeek.put("Thursday", 3);
        daysOfWeek.put("Friday", 4);
        daysOfWeek.put("Saturday", 5);
        daysOfWeek.put("Sunday", 6);

        //CREATE TABLE pooja(id INTEGER PRIMARY KEY,title TEXT, desc TEXT, contentUrl TEXT, status BOOLEAN)
        db.execSQL("CREATE TABLE " + TABLE_POOJA +
                "(" + KEY_ID + " INTEGER PRIMARY KEY, " + KEY_TITLE + " TEXT, " + KEY_DESC + " TEXT, " + KEY_URL + " TEXT, " + KEY_STATUS + " BOOLEAN " + ")"
        );

//        CREATE TABLE days2(id INTEGER , day TEXT,PRIMARY KEY(id,day) );
        db.execSQL("CREATE TABLE " + TABLE_DAY +
                "(" + KEY_ID + " INTEGER , " + KEY_DAY + " TEXT, PRIMARY KEY ( " + KEY_ID + " , " + KEY_DAY + ") ); "
        );

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    public void addPooja(int id, String title, String desc, String contentUrl, boolean status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_ID, id);
        values.put(KEY_TITLE, title);
        values.put(KEY_DESC, desc);
        values.put(KEY_URL, contentUrl);
        values.put(KEY_STATUS, status);
        db.insertWithOnConflict(TABLE_POOJA, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        Log.d("testjson", "db updated");
    }

    public void addDay(int id, String day) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_ID, id);
        values.put(KEY_DAY, day);
        db.insertWithOnConflict(TABLE_DAY, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void clearDaysForPooja(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_DAY, KEY_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public int importFromSqliteFile(java.io.File externalDbFile) {
        int imported = 0;
        try (SQLiteDatabase extDb = SQLiteDatabase.openDatabase(
                externalDbFile.getAbsolutePath(),
                null,
                SQLiteDatabase.OPEN_READONLY
        )) {
            java.util.Set<Integer> validIdsWithDays = new java.util.HashSet<>();
            try (Cursor dayCursor = extDb.rawQuery("SELECT id, day FROM " + TABLE_DAY, null)) {
                while (dayCursor.moveToNext()) {
                    int id = dayCursor.getInt(0);
                    String day = dayCursor.isNull(1) ? null : dayCursor.getString(1);
                    if (day != null && !day.trim().isEmpty()) {
                        if (validIdsWithDays.add(id)) {
                            clearDaysForPooja(id);
                        }
                        addDay(id, day.trim());
                    }
                }
            }
            try (Cursor poojaCursor = extDb.rawQuery(
                    "SELECT id, title, description, contentUrl, status FROM " + TABLE_POOJA,
                    null
            )) {
                while (poojaCursor.moveToNext()) {
                    int id = poojaCursor.getInt(0);
                    if (!validIdsWithDays.contains(id)) {
                        continue;
                    }
                    String title = poojaCursor.isNull(1) ? "" : poojaCursor.getString(1);
                    String desc = poojaCursor.isNull(2) ? "" : poojaCursor.getString(2);
                    String url = poojaCursor.isNull(3) ? "" : poojaCursor.getString(3);
                    boolean status = poojaCursor.getInt(4) == 1;
                    if (!title.trim().isEmpty() && !url.trim().isEmpty()) {
                        addPooja(id, title.trim(), desc.trim(), url.trim(), status);
                        imported++;
                    }
                }
            }
        }
        return imported;
    }

    //    SELECT p.id , p.title , p.description, p.contentUrl, p.status, d.day
//    FROM poojas p
//    JOIN days d ON p.id=d.id
//    WHERE d.day='Monday';
    public ArrayList<PoojaModel> fetchPooja(String currentDay) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT "
                        + TABLE_POOJA + "." + KEY_ID + " , "
                        + TABLE_POOJA + "." + KEY_TITLE + " , "
                        + TABLE_POOJA + "." + KEY_DESC + " , "
                        + TABLE_POOJA + "." + KEY_URL + " , "
                        + TABLE_POOJA + "." + KEY_STATUS
                        + " FROM "
                        + TABLE_POOJA
                        + " JOIN " + TABLE_DAY
                        + " ON " + TABLE_POOJA + "." + KEY_ID + " = " + TABLE_DAY + "." + KEY_ID
                        + " WHERE " + TABLE_DAY + "." + KEY_DAY + " = " + "'" + currentDay + "'" + " ;"
                , null
        );
        ArrayList<PoojaModel> arrayList = new ArrayList<>();

        while (cursor.moveToNext()) {
            PoojaModel poojaModel;
            poojaModel = new PoojaModel(
                    cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getInt(4)
            );
            arrayList.add(poojaModel);
        }
        cursor.close();
        return arrayList;
    }
    YouTubePlayerTracker tracker = new YouTubePlayerTracker();

//    public void playvid(Context context, String finalVideoId)
//    {
//        YouTubePlayerView youTubePlayerView = ((Activity) context).findViewById(R.id.youtube);
////        getLifecycle().addObserver(youTubePlayerView);
//        youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
//            @Override
//            public void onReady(@NonNull YouTubePlayer youTubePlayer) {
//                youTubePlayer.loadVideo(finalVideoId, 2);
//                youTubePlayer.play();
//                youTubePlayer.mute();
//                youTubePlayer.addListener(tracker);
//                tracker.getState();
//                tracker.getCurrentSecond();
//                tracker.getVideoDuration();
//                tracker.getVideoId();
//
//            }
//        });
//    }
    public void ensureDailyReset(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.content.SharedPreferences sharedPreferences =
                    context.getSharedPreferences("lastOpen", Context.MODE_PRIVATE);
            String today = String.valueOf(java.time.LocalDate.now());
            String prevDate = sharedPreferences.getString("date", null);
            if (prevDate == null) {
                sharedPreferences.edit().putString("date", today).apply();
            } else if (!prevDate.equals(today)) {
                sharedPreferences.edit().putString("date", today).apply();
                updateDbForNewDay();
            }
        }
    }

    public int[] getDayCounts(String currentDay) {
        SQLiteDatabase db = this.getReadableDatabase();
        int checked = 0;
        int total = 0;
        try (Cursor cursor = db.rawQuery(
                "SELECT " + TABLE_POOJA + "." + KEY_STATUS
                        + " FROM " + TABLE_POOJA
                        + " JOIN " + TABLE_DAY
                        + " ON " + TABLE_POOJA + "." + KEY_ID + " = " + TABLE_DAY + "." + KEY_ID
                        + " WHERE " + TABLE_DAY + "." + KEY_DAY + " = ?",
                new String[]{currentDay}
        )) {
            while (cursor.moveToNext()) {
                total++;
                if (cursor.getInt(0) == 1) {
                    checked++;
                }
            }
        }
        return new int[]{checked, total};
    }

    public Map<String, int[]> getAllDayCounts() {
        Map<String, int[]> summary = new HashMap<>();
        for (String d : DAYS_ORDER) {
            summary.put(d, new int[]{0, 0});
        }
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(
                "SELECT d." + KEY_DAY + ", COUNT(*), SUM(CASE WHEN p." + KEY_STATUS + " = 1 THEN 1 ELSE 0 END)"
                        + " FROM " + TABLE_DAY + " d"
                        + " JOIN " + TABLE_POOJA + " p ON p." + KEY_ID + " = d." + KEY_ID
                        + " GROUP BY d." + KEY_DAY,
                null
        )) {
            while (cursor.moveToNext()) {
                String day = cursor.getString(0);
                int total = cursor.getInt(1);
                int checked = cursor.getInt(2);
                if (day != null) {
                    summary.put(day, new int[]{checked, total});
                }
            }
        }
        return summary;
    }

    //get total time and number
    public String fetchPoojaDetails(Context context, String currentDay) {
        int[] counts = getDayCounts(currentDay);
        return counts[0] + " / " + counts[1];
    }


    public void updateDbForStatus(int id, boolean newStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_STATUS, newStatus);
        db.update(TABLE_POOJA, contentValues, KEY_ID + " = " + id, null);
    }

    public void updateDbForNewDay() {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_STATUS, false);
        db.update(TABLE_POOJA, contentValues, null, null);
    }

    public static final String[] DAYS_ORDER = {
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    };

    static class JoinedPoojaRow {
        final int id;
        final String title;
        final String description;
        final String contentURL;
        final boolean status;
        final String dayValue;

        JoinedPoojaRow(int id, String title, String description, String contentURL, boolean status, String dayValue) {
            this.id = id;
            this.title = title != null ? title : "";
            this.description = description != null ? description : "";
            this.contentURL = contentURL != null ? contentURL : "";
            this.status = status;
            this.dayValue = dayValue;
        }
    }

    static class PoojaExportRecord {
        final int id;
        final String title;
        final String description;
        final String contentURL;
        final boolean status;
        final int[] days = new int[7];

        PoojaExportRecord(int id, String title, String description, String contentURL, boolean status) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.contentURL = contentURL;
            this.status = status;
        }
    }

    static List<PoojaExportRecord> aggregatePoojaRecords(List<JoinedPoojaRow> rows) {
        Map<Integer, PoojaExportRecord> grouped = new LinkedHashMap<>();
        if (rows == null) {
            return new ArrayList<>();
        }
        for (JoinedPoojaRow row : rows) {
            PoojaExportRecord record = grouped.get(row.id);
            if (record == null) {
                record = new PoojaExportRecord(row.id, row.title, row.description, row.contentURL, row.status);
                grouped.put(row.id, record);
            }
            if (row.dayValue != null) {
                for (int i = 0; i < DAYS_ORDER.length; i++) {
                    if (DAYS_ORDER[i].equalsIgnoreCase(row.dayValue.trim())) {
                        record.days[i] = 1;
                        break;
                    }
                }
            }
        }
        return new ArrayList<>(grouped.values());
    }

    //export to json
    public String exportToJson() {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] projection = {
                "poojas.id",
                "poojas.title",
                "poojas.description",
                "poojas.contentUrl",
                "poojas.status",
                "days.day"
        };
        String query = "SELECT " + TextUtils.join(", ", projection) +
                " FROM " + TABLE_POOJA +
                " LEFT JOIN " + TABLE_DAY +
                " ON poojas.id = days.id" +
                " ORDER BY poojas.id ASC";
        Cursor cursor = db.rawQuery(query, null);

        List<JoinedPoojaRow> joinedRows = new ArrayList<>();
        while (cursor.moveToNext()) {
            int id = cursor.getInt(0);
            String title = cursor.getString(1);
            String description = cursor.getString(2);
            String contentURL = cursor.getString(3);
            boolean status = cursor.getInt(4) == 1;
            String dayValue = cursor.isNull(5) ? null : cursor.getString(5);
            joinedRows.add(new JoinedPoojaRow(id, title, description, contentURL, status, dayValue));
        }
        cursor.close();

        List<PoojaExportRecord> records = aggregatePoojaRecords(joinedRows);
        JSONArray jsonArray = new JSONArray();
        for (PoojaExportRecord record : records) {
            // Skip orphan records that aren't scheduled on any day
            boolean hasAnyDay = false;
            for (int d : record.days) {
                if (d == 1) {
                    hasAnyDay = true;
                    break;
                }
            }
            if (!hasAnyDay) {
                continue;
            }
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", record.id);
                jsonObject.put("title", record.title);
                jsonObject.put("desc", record.description);
                jsonObject.put("contentURL", record.contentURL);
                jsonObject.put("status", record.status);

                JSONArray daysArray = new JSONArray();
                for (int d : record.days) {
                    daysArray.put(d);
                }
                jsonObject.put("days", daysArray);
                jsonArray.put(jsonObject);
            } catch (JSONException e) {
                Log.e("export", "Failed to build JSON object for id=" + record.id, e);
            }
        }

        String jsonString;
        try {
            jsonString = jsonArray.toString(2);
        } catch (JSONException e) {
            jsonString = jsonArray.toString();
        }

        Log.d("export", jsonString);
        return jsonString;
    }

    public static int deterministicId(String title, String contentUrl) {
        String key = (title == null ? "" : title.trim().toLowerCase()) + "|"
                + (contentUrl == null ? "" : contentUrl.trim());
        int hash = key.hashCode() & 0x7fffffff;
        return 100000 + (hash % 899999);
    }

    public int getTotalUniquePoojaCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT COUNT(DISTINCT " + KEY_ID + ") FROM " + TABLE_DAY, null)) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        }
        return 0;
    }

    public void deleteFromDb(int id, String day) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_DAY, "id = ? AND day = ?", new String[]{String.valueOf(id), day});
        db.delete(
                TABLE_POOJA,
                KEY_ID + " = ? AND NOT EXISTS (SELECT 1 FROM " + TABLE_DAY + " WHERE " + TABLE_DAY + "." + KEY_ID + " = ?)",
                new String[]{String.valueOf(id), String.valueOf(id)}
        );
    }

    public int getFirstUnchecked(String day) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT "
                        + TABLE_POOJA + "." + KEY_ID + " , "
                        + TABLE_POOJA + "." + KEY_TITLE + " , "
                        + TABLE_POOJA + "." + KEY_DESC + " , "
                        + TABLE_POOJA + "." + KEY_URL + " , "
                        + TABLE_POOJA + "." + KEY_STATUS
                        + " FROM "
                        + TABLE_POOJA
                        + " JOIN " + TABLE_DAY
                        + " ON " + TABLE_POOJA + "." + KEY_ID + " = " + TABLE_DAY + "." + KEY_ID
                        + " WHERE " + TABLE_DAY + "." + KEY_DAY + " = " + "'" + day + "'" + " ;"
                , null
        );
        int count = 0;
        while (cursor.moveToNext()) {
            if (cursor.getInt(4) == 0) {
                cursor.close();
                return count;
            }
            count++;
        }
        cursor.close();
        return count;
    }
}
