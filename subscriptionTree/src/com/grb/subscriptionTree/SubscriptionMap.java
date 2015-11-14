package com.grb.subscriptionTree;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import com.grb.util.DestinationUtil;
import com.grb.util.UTF8Util;

public class SubscriptionMap<T> {
    
    volatile private SubscriptionHashMap<T> mNonWildSubscriptions;
    volatile private SubscriptionTree<T> mWildSubscriptions;
    volatile private int mNumEntries;

    private ReentrantLock mLock;
    private SubscriptionHashMap<T> mNonWildSubscriptionsCopy;
    private SubscriptionTree<T> mWildSubscriptionsCopy;
    private int mNumEntriesCopy;
    
    public SubscriptionMap() {
        mNonWildSubscriptions = new SubscriptionHashMap<T>();
        mWildSubscriptions = new SubscriptionTree<T>();
        mNumEntries = 0;
        mLock = new ReentrantLock();
        mNonWildSubscriptionsCopy = null;
        mWildSubscriptionsCopy = null;
        mNumEntriesCopy = 0;
    }
    
    public int getNumEntries() {
        return mNumEntries;
    }
    
    public SubscriptionHashMap<T> getNonWildSubscriptions() {
        return mNonWildSubscriptions;
    }
    
    public SubscriptionTree<T> getWildSubscriptions() {
        return mWildSubscriptions;
    }
    
    public void get(byte[] data, List<SubscriptionMatchEntry<T>> entries) {
        mNonWildSubscriptions.get(data, entries);
        mWildSubscriptions.get(data, entries);
    }

    synchronized public void startTransaction() {
        if (mLock.isHeldByCurrentThread()) {
            return;
        }
        mLock.lock();
        mNonWildSubscriptionsCopy = null;
        mWildSubscriptionsCopy = null;
        mNumEntriesCopy = mNumEntries;        
    }
    
    synchronized public void put(String subscription, T entry) {
        if (!mLock.isHeldByCurrentThread()) {
            throw new IllegalStateException("startTransaction() must acquire the lock first");
        }
        if (DestinationUtil.isWildCardedCrb(subscription) ||
                DestinationUtil.isWildCardedTrb(subscription)) {
            if (mWildSubscriptionsCopy == null) {
                mWildSubscriptionsCopy = new SubscriptionTree<T>(mWildSubscriptions);
            }
            mWildSubscriptionsCopy.put(UTF8Util.toUTF8(subscription), entry);
        } else {
            if (mNonWildSubscriptionsCopy == null) {
                mNonWildSubscriptionsCopy = mNonWildSubscriptions.put(UTF8Util.toUTF8(subscription), entry);
            } else {
                mNonWildSubscriptionsCopy = mNonWildSubscriptionsCopy.put(UTF8Util.toUTF8(subscription), entry);
            }
        }
        mNumEntriesCopy++;
    }

    synchronized public SubscriptionRemoveResult remove(String subscription, T entry) {
        if (!mLock.isHeldByCurrentThread()) {
            throw new IllegalStateException("startTransaction() must acquire the lock first");
        }
        SubscriptionRemoveResult result = new SubscriptionRemoveResult();
        if (DestinationUtil.isWildCardedCrb(subscription) ||
                DestinationUtil.isWildCardedTrb(subscription)) {
            if (mWildSubscriptionsCopy == null) {
                mWildSubscriptionsCopy = new SubscriptionTree<T>(mWildSubscriptions);
            }
            mWildSubscriptionsCopy.remove(UTF8Util.toUTF8(subscription), entry, result);
        } else {
            if (mNonWildSubscriptionsCopy == null) {
                mNonWildSubscriptionsCopy = mNonWildSubscriptions.remove(UTF8Util.toUTF8(subscription), 
                        entry, result);
            } else {
                mNonWildSubscriptionsCopy = mNonWildSubscriptionsCopy.remove(UTF8Util.toUTF8(subscription), 
                        entry, result);
            }
        }
        if (result.isFound()) {
            mNumEntriesCopy--;
        }
        return result;
    }

    synchronized public void commit() {
        if (!mLock.isHeldByCurrentThread()) {
            throw new IllegalStateException("startTransaction() must acquire the lock first");
        }
        if (mNonWildSubscriptionsCopy != null) {
            mNonWildSubscriptions = mNonWildSubscriptionsCopy;
            mNumEntries = mNumEntriesCopy;        
        }
        if (mWildSubscriptionsCopy != null) {
            mWildSubscriptions = mWildSubscriptionsCopy;
            mNumEntries = mNumEntriesCopy;        
        }
        mLock.unlock();
    }
    
    synchronized public void rollback() {
        if (!mLock.isHeldByCurrentThread()) {
            throw new IllegalStateException("startTransaction() must acquire the lock first");
        }
        mLock.unlock();
    }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append(mNonWildSubscriptions);
        bldr.append("\r\n");
        bldr.append(mWildSubscriptions);
        return bldr.toString();
    }
}
