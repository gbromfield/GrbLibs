package com.grb.service.state;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.grb.util.logging.LoggingContext;

/**
 * A StateService is a utility used to manage a state machine.
 * This service:
 * <li> synchronises access to the current state.
 * <li> allows a listener to veto or allow state changes.
 * <li> allows threads to block waiting for states to occur.
 *
 * @param <S> The class used for the state value (most commonly an int or an enum).
 */
public class StateService<S> implements LoggingContext {
    private static final Log Trace = LogFactory.getLog(StateService.class);
    
    protected class WaitContext {
        public ArrayList<S> states = new ArrayList<S>();
        public ArrayBlockingQueue<S> queue = new ArrayBlockingQueue<S>(1);
        
        public String toString() {
            return states.toString();
        }
    }
    
    protected class StateChangeControllerComparator implements Comparator<StateChangeController<S>> {
        public int compare(StateChangeController<S> o1, StateChangeController<S> o2) {
            Integer first = new Integer(o1.getPriority());
            Integer second = new Integer(o2.getPriority());
            return first.compareTo(second);            
        }
    }
    
    volatile protected S mState;
    protected Object mStateChangeLock;
    protected ArrayList<StateChangeCallback<S>> mCallbacks;
    protected TreeSet<StateChangeController<S>> mControllers;
    protected ArrayList<StateChangeListener<S>> mListeners;
    protected LinkedList<WaitContext> mWaitContexts;
    protected LoggingContext mLoggingCtx;
    protected Executor mStateChangeListenerExecutor;
    
    public StateService(S initialState) {
        this(initialState, null, (StateServiceHandler<S>[])null);
    }

    public StateService(S initialState, Executor stateChangeListenerExecutor) {
        this(initialState, stateChangeListenerExecutor, (StateServiceHandler<S>[])null);
    }

    /**
     * Constructs a new state service for the management of states.
     * 
     * @param initialState The initial state value.
     * @param stateChangeListenerExecutor The executor used for post state change events.
     * @param listener The listener that listens to state changes.
     */
    public StateService(S initialState, Executor stateChangeListenerExecutor, StateServiceHandler<S>... handlers) {
        mState = initialState;
        mStateChangeListenerExecutor = stateChangeListenerExecutor;
        mStateChangeLock = new Object();
        mCallbacks = null;
        mControllers = null;
        mListeners = null;
        if (handlers != null) {
            for(int i = 0; i < handlers.length; i++) {
                addHandler(handlers[i]);
            }
        }
        mWaitContexts = new LinkedList<WaitContext>();
        mLoggingCtx = this;
    }

    /**
     * 
     * @param handler
     */
    public void addHandler(StateServiceHandler<S> handler) {
        synchronized(mStateChangeLock) {
            if (handler instanceof StateChangeCallback) {
                if (mCallbacks == null) {
                    mCallbacks = new ArrayList<StateChangeCallback<S>>();
                }
                mCallbacks.add((StateChangeCallback<S>)handler);
            }
            if (handler instanceof StateChangeController) {
                if (mControllers == null) {
                    mControllers = new TreeSet<StateChangeController<S>>(new StateChangeControllerComparator());
                }
                mControllers.add((StateChangeController<S>)handler);
            }
            if (handler instanceof StateChangeListener) {
                if (mStateChangeListenerExecutor == null) {
                    throw new IllegalStateException("A stateChangeListenerExecutor is needed to dispatch events");
                }
                if (mListeners == null) {
                    mListeners = new ArrayList<StateChangeListener<S>>();
                }
                mListeners.add((StateChangeListener<S>)handler);
            }
        }
    }

    /**
     * 
     * @param handler
     */
    public void removeHandler(StateServiceHandler<S> handler) {
        synchronized(mStateChangeLock) {
            if (handler instanceof StateChangeCallback) {
                if (mCallbacks != null) {
                    mCallbacks.remove((StateChangeCallback<S>)handler);
                }
            } 
            if (handler instanceof StateChangeController) {
                if (mControllers != null) {
                    mControllers.remove((StateChangeController<S>)handler);
                }
            }
            if (handler instanceof StateChangeListener) {
                if (mListeners != null) {
                    mListeners.remove((StateChangeListener<S>)handler);
                }
            }
        }
    }

