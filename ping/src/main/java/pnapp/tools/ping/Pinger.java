/*
 *  Copyright (c) 2016 P.N.Alekseev <pnaleks@gmail.com>
 */
package pnapp.tools.ping;

import android.os.Handler;
import android.os.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 
 * Асинхронный вызов системной функции ping и работа с ней используя {@link ScheduledThreadPoolExecutor}
 */
class Pinger {
	/** Шаблон для анализа удачного ping */
	static final Pattern p = Pattern.compile("(\\d+) bytes .*: icmp_seq=(\\d+) ttl=(\\d+) time=(\\d*.?\\d+) ms");

    /** Задача не активна и может быть запущена */
    static final int STATUS_READY = 0;
    /** Задача активна, данные доступны
    @Deprecated
    public static final int STATUS_SUCCESS = 1;
     */
	/** Задача активна, при выполнении произошла ошибка, информация об ошибке в {@link #getData()} */
	static final int STATUS_ERROR = 2;
    /** Задача активна, получен отклик, данные обновлены */
    static final int STATUS_RESPONSE = 3;
    /** Задача активна, получены данные (не отклик) */
    static final int STATUS_INFO = 4;

    private static final long DEFAULT_TIMER_PERIOD = 1000;
    private static final long MINIMAL_TIMER_PERIOD = 200;

    /* Сообщения от вспомогательных потоков */
    private static final int MESSAGE_ERROR   = 0; // информация об ошибке от команды ping
    private static final int MESSAGE_SUCCESS = 1; // успешный результат команды ping
    private static final int MESSAGE_DONE    = 2; // команда ping завершена или должна быть завершена
    private static final int MESSAGE_TIMER   = 3; // событие таймера
	
	/** Объект с интерфейсом обработчика */
	private OnPingListener mListener;
	
	/** Текущий статус выполнения команды */
	private int mStatus = STATUS_READY;

	/** Текущая строка вывода команды ping */
	private String mData = "";


    /** Счетчик отправленных пакетов, вычисляется по значению icmp_seq */
    private double mRequests = 0;
    /** Число срабатываний таймера с периодом -i, но не более -c */
    private double mTicks = 0;
    /** Счетчик срабатываний таймера после последнего отклика */
    private double mTimeout;
    /** Счетчик отправленных запросов в предыдущих вызовах */
    private double mPriorRequests;
	/** Счетчик принятых пакетов */
	private double mResponses = 0;
	/** Задержка последнего запроса */
	private double mLast = 0;
	/** Суммарная задержка, для вычисления среднего */
	private double mSum = 0;
	/** Сумма квадратов задержек, для вычисления среднеквадратичного откланения */
	private double mSumOfSquares = 0;
	/** Максиматьная задержка */
	private double mMax = 0;
	/** Минимальная задержка */
	private double mMin = Double.MAX_VALUE;
    /** Значение опции -c */
    private int mCount = 0;
    /** Значение опции -i */
    private long mInterval = DEFAULT_TIMER_PERIOD;

