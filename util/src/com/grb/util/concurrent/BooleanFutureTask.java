package com.grb.util.concurrent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class BooleanFutureTask implements Future<Boolean> {   
    private CountDownLatch mLatch;
    private boolean mSuccess;
    private ExecutionException mException;
    private Object mLock;
    
    public BooleanFutureTask() {
        mLatch = new CountDownLatch(1);
        mSuccess = true;
        mException = null;
        mLock = new Object();
    }

    /**
     * mayInterruptIfRunning is not checked
     */
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    public Boolean get() throws InterruptedException, ExecutionException {
        mLatch.await();
        return processResult();
    }

    public Boolean get(long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException {
        if (!mLatch.await(timeout, unit)) {
            throw new TimeoutException();
        }
        return processResult();
    }

    public boolean isCancelled() {
        return false;
    }

    public boolean isDone() {
        return (mLatch.getCount() == 0);
    }
    
    public void setDone() {
        synchronized(mLock) {            
            if (!isDone()) {
                mSuccess = true;
                mLatch.countDown();
            }
        }
    }

    public void setDone(Throwable t) {
        synchronized(mLock) {
            if (!isDone()) {
                mSuccess = false;
                mException = new ExecutionException(t);
                mLatch.countDown();
            }
        }
    }
    
    private Boolean processResult() throws ExecutionException {
        if (mSuccess) {
            return true;
        } else {
            throw mException;
        }
    }
}
