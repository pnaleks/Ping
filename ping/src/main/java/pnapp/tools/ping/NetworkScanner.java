/*
 *  Copyright (c) 2016 P.N.Alekseev <pnaleks@gmail.com>
 */
package pnapp.tools.ping;

import android.content.Context;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pnapp.tools.ping.Pinger.OnPingListener;

class NetworkScanner implements OnPingListener {
	/** Шаблон строки команды ping с просроченным TL */
	private static final Pattern pTtlExceeded = Pattern.compile("From (\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\D.*Time to live exceeded");
	
	/** "Глубина" сканирования по TTL, самый дальний шлюз */
	private static final int MAX_TTL = 10;
	
	private Pinger mPinger;
	
    private boolean mReady;
	
	private int mTTL;

    /** Контекст для доступа к ресурсам */
    private Context mContext;

    private static NetworkScanner mInstance;

    static NetworkScanner getInstance(Context context) {
        if ( mInstance == null ) mInstance = new NetworkScanner();
        mInstance.mContext = context;
        return mInstance;
    }

	private NetworkScanner() {
        mPinger = new Pinger();
        mPinger.setOnPingListener(this);
        mReady = true;
	}
	
	boolean scan() {
        if ( !mReady ) return false;
        mTTL = 1;
        mReady = false;
        onPing(mPinger);
        return true;
    }
	
	@Override
	public void onPing(Pinger pinger) {
		Matcher m = pTtlExceeded.matcher(pinger.getData());

        if ( pinger.getStatus() == Pinger.STATUS_READY ) {
			if ( mTTL < MAX_TTL ) {
                mPinger.ping("-c 1 -W 1 -t", Integer.toString(mTTL), "8.8.8.8");
                mTTL++;
            } else {
                mReady = true;
            }
            return;
		}

        if ( m.matches() ) {
			Integer[] b = {
                    Integer.valueOf(m.group(1)), Integer.valueOf(m.group(2)),
                    Integer.valueOf(m.group(3)), Integer.valueOf(m.group(4)) };
            boolean local =
                       (b[0] == 192 && b[1] == 168)
                    || (b[0] == 172 && b[1] >=  16 && b[1] < 32)
                    || (b[0] ==  10);
            String description;
            if ( local ) {
                description = mContext.getString(R.string.format_gateway_local, mTTL - 1);
            } else {
                description = mContext.getString(R.string.format_gateway_internet, mTTL - 1);
                mTTL = MAX_TTL;
            }
            if ( mOnNetworkNodeFoundListener != null)
                mOnNetworkNodeFoundListener.onNetworkNodeFound( String.format(Locale.US,"%d.%d.%d.%d",(Object[])b), description);
		}
	}

	/** Интерфейс для обработчика найденных сетевых узлов */
	public interface OnNetworkNodeFoundListener {
		void onNetworkNodeFound(String host, String description);
	}
	private OnNetworkNodeFoundListener mOnNetworkNodeFoundListener;
	public void setOnNetworkNodeFoundListener ( OnNetworkNodeFoundListener listener ) {
		mOnNetworkNodeFoundListener = listener;
	}
}

