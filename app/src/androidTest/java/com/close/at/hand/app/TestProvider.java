package com.close.at.hand.app;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.util.Log;

import com.close.at.hand.app.data.DatabaseMap;

import java.util.Map;
import java.util.Set;

public class TestProvider extends AndroidTestCase {

    public static final String LOG_TAG = TestProvider.class.getSimpleName();
    private String placeId = "ChIJN1rU1nSFqkARt_B13ZcY8PI";

    public void testDeleteDb() throws Throwable {
        mContext.getContentResolver().delete(
                DatabaseMap.PlaceEntry.CONTENT_URI,
                null,
                null);
    }

    public void testGetType() {
        String type = mContext.getContentResolver().getType(DatabaseMap.PlaceEntry.CONTENT_URI);
        assertEquals(DatabaseMap.PlaceEntry.CONTENT_TYPE, type);

        type = mContext.getContentResolver().getType(DatabaseMap.PlaceEntry.BuildPlaceWithIdUri(placeId));
        assertEquals(DatabaseMap.PlaceEntry.CONTENT_ITEM_TYPE, type);

        type = mContext.getContentResolver().getType(DatabaseMap.TypesEntry.CONTENT_URI);
        assertEquals(DatabaseMap.TypesEntry.CONTENT_TYPE, type);

        type = mContext.getContentResolver().getType(DatabaseMap.ReviewsEntry.CONTENT_URI);
        assertEquals(DatabaseMap.ReviewsEntry.CONTENT_TYPE, type);

        type = mContext.getContentResolver().getType(DatabaseMap.ReviewsEntry.buildReviewWithPlaceIdUri(placeId));
        assertEquals(DatabaseMap.ReviewsEntry.CONTENT_TYPE, type);

        type = mContext.getContentResolver().getType(DatabaseMap.PlaceTypeEntry.buildPlaceIdUri(placeId));
        assertEquals(DatabaseMap.PlaceTypeEntry.CONTENT_TYPE, type);

        type = mContext.getContentResolver().getType(DatabaseMap.PlaceTypeEntry.buildTypeIdUri(0));
        assertEquals(DatabaseMap.PlaceTypeEntry.CONTENT_TYPE, type);
    }

    public void testInsertReadProvider() {
        ContentValues cv = new ContentValues();

        cv.put(DatabaseMap.PlaceEntry._ID, placeId);
        cv.put(DatabaseMap.PlaceEntry.PLACE_NAME, "Starbucks Coffee");
        cv.put(DatabaseMap.PlaceEntry.PLACE_ADDRESS, "ulitsa Gen. Yosif V. Gurko 62, Sofia");
        cv.put(DatabaseMap.PlaceEntry.PLACE_LAT, "42.690506");
        cv.put(DatabaseMap.PlaceEntry.PLACE_LNG, "23.317773");
        cv.put(DatabaseMap.PlaceEntry.PLACE_NUMBER, "+359 2 986 3294");
        cv.put(DatabaseMap.PlaceEntry.PLACE_WEB, "http://www.starbucks.bg/bg/");

        Uri placeRowUri = mContext.getContentResolver().insert(DatabaseMap.PlaceEntry.CONTENT_URI, cv);

        Cursor placeCursor = mContext.getContentResolver().query(
                DatabaseMap.PlaceEntry.CONTENT_URI,  // Uri to Query
                null, // all columns
                null, // Columns for the "where" clause
                null, // Values for the "where" clause
                null // sort order
        );

        validateCursor(placeCursor, cv);

        cv.clear();

        cv.put(DatabaseMap.ReviewsEntry.PLACE_ID, placeId);
        cv.put(DatabaseMap.ReviewsEntry.USER_NAME, "Gan Cho");
        cv.put(DatabaseMap.ReviewsEntry.USER_COMMENT, "No comment");

        Uri reviewRowUri =  mContext.getContentResolver().insert(DatabaseMap.ReviewsEntry.CONTENT_URI, cv);
        Uri reviewRowUri2 =  mContext.getContentResolver().insert(DatabaseMap.ReviewsEntry.CONTENT_URI, cv);

        Cursor reviewCursor = mContext.getContentResolver().query(
                DatabaseMap.ReviewsEntry.CONTENT_URI,   // Uri to Query
                null, // all columns
                null, // Columns for the "where" clause
                null, // Values for the "where" clause
                null // sort order
        );

        Log.d(LOG_TAG, "Reviews: " + reviewCursor.getCount());
        validateCursor(reviewCursor, cv);

        cv.clear();

        cv.put(DatabaseMap.PlaceTypeEntry.PLACE_ID, placeId);
        cv.put(DatabaseMap.PlaceTypeEntry.TYPE_ID, 0);

        Uri placeTypewRowUri = mContext.getContentResolver().insert(DatabaseMap.PlaceTypeEntry.CONTENT_URI, cv);
    }

    public void testCascadeDeleteProvider () {

        String selection = DatabaseMap.PlaceEntry._ID + " = ?";
        String[] params = new String[] {placeId};
        int rowsDeleted = mContext.getContentResolver().delete(DatabaseMap.PlaceEntry.CONTENT_URI, selection, params);

        Cursor reviewCursor = mContext.getContentResolver().query(
                DatabaseMap.ReviewsEntry.buildReviewWithPlaceIdUri(placeId),   // Uri to Query
                null, // all columns
                null, // Columns for the "where" clause
                null, // Values for the "where" clause
                null // sort order
        );

        Log.d(LOG_TAG, "Review count: " + reviewCursor.getCount());

        assertFalse(reviewCursor.moveToNext());
        Log.d(LOG_TAG, "Rows deleted: " + rowsDeleted);
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
