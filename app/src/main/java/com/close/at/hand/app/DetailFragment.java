package com.close.at.hand.app;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.close.at.hand.app.data.DatabaseMap;
import com.close.at.hand.app.data.FetchPlaceDetailsService;

public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, View.OnClickListener {

    public static final String LOG_TAG = DetailFragment.class.getSimpleName();

    public static final String PLACE_KEY = "place_key";

    private static final int PLACE_LOADER = 2;
    private static final int REVIEWS_LOADER = 3;

    private static final String[] PLACE_COLUMNS = {
            DatabaseMap.PlaceEntry._ID,
            DatabaseMap.PlaceEntry.PLACE_NAME,
            DatabaseMap.PlaceEntry.PLACE_ADDRESS,
            DatabaseMap.PlaceEntry.PLACE_NUMBER,
            DatabaseMap.PlaceEntry.PLACE_WEB,
            DatabaseMap.PlaceEntry.PLACE_FAVORITE
    };

    private static final String[] REVIEWS_COLUMNS = {
            DatabaseMap.ReviewsEntry.USER_NAME,
            DatabaseMap.ReviewsEntry.USER_COMMENT,
            DatabaseMap.ReviewsEntry.USER_RATING
    };

    private TextView mPlaceName, mPlaceAddress, mPlaceNumber, mPlaceWeb, mReviewTitle;
    private LinearLayout mNumberContainer, mWebContainer;
    private LinearLayout mReviewsHolder;
    private String mShareUrl, mShareName, mShareAddress;
    private ShareActionProvider mShareActionProvider;
    private LinearLayout mDetailsContainer;
    private ProgressBar mDetailsLoading;

    private ScrollView mScrollView;
    public static final String KEY_SCROLL_X = "scroll_x";
    public static final String KEY_SCROLL_Y = "scroll_y";

    public DetailFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(PLACE_LOADER, null, this);
        getLoaderManager().initLoader(REVIEWS_LOADER, null, this);
        getData(getArguments().getString(PLACE_KEY));
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        mScrollView = (ScrollView) rootView.findViewById(R.id.detail_scroll_view);

        mPlaceName = (TextView) rootView.findViewById(R.id.detail_name);
        mPlaceAddress = (TextView) rootView.findViewById(R.id.detail_address);
        mPlaceNumber = (TextView) rootView.findViewById(R.id.detail_number);
        mPlaceWeb = (TextView) rootView.findViewById(R.id.detail_web);
        mReviewsHolder = (LinearLayout) rootView.findViewById(R.id.detail_reviews_holder);
        mDetailsContainer = (LinearLayout) rootView.findViewById(R.id.detail_container);
        mDetailsLoading = (ProgressBar) rootView.findViewById(R.id.detail_loading);

        mNumberContainer = (LinearLayout) rootView.findViewById(R.id.detail_number_container);
        mWebContainer = (LinearLayout) rootView.findViewById(R.id.detail_web_container);
        mReviewTitle = (TextView) rootView.findViewById(R.id.detail_reviews_title);

        ImageView callIcon = (ImageView) rootView.findViewById(R.id.detail_number_call);
        ImageView webIcon = (ImageView) rootView.findViewById(R.id.detail_web_open);

        callIcon.setOnClickListener(this);
        webIcon.setOnClickListener(this);

        return rootView;
    }

    /**
     * Checks if place address is empty. And if empty fetches place details from API
     */
    private void getData(String placeId) {
        String[] PLACE_COLUMNS = {
                DatabaseMap.PlaceEntry.PLACE_ADDRESS
        };

        Cursor cursor = getActivity().getContentResolver().query(
                DatabaseMap.PlaceEntry.BuildPlaceWithIdUri(placeId),
                PLACE_COLUMNS,
                null, null, null);
        cursor.moveToNext();
        if (cursor.getString(cursor.getColumnIndex(DatabaseMap.PlaceEntry.PLACE_ADDRESS)).isEmpty()) {
            Log.d(LOG_TAG, "Starting fetch details service");
            Intent intent = new Intent(getActivity(), FetchPlaceDetailsService.class);
            intent.putExtra(FetchPlaceDetailsService.PLACE_ID_KEY, placeId);
            getActivity().startService(intent);
        }
        cursor.close();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        mDetailsContainer.setVisibility(View.GONE);
        mDetailsLoading.setVisibility(View.VISIBLE);
        String placeId = getArguments().getString(PLACE_KEY);
        Log.d(LOG_TAG, placeId);
        switch (i) {
            case PLACE_LOADER:
                Uri placeFromIdUri = DatabaseMap.PlaceEntry.BuildPlaceWithIdUri(placeId);
                return new CursorLoader(
                        getActivity(),
                        placeFromIdUri,
                        PLACE_COLUMNS,
                        null,
                        null,
                        null
                );
            case REVIEWS_LOADER:
                Uri reviewsFromPlaceIdUri = DatabaseMap.ReviewsEntry.buildReviewWithPlaceIdUri(placeId);
                return new CursorLoader(
                        getActivity(),
                        reviewsFromPlaceIdUri,
                        REVIEWS_COLUMNS,
                        null,
                        null,
                        null
                );
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        switch (cursorLoader.getId()) {
            case PLACE_LOADER:
                updatePlaceDetails(cursor);
                break;
            case REVIEWS_LOADER:
                updatePlaceReviews(cursor);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        getLoaderManager().restartLoader(cursorLoader.getId(), null, this);
    }

    private void updatePlaceDetails(Cursor cursor) {
        if (!cursor.moveToFirst()) {
            return;
        }
        String placeName = cursor.getString(cursor.getColumnIndex(DatabaseMap.PlaceEntry.PLACE_NAME));
        mPlaceName.setText(placeName);
        String placeAddress = cursor.getString(cursor.getColumnIndex(DatabaseMap.PlaceEntry.PLACE_ADDRESS));
        if (!placeAddress.isEmpty()) {
            mDetailsContainer.setVisibility(View.VISIBLE);
            mDetailsLoading.setVisibility(View.GONE);
        }
        mPlaceAddress.setText(placeAddress);

        String placeNumber = cursor.getString(cursor.getColumnIndex(DatabaseMap.PlaceEntry.PLACE_NUMBER));
        if (!placeNumber.isEmpty()) {
            mPlaceNumber.setText(placeNumber);
            mNumberContainer.setVisibility(View.VISIBLE);
        } else {
            mNumberContainer.setVisibility(View.GONE);
        }

        String placeWeb = cursor.getString(cursor.getColumnIndex(DatabaseMap.PlaceEntry.PLACE_WEB));
        if (!placeWeb.isEmpty()) {
            mPlaceWeb.setText(placeWeb);
            mWebContainer.setVisibility(View.VISIBLE);
        } else {
            mWebContainer.setVisibility(View.GONE);
        }


        mShareUrl = placeWeb;
        mShareName = placeName;
        mShareAddress = placeAddress;
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(createShareIntent());
        }

    }

    private void updatePlaceReviews(Cursor cursor) {
        mReviewsHolder.removeAllViews();
        cursor.moveToPosition(-1);

        while (cursor.moveToNext()) {
            String title = cursor.getString(cursor.getColumnIndex(DatabaseMap.ReviewsEntry.USER_NAME));
            String text = cursor.getString(cursor.getColumnIndex(DatabaseMap.ReviewsEntry.USER_COMMENT));
            int rating = cursor.getInt(cursor.getColumnIndex(DatabaseMap.ReviewsEntry.USER_RATING));

            LinearLayout userContainer = new LinearLayout(getActivity());
            userContainer.setOrientation(LinearLayout.VERTICAL);
            userContainer.addView(createReviewTitle(title));
            userContainer.addView(createReviewRating(rating));
            userContainer.addView(createReviewText(text));
            mReviewsHolder.addView(userContainer);
        }

        // Hide review title if no reviews are added in the holder
        if (mReviewsHolder.getChildCount() == 0) {
            mReviewTitle.setVisibility(View.GONE);
        } else {
            mReviewTitle.setVisibility(View.VISIBLE);
        }
    }

    private TextView createReviewTitle(String title) {
        TextView titleView = new TextView(getActivity());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(5, 5, 5, 5);
        titleView.setLayoutParams(params);
        titleView.setTextAppearance(getActivity(), android.R.style.TextAppearance_DeviceDefault_Medium);
        titleView.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
        titleView.setText(title);
        return titleView;
    }

    private RatingBar createReviewRating(int rating) {
        RatingBar ratingBar = new RatingBar(getActivity(), null, android.R.attr.ratingBarStyleSmall);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(5, 5, 5, 5);
        ratingBar.setLayoutParams(params);
        ratingBar.setRating(rating);
        return ratingBar;
    }

    private TextView createReviewText(String text) {
        TextView textView = new TextView(getActivity());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(5, 0, 5, 10);
        textView.setLayoutParams(params);
        textView.setTextAppearance(getActivity(), android.R.style.TextAppearance_DeviceDefault_Small);
        textView.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
        textView.setTextColor(getResources().getColor(R.color.grey_700));
        textView.setText(text);
        return textView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // This adds items to the action bar if it is present.
        inflater.inflate(R.menu.detail_fragment, menu);

        // Retrieve the share menu item
        MenuItem menuItem = menu.findItem(R.id.action_share);

        // Get the provider and hold it to set/change the share intent.
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(createShareIntent());
        } else {
            Log.d(LOG_TAG, "Share action provider is null?");
        }

        if (getFavoriteState() == getResources().getInteger(R.integer.favorite_add)) {
            menu.findItem(R.id.action_favorite)
                    .setIcon(getResources()
                            .getDrawable(R.drawable.ic_action_bookmark_selected));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(LOG_TAG, "Share: " + (item.getItemId() == R.id.action_share));
        switch (item.getItemId()) {
            case R.id.action_favorite:
                updateFavoriteState(item);
                return true;
            case R.id.action_share:
                Log.d(LOG_TAG, "share");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private int getFavoriteState() {
        String placeId = getArguments().getString(PLACE_KEY);
        Uri uri = DatabaseMap.PlaceEntry.BuildPlaceWithIdUri(placeId);
        String[] projection = new String[]{DatabaseMap.PlaceEntry.PLACE_FAVORITE};
        Cursor cursor = getActivity().getContentResolver().query(
                uri,
                projection,
                null,
                null,
                null
        );

        cursor.moveToFirst();
        int favorite = cursor.getInt(cursor.getColumnIndex(DatabaseMap.PlaceEntry.PLACE_FAVORITE));
        return favorite;
    }

    private void updateFavoriteState(MenuItem item) {
        String placeId = getArguments().getString(PLACE_KEY);
        Uri uri = DatabaseMap.PlaceEntry.BuildPlaceWithIdUri(placeId);
        ContentValues contentValues = new ContentValues();
        if (getFavoriteState() == getResources().getInteger(R.integer.favorite_remove)) {
            contentValues.put(DatabaseMap.PlaceEntry.PLACE_FAVORITE,
                    getResources().getInteger(R.integer.favorite_add));
            getActivity().getContentResolver().update(uri, contentValues, null, null);
            item.setIcon(getResources().getDrawable(R.drawable.ic_action_bookmark_selected));
        } else {
            contentValues.put(DatabaseMap.PlaceEntry.PLACE_FAVORITE,
                    getResources().getInteger(R.integer.favorite_remove));
            getActivity().getContentResolver().update(uri, contentValues, null, null);
            item.setIcon(getResources().getDrawable(R.drawable.ic_action_bookmark_add));
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.detail_number_call:
                callNumber();
                break;
            case R.id.detail_web_open:
                openWeb();
                break;
        }
    }

    private void callNumber() {
        Uri number = Uri.parse("tel:" + mPlaceNumber.getText().toString());
        Intent callIntent = new Intent(Intent.ACTION_DIAL, number);
        startActivity(callIntent);
    }

    private void openWeb() {
        Uri web = Uri.parse(mPlaceWeb.getText().toString());
        Intent webIntent = new Intent(Intent.ACTION_VIEW, web);
        startActivity(webIntent);
    }

    private Intent createShareIntent() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, mShareName);
        if (mShareUrl == null || mShareUrl.isEmpty()) {
            intent.putExtra(Intent.EXTRA_TEXT, mShareAddress);
        } else {
            intent.putExtra(Intent.EXTRA_TEXT, mShareUrl);
        }
        return intent;
    }

    @Override
    public void onStop() {
        super.onStop();

        // Save scroll position
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                .putInt(KEY_SCROLL_X, mScrollView.getScrollX()).commit();
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                .putInt(KEY_SCROLL_Y, mScrollView.getScrollY()).commit();
    }

    @Override
    public void onResume() {
        super.onResume();

        final int scrollX = PreferenceManager.getDefaultSharedPreferences(getActivity())
                .getInt(KEY_SCROLL_X, 0);
        final int scrollY = PreferenceManager.getDefaultSharedPreferences(getActivity())
                .getInt(KEY_SCROLL_Y, 0);

        mScrollView.post(new Runnable() {
            @Override
            public void run() {
                mScrollView.scrollTo(scrollX, scrollY);
            }
        });
    }
}