    /**
     * Sets the logging context to be used in log messages.
     * 
     * @param loggingCtx Logging context to be used.
     */
    public void setLoggingContext(LoggingContext loggingCtx) {
        mLoggingCtx = loggingCtx;
    }
        
    /**
     * Gets the current state.
     * 
     * @return The current state.
     */
    public S getState() {
        synchronized(mStateChangeLock) {
            return mState;
        }
    }

    /**
     * Gets the state change lock.
     * 
     * @return The state change lock
     */
    public Object getStateChangeLock() {
        return mStateChangeLock;
    }
    
    /**
     * Changes the state to the new state.
     * Does not call the listeners if the new state equals the current state.
     * <p>
     * Calls the listener preChange() callback after acquiring the state lock.
     * The preChange() callback can return false or throw to veto the change.
     * Any throw from the preChange() callback will be thrown back to the caller
     * of this method. 
     * <p>
     * If a change has not been vetoed, all threads waiting on the new state are 
     * signalled and the postChange() callback is called without the state lock.
     * 
     * @param newState New state to transition to.
     */
    public void changeStateNoThrow(S newState) {
        changeStateNoThrow(newState, null);
    }

    /**
     * Changes the state to the new state.
     * Does not call the listeners if the new state equals the current state.
     * <p>
     * Calls the listener preChange() callback after acquiring the state lock.
     * The preChange() callback can return false or throw to veto the change.
     * Any throw from the preChange() callback will be thrown back to the caller
     * of this method. 
     * <p>
     * If a change has not been vetoed, all threads waiting on the new state are 
     * signalled and the postChange() callback is called without the state lock.
     * 
     * @param newState New state to transition to.
     * @param userData User data to pass to the state change listener.
     */
    public void changeStateNoThrow(S newState, final Object userData) {
        changeStateNoThrow(newState, userData, (StateServiceHandler<S>[])null);
    }

    /**
     * Changes the state to the new state.
     * Does not call the listeners if the new state equals the current state.
     * <p>
     * Calls the listener preChange() callback after acquiring the state lock.
     * The preChange() callback can return false or throw to veto the change.
     * Any throw from the preChange() callback will be thrown back to the caller
     * of this method. 
     * <p>
     * If a change has not been vetoed, all threads waiting on the new state are 
     * signalled and the postChange() callback is called without the state lock.
     * 
     * @param newState New state to transition to.
     * @param userData User data to pass to the state change listener.
     * @param handlers Controllers or listeners for the state change. 
     */
    public void changeStateNoThrow(S newState, final Object userData, StateServiceHandler<S>... handlers) {
        try {
            changeState(newState, userData, handlers);
        } catch(Exception e) {
            if (mLoggingCtx.getLog().isErrorEnabled()) {
                mLoggingCtx.getLog().error(mLoggingCtx.formatLog("Error changing state"), e);
            }
        }
    }

    /**
     * Changes the state to the new state.
     * Does not call the listeners if the new state equals the current state.
     * <p>
     * Calls the listener preChange() callback after acquiring the state lock.
     * The preChange() callback can return false or throw to veto the change.
     * Any throw from the preChange() callback will be thrown back to the caller
     * of this method. 
     * <p>
     * If a change has not been vetoed, all threads waiting on the new state are 
     * signalled and the postChange() callback is called without the state lock.
     * 
     * @param newState New state to transition to.
     * 
     * @throws Exception Exception thrown from the preChange() callback.
     */
    public void changeState(S newState) throws Exception {
        changeState(newState, null);
    }

    /**
     * Changes the state to the new state.
     * Does not call the listeners if the new state equals the current state.
     * <p>
     * Calls the listener preChange() callback after acquiring the state lock.
     * The preChange() callback can return false or throw to veto the change.
     * Any throw from the preChange() callback will be thrown back to the caller
     * of this method. 
     * <p>
     * If a change has not been vetoed, all threads waiting on the new state are 
     * signalled and the postChange() callback is called without the state lock.
     * 
     * @param newState New state to transition to.
     * @param userData User data to pass to the state change listener.
     * 
     * @throws Exception Exception thrown from the preChange() callback.
     */
    public void changeState(S newState, final Object userData) throws Exception {
        changeState(newState, userData, (StateServiceHandler<S>[])null);
    }
    
