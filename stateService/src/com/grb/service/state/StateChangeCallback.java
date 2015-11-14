package com.grb.service.state;

public interface StateChangeCallback<S> extends StateServiceHandler<S> {
    /**
     * Called after a state change occurs.
     * Holds the state lock.
     * 
     * @param oldValue The old state value.
     * @param newValue The new state value.
     * @param userData User data passed to the changeState method.
     */
    public void onStateChangeConfirmed(S oldState, S newState, Object userData);
}
