package com.close.at.hand.app;

import android.content.Context;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;

public class Utility {

    public static String formatRadius(int radius) {
        String result;
        if (radius > 999) {
            double rKm = radius * 0.001;
            result = String.format("%.2f", rKm) + " km";
        } else {
            result = String.valueOf(radius) + "m";
        }
        return result;
    }

    public static float calculateDistance(Context context, double lat, double lng) {
        double mPositionLat = Double.valueOf(PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.pref_lat), String.valueOf(0)));
        double mPositionLng = Double.valueOf(PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.pref_lng), String.valueOf(0)));

        Location mLocation = new Location("");
        mLocation.setLatitude(mPositionLat);
        mLocation.setLongitude(mPositionLng);

        Location targetLocation = new Location("");
        targetLocation.setLatitude(lat);
        targetLocation.setLongitude(lng);

        return mLocation.distanceTo(targetLocation);
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
