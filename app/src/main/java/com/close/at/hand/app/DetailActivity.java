package com.close.at.hand.app;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.close.at.hand.app.data.DatabaseMap;
import com.close.at.hand.app.settings.SettingsActivity;

import java.io.FileNotFoundException;


public class DetailActivity extends ActionBarActivity {
    private String placeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        setProgressBarIndeterminateVisibility(true);

        if (savedInstanceState == null) {
            placeId = getIntent().getStringExtra(DetailFragment.PLACE_KEY);
        } else {
            placeId = savedInstanceState.getString(DetailFragment.PLACE_KEY);
        }

        Bundle args = new Bundle();
        args.putString(DetailFragment.PLACE_KEY, placeId);

        DetailFragment fragment = new DetailFragment();
        fragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.place_container, fragment).commit();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(DetailFragment.PLACE_KEY, placeId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateActionBarIcon(placeId);
    }

    /**
     * Checks if place address is empty. And if empty fetches place details from API
     */
    private void updateActionBarIcon(String placeId) {
        String[] PLACE_COLUMNS = {
                DatabaseMap.PlaceEntry.PLACE_ICON_FILE_NAME
        };

        Cursor cursor = getContentResolver().query(
                DatabaseMap.PlaceEntry.BuildPlaceWithIdUri(placeId),
                PLACE_COLUMNS,
                null, null, null);
        cursor.moveToNext();

        try {
            Bitmap bitmap = BitmapFactory.decodeStream(openFileInput(cursor.getString(cursor.getColumnIndex(DatabaseMap.PlaceEntry.PLACE_ICON_FILE_NAME))));
            getSupportActionBar().setIcon(new BitmapDrawable(getResources(), bitmap));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        cursor.close();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.action_map:
                Intent intent = new Intent(this, MapActivity.class);
                intent.putExtra(MapActivity.SHOW_ONE_PLACE, true);
                intent.putExtra(MapActivity.SHOW_ONE_PLACE_ID, placeId);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
