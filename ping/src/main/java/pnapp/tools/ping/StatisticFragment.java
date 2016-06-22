package pnapp.tools.ping;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Locale;

/**
 * @author P.N.Alekseev
 * @author pnaleks@gmail.com
 */
public class StatisticFragment extends Fragment {
    private static final String STR_UNKNOWN = "—";
	private View mView;

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

    public void setHostName(String name) {
        mHostName = (name == null) ? STR_UNKNOWN : name;
        if ( mView != null ) setText(R.id.host_name, mHostName);
    }

    public void setHostAddress(String address) {
        mHostAddress = (address == null) ? STR_UNKNOWN : address;
        if ( mView != null ) setText(R.id.ip_address, mHostAddress);
    }


	public void put(Resolver r) {
        setHostName(r.getHostName());
        setHostAddress(r.getHostAddress());
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
        setHostName("");
        setHostAddress("");
        setText(R.id.last_value,  "");
        setText(R.id.count_value, "");
        setText(R.id.mean_value,  "");
        setText(R.id.max_value,   "");
    }
}
