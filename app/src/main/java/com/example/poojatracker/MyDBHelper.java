package com.example.poojatracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class MyDBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME="PoojaDB";
    private static final int DATABASE_VERSION=1;
    //table poojas
    private static final String TABLE_POOJA="poojas";
    private static final String KEY_ID="id";
    private static final String KEY_TITLE="title";
    private static final String KEY_DESC="desc";
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


            db.execSQL("CREATE TABLE " + TABLE_DAY +
                    "(" + KEY_ID +  " INTEGER , " + KEY_DAY + " TEXT " +  " ) "
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
    }
    public void addDay(int id,String day)
    {
        SQLiteDatabase db=this.getWritableDatabase();
        ContentValues values =new ContentValues();
        values.put(KEY_ID,id);
        values.put(KEY_DAY,day);
        db.insert(TABLE_DAY,null,values);
    }
    public ArrayList<PoojaModel> fetchPooja()
    {
        ArrayList<PoojaModel> arrayList = new ArrayList<>();

        return arrayList;

    }
}
