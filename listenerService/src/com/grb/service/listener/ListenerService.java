package com.grb.service.listener;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.grb.util.logging.LoggingContext;


public class ListenerService<L,E> implements LoggingContext {
    private static final Log Trace = LogFactory.getLog(ListenerService.class);

	protected ArrayList<L> mListeners;
	protected Object mLock;
	protected ListenerCallback<L,E> mListenerCallback;
    protected VetoableListenerCallback<L,E> mVetoableListenerCallback;
	protected ExecutorService mExecutor;
    protected LoggingContext mLoggingCtx;

	public ListenerService(ListenerCallback<L,E> callback) {
		mListeners = null;
		mLock = new Object();
		mListenerCallback = callback;
		mVetoableListenerCallback = null;
		mExecutor = null;
		mLoggingCtx = this;
	}

	public ListenerService(ListenerCallback<L,E> callback, ExecutorService executor) {
		mListeners = null;
		mLock = new Object();
		mListenerCallback = callback;
        mVetoableListenerCallback = null;
		mExecutor = executor;
		mLoggingCtx = this;
	}

    public ListenerService(VetoableListenerCallback<L,E> callback) {
        mListeners = null;
        mLock = new Object();
        mListenerCallback = null;
        mVetoableListenerCallback = callback;
        mExecutor = null;
        mLoggingCtx = this;
    }

    /**
     * Sets the logging context to be used in log messages.
     * 
     * @param loggingCtx Logging context to be used.
     */
    public void setLoggingContext(LoggingContext loggingCtx) {
        mLoggingCtx = loggingCtx;
    }

	public void addListener(L listener) {
		synchronized(mLock) {
			if (mListeners == null) {
				mListeners = new ArrayList<L>();
			}				
			mListeners.add(listener);
		}
	}
	
	public void removeListener(L listener) {
		synchronized(mLock) {
			if (mListeners != null) {
				mListeners.remove(listener);
			}
		}
	}

	public void notifyListeners(final E event) {
		notifyListeners(event, null);
	}
	
	@SuppressWarnings("unchecked")
	public void notifyListeners(final E event, final Object userData) {
		final ArrayList<L> clone;
		synchronized(mLock) {
			if (mListeners != null) {
				clone = (ArrayList<L>)mListeners.clone();
			} else {
				return;
			}
		}
		if (mExecutor == null) {
			for(int i = 0; i < clone.size(); i++) {
			    try {
	                mListenerCallback.onListenerCallback(clone.get(i), event, userData);
			    } catch(Throwable t) {
                    if (mLoggingCtx.getLog().isErrorEnabled()) {
                        mLoggingCtx.getLog().error(mLoggingCtx.formatLog("Event: " + event + " threw unexpectedly by listener: " + clone.get(i)), t);
                    }
			    }
			}
		} else {
			mExecutor.execute(new Runnable() {
				public void run() {
					for(int i = 0; i < clone.size(); i++) {
					    try {
	                        mListenerCallback.onListenerCallback(clone.get(i), event, userData);
					    } catch(Throwable t) {
		                    if (mLoggingCtx.getLog().isErrorEnabled()) {
		                        mLoggingCtx.getLog().error(mLoggingCtx.formatLog("Event: " + event + " threw unexpectedly by listener: " + clone.get(i)), t);
		                    }
					    }
					}
				}
			});
		}
	}

    public void notifyVetoableListeners(final E event) throws ListenerVetoException {
        notifyVetoableListeners(event, null);
    }

    @SuppressWarnings("unchecked")
    public void notifyVetoableListeners(final E event, final Object userData) throws ListenerVetoException {
        final ArrayList<L> clone;
        synchronized(mLock) {
            if (mListeners != null) {
                clone = (ArrayList<L>)mListeners.clone();
            } else {
                return;
            }
        }
        for(int i = 0; i < clone.size(); i++) {
            try {
                mVetoableListenerCallback.onVetoableListenerCallback(clone.get(i), event, userData);
            } catch(ListenerVetoException e) {
                if (mLoggingCtx.getLog().isDebugEnabled()) {
                    mLoggingCtx.getLog().debug(mLoggingCtx.formatLog("Event: " + event + " vetoed by listener: " + clone.get(i)), e);
                }
                throw e;
            } catch(Throwable t) {
                if (mLoggingCtx.getLog().isErrorEnabled()) {
                    mLoggingCtx.getLog().error(mLoggingCtx.formatLog("Event: " + event + " threw unexpectedly by listener: " + clone.get(i)), t);
                }
            }
        }
    }

    public String formatLog(String msg) {
        return msg;
    }

    public Log getLog() {
        return Trace;
    }
}
