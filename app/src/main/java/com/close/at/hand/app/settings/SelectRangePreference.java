package com.close.at.hand.app.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.SeekBar;
import android.widget.TextView;

import com.close.at.hand.app.R;
import com.close.at.hand.app.Utility;

/** Custom SeekBar Preference for settings activity */
public class SelectRangePreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener, OnClickListener {

    private int mCurrentValue;
    private SeekBar mSeekBar;
    private TextView mValueText;
    private Context mContext;

    public SelectRangePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mCurrentValue = PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(context.getString(R.string.pref_radius_key),
                        context.getResources().getInteger(R.integer.def_search_radius));
    }

    @Override
    protected View onCreateDialogView() {
        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.pref_seekbar, null);
        mValueText = (TextView) view.findViewById(R.id.pref_seek_value);
        mSeekBar = (SeekBar) view.findViewById(R.id.pref_seek_bar);
        mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBar.setProgress(mCurrentValue);
        return view;
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (shouldPersist()) {
            mCurrentValue = mSeekBar.getProgress();
            persistInt(mCurrentValue);
            callChangeListener(Integer.valueOf(mCurrentValue));
        }
        ((AlertDialog) getDialog()).dismiss();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        int minRadius = mContext.getResources().getInteger(R.integer.min_search_radius);
        if (i < minRadius) {
            mValueText.setText(Utility.formatRadius(minRadius));
            mSeekBar.setProgress(minRadius);
        } else {
            mValueText.setText(Utility.formatRadius(i));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}
