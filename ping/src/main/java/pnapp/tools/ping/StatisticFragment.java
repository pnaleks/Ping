/*
 *  Copyright (c) 2016 P.N.Alekseev <pnaleks@gmail.com>
 */
package pnapp.tools.ping;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Locale;

public class StatisticFragment extends Fragment {
    private static final String ARG_HOST_NAME = "host_name";
    private static final String ARG_HOST_ADDRESS = "host_address";
    private static final String ARG_VALUES = "values";

    private static final String STR_UNKNOWN = "—";
 	private View mView;

    private ContentLoadingProgressBar mResolveProgressBar;
    private ContentLoadingProgressBar mReverseProgressBar;


    public static String mHostName = "";
    public static String mHostAddress = "";

    private String[] mValues;
    private TextView[] mValueViews;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mView = inflater.inflate(R.layout.statistic_fragment, container, false);

        mValueViews = new TextView[] {
                (TextView) mView.findViewById(R.id.last_value),
                (TextView) mView.findViewById(R.id.count_value),
                (TextView) mView.findViewById(R.id.mean_value),
                (TextView) mView.findViewById(R.id.max_value)
        };

        if ( savedInstanceState != null ) {
            mHostName = savedInstanceState.getString(ARG_HOST_NAME);
            mHostAddress = savedInstanceState.getString(ARG_HOST_ADDRESS);
            mValues = savedInstanceState.getStringArray(ARG_VALUES);
        }

        if (mHostName != null) setText(R.id.host_name, mHostName);
        if (mHostAddress != null) setText(R.id.ip_address, mHostAddress);
        if (mValues != null) updateValueViews();

        mResolveProgressBar = (ContentLoadingProgressBar) mView.findViewById(R.id.progress_resolve);
        mReverseProgressBar = (ContentLoadingProgressBar) mView.findViewById(R.id.progress_reverse);

        mResolveProgressBar.hide();
        mReverseProgressBar.hide();

		return mView;
	}

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_HOST_NAME, mHostName);
        outState.putString(ARG_HOST_ADDRESS, mHostAddress);
        outState.putStringArray(ARG_VALUES, mValues);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mView = null;
    }

    public void setHostName(String name, boolean showProgress) {
        mHostName = (name == null) ? STR_UNKNOWN : name;
        if ( mView != null ) setText(R.id.host_name, mHostName);
        if (showProgress)
            mReverseProgressBar.show();
        else
            mReverseProgressBar.hide();
    }

    public void setHostAddress(String address, boolean showProgress) {
        mHostAddress = (address == null) ? STR_UNKNOWN : address;
        if ( mView != null ) setText(R.id.ip_address, mHostAddress);
        if (showProgress)
            mResolveProgressBar.show();
        else
            mResolveProgressBar.hide();
    }


	public void put(Resolver r) {
        setHostName(r.getHostName(), false);
        setHostAddress(r.getHostAddress(), false);
	}

	public void put(Pinger p) {
        double req = p.getRequests();
        double res = p.getResponses();
        double los = req > 0 ? (req - res) * 100.0 / req : 0;
        if ( res > 0 ) {
            mValues = new String[] {
                    String.format(Locale.US, "%.3f", p.getLast()),
                    String.format(Locale.US, "%.0f / %.0f (%.1f%%)", req, res, los),
                    String.format(Locale.US, "%.3f \u00b1 %.3f", p.getMean(), p.getMStd()),
                    String.format(Locale.US, "%.3f / %.3f", p.getMax(), p.getMin())
            };
        } else {
            mValues = new String[] {"", "", "", ""};
            if (req > 0) mValues[1] = String.format(Locale.US, "%.0f / 0 (100%%)", req);
        }
        updateValueViews();
	}

    private void updateValueViews() {
        if (mView != null) {
            for(int i = 0; i < 4; i++) mValueViews[i].setText(mValues[i]);
        }
    }

	private void setText(int id, String text) {
		TextView view = (TextView) mView.findViewById(id);
		view.setText(text);
	}
	
    public void clear() {
        setHostName("", false);
        setHostAddress("", false);
        setText(R.id.last_value,  "");
        setText(R.id.count_value, "");
        setText(R.id.mean_value,  "");
        setText(R.id.max_value,   "");
    }

}