    /**
     * Changes the state to the new state.
     * Does not call the listeners if the new state equals the current state.
     * <p>
     * Calls the listener preChange() callback after acquiring the state lock.
     * The preChange() callback can return false or throw to veto the change.
     * Any throw from the preChange() callback will be thrown back to the caller
     * of this method. 
     * <p>
     * If a change has not been vetoed, all threads waiting on the new state are 
     * signalled and the postChange() callback is called without the state lock.
     * 
     * @param newState New state to transition to.
     * @param userData User data to pass to the state change listener.
     * @param scController Controller for the state change. 
     * 
     * @throws Exception Exception thrown from the preChange() callback.
     */
    public void changeState(S newState, final Object userData, StateServiceHandler<S>... handlers) throws Exception {
        S oldState;
        synchronized(mStateChangeLock) {
            if (mState == newState) {
                if (mLoggingCtx.getLog().isDebugEnabled()) {
                    mLoggingCtx.getLog().debug(mLoggingCtx.formatLog("State change request to same state \"" + mState + "\""), 
                            new Exception("exception to track originator"));
                }
                return;
            }
            if (!processControllers(newState, userData, handlers)) {
                return;
            }

            oldState = mState;
            mState = newState;
            
            if (mLoggingCtx.getLog().isDebugEnabled()) {
                mLoggingCtx.getLog().debug(mLoggingCtx.formatLog(
                        "State change from \"" + oldState + "\" to \"" + 
                        newState + "\" (userData = " + userData + ")"));
            }

            ArrayList<StateChangeCallback<S>> callbacks = getCallbacks(handlers);
            processCallbacks(oldState, newState, userData, callbacks);
            
            processWaitContexts(newState);
            
            processListeners(oldState, newState, userData, mListeners);
        }
    }

    /**
     * Waits for any of the state(s) passed as parameters to occur.
     * This method takes a list of states because you normally want to be
     * defensive waiting for states. For example, if you were connecting
     * you would want to wait for state "connected" or "closed" to occur.
     * 
     * @param states The states to wait for occurrence.
     * 
     * @return The state that occurred. will be one of the passed in state parameters.
     */
    public S waitForStates(S ... states) {
        return waitForStates(0, null, states);
    }

