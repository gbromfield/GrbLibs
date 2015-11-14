package com.grb.subscriptionTree;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SubscriptionMatchEntry<T> {
    static private final Log Trace = LogFactory.getLog(SubscriptionMatchEntry.class);

    private byte[] mMatch;
    private List<T> mEntries;
    
    public SubscriptionMatchEntry(byte[] match, List<T> entries) {
        mMatch = match;
        if (entries == null) {
            if (Trace.isErrorEnabled()) {
                Trace.error("Null entries found in a match");
            }
            mEntries = new ArrayList<T>();
        } else {
            mEntries = new ArrayList<T>(entries.size());
            for(int i = 0; i < entries.size(); i++) {
                mEntries.add(entries.get(i));
            }
        }
    }
    
    public byte[] getMatch() {
        return mMatch;
    }
    
    public List<T> getEntries() {
        return mEntries;
    }
    
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("match = \"");
        bldr.append(new String(mMatch));
        bldr.append("\", entries = [");
        if (mEntries == null) {
            bldr.append("null");
        } else {
            for(int i = 0; i < mEntries.size(); i++) {
                if (i > 0) {
                    bldr.append(",");
                }
                bldr.append(mEntries.get(i));
            }
        }
        bldr.append("]");
        return bldr.toString();
    }
}
