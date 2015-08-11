package com.close.at.hand.app;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

import com.close.at.hand.app.data.DatabaseMap;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;

public class MapActivity extends ActionBarActivity implements
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnInfoWindowClickListener {

    public static final String SHOW_FAVORITES = "show_favorites";
    public static final String SHOW_ONE_PLACE = "show_one";
    public static final String SHOW_ONE_PLACE_ID = "show_one_id";

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private final int MAP_ZOOM = 15;
    private LatLng myPosition;
    private HashMap<Marker, String> placeMarkers;
    private boolean showFavorites, showOnePlace;
    private String placeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        showFavorites = getIntent().getBooleanExtra(SHOW_FAVORITES, false);
        showOnePlace = getIntent().getBooleanExtra(SHOW_ONE_PLACE, false);
        if (showOnePlace) {
            placeId = getIntent().getStringExtra(SHOW_ONE_PLACE_ID);
        }
        setUpMapIfNeeded();

        String radius = String.valueOf(PreferenceManager
                .getDefaultSharedPreferences(this)
                .getInt(getString(R.string.pref_radius_key),
                        getResources().getInteger(R.integer.def_search_radius)));
        getSupportActionBar().setTitle("Radius: " + Utility.formatRadius(Integer.valueOf(radius)));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    private void setUpMap() {
        double lat = Double.valueOf(PreferenceManager.getDefaultSharedPreferences(this)
                .getString(getString(R.string.pref_lat), String.valueOf(0)));
        double lng = Double.valueOf(PreferenceManager.getDefaultSharedPreferences(this)
                .getString(getString(R.string.pref_lng), String.valueOf(0)));
        myPosition = new LatLng(lat, lng);
        mMap.setOnInfoWindowClickListener(this);
        addPlaceMarkers();
        updateCamera();
    }

    private void updateCamera() {
        mMap.addMarker(new MarkerOptions()
                .position(myPosition)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

        if (showOnePlace) {
            mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {

                @Override
                public void onCameraChange(CameraPosition arg0) {
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    for (Marker marker : placeMarkers.keySet()) {
                        builder.include(marker.getPosition());
                    }
                    builder.include(myPosition);
                    LatLngBounds bounds = builder.build();
                    int padding = 80; // offset from edges of the map in pixels
                    CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                    // Move camera.
                    mMap.moveCamera(cu);
                    // Remove listener to prevent position reset on camera move.
                    mMap.setOnCameraChangeListener(null);
                }
            });
        } else {
            CameraPosition newCamPos = new CameraPosition(myPosition, MAP_ZOOM,
                    mMap.getCameraPosition().tilt, // use old tilt
                    mMap.getCameraPosition().bearing); // use old bearing

            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(newCamPos),
                    1000, null);
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        marker.showInfoWindow();
        return true;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        if (!showOnePlace) {
            String placeId = placeMarkers.get(marker);
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra(DetailFragment.PLACE_KEY, placeId);
            startActivity(intent);
        }
    }

    private void addPlaceMarkers() {
        placeMarkers = new HashMap<Marker, String>();
        String[] PLACE_COLUMNS = {
                DatabaseMap.PlaceEntry._ID,
                DatabaseMap.PlaceEntry.PLACE_NAME,
                DatabaseMap.PlaceEntry.PLACE_LAT,
                DatabaseMap.PlaceEntry.PLACE_LNG,
                DatabaseMap.PlaceEntry.PLACE_FAVORITE
        };

        Uri getPlacesUri;
        if (showOnePlace) {
            getPlacesUri = DatabaseMap.PlaceEntry.BuildPlaceWithIdUri(placeId);
        } else if (showFavorites) {
            getPlacesUri = DatabaseMap.PlaceEntry
                    .buildPlaceByFavoriteUri(getResources().getInteger(R.integer.favorite_add));
        } else {
            getPlacesUri = DatabaseMap.PlaceEntry
                    .buildPlaceByFavoriteUri(getResources().getInteger(R.integer.favorite_remove));
        }

        Cursor c = getContentResolver().query(
                getPlacesUri,
                PLACE_COLUMNS,
                null, null, null);

        while (c.moveToNext()) {
            LatLng pPosition = new LatLng(
                    Double.valueOf(c.getString(c.getColumnIndex(DatabaseMap.PlaceEntry.PLACE_LAT))),
                    Double.valueOf(c.getString(c.getColumnIndex(DatabaseMap.PlaceEntry.PLACE_LNG))));
            String pName = c.getString(c.getColumnIndex(DatabaseMap.PlaceEntry.PLACE_NAME));
            Marker pMarker = mMap.addMarker(new MarkerOptions()
                    .position(pPosition)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    .title(pName));
            if (showOnePlace) {
                pMarker.showInfoWindow();
            }
            String placeId = c.getString(c.getColumnIndex(DatabaseMap.PlaceEntry._ID));
            placeMarkers.put(pMarker, placeId);
        }
    }
}
