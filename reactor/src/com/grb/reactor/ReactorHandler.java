package com.grb.reactor;

import java.nio.channels.SelectionKey;

/**
 * These "handle" methods throw as a convenience to the user so that they don't have to catch
 * exceptions. If a callback method throws, the handleClose() will be closed on
 * channel and selector errors, and handleException() on everything else.
 * @author Graham
 *
 */
public interface ReactorHandler {
    // notifications from the reactor
    public void handleClose(Reactor reactor, SelectionKey selKey, Throwable t);
    public void handleException(Reactor reactor, SelectionKey selKey, Throwable t);
}
