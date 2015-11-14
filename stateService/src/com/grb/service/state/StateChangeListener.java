package com.grb.service.state;

public interface StateChangeListener<S> extends StateServiceHandler<S> {

    /**
     * Called after a state change occurs.
     * Does not hold the state lock.
     * 
     * @param oldValue The old state value.
     * @param newValue The new state value.
     * @param userData User data passed to the changeState method.
     */
    public void onStateChange(S oldState, S newState, Object userData);
}
