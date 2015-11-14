package com.grb.subscriptionTree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class SubscriptionTree<T> {
    static private final byte STAR              = 42;
    static private final byte SLASH             = 47;
    static private final byte GREATER_THAN      = 62;
    static private final byte[] WILDCARD        = new byte[] {GREATER_THAN};
        
    private class SubscriptionTreeNode {        
        private byte[] mData;
        private int mLength;
        private HashMap<Byte, SubscriptionTreeNode> mTextChildren;
        private SubscriptionTreeNode mTextWildNode;
        private HashMap<Byte, SubscriptionTreeNode> mLevelChildren;
        private SubscriptionTreeNode mLevelWildNode;
        private SubscriptionTreeNode mLevelStarNode;
        private byte[] mSubscription;

        /**
         * The list of matching entries for this node.
         */
        private List<T> mEntries;
        
        /**
         * True if this is the root node.
         */
        private boolean mIsRoot;
        
        public SubscriptionTreeNode() {
            mData = new byte[0];
            mLength = 0;
            mTextChildren = null;
            mTextWildNode = null;
            mLevelChildren = null;
            mLevelWildNode = null;
            mLevelStarNode = null;
            mSubscription = null;
            mEntries = null;
            mIsRoot = false;
        }

        public SubscriptionTreeNode(SubscriptionTreeNode node) {
            mData = node.mData;
            mLength = node.mLength;
            if (node.mTextChildren == null) {
                mTextChildren = null;
            } else {
                mTextChildren = new HashMap<Byte, SubscriptionTreeNode>();
                Iterator<Byte> it = node.mTextChildren.keySet().iterator();
                while(it.hasNext()) {
                    Byte key = it.next();
                    SubscriptionTreeNode value = node.mTextChildren.get(key);
                    mTextChildren.put(key, new SubscriptionTreeNode(value));
                }
            }
            if (node.mTextWildNode == null) {
                mTextWildNode = null;
            } else {
                mTextWildNode = new SubscriptionTreeNode(node.mTextWildNode);
            }
            if (node.mLevelChildren == null) {
                mLevelChildren = null;
            } else {
                mLevelChildren = new HashMap<Byte, SubscriptionTreeNode>();
                Iterator<Byte> it = node.mLevelChildren.keySet().iterator();
                while(it.hasNext()) {
                    Byte key = it.next();
                    SubscriptionTreeNode value = node.mLevelChildren.get(key);
                    mLevelChildren.put(key, new SubscriptionTreeNode(value));
                }
            }
            if (node.mLevelWildNode == null) {
                mLevelWildNode = null;
            } else {
                mLevelWildNode = new SubscriptionTreeNode(node.mLevelWildNode);
            }
            if (node.mLevelStarNode == null) {
                mLevelStarNode = null;
            } else {
                mLevelStarNode = new SubscriptionTreeNode(node.mLevelStarNode);
            }
            mSubscription = node.mSubscription;
            if (node.mEntries == null) {
                mEntries = null;
            } else {
                mEntries = new ArrayList<T>();
                for(int i = 0; i < node.mEntries.size(); i++) {
                    mEntries.add(node.mEntries.get(i));
                }
            }
            mIsRoot = node.mIsRoot;
        }

        private SubscriptionTreeNode(byte[] subscription, int offset, int length, T entry) {
            mData = null;
            mLength = 0;
            mTextChildren = null;
            mTextWildNode = null;
            mLevelChildren = null;
            mLevelWildNode = null;
            mLevelStarNode = null;
            mSubscription = null;
            mEntries = null;
            mIsRoot = false;
            
            for(int i = 0; i < length; i++) {
                int index = offset + i;
                if (subscription[index] == STAR) {
                    if (i == 0) {                               // the string "*" or "*/..."
                        // skip
                    } else if (i == (length-1)) {               // the string "...*"
                        setData(subscription, offset, i);
                        addTextChild(new SubscriptionTreeNode(subscription, index, 1, entry));
                        return;
                    } else {                                    // the string "...*/...."
                        setData(subscription, offset, i);
                        addTextChild(new SubscriptionTreeNode(subscription, index, length - i, entry));
                        return;
                    }
                } else if (subscription[index] == SLASH) {
                    setData(subscription, offset, i);
                    // skip over "/"
                    addLevelChild(new SubscriptionTreeNode(subscription, index + 1, length - (i + 1), entry));
                    return;
                }
            }
            setData(subscription, offset, length);
            mSubscription = subscription;
            addEntry(entry);
        }

        public void get(byte[] data, List<SubscriptionMatchEntry<T>> entries) {
            get(data, 0, data.length, entries);
        }

        public void get(byte[] data, int offset, int length, List<SubscriptionMatchEntry<T>> entries) {
            // is this a * level
            if ((mLength == 1) && (mData[0] == STAR)) {
                // skip to the next level if there is one
                int index = offset;
                int end = offset + length;
                while((index < end) && (data[index] != SLASH)) {
                    index++;
                }
                if (index < end) {
                    // new level found
                    getToChildren(data, index, length - (index - offset), entries);
                } else {
                    if (isTerminal()) {
                        entries.add(new SubscriptionMatchEntry<T>(mSubscription, mEntries));
                    }
                }
            } else {
                if (length < mLength) {
                    return;
                }
                for(int i = 1; i < mLength; i++) {
                    if (mData[i] != data[offset + i]) {
                        return;
                    }
                }
                if (mLength == length) {
                    if (isTerminal()) {
                        entries.add(new SubscriptionMatchEntry<T>(mSubscription, mEntries));
                    } 
                    if ((mTextWildNode != null) && (mTextWildNode.isTerminal())) {
                        // for matching topic to topic* (0 or more matching)
                        entries.add(new SubscriptionMatchEntry<T>(mTextWildNode.mSubscription, mTextWildNode.mEntries));
                    }
                    return;
                }
                getToChildren(data, offset + mLength, length - mLength, entries);
            }            
        }

        public void put(byte[] subscription, int offset, int length, T entry) {
            // skip first character because it was matched to get to this point
            for(int i = 1; i < mLength; i++) {
                if (i == length) {
                    // input is shorter than node text
                    addTextChild(i, subscription, offset, length, entry);
                    return;
                } else {
                    if (subscription[offset + i] == SLASH) {
                        addLevelChild(i, subscription, offset, length, entry);
                        return;
                    } else if ((mData[i] != subscription[offset + i])) {
                        addTextChild(i, subscription, offset, length, entry);
                        return;
                    }
                }
            }
            int index = offset + mLength;
            if (index >= subscription.length) {
                // exact match
                mSubscription = subscription;
                addEntry(entry);
            } else {
                // superset case (adding gaga to ga)
                SubscriptionTreeNode child;
                if (subscription[index] == SLASH) {
                    child = getLevelChild(subscription[index + 1], (index == (subscription.length - 2)));         // skip over slash
                    if (child == null) {
                        addLevelChild(new SubscriptionTreeNode(subscription, 
                                index + 1, length - mLength - 1, entry));
                    } else {
                        child.put(subscription, index + 1, length - mLength - 1, entry);
                    }
                } else {
                    child = getTextChild(subscription[index]);
                    if (child == null) {
                        addTextChild(new SubscriptionTreeNode(subscription, index, length - mLength, entry));
                    } else {
                        child.put(subscription, index, length - mLength, entry);
                    }
                }
            }
        }

        public void remove(byte[] subscription, T entry, SubscriptionRemoveResult result) {
            remove(subscription, 0, subscription.length, entry, result);
        }

        public void remove(byte[] subscription, int offset, int length, T entry, SubscriptionRemoveResult result) {
            for(int i = 0; i < mLength; i++) {
                if (i >= length) {
                    // current node is longer than the subscription - no exact match
                    return;
                }
                if (mData[i] != subscription[offset + i]) {
                    // difference in text
                    return;
                }
            }
            if (mLength == length) {
                // Exact match
                removeEntry(entry, result);
            } else {
                // more bytes - check children
                int next = offset + mLength;
                if (subscription[next] == SLASH) {
                    next++;
                    SubscriptionTreeNode child = getLevelChild(subscription[next], (next == (subscription.length - 1)));
                    if (child == null) {
                        return;
                    }
                    child.remove(subscription, next, length - (next - offset), entry, result);
                    if (result.isFound() && (result.numEntriesFound() == 1) && (child.isDisposable())) {
                        removeLevelChild(subscription[next], (next == (subscription.length - 1)));
                        merge();
                    }                    
                } else {
                    SubscriptionTreeNode child = getTextChild(subscription[next]);
                    if (child == null) {
                        return;
                    }
                    child.remove(subscription, next, length - (next - offset), entry, result);
                    if (result.isFound() && (result.numEntriesFound() == 1) && (child.isDisposable())) {
                        removeTextChild(subscription[next]);
                        merge();
                    }
                }
            }
        }

        private byte getKey() {
            return mData[0];
        }

        private SubscriptionTreeNode getTextChild(byte b) {
            if (b == STAR) {
                return mTextWildNode;
            } else {
                if (mTextChildren == null) {
                    return null;
                }
                return mTextChildren.get(b);
            }
        }

        private void removeTextChild(byte b) {
            if (b == STAR) {
                mTextWildNode = null;
            } else {
                if (mTextChildren != null) {
                    mTextChildren.remove(b);
                }
            }
        }

        private SubscriptionTreeNode getLevelChild(byte b, boolean isLast) {
            if ((b == GREATER_THAN) && (isLast)) {
                return mLevelWildNode;
            } else if (b == STAR) {
                return mLevelStarNode;
            } else {
                if (mLevelChildren == null) {
                    return null;
                }
                return mLevelChildren.get(b);
            }
        }

        private void removeLevelChild(byte b, boolean isLast) {
            if ((b == GREATER_THAN) && (isLast)) {
                mLevelWildNode = null;
            } else if (b == STAR) {
                mLevelStarNode = null;
            } else {
                if (mLevelChildren != null) {
                    mLevelChildren.remove(b);
                }
            }
        }

        public int numNodes() {
            int num = 1;
            if (mLevelWildNode != null) {
                num += mLevelWildNode.numNodes();
            }
            if (mLevelStarNode != null) {
                num += mLevelStarNode.numNodes();
            }
            if (mLevelChildren != null) {
                Iterator<Byte> it = mLevelChildren.keySet().iterator();
                while(it.hasNext()) {
                    SubscriptionTreeNode child = mLevelChildren.get(it.next());
                    num += child.numNodes();
                }
            }
            if (mTextWildNode != null) {
                num += mTextWildNode.numNodes();
            }
            if (mTextChildren != null) {
                Iterator<Byte> it = mTextChildren.keySet().iterator();
                while(it.hasNext()) {
                    SubscriptionTreeNode child = mTextChildren.get(it.next());
                    num += child.numNodes();
                }
            }
            return num;
        }

        public boolean hasChildren() {
            if ((mLevelWildNode != null) || (mLevelStarNode != null) || (mTextWildNode != null)) {
                return true;
            }
            if ((mLevelChildren != null) && (mLevelChildren.size() > 0)) {
                return true;
            }
            if ((mTextChildren != null) && (mTextChildren.size() > 0)) {
                return true;
            }
            return false;
        }
        
        public boolean isTerminal() {
            return ((mEntries != null) && (mEntries.size() > 0));
        }
        
        public boolean isDisposable() {
            return ((!isTerminal()) && (!hasChildren()));
        }
        
        private void getToChildren(byte[] data, int index, int length, List<SubscriptionMatchEntry<T>> entries) {
            if (data[index] == SLASH) {
                // check for level wildcard
                if (mLevelWildNode != null) {
                    entries.add(new SubscriptionMatchEntry<T>(mLevelWildNode.mSubscription, mLevelWildNode.mEntries));
                }
                // check for level * wildcard
                if (mLevelStarNode != null) {
                    mLevelStarNode.get(data, index + 1, length - 1, entries);
                }
                // check for text * wildcard
                if (mTextWildNode != null) {
                    mTextWildNode.getToChildren(data, index, length, entries);
                }
                // check for new level
                SubscriptionTreeNode child = getLevelChild(data[index + 1], false);
                if (child != null) {
                    child.get(data, index + 1, length - 1, entries);
                }
            } else {
                // check for text wildcard
                if (mTextWildNode != null) {
                    mTextWildNode.get(data, index, length, entries);
                }
                SubscriptionTreeNode child = getTextChild(data[index]);
                if (child == null) {
                    return;
                }
                child.get(data, index, length, entries);
            }
        }
        
        private void addTextChild(SubscriptionTreeNode node) {
            if (node.getKey() == STAR) {
                mTextWildNode = node;
            } else {
                if (mTextChildren == null) {
                    mTextChildren = new HashMap<Byte, SubscriptionTreeNode>();
                }
                mTextChildren.put(node.getKey(), node);
            }
        }
        
        private void addTextChild(int index, byte[] subscription, int offset, int length, T entry) {
            split(index);
            if (index < length) {
                addTextChild(new SubscriptionTreeNode(subscription, offset + index, 
                        length - index, entry));
            } else {
                mSubscription = subscription;
                addEntry(entry);
            }
        }
        
        private void addLevelChild(SubscriptionTreeNode node) {
            if ((node.getKey() == GREATER_THAN) && (node.mLength == 1)) {
                mLevelWildNode = node;
            } else if (node.getKey() == STAR) {
                mLevelStarNode = node;
            } else {
                if (mLevelChildren == null) {
                    mLevelChildren = new HashMap<Byte, SubscriptionTreeNode>();
                }
                mLevelChildren.put(node.getKey(), node);
            }
        }

        private void addLevelChild(int index, byte[] subscription, int offset, int length, T entry) {
            split(index);
            addLevelChild(new SubscriptionTreeNode(subscription, offset + index + 1, length - mLength - 1, entry));
        }
        
        private void setData(byte[] data, int offset, int length) {
            mData = new byte[length];
            for(int i = 0; i < length; i++) {
                mData[i] = data[offset+i];
            }
            mLength = length;
        }
            
        private void addEntry(T entry) {
            if (entry != null) {
                if (mEntries == null) {
                    mEntries = new ArrayList<T>();
                }
                mEntries.add(entry);
            }
        }

        private void removeEntry(T entry, SubscriptionRemoveResult result) {
            if ((entry != null) && (mEntries != null)) {
                result.setNumEntriesFound(mEntries.size());
                for(int i = 0; i < mEntries.size(); i++) {
                    if (mEntries.get(i).equals(entry)) {
                        result.setFound();
                        mEntries.remove(i);
                        if (mEntries.size() == 0) {
                            mEntries = null;
                        }
                        return;
                    }
                }
            }
        }

        private void split(int index) {
            byte[] newData = new byte[index];
            for(int j = 0; j < index; j++) {
                newData[j] = mData[j];
            }
            SubscriptionTreeNode newNode = new SubscriptionTreeNode(mData, index, mLength - index, null);
            newNode.mSubscription = mSubscription;
            mData = newData;
            mLength = index;
            mSubscription = null;
            if (mEntries != null) {
                for (int i = 0; i < mEntries.size(); i++) {
                    newNode.addEntry(mEntries.get(i));
                }
                mEntries.clear();
            }
            newNode.mLevelWildNode = mLevelWildNode;
            mLevelWildNode = null;
            newNode.mLevelStarNode = mLevelStarNode;
            mLevelStarNode = null;
            if (mLevelChildren != null) {
                Iterator<Byte> it = mLevelChildren.keySet().iterator();
                while(it.hasNext()) {
                    SubscriptionTreeNode child = mLevelChildren.get(it.next());
                    it.remove();
                    newNode.addLevelChild(child);
                }
            }
            newNode.mTextWildNode = mTextWildNode;
            mTextWildNode = null;
            if (mTextChildren != null) {
                Iterator<Byte> it = mTextChildren.keySet().iterator();
                while(it.hasNext()) {
                    SubscriptionTreeNode child = mTextChildren.get(it.next());
                    it.remove();
                    newNode.addTextChild(child);
                }
            }
            addTextChild(newNode);
        }

        private void merge() {
            if ((mLevelWildNode != null) || (mLevelStarNode != null) || (mTextWildNode != null)) {
                return;
            }
            if ((mLevelChildren != null) && (mLevelChildren.size() > 0)) {
                return;
            }
            if ((mTextChildren == null) || (mTextChildren.size() != 1)) {
                return;
            }
            if (isTerminal()) {
                return;
            }
            if (mIsRoot) {
                return;
            }
            Iterator<Byte> it = mTextChildren.keySet().iterator();
            byte key = it.next();
            SubscriptionTreeNode child = mTextChildren.get(key);
            byte[] newData = new byte[mLength + child.mLength];
            for(int i = 0; i < mLength; i++) {
                newData[i] = mData[i];
            }
            for(int i = 0; i < child.mLength; i++) {
                newData[i + mLength] = child.mData[i];
            }
            mData = newData;
            mLength = mLength + child.mLength;
            mTextChildren = child.mTextChildren;
            mTextWildNode = child.mTextWildNode;
            mLevelChildren = child.mLevelChildren;
            mLevelWildNode = child.mLevelWildNode;
            mLevelStarNode = child.mLevelStarNode;
            mSubscription = child.mSubscription;
            mEntries = child.mEntries;
        }

        public String toString() {
            StringBuilder bldr = new StringBuilder();
            toString(bldr, 0, false);
            return bldr.toString();
        }
        
        public void toString(StringBuilder bldr, int leadingSpaces, boolean leadingSlash) {        
            for(int i = 0; i < leadingSpaces; i++) {
                bldr.append(" ");
            }
            if (isTerminal()) {
                if (leadingSlash) {
                    bldr.append("[/");
                } else {
                    bldr.append("[");
                }
            } else {
                if (leadingSlash) {
                    bldr.append("(/");
                } else {
                    bldr.append("(");
                }
            }
            bldr.append(new String(mData));
            if (isTerminal()) {
                bldr.append("]");
                if (mEntries.size() > 1) {
                    bldr.append('(');
                    bldr.append(mEntries.size());
                    bldr.append(')');
                }
                bldr.append("\r\n");
            } else {
                bldr.append(")\r\n");
            }
            if (mTextChildren != null) {
                Iterator<Byte> it = mTextChildren.keySet().iterator();
                while(it.hasNext()) {
                    SubscriptionTreeNode child = mTextChildren.get(it.next());
                    child.toString(bldr, leadingSpaces + mLength + 2, false);
                }
            }
            if (mTextWildNode != null) {
                mTextWildNode.toString(bldr, leadingSpaces + mLength + 2, false);
            }
            if (mLevelChildren != null) {
                Iterator<Byte> it = mLevelChildren.keySet().iterator();
                while(it.hasNext()) {
                    SubscriptionTreeNode child = mLevelChildren.get(it.next());
                    child.toString(bldr, leadingSpaces + mLength + 2, true);
                }
            }
            if (mLevelWildNode != null) {
                mLevelWildNode.toString(bldr, leadingSpaces + mLength + 2, true);
            }
            if (mLevelStarNode != null) {
                mLevelStarNode.toString(bldr, leadingSpaces + mLength + 2, true);
            }
        }
    }

    private SubscriptionTreeNode mRoot;
    
    public SubscriptionTree() {
        clear();
    }

    public SubscriptionTree(SubscriptionTree<T> tree) {
        mRoot = new SubscriptionTreeNode(tree.mRoot);
    }
    
    public void get(byte[] data, List<SubscriptionMatchEntry<T>> entries) {
        get(data, 0, data.length, entries);
    }
    
    public void get(byte[] data, int offset, int length, List<SubscriptionMatchEntry<T>> entries) {
        if ((mRoot.mEntries != null) && (mRoot.mEntries.size() > 0)) {
            entries.add(new SubscriptionMatchEntry<T>(mRoot.mSubscription, mRoot.mEntries));
        }
        mRoot.get(data, entries);
    }
    
    public void put(byte[] subscription, T entry) {
        if ((subscription.length == 1) && (subscription[0] == GREATER_THAN)) {
            mRoot.addEntry(entry);
        } else {
            mRoot.put(subscription, 0, subscription.length, entry);
        }
    }

    public void remove(byte[] subscription, T entry, SubscriptionRemoveResult result) {
        if ((subscription.length == 1) && (subscription[0] == GREATER_THAN)) {
            mRoot.removeEntry(entry, result);
        } else {
            mRoot.remove(subscription, entry, result);
        }
    }

    public void clear() {
        mRoot = new SubscriptionTreeNode();
        mRoot.mSubscription = WILDCARD;
        mRoot.mIsRoot = true;
    }

    public int numNodes() {
        return mRoot.numNodes();
    }
    
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        mRoot.toString(bldr, 0, false);
        return bldr.toString();
    }  
}
