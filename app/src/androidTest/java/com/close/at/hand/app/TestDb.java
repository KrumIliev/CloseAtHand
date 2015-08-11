package com.close.at.hand.app;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;
import android.util.Log;

import com.close.at.hand.app.data.DatabaseHelper;
import com.close.at.hand.app.data.DatabaseMap;

import java.util.Map;
import java.util.Set;

public class TestDb extends AndroidTestCase {

    public static final String LOG_TAG = TestDb.class.getSimpleName();

    public void testCreateDb() throws Throwable {
        mContext.deleteDatabase(DatabaseHelper.DATABASE_NAME);
        SQLiteDatabase db = new DatabaseHelper(
                this.mContext).getWritableDatabase();

        // Check if database is created ok
        assertEquals(true, db.isOpen());

        // Check if the types values are added on create
        Cursor typesCursor = db.query(DatabaseMap.TypesEntry.TABLE_NAME,
                null, // all columns
                null, // Columns for the "where" clause
                null, // Values for the "where" clause
                null, // columns to group by
                null, // columns to filter by row groups
                null // sort order
        );

        String[] types =  mContext.getResources().getStringArray(R.array.data_place_types);
        assertTrue(types.length == typesCursor.getCount());
        assertTrue(typesCursor.moveToFirst());

        // Check if the values match with expected
        String expectedType = types[0];
        String receivedType = typesCursor.getString(typesCursor.getColumnIndex(DatabaseMap.TypesEntry.TYPE_NAME));

        assertEquals(expectedType, receivedType);

        db.close();
    }

    public void testInsertReadDb () {
        // If there's an error in those massive SQL table creation Strings,
        // errors will be thrown here when you try to get a writable database.
        DatabaseHelper dbHelper = new DatabaseHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues cv = new ContentValues();

        String placeID = "ChIJN1rU1nSFqkARt_B13ZcY8PI";

        cv.put(DatabaseMap.PlaceEntry._ID, placeID);
        cv.put(DatabaseMap.PlaceEntry.PLACE_NAME, "Starbucks Coffee");
        cv.put(DatabaseMap.PlaceEntry.PLACE_ADDRESS, "ulitsa Gen. Yosif V. Gurko 62, Sofia");
        cv.put(DatabaseMap.PlaceEntry.PLACE_LAT, "42.690506");
        cv.put(DatabaseMap.PlaceEntry.PLACE_LNG, "23.317773");
        cv.put(DatabaseMap.PlaceEntry.PLACE_NUMBER, "+359 2 986 3294");
        cv.put(DatabaseMap.PlaceEntry.PLACE_WEB, "http://www.starbucks.bg/bg/");

        long placeRowId = db.insert(DatabaseMap.PlaceEntry.TABLE_NAME, null, cv);
        assertTrue(placeRowId != -1);

        Log.d(LOG_TAG, "New place row id: " + placeRowId);

        Cursor placeCursor = db.query(
                DatabaseMap.PlaceEntry.TABLE_NAME,  // Table to Query
                null, // all columns
                null, // Columns for the "where" clause
                null, // Values for the "where" clause
                null, // columns to group by
                null, // columns to filter by row groups
                null // sort order
        );

        validateCursor(placeCursor, cv);

        cv.clear();

        cv.put(DatabaseMap.ReviewsEntry.PLACE_ID, placeID);
        cv.put(DatabaseMap.ReviewsEntry.USER_NAME, "Gan Cho");
        cv.put(DatabaseMap.ReviewsEntry.USER_COMMENT, "No comment");

        long reviewRowId = db.insert(DatabaseMap.ReviewsEntry.TABLE_NAME, null, cv);
        assertTrue(reviewRowId != -1);

        Log.d(LOG_TAG, "New review row id: " + reviewRowId);

        Cursor reviewCursor =  db.query(
                DatabaseMap.ReviewsEntry.TABLE_NAME,  // Table to Query
                null, // all columns
                null, // Columns for the "where" clause
                null, // Values for the "where" clause
                null, // columns to group by
                null, // columns to filter by row groups
                null // sort order
        );

        validateCursor(reviewCursor, cv);

        cv.clear();

        cv.put(DatabaseMap.PlaceTypeEntry.PLACE_ID, placeID);
        cv.put(DatabaseMap.PlaceTypeEntry.TYPE_ID, 0);

        long placeTypewRowId = db.insert(DatabaseMap.PlaceTypeEntry.TABLE_NAME, null, cv);
        assertTrue(placeTypewRowId != -1);

        Log.d(LOG_TAG, "New place type row id: " + placeTypewRowId);
    }

    static void validateCursor(Cursor valueCursor, ContentValues expectedValues) {

        assertTrue(valueCursor.moveToFirst());

        Set<Map.Entry<String, Object>> valueSet = expectedValues.valueSet();
        for (Map.Entry<String, Object> entry : valueSet) {
            String columnName = entry.getKey();
            int idx = valueCursor.getColumnIndex(columnName);
            assertFalse(idx == -1);
            String expectedValue = entry.getValue().toString();
            assertEquals(expectedValue, valueCursor.getString(idx));
        }
        valueCursor.close();
    }
}
