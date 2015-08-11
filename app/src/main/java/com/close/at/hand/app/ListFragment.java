package com.close.at.hand.app;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.close.at.hand.app.data.DatabaseMap;

public class ListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int PLACE_LOADER = 0;

    private final String LOG_TAG = ListFragment.class.getSimpleName();

    private FragmentCallback callback;

    private static final String[] PLACE_COLUMNS = {
            DatabaseMap.PlaceEntry._ID,
            DatabaseMap.PlaceEntry.PLACE_NAME,
            DatabaseMap.PlaceEntry.PLACE_DISTANCE,
            DatabaseMap.PlaceEntry.PLACE_ICON_FILE_NAME,
            DatabaseMap.PlaceEntry.PLACE_FAVORITE
    };

    private ListView mList;
    private CustomListAdapter mCustomAdapter;

    public static final String KEY_LIST_FIRST_VISIBLE_POSITION = "position";
    public static final String KEY_LIST_TOP_POSITION = "top_position";

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(PLACE_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        callback = (FragmentCallback) getActivity();
        View rootView = inflater.inflate(R.layout.fragment_list, container, false);

        mCustomAdapter = new CustomListAdapter(getActivity(), null, 0);
        mList = (ListView) rootView.findViewById(R.id.main_list);
        mList.setAdapter(mCustomAdapter);
        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Cursor cursor = mCustomAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
                    callback.placeSelected(cursor
                            .getString(cursor.getColumnIndex(DatabaseMap.PlaceEntry._ID)));
                }
            }
        });
        return rootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        Log.d(LOG_TAG, "Creating loader: " + i);

        // Sort order:  Ascending, by distance.
        String sortOrder = DatabaseMap.PlaceEntry.PLACE_DISTANCE + " ASC";
        Uri getPlacesUri = DatabaseMap.PlaceEntry
                .buildPlaceByFavoriteUri(getResources().getInteger(R.integer.favorite_remove));
        return new CursorLoader(
                getActivity(),
                getPlacesUri,
                PLACE_COLUMNS,
                null,
                null,
                sortOrder
        );
    }

    @Override
    public void onStop() {
        super.onStop();

        // Save list position
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                .putInt(KEY_LIST_FIRST_VISIBLE_POSITION,
                        mList.getFirstVisiblePosition()).commit();

        View v = mList.getChildAt(0);
        int top = (v == null) ? 0 : v.getTop();

        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                .putInt(KEY_LIST_TOP_POSITION, top).commit();
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Log.d(LOG_TAG, "Finishing loader: " + cursor.getCount());
        if (cursor.getCount() != 0) {
            callback.updateProgressIndicator(false);
        }
        mCustomAdapter.swapCursor(cursor);

        // Restore list ot position
        int listPosition = PreferenceManager.getDefaultSharedPreferences(getActivity())
                .getInt(KEY_LIST_FIRST_VISIBLE_POSITION, 0);
        int listTopPosition = PreferenceManager.getDefaultSharedPreferences(getActivity())
                .getInt(KEY_LIST_TOP_POSITION, 0);

        if (listPosition > 0) {
            mList.setSelectionFromTop(listPosition, listTopPosition);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCustomAdapter.swapCursor(null);
    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections, and information loading state
     */
    public static interface FragmentCallback {
        /**
         * Callback for when an place has been selected.
         */
        public void placeSelected(String placeId);

        /**
         * Callback for when information is processed.
         *
         * @param state - true to show, false to hide
         */
        public void updateProgressIndicator(boolean state);
    }
}
