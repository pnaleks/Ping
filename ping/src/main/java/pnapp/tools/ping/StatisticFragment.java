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
    private static final String STR_UNKNOWN = "—";
 	private View mView;

    private ContentLoadingProgressBar mResolveProgressBar;
    private ContentLoadingProgressBar mReverseProgressBar;


    public String mHostName = "";
    public String mHostAddress = "";

    private Pinger mLastPinger;

    private static StatisticFragment mInstance = null;
    public StatisticFragment() { mInstance = this; }
    public static StatisticFragment getInstance() {
        if ( mInstance != null ) return mInstance;
        return new StatisticFragment();
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mView = inflater.inflate(R.layout.statictic_fragment, container, false);

        if ( savedInstanceState != null ) {
            mHostName = savedInstanceState.getString("mHostName");
            mHostAddress = savedInstanceState.getString("mHostAddress");
        }

        if (mHostName != null) setText(R.id.host_name, mHostName);
        if (mHostAddress != null) setText(R.id.ip_address, mHostAddress);
        if (mLastPinger != null) put(mLastPinger);

        mResolveProgressBar = (ContentLoadingProgressBar) mView.findViewById(R.id.progress_resolve);
        mReverseProgressBar = (ContentLoadingProgressBar) mView.findViewById(R.id.progress_reverse);

        mResolveProgressBar.hide();
        mReverseProgressBar.hide();

		return mView;
	}

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("mHostName", mHostName);
        outState.putString("mHostAddress",mHostAddress);
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
        mLastPinger = p;
        if ( mView == null ) return;
        double req = p.getRequests();
        double res = p.getResponses();
        double los = req > 0 ? (req - res) * 100.0 / req : 0;
        if ( res > 0 ) {
            setText(R.id.last_value, "%.3f", p.getLast());
            setText(R.id.count_value, "%.0f / %.0f (%.1f%%)", req, res, los );
            setText(R.id.mean_value, "%.3f \u00b1 %.3f", p.getMean(), p.getMStd());
            setText(R.id.max_value, "%.3f / %.3f", p.getMax(), p.getMin());
        } else {
            setText(R.id.last_value,  "");
            if ( req > 0 ) setText(R.id.count_value, "%.0f / 0 (100%%)", req);
            else         setText(R.id.count_value, "");
            setText(R.id.mean_value,  "");
            setText(R.id.max_value,   "");
        }
	}

	private void setText(int id, String text) {
		TextView view = (TextView) mView.findViewById(id);
		view.setText(text);
	}
	
	private void setText(int id, String format, Object... value ) {
		TextView view = (TextView) mView.findViewById(id);
		view.setText( String.format(Locale.US, format, value) );
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
