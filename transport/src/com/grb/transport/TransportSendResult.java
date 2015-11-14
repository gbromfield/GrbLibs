package com.grb.transport;

import java.util.concurrent.Future;

public class TransportSendResult {    
    protected Future<Boolean> mFuture;
    
    /**
     * Gets the future to use to find out when a send has been completed.
     * Null if all the bytes were written.
     * 
     * @return The future use to find out when a send has been completed
     */
    public Future<Boolean> getFuture() {
        return mFuture;
    }

    /**
     * Set by the transport layer when a write doesn't complete.
     * 
     * @param future Future of the write.
     */
    public void setFuture(Future<Boolean> future) {
        mFuture = future;
    }
    
    public boolean isDone() {
        return (mFuture == null);
    }
}
