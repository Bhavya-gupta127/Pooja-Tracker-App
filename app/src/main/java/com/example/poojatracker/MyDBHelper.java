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
        db.insert(TABLE_POOJA, null, values);
        Log.d("testjson", "db updated");
    }

    public void addDay(int id, String day) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_ID, id);
        values.put(KEY_DAY, day);
        db.insert(TABLE_DAY, null, values);
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
    //get total time and number
    public String fetchPoojaDetails(Context context, String currentDay) {

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
        String details;
        int checked = 0;
        int unchecked = 0;
        long watchedDuration = 0;
        long unwatchedDuration = 0;



        RequestQueue requestQueue;
        requestQueue = Volley.newRequestQueue(context);
        while (cursor.moveToNext()) {

//            String videoId = "gXWXKjR-qII"; //default video
//            String url = cursor.getString(3);
//
//            for (int i = 0; i < url.length() - 1; i++) {
//                if (url.charAt(i) == 'v' && url.charAt(i + 1) == '=') {
//                    videoId = url.substring(i + 2, i + 13);
//                    break;
//                }
//            }
//            String finalVideoId = videoId;
//            String apiurl="https://www.googleapis.com/youtube/v3/videos?id="+videoId+"&part=contentDetails&key={your_key_here}";
//            try {

//                playvid(context,finalVideoId);
//                JsonObjectRequest jsonObjectRequest = new JsonArrayRequest(Request.Method.GET, apiurl, null, new Response.Listener<JSONObject>() {
//                    public void onResponse(JSONObject response) {
//                        try {
//                            for (int i = 0; i < response.length(); i++) {
//                                Log.d("bhavya", String.valueOf(response));
//                            }
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }, new Response.ErrorListener() {
//                    @Override
//                    public void onErrorResponse(VolleyError error) {
//                        Log.d("TestJson", String.valueOf(error));
//                    }
//                });
//                requestQueue.add(jsonObjectRequest);

                if (cursor.getInt(4) == 1) {
//                    watchedDuration += tracker.getVideoDuration();
                    checked++;
                } else {
//                    unwatchedDuration += tracker.getVideoDuration();
                    unchecked++;
                }
//            } catch (Exception e) {
//                Log.d("bhavya", "error");
//            }
        }
        details = checked + " / " + (unchecked + checked) ;
//                + "  " + watchedDuration + " / " + (watchedDuration + unwatchedDuration);
        return details;
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


    //export to json
    public String exportToJson() {
        SQLiteDatabase db = this.getWritableDatabase();
        String[] projection = {"poojas.id", "poojas.title", "poojas.description", "poojas.contentUrl", "poojas.status", "days.day"};
        String tableName = "poojas";
        String joinTable = "days";
        String joinColumn = "poojas.id";
        String foreignKey = "days.id";
        String query = "SELECT " + TextUtils.join(", ", projection) +
                " FROM " + tableName +
                " LEFT JOIN " + joinTable +
                " ON " + joinColumn + " = " + foreignKey;
        Cursor cursor = db.rawQuery(query, null);

// Create a JSONArray to hold the result
        JSONArray jsonArray = new JSONArray();

// Process the cursor and construct the JSON objects
        while (cursor.moveToNext()) {
            try {
                @SuppressLint("Range") int id = cursor.getInt(cursor.getColumnIndex("id"));
                @SuppressLint("Range") String title = cursor.getString(cursor.getColumnIndex("title"));
                @SuppressLint("Range") String description = cursor.getString(cursor.getColumnIndex("description"));
                @SuppressLint("Range") String contentURL = cursor.getString(cursor.getColumnIndex("contentUrl"));
                @SuppressLint("Range") boolean status = cursor.getInt(cursor.getColumnIndex("status")) == 1;

                @SuppressLint("Range") String dayValue = cursor.getString(cursor.getColumnIndex("day"));

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", id);
                jsonObject.put("title", title);
                jsonObject.put("desc", description);
                jsonObject.put("contentURL", contentURL);
                jsonObject.put("status", status);

                // Create a JSONArray for days
                JSONArray daysArray = new JSONArray();
                String[] daysOfWeek = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
                for (String day : daysOfWeek) {
                    if (dayValue.equals(day)) {
                        daysArray.put(1);
                    } else {
                        daysArray.put(0);
                    }
                }
                jsonObject.put("days", daysArray);

                jsonArray.put(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

// Convert the JSONArray to a JSON string
        String jsonString = jsonArray.toString();

        Log.d("export", jsonString);
        return jsonString;
    }

    public void deleteFromDb(int id, String day) {
        SQLiteDatabase db = this.getWritableDatabase();
//        db.delete(TABLE_POOJA, "id = ?", new String[] { String.valueOf(id)});
        db.delete(TABLE_DAY, "id = ? AND day = ?", new String[] { String.valueOf(id),day });
        db.close();
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
        int count=0;
        while (cursor.moveToNext()) {
            if(cursor.getInt(4)==0)
                return count;
            count++;
        }
        return count;
    }
}
