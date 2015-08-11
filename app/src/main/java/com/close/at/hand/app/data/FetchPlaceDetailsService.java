package com.close.at.hand.app.data;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.close.at.hand.app.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class FetchPlaceDetailsService extends IntentService {

    private final String LOG_TAG = FetchPlacesListService.class.getSimpleName();
    public static final String PLACE_ID_KEY = "get_place_details";

    private final String BASE_DETAIL_URL = "https://maps.googleapis.com/maps/api/place/details/json?";
    private final String PARAM_LANGUAGE = "language";
    private final String PARAM_LANGUAGE_VALUE = "en";
    private final String PARAM_API_KEY = "key";
    private final String PARAM_PLACE_ID = "placeid";

    private final String JSON_DETAIL_RESULT = "result";
    private final String JSON_ADDRESS = "formatted_address";
    private final String JSON_WEB = "website";
    private final String JSON_NUMBER = "international_phone_number";
    private final String JSON_REVIEW = "reviews";
    private final String JSON_REVIEW_AUTHOR = "author_name";
    private final String JSON_REVIEW_COMMENT = "text";
    private final String JSON_REVIEW_RATING = "rating";

    private String placeId;
    private String placeAddress;
    private String placeNumber;
    private String placeWeb;
    private String reviewAuthor;
    private String reviewText;
    private int reviewRating;

    public FetchPlaceDetailsService() {
        super("FetchDetailsService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (!intent.hasExtra(PLACE_ID_KEY)) {
            return;
        }

        placeId = intent.getStringExtra(PLACE_ID_KEY);

        Log.d(LOG_TAG, placeId);

        // Places API key
        String key = getString(R.string.places_api_key);

        Uri buildUri = Uri.parse(BASE_DETAIL_URL).buildUpon()
                .appendQueryParameter(PARAM_PLACE_ID, placeId)
                .appendQueryParameter(PARAM_LANGUAGE, PARAM_LANGUAGE_VALUE)
                .appendQueryParameter(PARAM_API_KEY, key)
                .build();

        String placeDetailJsonStr = getJsonFromServer(buildUri.toString());
        if (placeDetailJsonStr == null) {
            return;
        }

        getPlaceDetailsFromJson(placeDetailJsonStr);
        getReviewsFromJson(placeDetailJsonStr);

        return;
    }

    private String getJsonFromServer(String serverUrl) {

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String jsonResult = null;

        try {
            URL url = new URL(serverUrl);

            // Create the request to Google Places API, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return null;
            }

            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return null;
            }
            jsonResult = buffer.toString();
            return jsonResult;
        } catch (IOException e) {
            Log.e(LOG_TAG, e.toString(), e);
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
    }

    private void getPlaceDetailsFromJson(String jsonStr) {
        try {
            JSONObject placeDetailFile = new JSONObject(jsonStr);
            JSONObject placeDetailsObj = placeDetailFile.getJSONObject(JSON_DETAIL_RESULT);

            placeAddress = placeDetailsObj.getString(JSON_ADDRESS);

            // Not all places have listed number
            try {
                placeNumber = placeDetailsObj.getString(JSON_NUMBER);
            } catch (JSONException noNum) {
                placeNumber = "";
                Log.d(LOG_TAG, noNum.getMessage());
            }

            // Not all places have listed website
            try {
                placeWeb = placeDetailsObj.getString(JSON_WEB);
            } catch (JSONException noWeb) {
                placeWeb = "";
                Log.d(LOG_TAG, noWeb.getMessage());
            }

            ContentValues placeValues = new ContentValues();
            placeValues.put(DatabaseMap.PlaceEntry.PLACE_ADDRESS, placeAddress);
            placeValues.put(DatabaseMap.PlaceEntry.PLACE_NUMBER, placeNumber);
            placeValues.put(DatabaseMap.PlaceEntry.PLACE_WEB, placeWeb);

            Log.d(LOG_TAG, "Place details: " + placeValues.toString());

            Uri updatePlaceUri = DatabaseMap.PlaceEntry.BuildPlaceWithIdUri(placeId);
            int rows = getContentResolver().update(updatePlaceUri, placeValues, null, null);

            Log.d(LOG_TAG, "Row updated: " + rows);

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
    }

    private void getReviewsFromJson(String jsonStr) {
        ArrayList<ContentValues> cvReviews = new ArrayList<ContentValues>();

        try {
            JSONObject placeDetailFile = new JSONObject(jsonStr);
            JSONObject placeDetailsObj = placeDetailFile.getJSONObject(JSON_DETAIL_RESULT);
            JSONArray reviewArr = placeDetailsObj.getJSONArray(JSON_REVIEW);

            for (int j = 0; j < reviewArr.length(); j++) {

                // Retrieving review from json array
                JSONObject reviewObj = reviewArr.getJSONObject(j);
                reviewAuthor = reviewObj.getString(JSON_REVIEW_AUTHOR);
                reviewText = reviewObj.getString(JSON_REVIEW_COMMENT);
                reviewRating = reviewObj.getInt(JSON_REVIEW_RATING);

                // Adding review to content values array
                ContentValues cvRev = new ContentValues();
                cvRev.put(DatabaseMap.ReviewsEntry.PLACE_ID, placeId);
                cvRev.put(DatabaseMap.ReviewsEntry.USER_NAME, reviewAuthor);
                cvRev.put(DatabaseMap.ReviewsEntry.USER_COMMENT, reviewText);
                cvRev.put(DatabaseMap.ReviewsEntry.USER_RATING, reviewRating);
                cvReviews.add(cvRev);
            }

            ContentValues[] reviewsToInsert = new ContentValues[cvReviews.size()];
            for (int p = 0; p < cvReviews.size(); p++) {
                reviewsToInsert[p] = cvReviews.get(p);
            }

            // Adding reviews to database
            getContentResolver().bulkInsert(DatabaseMap.ReviewsEntry.CONTENT_URI, reviewsToInsert);

        } catch (JSONException noReviews) {
            Log.d(LOG_TAG, "Place: " + placeId + " has no reviews");
            Log.d(LOG_TAG, noReviews.getMessage());
        }
    }
}
