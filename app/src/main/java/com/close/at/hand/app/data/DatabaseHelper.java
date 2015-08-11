package com.close.at.hand.app.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.close.at.hand.app.data.DatabaseMap.PlaceEntry;
import com.close.at.hand.app.data.DatabaseMap.ReviewsEntry;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "close.db";
    public static final String ENABLE_FOREIGN_KEYS = "PRAGMA foreign_keys = ON;";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        final String SQL_CREATE_PLACE_TABLE = "CREATE TABLE " + PlaceEntry.TABLE_NAME + " (" +
                PlaceEntry._ID + " TEXT PRIMARY KEY, " +
                PlaceEntry.PLACE_NAME + " TEXT NOT NULL, " +
                PlaceEntry.PLACE_ADDRESS + " TEXT DEFAULT '', " +
                PlaceEntry.PLACE_LAT + " TEXT NOT NULL, " +
                PlaceEntry.PLACE_LNG + " TEXT NOT NULL, " +
                PlaceEntry.PLACE_NUMBER + " TEXT DEFAULT '', " +
                PlaceEntry.PLACE_WEB + " TEXT DEFAULT '', " +
                PlaceEntry.PLACE_ICON_FILE_NAME + " TEXT NOT NULL, " +
                PlaceEntry.PLACE_DISTANCE + " INTEGER NOT NULL, " +
                PlaceEntry.PLACE_FAVORITE + " INTEGER DEFAULT 0, " +

                " UNIQUE (" + PlaceEntry._ID + ") ON CONFLICT IGNORE);";

        final String SQL_CREATE_REVIEW_TABLE = "CREATE TABLE " + ReviewsEntry.TABLE_NAME + " (" +
                ReviewsEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                ReviewsEntry.PLACE_ID + " TEXT NOT NULL, " +
                ReviewsEntry.USER_NAME + " TEXT NOT NULL, " +
                ReviewsEntry.USER_RATING + " INTEGER NOT NULL, " +
                ReviewsEntry.USER_COMMENT + " TEXT NOT NULL," +

                // Set up the place_id column as a foreign key to places table.
                " FOREIGN KEY (" + ReviewsEntry.PLACE_ID  + ") REFERENCES " +
               PlaceEntry.TABLE_NAME + " (" + PlaceEntry._ID + ") " +
                "ON DELETE CASCADE);";

        sqLiteDatabase.execSQL(SQL_CREATE_PLACE_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_REVIEW_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + PlaceEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + ReviewsEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
