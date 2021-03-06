/*
 *  Copyright (c) 2016 P.N.Alekseev <pnaleks@gmail.com>
 */
package pnapp.tools.ping;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceFragment;

import java.util.Locale;

public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
    public static String getOptions(SharedPreferences prefs, Pinger pinger) {
        if ( prefs.getBoolean(PingActivity.PREF_ENABLE_OPTIONS,false) ) {
            pinger.setCount( (int) prefs.getFloat(PingActivity.PREF_COUNT,0F) );
            pinger.setInterval( prefs.getFloat(PingActivity.PREF_INTERVAL,0F) );
            float f;
            String ops = "";
            if ( (f = prefs.getFloat(PingActivity.PREF_TTL,0F)     ) > 0 ) ops += String.format(Locale.US, "-t %.0f ", f);
            if ( (f = prefs.getFloat(PingActivity.PREF_DEADLINE,0F)) > 0 ) ops += String.format(Locale.US, "-w %.0f ", f);
            if ( (f = prefs.getFloat(PingActivity.PREF_TIMEOUT,0F) ) > 0 ) ops += String.format(Locale.US, "-W %.0f ", f);
            if ( (f = prefs.getFloat(PingActivity.PREF_PACKET_SIZE,-1F) ) >= 0 ) ops += String.format(Locale.US, "-s %.0f ", f);
            if ( ops.isEmpty() ) return null;
            return ops.trim();
        }
        pinger.setCount(0);
        pinger.setInterval(0);
        return null;
    }

    public static String getOptions(SharedPreferences prefs) {
        if ( prefs.getBoolean(PingActivity.PREF_ENABLE_OPTIONS,false) ) {
            String ops = "";
            float f;
            if ( (f = prefs.getFloat(PingActivity.PREF_COUNT,0F)   ) > 0 ) ops += String.format(Locale.US, "-c %.0f ", f);
            if ( (f = prefs.getFloat(PingActivity.PREF_INTERVAL,0F)) > 0 ) ops += String.format(Locale.US, "-i %.1f ", f);
            if ( (f = prefs.getFloat(PingActivity.PREF_TTL,0F)     ) > 0 ) ops += String.format(Locale.US, "-t %.0f ", f);
            if ( (f = prefs.getFloat(PingActivity.PREF_DEADLINE,0F)) > 0 ) ops += String.format(Locale.US, "-w %.0f ", f);
            if ( (f = prefs.getFloat(PingActivity.PREF_TIMEOUT,0F) ) > 0 ) ops += String.format(Locale.US, "-W %.0f ", f);
            if ( (f = prefs.getFloat(PingActivity.PREF_PACKET_SIZE,-1F) ) >= 0 ) ops += String.format(Locale.US, "-s %.0f ", f);
            if ( ops.isEmpty() ) return null;
            return ops.trim();
        }
        return null;
    }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
        if( !((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).hasVibrator() ) {
            findPreference(PingActivity.PREF_VIBRATE).setEnabled(false);
        }
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (PingActivity.PREF_APP_THEME.equals(key)) {
            Intent intent = new Intent(getActivity(), PingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return;
        }
        if ( prefs.getBoolean(PingActivity.PREF_ENABLE_OPTIONS,false) ) {
            String summary = getOptions(prefs);
            if (summary != null) {
                ((CheckBoxPreference) findPreference(PingActivity.PREF_ENABLE_OPTIONS))
                        .setSummaryOn(summary);
            } else {
                ((CheckBoxPreference) findPreference(PingActivity.PREF_ENABLE_OPTIONS))
                        .setSummaryOn(R.string.pref_enable_options_summary_on);
            }
        }
	}
	
	@Override
	public void onResume() {
		super.onResume();
		SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
        onSharedPreferenceChanged(prefs, "");
		prefs.registerOnSharedPreferenceChangeListener(this);

        findPreference(PingActivity.PREF_APP_THEME).setSummary(
                getResources().getStringArray(R.array.pref_app_theme_entries)[
                        Integer.valueOf(prefs.getString(PingActivity.PREF_APP_THEME,"0")) ]
        );
	}
	
	@Override
	public void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}
}
