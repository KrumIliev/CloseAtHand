package com.close.at.hand.app;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.util.LruCache;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.close.at.hand.app.data.DatabaseMap;

public class CustomListAdapter extends CursorAdapter {

    private final String LOG_TAG = CustomListAdapter.class.getSimpleName();
    private LruCache<String, Bitmap> mMemoryCache;

    public CustomListAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;

        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB_MR1) {
                    return bitmap.getByteCount() / 1024;
                } else {
                    return bitmap.getRowBytes() * bitmap.getHeight() / 1024;
                }
            }
        };
    }

    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        View view = LayoutInflater.from(context).inflate(R.layout.fragment_list_item, viewGroup, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        String iconFileName = cursor.getString(cursor.getColumnIndex(DatabaseMap.PlaceEntry.PLACE_ICON_FILE_NAME));
        final Bitmap icon = getBitmapFromMemCache(iconFileName);

        if (icon != null) {
            viewHolder.iconView.setImageBitmap(icon);
        } else {
            new LoadImage(viewHolder.iconView, iconFileName).execute();
        }

        viewHolder.nameView.setText(cursor.getString(cursor.getColumnIndex(DatabaseMap.PlaceEntry.PLACE_NAME)));
        viewHolder.distanceView.setText(String.valueOf(Utility
                .formatRadius(cursor.getInt(cursor.getColumnIndex(DatabaseMap.PlaceEntry.PLACE_DISTANCE)))));
    }

    public static class ViewHolder {
        public final ImageView iconView;
        public final TextView nameView;
        public final TextView distanceView;

        public ViewHolder(View view) {
            iconView = (ImageView) view.findViewById(R.id.list_icon);
            nameView = (TextView) view.findViewById(R.id.list_place_name);
            distanceView = (TextView) view.findViewById(R.id.list_place_distance);
        }
    }

    private class LoadImage extends AsyncTask<Void, String, Bitmap> {

        private ImageView imageView;
        private String fileName;

        public LoadImage(ImageView image, String fileName) {
            imageView = image;
            this.fileName = fileName;
        }

        protected Bitmap doInBackground(Void... args) {
            Bitmap bitmap = null;
            try {
                bitmap = BitmapFactory.decodeStream(mContext.openFileInput(fileName));
            } catch (Exception e) {
                Log.e(LOG_TAG, e.toString());
            }
            return bitmap;
        }

        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null && imageView != null) {
                imageView.setImageBitmap(bitmap);
                addBitmapToMemoryCache(fileName, bitmap);
            }
        }
    }
}
