package com.grb.subscriptionTree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.grb.util.ByteArray;

public class SubscriptionHashMap<T> {
    private HashMap<ByteArray, List<T>> mSubscriptionMap;
    
    public SubscriptionHashMap() {
        mSubscriptionMap = null;
    }
    
    public Map<ByteArray, List<T>> getMap() {
        return mSubscriptionMap;
    }
    
    public void get(byte[] data, List<SubscriptionMatchEntry<T>> entries) {
        if (mSubscriptionMap != null) {
            ByteArray ba = new ByteArray(data);
            List<T> matches = mSubscriptionMap.get(ba);
            if (matches != null) {
                entries.add(new SubscriptionMatchEntry<T>(data, matches));
            }
        }
    }
    
    public SubscriptionHashMap<T> put(byte[] data, T entry) {
        SubscriptionHashMap<T> retMap = new SubscriptionHashMap<T>();
        if (mSubscriptionMap == null) {
            retMap.mSubscriptionMap = new HashMap<ByteArray, List<T>>();
        } else {
            retMap.mSubscriptionMap = new HashMap<ByteArray, List<T>>(mSubscriptionMap);
        }
        ByteArray subBytes = new ByteArray(data);
        List<T> entries = retMap.mSubscriptionMap.get(subBytes);
        if (entries == null) {
            entries = new ArrayList<T>();
        } else {
            entries = new ArrayList<T>(entries);
        }
        retMap.mSubscriptionMap.put(subBytes, entries);
        entries.add(entry);
        return retMap;
    }

    public SubscriptionHashMap<T> remove(byte[] data, T entry, SubscriptionRemoveResult result) {
        if (mSubscriptionMap != null) {
            SubscriptionHashMap<T> retMap = new SubscriptionHashMap<T>();
            retMap.mSubscriptionMap = new HashMap<ByteArray, List<T>>(mSubscriptionMap);
            ByteArray subBytes = new ByteArray(data);
            List<T> entries = retMap.mSubscriptionMap.get(subBytes);
            if (entries != null) {
                result.setNumEntriesFound(entries.size());
                for(int i = 0; i < entries.size(); i++) {
                    T value = entries.get(i);
                    if (value == entry) {
                        // found
                        result.setFound();
                        if (entries.size() == 1) { 
                            if (retMap.mSubscriptionMap.size() == 1) {
                                // only entry
                                retMap.mSubscriptionMap = null;
                            } else {
                                retMap.mSubscriptionMap.remove(subBytes);
                            }
                        } else {
                            entries = new ArrayList<T>(entries);
                            entries.remove(i);
                            retMap.mSubscriptionMap.put(subBytes, entries);
                        }
                        return retMap;
                    }
                }
            }
        }
        // if an entry isn't found - return yourself
        return this;
    }

    public void clear() {
        mSubscriptionMap = null;
    }
    
    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        if (mSubscriptionMap == null) {
            bldr.append("null");
        } else {
            Iterator<ByteArray> it = mSubscriptionMap.keySet().iterator();
            while(it.hasNext()) {
                ByteArray ba = it.next();
                bldr.append(new String(ba.getBuffer(), ba.getOffset(), ba.getLength()));
                if (it.hasNext()) {
                    bldr.append("\r\n");
                }
            }
        }
        return bldr.toString();
    }
}
