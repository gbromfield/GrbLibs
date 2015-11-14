package com.grb.service.state;

public interface StateChangeController<S> extends StateServiceHandler<S> {
    /**
     * The highest priority that a controller can be.
     */
    public static final int HIGHEST_PRIORITY = 1;

    /**
     * The lowest priority that a controller can be.
     */
    public static final int LOWEST_PRIORITY = 100;

    /**
     * Called before a state change occurs. 
     * Return null to accept the state change, a string describing 
     * the reason to veto.
     * Throw an exception to notify the caller of the veto.
     * Holds the state lock.
     * 
     * @param currentValue Current state value.
     * @param proposedValue New state value.
     * @param userData User data passed to the changeState method.
     * @return Null to accept. Any string to veto.
     * @throws Exception Vetoes the state change.
     */
    public String onStateChangeRequested(S currentState, S proposedState, Object userData) throws Exception;

    /**
     * Gets the priority of the controller to be used to calculate the 
     * calling order of the controllers. Valid values are between
     * {@link #HIGHEST_PRIORITY} and {@link #LOWEST_PRIORITY}.
     * 
     * @return This controller's priority.
     */
    public int getPriority();
}
