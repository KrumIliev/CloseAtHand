package com.close.at.hand.app.data;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

public class DatabaseMap {

    public static final String CONTENT_AUTHORITY = "com.close.at.hand.app";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_PLACE = "places";
    public static final String PATH_REVIEW = "reviews";

    public static final class PlaceEntry implements BaseColumns {

        // Table name
        public static final String TABLE_NAME = "places";

        // Place name provided by the API
        public static final String PLACE_NAME = "name";

        // Place address provided by the API
        public static final String PLACE_ADDRESS = "address";

        // Place coordinates provided by the API
        public static final String PLACE_LAT = "lat";
        public static final String PLACE_LNG = "lng";

        // Place number provided by the API
        public static final String PLACE_NUMBER = "number";

        // Place website provided by the API
        public static final String PLACE_WEB = "website";

        // Place icon url provided by the API
        public static final String PLACE_ICON_FILE_NAME = "icon";

        // Place distance from user
        public static final String PLACE_DISTANCE = "distance";

        // If the place is added to user bookmarks
        public static final String PLACE_FAVORITE = "favorite";

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_PLACE).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_PLACE;

        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_PLACE;

        public static Uri BuildPlaceWithIdUri(String placeId) {
            return CONTENT_URI.buildUpon().appendPath(placeId).build();
        }

        public static Uri buildPlaceUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildPlaceByFavoriteUri(int favorite) {
            return ContentUris.withAppendedId(CONTENT_URI, favorite);
        }

        public static String getPlaceIdFromUri(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        public static long getPlaceBookmarkFromUri(Uri uri) {
            return ContentUris.parseId(uri);
        }
    }

    public static final class ReviewsEntry implements BaseColumns {

        // Table name
        public static final String TABLE_NAME = "reviews";

        // Column with the foreign key into the reviews table.
        public static final String PLACE_ID = "place_id";

        // Review user name
        public static final String USER_NAME = "user_name";

        // Review user rating
        public static final String USER_RATING = "user_rating";

        // Review user comment
        public static final String USER_COMMENT = "user_comment";

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_REVIEW).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_REVIEW;

        public static Uri buildReviewUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildReviewWithPlaceIdUri(String placeId) {
            return CONTENT_URI.buildUpon().appendPath(placeId).build();
        }

        public static String getPlaceIdFromUri(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }
}
