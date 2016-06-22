package pnapp.tools.ping;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * @author P.N. Alekseev
 * @author pnaleks@gmail.com
 */
public class ConsoleFragment extends Fragment {
    public static final String PREF_BUFFER_SIZE = "pref_buffer_size";
    public static final String PREF_VERBOSE     = "pref_verbose_output";
    public static final int DEFAULT_BUFFER_SIZE = 100;
	/** Список строк */
	private ListView mListView;
	/** Адаптер консоли (списка строк) */
    private ArrayAdapter<String> mAdapter;
    /** Размер буфера консоли */
    private int mBufferSize;
    /** Буфер консоли */
    public ArrayList<String> mBuffer = new ArrayList<>();
    /** Флаг включения подробного вывода */
    protected boolean mVerbose;

    private static ConsoleFragment mInstance;
    public ConsoleFragment() { mInstance = this; }
    public static ConsoleFragment getInstance() {
        if ( mInstance != null ) return mInstance;
        return new ConsoleFragment();
    }


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.console_fragment, container, false);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mBufferSize = (int) sp.getFloat(PREF_BUFFER_SIZE, DEFAULT_BUFFER_SIZE);
        mVerbose    = sp.getBoolean(PREF_VERBOSE, true);

        mAdapter = new ArrayAdapter<>( getActivity(), R.layout.console_list_item);
		mListView = (ListView) v.findViewById(R.id.console);
		mListView.setAdapter( mAdapter );

        if( savedInstanceState != null ) mBuffer = savedInstanceState.getStringArrayList("mBuffer");
        if( mBuffer != null ) {
            while (mBuffer.size() > mBufferSize) mBuffer.remove(0);
            mAdapter.addAll( mBuffer );
        }

		return v;
	}

    @Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
        outState.putStringArrayList("mBuffer", mBuffer);
	}

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mVerbose    = sp.getBoolean(PREF_VERBOSE, true);
        mBufferSize = (int) sp.getFloat(PREF_BUFFER_SIZE, DEFAULT_BUFFER_SIZE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mListView = null;
    }

	public void put(String s) {
        mBuffer.add(s);
        while ( mBuffer.size() > mBufferSize ) mBuffer.remove(0);

        if ( mListView != null ) {
            mAdapter.add(s);
            while ( mAdapter.getCount() > mBufferSize ) mAdapter.remove(mAdapter.getItem(0));
            mListView.setSelection( mListView.getCount() - 1 );
        }
    }

    public void put(Resolver r) {
        switch (r.getStatus()) {
            case Resolver.STATUS_UNKNOWN:  put(r.getErrString()); break;
            case Resolver.STATUS_PENDING:  if (mVerbose) put("resolver ready"); break;
            case Resolver.STATUS_RUNNING:  if (mVerbose) put("resolver busy"); break;
            case Resolver.STATUS_FINISHED: if (mVerbose)  put("resolved " + r.getInetAddress().toString()); break;
            default:                       put("unspecified resolver status " + String.valueOf(r.getStatus())); break;
        }
    }
    public void put(Pinger p)   {
        switch ( p.getStatus() ) {
            case Pinger.STATUS_ERROR:    put( p.getData() ); break;
            case Pinger.STATUS_RESPONSE: put( p.getData() ); break;
            case Pinger.STATUS_READY:    if ( mVerbose ) put("ping ready"); break;
            case Pinger.STATUS_INFO:     if ( mVerbose ) put( p.getData() ); break;
            default:                     put( "unexpected ping status " + String.valueOf(p.getStatus()) ); break;
        }
    }

    public void clear() {
        mBuffer.clear();
        mAdapter.clear();
    }
}
