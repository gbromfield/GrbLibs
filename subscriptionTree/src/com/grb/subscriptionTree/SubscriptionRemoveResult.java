package com.grb.subscriptionTree;

public class SubscriptionRemoveResult {
    
    /**
     * True if the subscription remove was successful.
     */
    private boolean mIsFound;
    
    /**
     * The number of entries matching the subscription (before any remove).
     * 
     * So the check should be if (isFound() && (numEntriesFound == 1))
     * then this subscription is not needed anymore
     */
    private int mNumEntriesFound;
        
    public SubscriptionRemoveResult() {
        mIsFound = false;
        mNumEntriesFound = 0;
    }
    
    public void setFound() {
        mIsFound = true;
    }
    
    public void setNumEntriesFound(int numEntriesFound) {
        mNumEntriesFound = numEntriesFound;
    }
    
    public boolean isFound() {
        return mIsFound;
    }
    
    public int numEntriesFound() {
        return mNumEntriesFound;
    }
}
