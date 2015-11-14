package com.grb.service.timer.intervalTimer;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.grb.util.logging.LoggingContext;


public class IntervalTimer implements LoggingContext 
{
    private static final Log Trace = LogFactory.getLog(IntervalTimer.class);

    /**
	 * System Timer To Use 
	 */
	static public Timer TimerToUse = new Timer("IntervalTimer", true);
		
	static protected HashMap<Integer, IntervalTimer> IntervalTimerMap = new HashMap<Integer, IntervalTimer>();
	
	static public IntervalTimer GetIntervalTimer(int intervalInMS) {
		synchronized(IntervalTimerMap) {
			IntervalTimer intervalTimer = IntervalTimerMap.get(intervalInMS);
			if (intervalTimer == null) {
				intervalTimer = new IntervalTimer(TimerToUse, intervalInMS);
				IntervalTimerMap.put(intervalInMS, intervalTimer);
			}
			return intervalTimer;
		}
	}
	
	static public IntervalTimer GetIntervalTimer(int timeout, TimeUnit unit) {
	    int intervalInMS;
	    if (unit.equals(TimeUnit.MILLISECONDS)) {
	        intervalInMS = timeout;
	    } else if (unit.equals(TimeUnit.SECONDS)) {
	        intervalInMS = timeout * 1000;
	    } else if (unit.equals(TimeUnit.MINUTES)) {
	        intervalInMS = timeout * 1000 * 60;
	    } else if (unit.equals(TimeUnit.HOURS)) {
            intervalInMS = timeout * 1000 * 60 * 60;
        } else if (unit.equals(TimeUnit.DAYS)) {
            intervalInMS = timeout * 1000 * 60 * 60 * 24;
	    } else {
	        throw new IllegalArgumentException("Invalid time unit for IntervalTimer: " + unit);
	    }
	    return GetIntervalTimer(intervalInMS);
	}
	
	protected enum TimerIntervalTaskState {
		Queued,
		Scheduled,
		Running,
		Cancelled
	}
	
	protected class TimerIntervalTask extends TimerTask {
		private IntervalTimer mIntervalTimer;
		private TimerIntervalTaskState mState;
		private IntervalTimerTimeoutListener mListener;
		private Date mDate;
		private boolean mRecur;
		
		public TimerIntervalTask(IntervalTimer intervalTimer, 
				IntervalTimerTimeoutListener listener, Date date, boolean recur) {
			mIntervalTimer = intervalTimer;
			mListener = listener;
			mDate = date;
			setState(TimerIntervalTaskState.Queued);
		}

		public IntervalTimerTimeoutListener getListener() {
			return mListener;
		}
					
		public Date getDate() {
			return mDate;
		}

		public boolean recurs() {
			return mRecur;
		}
		
		public TimerIntervalTaskState getState() {
			return mState;
		}

		synchronized public void setState(TimerIntervalTaskState newState) {
            if (mLoggingCtx.getLog().isDebugEnabled()) {
                mLoggingCtx.getLog().debug(mLoggingCtx.formatLog(mListener + " state change to: " + newState));
            }
			mState = newState;
		}
		
		@Override
		public void run() {
			mIntervalTimer.onTimeout(this);
		}

		@Override
		public String toString() {
			return mListener.toString() + " ( recur=" + mRecur + ",state=" + mState + ")";
		}
	}
		
	protected Timer mTimer;
	protected int mIntervalInMS;
	protected LinkedList<TimerIntervalTask> mTimerQueue; 
	protected Object mLock;
    protected LoggingContext mLoggingCtx;

	public IntervalTimer(Timer timer, int intervalInMS) {
		mTimer = timer;
		mIntervalInMS = intervalInMS;
		mTimerQueue = new LinkedList<TimerIntervalTask>();
		mLock = new Object();
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

	public int getIntervalInMS() {
		return mIntervalInMS;
	}
	
	public void schedule(IntervalTimerTimeoutListener listener) {
		schedule(listener, false);
	}

	public void schedule(IntervalTimerTimeoutListener listener, boolean recur) {
		synchronized(mLock) {
			Date date = new Date(System.currentTimeMillis() + mIntervalInMS);
			TimerIntervalTask task = new TimerIntervalTask(this, listener, date, recur);
			mTimerQueue.add(task);
			if (mTimerQueue.size() == 1) {
				task.setState(TimerIntervalTaskState.Scheduled);
				mTimer.schedule(task, date);
			}
		}
	}
	
	public boolean cancel(IntervalTimerTimeoutListener listener) {
		synchronized(mLock) {
			for (int i = 0; i < mTimerQueue.size(); i++) {
				TimerIntervalTask task = mTimerQueue.get(i);
				if (task.getListener().equals(listener)) {
					mTimerQueue.remove(i);
					task.setState(TimerIntervalTaskState.Cancelled);
					return true;
				}
			}
			return false;
		}
	}
	
	public void purge() {
		synchronized(mLock) {
			mTimerQueue.clear();
		}
	}
	
	protected void onTimeout(TimerIntervalTask timerTask) {
		boolean execute = false;
		synchronized(mLock) {
			if (!timerTask.getState().equals(TimerIntervalTaskState.Cancelled)) {
				// if the task wasn't cancelled it should be the first in the list
			    if ((mTimerQueue.size() > 0) && (mTimerQueue.get(0) == timerTask)) {
	            	timerTask.setState(TimerIntervalTaskState.Running);
	            	mTimerQueue.removeFirst();
	            	execute = true;
	            	
	            	// handle recurring task
	            	if (timerTask.recurs()) {
	        			Date date = new Date(System.currentTimeMillis() + mIntervalInMS);
	    				TimerIntervalTask task = new TimerIntervalTask(this, timerTask.getListener(), date, true);
	    				mTimerQueue.add(task);
	            	}	            	
			    } else {
		            if (mLoggingCtx.getLog().isErrorEnabled()) {
		                mLoggingCtx.getLog().error(mLoggingCtx.formatLog("Timer Task (" + timerTask + ") not found in queue"));
		            } 
			    }
			}
            // check for a waiting task and schedule
            if (mTimerQueue.size() > 0) {
            	TimerIntervalTask nextTask = mTimerQueue.get(0);
            	nextTask.setState(TimerIntervalTaskState.Scheduled);
                mTimer.schedule(nextTask, nextTask.getDate());
            }
		}
		if (execute) {
			timerTask.getListener().onIntervalTimeout();
		}
	}

	@Override
	public String formatLog(String msg) {
		return msg;
	}

	@Override
	public Log getLog() {
		return Trace;
	}
}
