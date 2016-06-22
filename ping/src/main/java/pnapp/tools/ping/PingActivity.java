package pnapp.tools.ping;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.LayoutParams;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashSet;

import pnapp.tools.ping.Pinger.OnPingListener;
import pnapp.tools.ping.Resolver.OnResolvedListener;

/**
 * @author P.N. Alekseev
 * @author pnaleks@gmail.com
 */
public class PingActivity extends AppCompatActivity implements
        DrawerFragment.OnDrawerItemSelectedListener,
        CommandEntry.Callback,
        NetworkScanner.OnNetworkNodeFoundListener,
        OnPingListener,
		OnResolvedListener {

    // Имена параметров для сохранения состояния
	public static final String PREF_SELECTED_FRAGMENT = "pref_selected_fragment";
	public static final String PREF_AUTO_COMPLETE_SET = "pref_auto_complete_set";
	public static final String PREF_FAVORITES_SET     = "pref_favorites_set";
	public static final String PREF_INPUT_TEXT        = "pref_input_text";

    // Имена параметров для настройки
    public static final String PREF_LOOK_AROUND       = "pref_look_around";
    public static final String PREF_RESOLVE_ADDRESS   = "pref_resolve_address";
    public static final String PREF_BEEP              = "pref_beep";

    // Имена параметров для опций команды ping
    public static final String PREF_ENABLE_OPTIONS    = "pref_enable_options";
    public static final String PREF_COUNT             = "pref_count";
    public static final String PREF_INTERVAL          = "pref_interval";
    public static final String PREF_TTL               = "pref_ttl";
    public static final String PREF_DEADLINE          = "pref_deadline";
    public static final String PREF_TIMEOUT           = "pref_timeout";
    public static final String PREF_PACKET_SIZE       = "pref_packet_size";

    /** Содержит строки для автоподстановки */
    private HashSet<String> mAutoCompleteSet;
    /** Содержит список избранного */
    private HashSet<String> mFavoritesSet;

    /** Адаптер автоподстановки строки ввода */
    private ArrayAdapter<String> mAutoCompleteAdapter;
    /** Адаптер боковой панели */
    private DrawerAdapter mAdapter;

    private ViewPager mViewPager;
    
    /** Элемент ввода имени или адреса для Action Bar */
    private CommandEntry mEntryView;

	/** Определять имя хоста по адресу */
	private boolean mResolveAddress;

	private String mInputText;
	
	private Resolver mResolver = new Resolver();
	private static Pinger mPinger = new Pinger();
	
	private boolean mCancelled;
	private String mHost;
    private String mOptions = "";

    private MediaPlayer mMediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ping_activity);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        FragmentManager fm = getSupportFragmentManager();
 
        mPinger.setOnPingListener(this);
        mResolver.setOnResolvedListener(this);
        
		mInputText         = sp.getString(PREF_INPUT_TEXT, "");
        int page           = sp.getInt(PREF_SELECTED_FRAGMENT, 0);

		mAutoCompleteSet = (HashSet<String>) sp.getStringSet(PREF_AUTO_COMPLETE_SET, new HashSet<String>() );
        mFavoritesSet = (HashSet<String>) sp.getStringSet(PingActivity.PREF_FAVORITES_SET, new HashSet<String>());

		mAutoCompleteAdapter = new ArrayAdapter<>(this, R.layout.list_item);
		mAutoCompleteAdapter.addAll(mAutoCompleteSet);

        DrawerFragment drawer = (DrawerFragment) fm.findFragmentById(R.id.navigation_drawer);

        // Set up the drawer.
        drawer.setUp( R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout) );
        mAdapter = drawer.getAdapter();

        View v = findViewById(R.id.container);
        if ( v != null ) {
            mViewPager = (ViewPager) v;
            FragmentPagerAdapter adapter = new FragmentPagerAdapter(fm) {
                @Override
                public Fragment getItem(int position) {
                    switch (position) {
                        case 0: return StatisticFragment.getInstance();
                        case 1: return ConsoleFragment.getInstance();
                    }
                    return null;
                }

                @Override
                public int getCount() {
                    return 2;
                }

            };
            mViewPager.setAdapter(adapter);
            mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                @Override
                public void onPageSelected(int position) { updatePageIndicator(position); }
            });
            mViewPager.setCurrentItem(page);
            updatePageIndicator(page);
        } else {
            if ( savedInstanceState == null ) {
                fm.beginTransaction().add(R.id.container1, StatisticFragment.getInstance()).commit();
                fm.beginTransaction().add(R.id.container2, ConsoleFragment.getInstance()).commit();
            }
        }

        if ( !mAdapter.exists(R.string.drawer_group_bookmarks) )
        {
            mAdapter.addGroup(R.string.drawer_group_bookmarks, R.drawable.ic_bookmark_border_white_24dp);
            for (String item : mFavoritesSet)
                mAdapter.addChild(R.string.drawer_group_bookmarks, item);
        }

        if ( sp.getBoolean(PingActivity.PREF_LOOK_AROUND, false)
                && !mAdapter.exists(R.string.drawer_group_neighborhood) )
        {
            updateNeighborhood();
        }

        mEntryView = new CommandEntry(this);
        mEntryView.setAdapter(mAutoCompleteAdapter);
        mEntryView.setBookmarks(mFavoritesSet);
    	mEntryView.setText(mInputText);

        if ( mPinger.isRunning() ) mEntryView.toggleAction();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        mResolveAddress = sp.getBoolean(PREF_RESOLVE_ADDRESS, true);
        mOptions = SettingsFragment.getOptions(sp, mPinger);

        if ( sp.getBoolean(PREF_BEEP,false) ) {
            mMediaPlayer = MediaPlayer.create(this, R.raw.beep07);
        }
        else if ( mMediaPlayer != null ) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    @Override
	protected void onPause() {
		super.onPause();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor e = sp.edit();
		e.putStringSet(PREF_AUTO_COMPLETE_SET, mAutoCompleteSet);
        e.putStringSet(PingActivity.PREF_FAVORITES_SET, mFavoritesSet);
        if ( mViewPager != null ) e.putInt(PREF_SELECTED_FRAGMENT, mViewPager.getCurrentItem());
		e.putString(PREF_INPUT_TEXT, mInputText);
		e.apply();
	}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mResolver.setOnResolvedListener(null);
        mPinger.setOnPingListener(null);
        if ( mMediaPlayer != null ) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if( mViewPager != null ) mViewPager.clearOnPageChangeListeners();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("host", mHost);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        StatisticFragment.getInstance().put(mPinger);
        mHost = savedInstanceState.getString("host");
    }

    @Override
    public void onDrawerItemSelected(DrawerAdapter.Item item) {
		if ( item.mType == DrawerAdapter.TYPE_CHILD ) {
			mEntryView.setText(item.mName);
		}
    }

    public void updatePageIndicator(int position) {
        TextView t = (TextView) findViewById(R.id.page_indicator);
        if ( t != null ) {
            switch (position) {
                case 0:
                    t.setText("\u25cf \u25cb");
                    break;
                case 1:
                    t.setText("\u25cb \u25cf");
                    break;
                default:
                    t.setText("");
                    break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setCustomView(mEntryView, new ActionBar.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        }
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch ( item.getItemId() ) {
            case R.id.action_scan:
                FragmentManager fm = getSupportFragmentManager();
                DrawerFragment drawer = (DrawerFragment) fm.findFragmentById(R.id.navigation_drawer);
                if (drawer != null) updateNeighborhood();
                return true;
            case R.id.action_reset:
                if ( mPinger != null ) mPinger.resetCounters();
                StatisticFragment.getInstance().put(mPinger);
                return true;
            case R.id.action_clear_console:
                ConsoleFragment.getInstance().clear();
                return true;
            case R.id.action_man:
                Intent intent = new Intent(this, SettingsActivity.class);
                intent.putExtra("task","man");
                startActivity(intent);
                return true;
    	    case R.id.action_settings:
                startActivity( new Intent(this, SettingsActivity.class) );
    		    return true;
    	}
    	
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
        if( keyCode == KeyEvent.KEYCODE_BACK )  {
            if ( mPinger.isRunning() ) mPinger.cancel();
            mPinger.resetCounters();
            StatisticFragment.getInstance().clear();
            ConsoleFragment.getInstance().clear();
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
	public void onResolved(Resolver resolver) {
		ConsoleFragment.getInstance().put(resolver);
        StatisticFragment.getInstance().put(resolver);
		if (mCancelled) {
			mEntryView.toggleAction();
		} else {
			mHost = resolver.getHostName();
			if ( mHost != null ) {
				if ( mAutoCompleteSet.add(mHost) ) mAutoCompleteAdapter.add(mHost);
			}
			mHost = resolver.getHostAddress();
			if ( mHost != null ) {
				if ( mAutoCompleteSet.add(mHost) ) mAutoCompleteAdapter.add(mHost);
				mPinger.ping(mOptions ,mHost);
			} else {
                mEntryView.toggleAction();
            }
		}
	}

	@Override
	public void onPing(Pinger pinger) {
        ConsoleFragment.getInstance().put(pinger);
        StatisticFragment.getInstance().put(pinger);
        if( mMediaPlayer != null && pinger.getStatus() == Pinger.STATUS_RESPONSE )
        {
            if ( mMediaPlayer.isPlaying() ) mMediaPlayer.seekTo(0);
            else mMediaPlayer.start();
        }
		if ( !mPinger.isRunning() ) {
            mEntryView.toggleAction();
		}
	}

    @Override
    public void onActionPlay(String host) {
        if ( !mInputText.equals(host)) {
            mPinger.resetCounters();
            StatisticFragment.getInstance().put(mPinger);
        }
        mInputText = host;
        mCancelled = false;
        StatisticFragment fragment = StatisticFragment.getInstance();
        if ( Resolver.isHostAddress(host) ) {
            fragment.setHostName(null);
            fragment.setHostAddress(host);
        } else {
            fragment.setHostName(host);
            fragment.setHostAddress(null);
        }
        mResolver.resolve(host, mResolveAddress);
        mEntryView.toggleAction();
    }

    @Override
    public void onActionStop() {
        mCancelled = true;
        if ( !mPinger.isRunning() && !mResolver.isRunning() ) {
            mEntryView.toggleAction();
        } else {
            mPinger.cancel();
        }
    }

    public void addNeighbor(int ip, String description) {
        if ( ip != 0 ) {
            @SuppressLint("DefaultLocale")
            String string = String.format("%d.%d.%d.%d", ip & 0x0FF, (ip >> 8) & 0x0FF, (ip >> 16) & 0x0FF, (ip >> 24) & 0x0FF);
            onNetworkNodeFound(string,description);
        }
    }
    public void updateNeighborhood() {
        if ( !mAdapter.removeChildren(R.string.drawer_group_neighborhood) ) {
            mAdapter.addGroup(R.string.drawer_group_neighborhood,R.drawable.ic_search_white_24dp);
        }
        WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (manager != null && manager.isWifiEnabled()) {
            DhcpInfo info = manager.getDhcpInfo();
            if ( info != null ) {
                addNeighbor(info.serverAddress,"WiFi Server");
                addNeighbor(info.gateway,      "WiFi Gateway");
                addNeighbor(info.dns1,         "WiFi DNS1");
                addNeighbor(info.dns2,         "WiFi DNS2");
            }
        }
        NetworkScanner.getInstance(this).setOnNetworkNodeFoundListener(this);
        NetworkScanner.getInstance(this).scan();
    }

    @Override
    public void onBookmarkToggle(String text, boolean isBookmark) {

        if (isBookmark) {
            if ( mFavoritesSet.remove(text) ) mAdapter.removeChild(R.string.drawer_group_bookmarks, text);
        } else {
            if ( mFavoritesSet.add(text) ) mAdapter.addChild(R.string.drawer_group_bookmarks, text);
        }
    }

    @Override
    public void onNetworkNodeFound(String host, String description) {
        if ( mAdapter.addChild(R.string.drawer_group_neighborhood, host, description) && !isDrawerOpen() )
            Toast.makeText(getApplicationContext(), getResources().getText(R.string.node_found) + ": " + host, Toast.LENGTH_SHORT).show();
    }

    public boolean isDrawerOpen() {
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        View fragmentContainerView = findViewById(R.id.navigation_drawer);
        return drawerLayout != null && fragmentContainerView != null && drawerLayout.isDrawerOpen(fragmentContainerView);
    }

}
