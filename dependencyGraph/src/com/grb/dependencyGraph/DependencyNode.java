package com.grb.dependencyGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;

public class DependencyNode implements Runnable {
    
    public enum State {
        DependencyFailure,  // terminal
        Failure,            // terminal
        New,
        Queued,
        Running,
        Success             // terminal
    }
    
    protected String mName;
    protected ThreadPoolExecutor mExecutor;
    protected Object mUserData;
    protected State mState;
    protected Object mStateLock;
    protected ArrayList<DependencyNode> mParents;   // assumed not changed 
    protected int mNumParents;
    protected ArrayList<DependencyNode> mChildren; // assumed not changed
    protected ArrayList<DependencyNodeListener> mListeners;
    protected ArrayList<CountDownLatch> mCountDownLatches;
    
    public DependencyNode(String name, ThreadPoolExecutor executor, Object userData) {
        this(name, executor, userData, null, null);
    }

    public DependencyNode(String name, ThreadPoolExecutor executor, Object userData, DependencyNodeListener listener, CountDownLatch countDownLatch) {
        mName = name;
        mExecutor = executor;
        mUserData = userData;
        mState = null;
        mStateLock = new Object();
        mParents = null;
        mNumParents = 0;
        mChildren = null;
        mListeners = null;
        if (listener != null) {
            addListener(listener);
        }
        mCountDownLatches = null;
        if (countDownLatch != null) {
            addCountDownLatch(countDownLatch);
        }
        changeState(State.New);
    }

    public String getName() {
        return mName;
    }
    
    public ThreadPoolExecutor getExecutor() {
        return mExecutor;
    }

    public Object getUserData() {
        return mUserData;
    }

    public State getState() {
        return mState;
    }
        
    public void addListener(DependencyNodeListener listener) {
        if (mListeners == null) {
            mListeners = new ArrayList<DependencyNodeListener>();
        }
        mListeners.add(listener);
    }
    
    public void removeListener(DependencyNodeListener listener) {
        if (mListeners != null) {
            mListeners.remove(listener);
        }
    }
    
    public void addCountDownLatch(CountDownLatch latch) {
        if (mCountDownLatches == null) {
            mCountDownLatches = new ArrayList<CountDownLatch>();
        }
        mCountDownLatches.add(latch);
    }
    
    public void removeCountDownLatch(CountDownLatch latch) {
        if (mCountDownLatches != null) {
            mCountDownLatches.remove(latch);
        }
    }
    
    // all children execute concurrently
    public void addChild(DependencyNode node) {
        if (mChildren == null) {
            mChildren = new ArrayList<DependencyNode>();
        }
        mChildren.add(node);
        node.addParent(this);
    }

    public ArrayList<DependencyNode> getChildren() {
        return mChildren;
    }
    
    protected void addParent(DependencyNode node) {
        if (mParents == null) {
            mParents = new ArrayList<DependencyNode>();
        }
        mParents.add(node);
        mNumParents++;
    }
    
    public ArrayList<DependencyNode> getParents() {
        return mParents;
    }
    
    // public for starting the root node(s)
    public void start() {
        changeState(State.Queued);
        mExecutor.execute(this);
    }
    
    // only signals done - could be used as a root or terminus
    public void run() {
        State doneState = State.Success;
        changeState(State.Running);
        if (mParents != null) {
            for (int i = 0; i < mParents.size(); i++) {
                if (!mParents.get(i).getState().equals(State.Success)) {
                    doneState = State.DependencyFailure;
                }
            }
        }
        signalDone(doneState);
    }

    // should not be called -> use signalDone
    synchronized protected void done(DependencyNode parent) {
        if (mNumParents == 1) {
            mNumParents = mParents.size();  // reset
            start();
        } else {
            mNumParents--;
        }
    }

    protected void signalDone(State newState) {
        changeState(newState);
        if (mCountDownLatches != null) {
            for (int i = 0; i < mCountDownLatches.size(); i++) {
                mCountDownLatches.get(i).countDown();
            }
        }
        if (mChildren != null) {
            for (int i = 0; i < mChildren.size(); i++) {
                mChildren.get(i).done(this);
            }
        }
    }
    
    protected void changeState(State newState) {
        DependencyNodeListener[] listeners = null;
        State oldState = null;
        synchronized(mStateLock) {
            oldState = mState;
            mState = newState;
            if ((mListeners != null) && (mListeners.size() > 0)) {
                listeners = new DependencyNodeListener[mListeners.size()];
                listeners = mListeners.toArray(listeners);
            }
        }
        if (listeners != null) {
            for(int i = 0; i < listeners.length; i++) {
                listeners[i].onStateChange(this, oldState, newState);
            }
        }
        listeners = null;
    }
    
    public String toPathString() {
        StringBuilder bldr = new StringBuilder();
        ArrayList<String> paths = new ArrayList<String>();
        toPaths(null, paths);
        for(int i = 0; i < paths.size(); i++) {
            bldr.append(paths.get(i));
            bldr.append("\n");
        }
        return bldr.toString();
    }

    protected void toPaths(String parent, List<String> paths) {
        String me;
        if (parent == null) {
            me = mName;
        } else {
            me = parent + " -> " + mName;
        }
        if ((mChildren == null) || (mChildren.size() == 0)) {
            paths.add(me);
        } else {
            for(int i = 0; i < mChildren.size(); i++) {
                mChildren.get(i).toPaths(me, paths);
            }
        }
    }

    @Override
    public String toString() {
        return mName;
    }
}