    private Handler.Callback mHandlerCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch( msg.what )
            {
            case MESSAGE_DONE:
                if ( mPingProcess == null ) return true;
                mPingProcess.destroy();
                mPingProcess = null;
                mScheduledThreadPoolExecutor.shutdownNow();
                mScheduledThreadPoolExecutor = null;
                if ( mTimeout > 0 ) mTimeout--;
                mData = "";
                mStatus = STATUS_READY;
                break;
            case MESSAGE_TIMER:
                if ( mCount == 0 || mTicks < mCount ) {
                    mTicks++;
                    mTimeout++;
                }
                return true;
            case MESSAGE_SUCCESS:
                mData = (String) msg.obj;
                Matcher m = p.matcher(mData);
                if (m.matches()) {
                    mResponses++;
                    mLast = Double.valueOf( m.group(4) );
                    mSum += mLast;
                    mSumOfSquares += mLast*mLast;
                    if ( mMax < mLast ) mMax = mLast;
                    if ( mMin > mLast ) mMin = mLast;

                    int icmp_seq = Integer.valueOf( m.group(2) );
                    if ( mRequests < icmp_seq ) mRequests = icmp_seq;

                    mTimeout = 0;
                    mStatus = STATUS_RESPONSE;
                } else {
                    mStatus = STATUS_INFO;
                }
                break;
            case MESSAGE_ERROR:
                mData = (String) msg.obj;
                mStatus = STATUS_ERROR;
                break;
            default:
                return false;
            }
            if( mListener != null ) mListener.onPing( Pinger.this );
            return true;
        }
    };

    /** Обработчик событий в UI-thread */
    private Handler mHandler = new Handler(mHandlerCallback);

    /** Исполняемый объект для вызова пользовательской функции из обработчика */
    private Process mPingProcess;

    private Runnable mErrorReader = new Runnable() {
        @Override
        public void run() {
            if ( mPingProcess == null ) return;
            BufferedReader buf = new BufferedReader(new InputStreamReader( mPingProcess.getErrorStream() ));
            String str;
            try {
                while ( (str = buf.readLine()) != null ) {
                    mHandler.sendMessage( mHandler.obtainMessage(MESSAGE_ERROR,str) );
                }
                buf.close();

            } catch (IOException e) {
                mHandler.sendMessage( mHandler.obtainMessage(MESSAGE_ERROR,e.getLocalizedMessage()) );
            }
        }
    };

    private Runnable mInputReader = new Runnable() {
        @Override
        public void run() {
            if ( mPingProcess == null ) return;
            BufferedReader buf = new BufferedReader(new InputStreamReader( mPingProcess.getInputStream() ));
            String str;
            try {
                while ( (str = buf.readLine()) != null ) {
                    mHandler.sendMessage( mHandler.obtainMessage(MESSAGE_SUCCESS,str) );
                }
                buf.close();
                mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_DONE, null));
            } catch (IOException e) {
                mHandler.sendMessage( mHandler.obtainMessage(MESSAGE_ERROR,e.getLocalizedMessage()) );
            }
        }
    };

    private Runnable mTimer = new Runnable() {
        @Override
        public void run() {
            if ( mPingProcess == null ) return;
            mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_TIMER, null));
        }
    };

    private ScheduledThreadPoolExecutor mScheduledThreadPoolExecutor;

	/** Интерфейс для обработки результатов команды */
	interface OnPingListener { void onPing(Pinger pinger); }
	
	/**
	 * Установка обработчика
	 * @param listener - новый обработчик
	 */
	void setOnPingListener( Pinger.OnPingListener listener ) {
		mListener = listener;
	}
	
	/** 
	 * Функция для запуска асинхронного ping
	 * @param args - опции вызова команды ping, может быть null
	 * @return Возвращает false если ping уже запущен или если IOException
	 */
	public boolean ping(String... args) {
		if ( mPingProcess != null ) return false;
        mStatus = STATUS_INFO;
        String commandString = "/system/bin/ping";

        if ( mInterval != 0 ) commandString += String.format(Locale.US," -i %.1f", (double)mInterval/1000.0 );
        if ( mCount != 0 ) commandString += " -c " + String.valueOf(mCount);

        for ( String arg : args) {
            if ( arg != null ) commandString += " " + arg;
        }

        mPriorRequests = getRequests();
        mTimeout = 0;
        mTicks = 0;
        mRequests = 0;

        try {
            mPingProcess = Runtime.getRuntime().exec( commandString );
            mData = commandString;
            (new Thread( mErrorReader )).start();
            (new Thread( mInputReader )).start();
            mScheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
            mScheduledThreadPoolExecutor.scheduleAtFixedRate(
                    mTimer,
                    0,
                    mInterval == 0 ? DEFAULT_TIMER_PERIOD : mInterval,
                    TimeUnit.MILLISECONDS);
        } catch (IOException e) {
            mData = e.getLocalizedMessage();
            mStatus = STATUS_ERROR;
            mPingProcess = null;
        }

        if( mListener != null ) mListener.onPing( Pinger.this );

        if ( mStatus == STATUS_ERROR ) {
            mStatus = STATUS_READY;
            mData = "";
            if (mListener != null) mListener.onPing(Pinger.this);
            return false;
        }

        return true;
	}
	
	/**
	 * Досрочное завершение команды ping по запросу пользователя
	 * @return Возвращает истину, если задача активна и запрос на отмену отправлен
	 */
	boolean cancel() {
        if ( mPingProcess == null ) return false;
        mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_DONE, null));
        return true;
	}

    /**
     * Установка значения параметра -с команды ping
     * @param c - значение параметра -c
     */
    void setCount(int c)
    {
        mCount = c;
    }

    /**
     * Установка значения параметра -i команды ping
     * @param i - значение параметра -i
     */
    void setInterval(float i)
    {
        long l = (long) (i * 1000F);
        if ( l > 0 && l < MINIMAL_TIMER_PERIOD ) mInterval = MINIMAL_TIMER_PERIOD;
        else mInterval = l;
    }


	/**
	 * Получение текущего статуса задачи
	 * @return текущий статус
	 */
	int getStatus() {
		return mStatus;
	}
	
	boolean isRunning() {
		return mStatus != STATUS_READY;
	}
	
	/**
	 * @return Последняя строка данных вывода команды ping
	 */
	String getData() {
		return mData;
	}

    /** @return Общее число запросов */
    double getRequests() { return mPriorRequests + mRequests + mTimeout; }
	/** @return Общее число откликов */
	double getResponses() { return mResponses; }
	/** @return Задержка отклика на последний запрос */
	double getLast()  { return mLast; }
	/** @return Средняя задержка отклика за {@link #getResponses()} запросов */
	double getMean()  { return mResponses > 0 ? mSum/mResponses : 0; }
	/** @return Среднеквадратичное отклонение задержки отклика за {@link #getResponses()} запросов */
	double getMStd()  { return mResponses > 0 ? Math.sqrt( mSumOfSquares/mResponses - mSum/mResponses*mSum/mResponses ) : 0; }
	/** @return Максимальная задержка отклика за {@link #getResponses()} запросов */
	double getMax()   { return mMax; }
	/** @return Минимальная задержка отклика за {@link #getResponses()} запросов */
	double getMin()   { return mMin == Double.MAX_VALUE ? 0 : mMin; }

    void resetCounters() {
        cancel();
        mTicks = mTimeout = 0.0;
        mRequests = mPriorRequests = mResponses = 0.0;
        mLast = mSum = mSumOfSquares = 0.0;
        mMax = 0.0;
        mMin = Double.MAX_VALUE;
    }

    /*
    public void store(Bundle outState) {
        double[] dat = {mRequests, mResponses, mLast, mSum, mSumOfSquares, mMax, mMin};
        outState.putDoubleArray("Pinger",dat);
    }

    public void restore(Bundle savedInstanceState) {
        double[] dat = savedInstanceState.getDoubleArray("Pinger");
        mRequests     = dat[0];
        mResponses    = dat[1];
        mLast         = dat[2];
        mSum          = dat[3];
        mSumOfSquares = dat[4];
        mMax          = dat[5];
        mMin          = dat[6];
    }
    */
}
