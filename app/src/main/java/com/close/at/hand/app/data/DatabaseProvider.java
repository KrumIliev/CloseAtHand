package com.close.at.hand.app.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.close.at.hand.app.R;

public class DatabaseProvider extends ContentProvider {

    public static final String LOG_TAG = DatabaseProvider.class.getSimpleName();

    private static final int PLACE = 1;
    private static final int PLACE_ID = 2;
    private static final int PLACE_BY_BOOKMARK = 3;
    private static final int REVIEW = 4;
    private static final int REVIEW_PLACE_ID = 5;

    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = DatabaseMap.CONTENT_AUTHORITY;

        matcher.addURI(authority, DatabaseMap.PATH_PLACE, PLACE);
        matcher.addURI(authority, DatabaseMap.PATH_PLACE + "/#", PLACE_BY_BOOKMARK);
        matcher.addURI(authority, DatabaseMap.PATH_PLACE + "/*", PLACE_ID);
        matcher.addURI(authority, DatabaseMap.PATH_REVIEW, REVIEW);
        matcher.addURI(authority, DatabaseMap.PATH_REVIEW + "/*", REVIEW_PLACE_ID);

        return matcher;
    }

    private DatabaseHelper mDatabaseHelper;
    private static UriMatcher mUriMatcher = buildUriMatcher();

    @Override
    public boolean onCreate() {
        mDatabaseHelper = new DatabaseHelper(getContext());
        return (mDatabaseHelper == null) ? false : true;
    }

    private SQLiteDatabase getDatabase () {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        db.execSQL(DatabaseHelper.ENABLE_FOREIGN_KEYS);
        return db;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        Cursor retCursor;

        switch (mUriMatcher.match(uri)) {
            case PLACE:
                retCursor = mDatabaseHelper.getReadableDatabase()
                        .query(DatabaseMap.PlaceEntry.TABLE_NAME,
                                projection,
                                selection,
                                selectionArgs,
                                null,
                                null,
                                sortOrder);
                break;
            case PLACE_ID:
                retCursor = mDatabaseHelper.getReadableDatabase()
                        .query(DatabaseMap.PlaceEntry.TABLE_NAME,
                                projection,
                                DatabaseMap.PlaceEntry._ID + "='" + DatabaseMap.PlaceEntry.getPlaceIdFromUri(uri) + "'",
                                null,
                                null,
                                null,
                                sortOrder);
                break;
            case PLACE_BY_BOOKMARK:
                retCursor = mDatabaseHelper.getReadableDatabase()
                        .query(DatabaseMap.PlaceEntry.TABLE_NAME,
                                projection,
                                DatabaseMap.PlaceEntry.PLACE_FAVORITE + "=" + (int) DatabaseMap.PlaceEntry.getPlaceBookmarkFromUri(uri),
                                null,
                                null,
                                null,
                                sortOrder);
                break;
            case REVIEW_PLACE_ID:
                retCursor = mDatabaseHelper.getReadableDatabase()
                        .query(DatabaseMap.ReviewsEntry.TABLE_NAME,
                                projection,
                                DatabaseMap.ReviewsEntry.PLACE_ID + "='" + DatabaseMap.ReviewsEntry.getPlaceIdFromUri(uri) + "'",
                                null,
                                null,
                                null,
                                sortOrder);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Override
    public String getType(Uri uri) {
        Log.d(LOG_TAG, uri.toString());
        final int match = mUriMatcher.match(uri);
        switch (match) {
            case PLACE:
                return DatabaseMap.PlaceEntry.CONTENT_TYPE;
            case PLACE_ID:
                return DatabaseMap.PlaceEntry.CONTENT_ITEM_TYPE;
            case PLACE_BY_BOOKMARK:
                return DatabaseMap.PlaceEntry.CONTENT_TYPE;
            case REVIEW:
                return DatabaseMap.ReviewsEntry.CONTENT_TYPE;
            case REVIEW_PLACE_ID:
                return DatabaseMap.ReviewsEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {

        final SQLiteDatabase db = getDatabase();
        Uri returnUri;

        switch (mUriMatcher.match(uri)) {
            case PLACE: {
                Log.d(LOG_TAG, contentValues.toString());
                long _id = db.insert(DatabaseMap.PlaceEntry.TABLE_NAME, null, contentValues);
                if (_id > 0) {
                    returnUri = DatabaseMap.PlaceEntry.buildPlaceUri(_id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            }
            case REVIEW: {
                long _id = db.insert(DatabaseMap.ReviewsEntry.TABLE_NAME, null, contentValues);
                if (_id > 0) {
                    returnUri = DatabaseMap.ReviewsEntry.buildReviewUri(_id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = getDatabase();
        int rowsDeleted;

        switch (mUriMatcher.match(uri)) {

            case PLACE:
                String mSelection = DatabaseMap.PlaceEntry.PLACE_FAVORITE + "=" + getContext().getResources().getInteger(R.integer.favorite_remove);
                rowsDeleted = db.delete(DatabaseMap.PlaceEntry.TABLE_NAME, mSelection, null);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        // Because a null deletes all rows
        if (selection == null || rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = getDatabase();
        int rowsUpdated;

        switch (mUriMatcher.match(uri)) {

            case PLACE_ID:
                rowsUpdated = db.update(
                        DatabaseMap.PlaceEntry.TABLE_NAME,
                        contentValues,
                        DatabaseMap.PlaceEntry._ID + "='" + DatabaseMap.PlaceEntry.getPlaceIdFromUri(uri) + "'",
                        selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = getDatabase();
        int returnCount;
        switch (mUriMatcher.match(uri)) {
            case PLACE:
                db.beginTransaction();
                returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(DatabaseMap.PlaceEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            case REVIEW:
                db.beginTransaction();
                returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(DatabaseMap.ReviewsEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            default:
                return super.bulkInsert(uri, values);
        }
    }
}
