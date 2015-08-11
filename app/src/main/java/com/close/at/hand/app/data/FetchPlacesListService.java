package com.close.at.hand.app.data;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.close.at.hand.app.ListFragment;
import com.close.at.hand.app.R;
import com.close.at.hand.app.Utility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

/**
 * An subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class FetchPlacesListService extends IntentService {

    private final String LOG_TAG = FetchPlacesListService.class.getSimpleName();

    // Place API request parameters
    private final String BASE_ALL_URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?";
    private final String PARAM_TYPES = "types";
    private final String PARAM_LOCATION = "location";
    private final String PARAM_LANGUAGE = "language";
    private final String PARAM_LANGUAGE_VALUE = "en";
    private final String PARAM_RADIUS = "radius";
    private final String PARAM_API_KEY = "key";
    private final String PARAM_PAGE_TOKEN = "pagetoken";

    // JSON parameters
    private final String JSON_ALL_RESULT = "results";
    private final String JSON_NEXT_PAGE = "next_page_token";
    private final String JSON_PLACE_ID = "place_id";
    private final String JSON_NAME = "name";
    private final String JSON_LOCATION_GEO = "geometry";
    private final String JSON_LOCATION_LOC = "location";
    private final String JSON_LOCATION_LAT = "lat";
    private final String JSON_LOCATION_LNG = "lng";
    private final String JSON_ICON = "icon";

    // Location parameters
    private String placeID;
    private String placeName;
    private double placeLat;
    private double placeLng;
    private int placeDistance;
    private String placeIconUrl;

    private boolean initialRequest = true;

    public FetchPlacesListService() {
        super("FetchDataService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        double lat = Double.valueOf(PreferenceManager.getDefaultSharedPreferences(this)
                .getString(getString(R.string.pref_lat), String.valueOf(0)));
        double lng = Double.valueOf(PreferenceManager.getDefaultSharedPreferences(this)
                .getString(getString(R.string.pref_lng), String.valueOf(0)));

        Log.d(LOG_TAG, "Coordinates: " + lat + "," + lng);

        if (lat == 0 || lng == 0) {
            return;
        }

        // Places API key
        String key = getString(R.string.places_api_key);

        // Retrieving JSON with places information from API
        String placesJsonStr = getJsonFromServer(createFetchAllUrl(lat, lng, key, null));
        if (placesJsonStr == null) {
            return;
        }

        // Resetting list position to 0 on list information update
        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putInt(ListFragment.KEY_LIST_FIRST_VISIBLE_POSITION, 0).commit();

        // Extracting and updating places information in database
        getPlacesFromJson(placesJsonStr, lat, lng, key);

        return;
    }

    private String createFetchAllUrl(double lat, double lng, String key, String pageToken) {
        String types = getPlaceTypes();
        String location = "" + lat + "," + lng;
        String radius = String.valueOf(PreferenceManager
                .getDefaultSharedPreferences(this)
                .getInt(getString(R.string.pref_radius_key),
                        getResources().getInteger(R.integer.def_search_radius)));

        Uri.Builder builder = Uri.parse(BASE_ALL_URL).buildUpon();
        builder.appendQueryParameter(PARAM_TYPES, types);
        builder.appendQueryParameter(PARAM_LOCATION, location);
        builder.appendQueryParameter(PARAM_LANGUAGE, PARAM_LANGUAGE_VALUE);
        builder.appendQueryParameter(PARAM_RADIUS, radius);
        builder.appendQueryParameter(PARAM_API_KEY, key);

        if (pageToken != null) {
            builder.appendQueryParameter(PARAM_PAGE_TOKEN, pageToken);
        }

        Uri buildUri = builder.build();

        String urlString = buildUri.toString();
        urlString = urlString.replaceAll("%7C", "|");
        urlString = urlString.replaceAll("%2C", ",");

        return urlString;
    }

    private String getPlaceTypes() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String typesKey = getString(R.string.pref_types_key);
        String typesAll = getString(R.string.type_all);
        String prefType = prefs.getString(typesKey, typesAll);
        if (prefType.equals(typesAll)) {
            StringBuilder buildTypes = new StringBuilder();
            String[] typesList = getResources().getStringArray(R.array.types_all_values);

            // Create Uri for
            for (String type : typesList) {
                buildTypes.append(type);
                buildTypes.append("|");
            }

            buildTypes.deleteCharAt(buildTypes.length() - 1);
            return buildTypes.toString();
        } else {
            return prefType;
        }
    }

    private String getJsonFromServer(String serverUrl) {

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String jsonResult;

        try {
            URL url = new URL(serverUrl);
            Log.d(LOG_TAG, url.toString());

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
            Log.d(LOG_TAG, jsonResult);
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

    private void getPlacesFromJson(String jsonStr, final double lat, final double lng, final String key) {
        try {

            if (initialRequest) {
                // Deleting old places from database
                getContentResolver().delete(DatabaseMap.PlaceEntry.CONTENT_URI, null, null);
                deleteIcons();
                initialRequest = false;
            }

            JSONObject placesJson = new JSONObject(jsonStr);
            JSONArray placesArray = placesJson.getJSONArray(JSON_ALL_RESULT);

            // Insert the new weather information into the database
            ArrayList<ContentValues> cvPlaces = new ArrayList<ContentValues>();

            for (int i = 0; i < placesArray.length(); i++) {
                JSONObject placeObj = placesArray.getJSONObject(i);
                placeID = placeObj.getString(JSON_PLACE_ID);
                placeName = placeObj.getString(JSON_NAME);
                placeLat = placeObj.getJSONObject(JSON_LOCATION_GEO).getJSONObject(JSON_LOCATION_LOC).getDouble(JSON_LOCATION_LAT);
                placeLng = placeObj.getJSONObject(JSON_LOCATION_GEO).getJSONObject(JSON_LOCATION_LOC).getDouble(JSON_LOCATION_LNG);
                placeDistance = (int) Utility.calculateDistance(this, placeLat, placeLng);
                placeIconUrl = placeObj.getString(JSON_ICON);

                ContentValues contentValues = new ContentValues();
                contentValues.put(DatabaseMap.PlaceEntry._ID, placeID);
                contentValues.put(DatabaseMap.PlaceEntry.PLACE_NAME, placeName);
                contentValues.put(DatabaseMap.PlaceEntry.PLACE_LAT, placeLat);
                contentValues.put(DatabaseMap.PlaceEntry.PLACE_LNG, placeLng);
                contentValues.put(DatabaseMap.PlaceEntry.PLACE_DISTANCE, placeDistance);
                contentValues.put(DatabaseMap.PlaceEntry.PLACE_ICON_FILE_NAME, downloadIcon(placeIconUrl));

                Log.d(LOG_TAG, "Values: " + contentValues.toString());
                cvPlaces.add(contentValues);
            }

            Log.d(LOG_TAG, "Places: " + cvPlaces.size());

            ContentValues[] placesToInsert = new ContentValues[cvPlaces.size()];
            for (int p = 0; p < cvPlaces.size(); p++) {
                placesToInsert[p] = cvPlaces.get(p);
            }

            // Adding new places to database
            int places = getContentResolver().bulkInsert(DatabaseMap.PlaceEntry.CONTENT_URI, placesToInsert);
            Log.d(LOG_TAG, "Inserted places: " + places);

            String nextPageToken = null;

            try {
                nextPageToken = placesJson.getString(JSON_NEXT_PAGE);
            } catch (JSONException e) {
                Log.d(LOG_TAG, "No more pages");
            }

            if (nextPageToken != null) {
                final String token = nextPageToken;
                Log.d(LOG_TAG, "Fetching next page");
                String nextPageJsonStr = getJsonFromServer(createFetchAllUrl(lat, lng, key, token));
                getPlacesFromJson(nextPageJsonStr, lat, lng, key);
            }

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.toString(), e);
        }
    }

    /**
     * Deletes all icons that are not used in favorites
     */
    private void deleteIcons() {
        Uri uri = DatabaseMap.PlaceEntry
                .buildPlaceByFavoriteUri(getResources().getInteger(R.integer.favorite_add));
        String[] projection = {DatabaseMap.PlaceEntry.PLACE_ICON_FILE_NAME};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        ArrayList<String> favoritesIcons = new ArrayList<String>();

        while (cursor.moveToNext()) {
            favoritesIcons.add(cursor.getString(cursor.getColumnIndex(DatabaseMap.PlaceEntry.PLACE_ICON_FILE_NAME)));
        }

        String[] files = fileList();
        for (String fileName : files) {
            if (!favoritesIcons.contains(fileName))
                deleteFile(fileName);
        }
    }

    private String downloadIcon(String iconUrl) {
        // Retrieving icon name from icon URL
        String[] urlArr = iconUrl.split("/");
        String iconFileName = urlArr[urlArr.length - 1];
        // Files array from app personal storage
        String[] files = fileList();

        // If icon file name is present in personal storage return icon name
        for (String fileName : files) {
            if (fileName.equals(iconFileName)) return iconFileName;
        }

        // Else download the icon
        try {
            URL url = new URL(iconUrl);
            URLConnection connection = url.openConnection();
            connection.connect();
            InputStream input = new BufferedInputStream(url.openStream());
            FileOutputStream fileOutputStream = openFileOutput(iconFileName, Context.MODE_PRIVATE);

            byte tmpBuffer[] = new byte[1024];
            int read = input.read(tmpBuffer);

            while (read != -1) {
                fileOutputStream.write(tmpBuffer, 0, read);
                read = input.read(tmpBuffer);
            }

            fileOutputStream.flush();
            fileOutputStream.close();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return iconFileName;
    }
}