    /**
     * Waits the given amount of time for any of the state(s) passed as 
     * parameters to occur.
     * This method takes a list of states because you normally want to be
     * defensive waiting for states. For example, if you were connecting
     * you would want to wait for state "connected" or "closed" to occur.
     * 
     * @param timeout Timeout value >0. 0 means wait forever.
     * @param unit The unit of the timeout.
     * @param states The states to wait for occurrence.
     * 
     * @return The state that occurred or null on timeout. If non-null, will be one of the passed in state parameters.
     */
    public S waitForStates(long timeout, TimeUnit unit, S ... states) {
        WaitContext ctx = null;
        synchronized(mStateChangeLock) {
            for (int i = 0; i < states.length; i++) {
                if (mState.equals(states[i])) {
                    return mState;
                }
            }
            ctx = new WaitContext();
            for (int i = 0; i < states.length; i++) {
                ctx.states.add(states[i]);                
            }
            mWaitContexts.add(ctx);
        }
        try {
            if (timeout > 0) {
                S state = ctx.queue.poll(timeout, unit);
                if (state == null) {
                    // timed out, cleanup the context
                    synchronized(mStateChangeLock) {
                        mWaitContexts.remove(ctx);
                    }
                }
                return state;
            } else {
                return ctx.queue.take();
            }
        } catch(InterruptedException e) {
            if (mLoggingCtx.getLog().isErrorEnabled()) {
                mLoggingCtx.getLog().error(mLoggingCtx.formatLog("Waiting for state interrupted"), e);
            }
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("state = ");
        bldr.append(mState);
        return bldr.toString();
    }

    public String formatLog(String msg) {
        return msg;
    }

    public Log getLog() {
        return Trace;
    }
    
    private boolean processControllers(S newState, Object userData, StateServiceHandler<S>... handlers) throws Exception {
        TreeSet<StateChangeController<S>> controllers = getControllers(handlers);
        if (controllers != null) {
            Iterator<StateChangeController<S>> it = controllers.iterator();
            while(it.hasNext()) {
                StateChangeController<S> controller = it.next();
                if (mLoggingCtx.getLog().isDebugEnabled()) {
                    try {
                        String reason = controller.onStateChangeRequested(mState, newState, userData);
                        if (reason != null) {
                            mLoggingCtx.getLog().debug(mLoggingCtx.formatLog("State change vetoed from \"" + mState + "\" to \"" + newState + "\" (" + reason + ")"));
                            return false;
                        }
                    } catch(Exception e) {
                        mLoggingCtx.getLog().debug(mLoggingCtx.formatLog("State change vetoed from \"" + mState + "\" to \"" + newState + "\" (" + e.getMessage() + ")"));
                        throw e;
                    }
                } else {
                    String reason = controller.onStateChangeRequested(mState, newState, userData);
                    if (reason != null) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void processCallbacks(S oldState, S newState, Object userData, ArrayList<StateChangeCallback<S>> callbacks) throws Exception {
        if (callbacks != null) {
            Iterator<StateChangeCallback<S>> it = callbacks.iterator();
            while(it.hasNext()) {
                StateChangeCallback<S> callback = it.next();
                callback.onStateChangeConfirmed(oldState, newState, userData);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void processListeners(final S oldState, final S newState, final Object userData, ArrayList<StateChangeListener<S>> listeners) throws Exception {
        if (listeners != null) {
            final ArrayList<StateChangeListener<S>> clone = (ArrayList<StateChangeListener<S>>)listeners.clone();
            mStateChangeListenerExecutor.execute(new Runnable() { 
                @Override
                public void run() {
                    Iterator<StateChangeListener<S>> it = clone.iterator();
                    while(it.hasNext()) {
                        StateChangeListener<S> listener = it.next();
                        listener.onStateChange(oldState, newState, userData);
                    }
                }
            });
        }
    }
    
    private void processWaitContexts(S newState) {
        int i = 0;
        while (i < mWaitContexts.size()) {
            WaitContext ctx = mWaitContexts.get(i);
            for(int j = 0; j < ctx.states.size(); j++) {
                if (newState.equals(ctx.states.get(j))) {
                    mWaitContexts.remove(i);
                    ctx.queue.add(newState);
                }
            }
            i++;
        }
    }
    
    private ArrayList<StateChangeCallback<S>> getCallbacks(StateServiceHandler<S>... handlers) {
        ArrayList<StateChangeCallback<S>> returnCallbacks = null;
        if (handlers != null) {
            for(int i = 0; i < handlers.length; i++) {
                if (handlers[i] instanceof StateChangeCallback) {
                    if (returnCallbacks == null) {
                        returnCallbacks = new ArrayList<StateChangeCallback<S>>();
                    }
                    returnCallbacks.add((StateChangeCallback<S>)handlers[i]);
                }
            }
        }
        if (returnCallbacks == null) {
            return mCallbacks;
        } else {
            if (mCallbacks != null) {
                returnCallbacks.addAll(mCallbacks);
            }
            return returnCallbacks;
        }
    }
    
    private TreeSet<StateChangeController<S>> getControllers(StateServiceHandler<S>... handlers) {
        TreeSet<StateChangeController<S>> returnSet = null;
        if (mControllers != null) {
            returnSet = new TreeSet<StateChangeController<S>>(mControllers);
        }
        if (handlers != null) {
            for(int i = 0; i < handlers.length; i++) {
                if (handlers[i] instanceof StateChangeController) {
                    if (returnSet == null) {
                        returnSet = new TreeSet<StateChangeController<S>>(new StateChangeControllerComparator());
                    }
                    returnSet.add((StateChangeController<S>)handlers[i]);
                }
            }
        }
        return returnSet;
    }
}
