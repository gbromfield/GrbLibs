package com.grb.service.timer.intervalTimer.test;

import java.util.concurrent.TimeUnit;

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;

import com.grb.service.timer.intervalTimer.IntervalTimer;
import com.grb.service.timer.intervalTimer.IntervalTimerTimeoutListener;

import junit.framework.TestCase;

public class Test extends TestCase {
//    private static final Log Trace = LogFactory.getLog(Test.class);

    public class TimeoutListener implements IntervalTimerTimeoutListener {
    	protected String name;
    	protected boolean fired = false;
    	
    	public TimeoutListener(String name) {
    		this.name = name;
    	}
    	
		@Override
		synchronized public void onIntervalTimeout() {
			fired = true;
		}	
		
		synchronized boolean hasFired() {
			return fired;
		}

		@Override
		public String toString() {
			return name;
		}
    }

    public void testTimer1() {
    	try {
    		TimeoutListener listener = new TimeoutListener("testTimer1");
        	IntervalTimer timer = IntervalTimer.GetIntervalTimer(5, TimeUnit.SECONDS);
        	timer.schedule(listener);
        	Thread.sleep(6000);
        	assertTrue(listener.fired);
    	} catch(Exception e) {
    		e.printStackTrace();
    		fail(e.getMessage());
    	}
    }

    public void testTimer2() {
    	try {
    		TimeoutListener listener1 = new TimeoutListener("testTimer2-1");
    		TimeoutListener listener2 = new TimeoutListener("testTimer2-2");
    		TimeoutListener listener3 = new TimeoutListener("testTimer2-3");
        	IntervalTimer timer = IntervalTimer.GetIntervalTimer(6, TimeUnit.SECONDS);
        	timer.schedule(listener1);
        	Thread.sleep(2000);
        	assertFalse(listener1.fired);
        	timer.schedule(listener2);
        	Thread.sleep(2000);
        	assertFalse(listener1.fired);
        	assertFalse(listener2.fired);
        	timer.schedule(listener3);
        	Thread.sleep(3000);
        	assertTrue(listener1.fired);
        	assertFalse(listener2.fired);
        	assertFalse(listener2.fired);
        	Thread.sleep(2000);
        	assertTrue(listener2.fired);
        	assertFalse(listener3.fired);
        	Thread.sleep(2000);
        	assertTrue(listener3.fired);
    	} catch(Exception e) {
    		e.printStackTrace();
    		fail(e.getMessage());
    	}
    }

    public void testCancel1() {
    	try {
    		TimeoutListener listener = new TimeoutListener("testCancel1");
        	IntervalTimer timer = IntervalTimer.GetIntervalTimer(5, TimeUnit.SECONDS);
        	timer.schedule(listener);
        	assertTrue(timer.cancel(listener));
        	Thread.sleep(6000);
        	assertFalse(listener.fired);
    	} catch(Exception e) {
    		e.printStackTrace();
    		fail(e.getMessage());
    	}
    }

    public void testCancel2() {
    	try {
    		TimeoutListener listener1 = new TimeoutListener("testCancel2-1");
    		TimeoutListener listener2 = new TimeoutListener("testCancel2-2");
        	IntervalTimer timer = IntervalTimer.GetIntervalTimer(5, TimeUnit.SECONDS);
        	timer.schedule(listener1);
        	timer.schedule(listener2);
        	assertTrue(timer.cancel(listener1));
        	assertTrue(timer.cancel(listener2));
        	Thread.sleep(6000);
        	assertFalse(listener1.fired);
        	assertFalse(listener2.fired);
    	} catch(Exception e) {
    		e.printStackTrace();
    		fail(e.getMessage());
    	}
    }

    public void testCancel3() {
    	try {
    		TimeoutListener listener1 = new TimeoutListener("testCancel3-1");
    		TimeoutListener listener2 = new TimeoutListener("testCancel3-2");
        	IntervalTimer timer = IntervalTimer.GetIntervalTimer(5, TimeUnit.SECONDS);
        	timer.schedule(listener1);
        	timer.schedule(listener2);
        	assertTrue(timer.cancel(listener2));
        	assertTrue(timer.cancel(listener1));
        	Thread.sleep(6000);
        	assertFalse(listener1.fired);
        	assertFalse(listener2.fired);
    	} catch(Exception e) {
    		e.printStackTrace();
    		fail(e.getMessage());
    	}
    }

    public void testCancel4() {
    	try {
    		TimeoutListener listener1 = new TimeoutListener("testCancel4-1");
    		TimeoutListener listener2 = new TimeoutListener("testCancel4-2");
        	IntervalTimer timer = IntervalTimer.GetIntervalTimer(5, TimeUnit.SECONDS);
        	timer.schedule(listener1);
        	timer.schedule(listener2);
        	assertTrue(timer.cancel(listener1));
        	Thread.sleep(6000);
        	assertFalse(listener1.fired);
        	assertTrue(listener2.fired);
    	} catch(Exception e) {
    		e.printStackTrace();
    		fail(e.getMessage());
    	}
    }

    public void testLateCancel() {
    	try {
    		TimeoutListener listener = new TimeoutListener("testLateCancel");
        	IntervalTimer timer = IntervalTimer.GetIntervalTimer(5, TimeUnit.SECONDS);
        	timer.schedule(listener);
        	assertTrue(timer.cancel(listener));
        	assertFalse(timer.cancel(listener));
        	Thread.sleep(6000);
        	assertFalse(listener.fired);
        	assertFalse(timer.cancel(listener));
    	} catch(Exception e) {
    		e.printStackTrace();
    		fail(e.getMessage());
    	}
    }
}
