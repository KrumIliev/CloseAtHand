package com.close.at.hand.app;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

import com.close.at.hand.app.data.DatabaseMap;
import com.close.at.hand.app.data.FetchPlacesListService;
import com.close.at.hand.app.settings.SettingsActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;


public class MainActivity extends ActionBarActivity implements ListFragment.FragmentCallback,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        LocationListener {

    private final String LOG_TAG = MainActivity.class.getSimpleName();

    // Minimum distance between to location points. This is used to check if
    // the last location and location update are 20m. apart.
    private final int MIN_DISTANCE = 20;

    private final int UPDATE_FAST_INTERVAL = 16;
    private final int UPDATE_PRIORITY = LocationRequest.PRIORITY_HIGH_ACCURACY;

    private LocationClient locationClient;
    private LocationRequest locationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_main);
        setProgressBarIndeterminateVisibility(true);

        if (getResources().getBoolean(R.bool.isTablet)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        int resp = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resp == ConnectionResult.SUCCESS) {
            locationClient = new LocationClient(this, this, this);
        } else {
            Log.d(LOG_TAG, "Google Play Service Error " + resp);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.action_map:
                if (Utility.isNetworkAvailable(this)) {
                    Intent intent = new Intent(this, MapActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, getString(R.string.m_no_connection), Toast.LENGTH_LONG).show();
                }
                return true;
            case R.id.action_favorites:
                startActivity(new Intent(this, FavoritesActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void placeSelected(String placeId) {

        // Resetting scroll position
        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putInt(DetailFragment.KEY_SCROLL_X, 0).commit();
        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putInt(DetailFragment.KEY_SCROLL_Y, 0).commit();

        if (getResources().getBoolean(R.bool.isTablet)) {
            Bundle args = new Bundle();
            args.putString(DetailFragment.PLACE_KEY, placeId);
            DetailFragment detailFragment = new DetailFragment();
            detailFragment.setArguments(args);
            getSupportFragmentManager().beginTransaction().replace(R.id.place_container, detailFragment).commit();
        } else {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra(DetailFragment.PLACE_KEY, placeId);
            startActivity(intent);
        }
    }

    @Override
    public void updateProgressIndicator(boolean state) {
        setProgressBarIndeterminateVisibility(state);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Location location = locationClient.getLastLocation();
        Log.d(LOG_TAG, "Location connected" + location.toString());
        // Update the location only if the user is more than MIN_DISTANCE from the previous location
        if (Utility.calculateDistance(this, location.getLatitude(), location.getLongitude()) > MIN_DISTANCE || isDatabaseEmpty()) {
            updateLocation(location);
        }

        // Setting up location update to see if the last location is correct
        locationRequest = LocationRequest.create();
        locationRequest.setFastestInterval(UPDATE_FAST_INTERVAL);
        locationRequest.setPriority(UPDATE_PRIORITY);
        locationClient.requestLocationUpdates(locationRequest, this);
    }

    private boolean isDatabaseEmpty() {
        Uri placesUri = DatabaseMap.PlaceEntry.CONTENT_URI;
        Cursor cursor = getContentResolver().query(placesUri, null, null, null, null);
        boolean result = !cursor.moveToNext();
        cursor.close();
        return result;
    }

    @Override
    public void onDisconnected() {
    }

    @Override
    public void onLocationChanged(Location location) {
        // Update the location only if the user is more than MIN_DISTANCE from the previous location
        if (Utility.calculateDistance(this, location.getLatitude(), location.getLongitude()) > MIN_DISTANCE) {
            updateLocation(location);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    private void updateLocation(Location location) {
        Log.d(LOG_TAG, "Location update:" + location.toString());
        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putString(getString(R.string.pref_lat),
                        String.valueOf(location.getLatitude())).commit();
        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putString(getString(R.string.pref_lng),
                        String.valueOf(location.getLongitude())).commit();

        // Fetch data with new location
        Intent intent = new Intent(this, FetchPlacesListService.class);
        startService(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        locationClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        locationClient.disconnect();
    }
}
