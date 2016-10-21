/*
 *  Copyright (c) 2016 P.N.Alekseev <pnaleks@gmail.com>
 */
package pnapp.tools.ping;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

import android.os.AsyncTask;

/**
 * Асинхронное определение имени и адреса интернет соединений
 */
class Resolver {
	private static final Pattern patHostAddress = Pattern.compile("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
	
	/** Задача еще не выполнялась */
	static final int STATUS_PENDING  = 0;
	/** Задача активна */
	static final int STATUS_RUNNING  = 1;
	/** Задача завершена, hostName определить не удалось */
	static final int STATUS_UNKNOWN  = 2;
	/** Задача успешно завершена */
	static final int STATUS_FINISHED = 3;

	/** Адрес */
	private InetAddress mAddress;
	
	/** Объект с интерфейсом обработчика */
	private OnResolvedListener mListener;
	
	/** Объект асинхронного исполнения */
	private AsyncResolver mResolver;
	
	/** Текущий статус выполнения команды */
	private int mStatus = STATUS_PENDING;
	
	private String mErrString;
	private String mSrcString;

    private boolean mReverse;

	/** Интерфейс для обработки результатов команды */
	interface OnResolvedListener { void onResolved(Resolver resolver); }
	
	/**
	 * Установка обработчика
	 * @param listener - новый обработчик
	 */
	void setOnResolvedListener( OnResolvedListener listener ) {
		mListener = listener;
	}

	/**
	 * Вызов асинхронного определения имени и адреса
	 * @param host - имя или адрес
     * @param reverse - если истина, IP-дрес будет использован для определения имени
	 * @return Возвращает ложь в случае если предыдущая операция не завершена и истину при успешном запуске процедуры
	 */
	boolean resolve(String host, boolean reverse) {
		if ( mResolver != null && mResolver.getStatus() == AsyncTask.Status.RUNNING ) {
			return false;
		}
		mSrcString = host;
		mStatus = STATUS_PENDING;
        mReverse = reverse;
		mResolver = new AsyncResolver();
		mResolver.execute(host);
		return true;
	}

	/**
	 * Получение текущего статуса задачи
	 * @return Одно из значений 
	 * <ul>
	 * <li>{@link #STATUS_PENDING}</li>
	 * <li>{@link #STATUS_RUNNING}</li>
	 * <li>{@link #STATUS_UNKNOWN}</li>
	 * <li>{@link #STATUS_FINISHED}</li>
	 * </ul>
	 */
	int getStatus() {
		return mStatus;
	}
	
	boolean isRunning() {
		return mStatus == STATUS_RUNNING;
	}
	
	String getErrString() {
		return mErrString;
	}
	
	/**
	 * @return Возвращает имя или null если операция не закончена или если имя компьютера определить не удалось.
     * Используйте {@link #getStatus()} чтобы определить причину
	 */
	String getHostName() {
        if ( mStatus != STATUS_FINISHED ) return null;
        if ( !mReverse ) {
            if ( isHostAddress(mSrcString) ) return null;
            return mSrcString;
        }
		return mAddress.getHostName();
	}
	
	/**
	 * @return Возвращает адрес или null если операция не закончена или если имя компьютера определить не удалось.
     * Используйте {@link #getStatus()} чтобы определить причину
	 */
	String getHostAddress() {
		if ( mStatus == STATUS_FINISHED ) return mAddress.getHostAddress();
		if ( mStatus == STATUS_UNKNOWN && isHostAddress(mSrcString) ) return mSrcString;
		return null;
	}
	
	/**
	 * @return Возвращает {@link InetAddress} или null если операция не закончена или если имя компьютера определить не удалось.
     * Используйте {@link #getStatus()} чтобы определить причину
	 */
	InetAddress getInetAddress() {
		return mStatus == STATUS_FINISHED ? mAddress : null;
	}
	
	/**
	 * Проверяет, что строка содержит корректный IP-адрес
	 * @param host - строка для анализа
	 * @return Истину, если строка содержит корректный IP-адрес
	 */
	static boolean isHostAddress(String host) {
		return patHostAddress.matcher(host).matches();
	}
	
	/**
	 * Асинхронное выполнение команды
	 * @see AsyncTask
	 */
	private class AsyncResolver extends AsyncTask<String, Void, InetAddress> {
		@Override
		protected void onPreExecute() {
			mStatus = STATUS_RUNNING;
		}
		
		@Override
		protected InetAddress doInBackground(String... host) {
			synchronized (this) {
				try {
					wait(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			InetAddress address = null;
			try {
				address = InetAddress.getByName(host[0]);
				if ( mReverse ) {
                    //noinspection ResultOfMethodCallIgnored
                    address.getHostName();
                }
			} catch (UnknownHostException e) {
				mErrString = e.getLocalizedMessage();
			}
			return address;
		}
		
		@Override
		protected void onPostExecute(InetAddress address) {
			mAddress = address;
			mStatus = address == null ? STATUS_UNKNOWN : STATUS_FINISHED;
			if ( mListener != null ) mListener.onResolved( Resolver.this );
		}
	}
}
