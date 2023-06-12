package com.example.poojatracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class MyDBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME="PoojaDB";
    private static final int DATABASE_VERSION=1;
    //table poojas
    private static final String TABLE_POOJA="poojas";
    private static final String KEY_ID="id";
    private static final String KEY_TITLE="title";
    private static final String KEY_DESC="description";
    private static final String KEY_URL="contentUrl";
    private static final String KEY_STATUS="status";
    //table days
    private static final String TABLE_DAY="days";
    private static final String KEY_DAY="day";



    public MyDBHelper(@Nullable Context context) {
        super(context,DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //CREATE TABLE pooja(id INTEGER PRIMARY KEY,title TEXT, desc TEXT, contentUrl TEXT, status BOOLEAN)
            db.execSQL("CREATE TABLE " + TABLE_POOJA +
                    "(" + KEY_ID +  " INTEGER PRIMARY KEY, " + KEY_TITLE + " TEXT, " + KEY_DESC + " TEXT, " + KEY_URL + " TEXT, " + KEY_STATUS + " BOOLEAN " + ")"
            );

//        CREATE TABLE days2(id INTEGER , day TEXT,PRIMARY KEY(id,day) );
            db.execSQL("CREATE TABLE " + TABLE_DAY +
                    "(" + KEY_ID +  " INTEGER , " + KEY_DAY + " TEXT, PRIMARY KEY ( " + KEY_ID + " , " +KEY_DAY +  ") ); "
            );

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
    public void addPooja(int id,String title,String desc,String contentUrl, boolean status)
    {
        SQLiteDatabase db=this.getWritableDatabase();
        ContentValues values =new ContentValues();
        values.put(KEY_ID,id);
        values.put(KEY_TITLE,title);
        values.put(KEY_DESC,desc);
        values.put(KEY_URL,contentUrl);
        values.put(KEY_STATUS,status);
        db.insert(TABLE_POOJA,null,values);
        Log.d("testjson","db updated");
    }
    public void addDay(int id,String day)
    {
        SQLiteDatabase db=this.getWritableDatabase();
        ContentValues values =new ContentValues();
        values.put(KEY_ID,id);
        values.put(KEY_DAY,day);
        db.insert(TABLE_DAY,null,values);
    }
//    SELECT p.id , p.title , p.description, p.contentUrl, p.status, d.day
//    FROM poojas p
//    JOIN days d ON p.id=d.id
//    WHERE d.day='Monday';
    public ArrayList<PoojaModel> fetchPooja(String currentDay)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor =db.rawQuery(
                "SELECT "
                        + TABLE_POOJA+"."+KEY_ID + " , "
                        + TABLE_POOJA+"."+KEY_TITLE + " , "
                        + TABLE_POOJA+"."+KEY_DESC + " , "
                        + TABLE_POOJA+"."+KEY_URL + " , "
                        + TABLE_POOJA+"."+KEY_STATUS
                        + " FROM "
                        + TABLE_POOJA
                        + " JOIN " + TABLE_DAY
                        + " ON " + TABLE_POOJA+"."+KEY_ID + " = " +TABLE_DAY+"."+KEY_ID
                        + " WHERE " + TABLE_DAY +"."+KEY_DAY+ " = " + "'" +currentDay+"'" +" ;"
                ,null
        );
        ArrayList<PoojaModel> arrayList = new ArrayList<>();

        while(cursor.moveToNext())
        {
            PoojaModel poojaModel;
            poojaModel = new PoojaModel(
                    cursor.getInt(0),cursor.getString(1),cursor.getString(2),cursor.getString(3),0
            );
            arrayList.add(poojaModel);
        }
        return arrayList;
    }
}
