package com.close.at.hand.app;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.close.at.hand.app.settings.SettingsActivity;


public class FavoritesActivity extends ActionBarActivity implements ListFragment.FragmentCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        if (getResources().getBoolean(R.bool.isTablet)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.favorites, menu);
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
                    intent.putExtra(MapActivity.SHOW_FAVORITES, true);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, getString(R.string.m_no_connection), Toast.LENGTH_LONG).show();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void placeSelected(String placeId) {
        if (getResources().getBoolean(R.bool.isTablet)) {
            Bundle args = new Bundle();
            args.putString(DetailFragment.PLACE_KEY, placeId);
            DetailFragment detailFragment = new DetailFragment();
            detailFragment.setArguments(args);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.place_container, detailFragment).commit();
        } else {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra(DetailFragment.PLACE_KEY, placeId);
            startActivity(intent);
        }
    }

    @Override
    public void updateProgressIndicator(boolean state) {
    }
}
