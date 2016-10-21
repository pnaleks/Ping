/*
 *  Copyright (c) 2016 P.N.Alekseev <pnaleks@gmail.com>
 */
package pnapp.tools.ping;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Объединяет {@link SeekBar} и {@link TextView} в одном {@link DialogPreference}.<br>
 * Атрибут {@link pnapp.tools.ping.R.attr#strFormat} задает формат для {@code TextView}, например {@code "Текущее значение %f"}.
 * В крайних положениях шкалы этот текст может быть переопределен атрибутами {@link pnapp.tools.ping.R.attr#strMin} и
 * {@link pnapp.tools.ping.R.attr#strMax}.<br>
 * Шкала может задаваться как обычным образом, используя атрибуты {@link pnapp.tools.ping.R.attr#min},
 * {@link pnapp.tools.ping.R.attr#max} и {@link pnapp.tools.ping.R.attr#resolution}, так и с использованием атрибута
 * {@link pnapp.tools.ping.R.attr#scale}.<br>
 * В последнем случае шкала задается строкой содержащей абсолютные значения для каждого положения шкалы и шаг шкалы в
 * каждом интервале заданных значений. Например, строка {@code "-1 0 i3 10 i5 20"} задает шкалу со значениями
 * (-1 0 3 6 9 10 15 20).
 */
public class SeekBarPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener {
    private static final float DEFAULT_VALUE = 0;
    private static final float DEFAULT_MIN = 0;
    private static final float DEFAULT_MAX = 100;
    private static final float DEFAULT_RESOLUTION = 1;

	private SeekBar  mSeekBar  = null;
	private TextView mTextView = null;

    /** Строка формата для {@link String#format(String, Object...)} содержащая единственную спецификацию 'f' */
	private String mStrFormat;
    /** Если заданна, то заменяет #mStrFormat в крайне левом положении шкалы */
    private String mStrMin;
    /** Если заданна, то заменяет #mStrFormat в крайне правом положении шкалы */
    private String mStrMax;


    private float mMin = DEFAULT_MIN;
    private float mMax = DEFAULT_MAX;

    private float mResolution = DEFAULT_RESOLUTION;
	private float mValue = DEFAULT_VALUE;

    private int mSeekBarMax;
    private int mSeekBarShift;

    private ArrayList<Float> mScale;

	public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SeekBarPreference,0,0);
        String scale;
        mScale = null;
        try {
            mMin = a.getFloat(R.styleable.SeekBarPreference_min, DEFAULT_MIN);
            mMax = a.getFloat(R.styleable.SeekBarPreference_max, DEFAULT_MAX);
            mResolution = a.getFloat(R.styleable.SeekBarPreference_resolution, DEFAULT_RESOLUTION);
            mStrFormat = a.getString(R.styleable.SeekBarPreference_strFormat);
            mStrMin = a.getString(R.styleable.SeekBarPreference_strMin);
            mStrMax = a.getString(R.styleable.SeekBarPreference_strMax);
            scale = a.getString(R.styleable.SeekBarPreference_scale);
        } finally {
            a.recycle();
        }

        if ( mStrFormat == null ) mStrFormat = "%f";

        if ( scale != null ) {
            Float step = 0F;
            Float value = 0F;
            mScale = new ArrayList<>();
            for( String str : scale.split(" ") ) {
                if( str.charAt(0) == 'i' ) {
                    step = Float.valueOf(str.substring(1));
                } else {
                    Float next = Float.valueOf(str);
                    if ( step != 0F ) {
                        while ( (value += step) < next ) {
                            mScale.add(value);
                        }
                    }
                    value = next;
                    mScale.add(value);
                }
            }
            mSeekBarShift = 0;
            mSeekBarMax = mScale.size() - 1;
            if ( mScale.size() > 2 ) {
                mMin = mScale.get(0);
                mMax = mScale.get(mSeekBarMax);
            } else {
                mScale = null;
            }
            Log.i("SeekBarPreference", String.format("Created '%s' with %d points", getTitle().toString(), mScale.size()));
        }

        if ( mScale == null ) {
            int i_max = (int) (mMax / mResolution);
            if (i_max < 0 && i_max * mResolution > mMax) i_max--;

            int i_min = (int) (mMin / mResolution);
            if (i_min > 0 && i_min * mResolution < mMin) i_min++;

            mSeekBarMax = i_max - i_min;
            mSeekBarShift = i_min;
        }

	}

    private String setText() {
        String res;
        if( mValue <= mMin && mStrMin != null ) res = mStrMin;
        else if ( mValue >= mMax && mStrMax != null ) res = mStrMax;
        else res = String.format(Locale.US, mStrFormat, mValue);
        if ( mTextView != null ) mTextView.setText(res);
        return res;
    }

    private int getProgress(float value) {
        if ( mScale != null ) {
            int cnt = 0;
            for ( Float item : mScale ) {
                if ( item >= value ) break;
                cnt++;
            }
            return cnt;
        }
        return (int)(value/mResolution) - mSeekBarShift;
    }

    private float getValue(int progress) {
        if ( mScale != null ) {
            return mScale.get(progress);
        }
        return (mSeekBarShift + mSeekBar.getProgress()) * mResolution;
    }

    @Override
	protected View onCreateDialogView() {
		
		mSeekBar = new SeekBar( getContext() );
		mSeekBar.setMax(mSeekBarMax);
        mSeekBar.setProgress( getProgress(mValue) );
		mSeekBar.setOnSeekBarChangeListener(this);

        mTextView = new TextView( getContext() );
        mTextView.setGravity( Gravity.CENTER );
        setText();

        LinearLayout v = new LinearLayout( getContext() );
		v.setOrientation( LinearLayout.VERTICAL );
		v.addView(mTextView);
		v.addView(mSeekBar);
		
		return v;
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (positiveResult) {
            mValue = getValue(mSeekBar.getProgress());
            setSummary( setText() );
			persistFloat( mValue );
		}
	}

    @Override
	protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
		if (restorePersistedValue) {
			mValue = getPersistedFloat(DEFAULT_VALUE);
		} else {
			mValue = (Float) defaultValue;
			persistFloat( mValue );
		}
        setSummary( setText() );
	}
	
	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getFloat(index, DEFAULT_VALUE);
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		mValue = getValue(progress);
		setText();
	}

	@Override public void onStartTrackingTouch (SeekBar seekBar) {}
	@Override public void onStopTrackingTouch  (SeekBar seekBar) {}
}
