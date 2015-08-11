package com.close.at.hand.app;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.close.at.hand.app.data.DatabaseMap;

public class FavoritesFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int PLACE_LOADER = 4;

    private ListFragment.FragmentCallback callback;

    private static final String[] PLACE_COLUMNS = {
            DatabaseMap.PlaceEntry._ID,
            DatabaseMap.PlaceEntry.PLACE_NAME,
            DatabaseMap.PlaceEntry.PLACE_DISTANCE,
            DatabaseMap.PlaceEntry.PLACE_ICON_FILE_NAME,
            DatabaseMap.PlaceEntry.PLACE_FAVORITE
    };

    private ListView mList;
    private CustomListAdapter mCustomAdapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(PLACE_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_list, container, false);

        callback = (ListFragment.FragmentCallback) getActivity();

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
        // Sort order:  Ascending, by distance.
        String sortOrder = DatabaseMap.PlaceEntry.PLACE_DISTANCE + " ASC";
        Uri getPlacesUri = DatabaseMap.PlaceEntry
                .buildPlaceByFavoriteUri(getResources().getInteger(R.integer.favorite_add));
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
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mCustomAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCustomAdapter.swapCursor(null);
    }
}
