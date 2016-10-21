/*
 *  Copyright (c) 2016 P.N.Alekseev <pnaleks@gmail.com>
 */
package pnapp.tools.ping;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;

public class SettingsActivity extends AppCompatActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		FrameLayout frame = new FrameLayout(this);
		frame.setId(R.id.frame);
		setContentView(frame);

		Fragment fragment;
		String task = getIntent().getStringExtra("task");
		if ( task != null && task.equals("man") ) {
			fragment = new ManPageFragment();
			setTitle(R.string.title_man_page);
		} else {
			fragment = new SettingsFragment();
		}

		getFragmentManager().beginTransaction()
			.add(R.id.frame, fragment)
			.commit();
		
		ActionBar actionBar = getSupportActionBar();
		if ( actionBar != null ) actionBar.setDisplayHomeAsUpEnabled(true);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if ( item.getItemId() == android.R.id.home ) {
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public static class ManPageFragment extends Fragment {
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			WebView view = new WebView(getActivity());
			view.loadUrl("file:///android_res/raw/ping.html");
			return view;
		}
	}
}
