package com.grb.reactor;

import java.nio.channels.SelectionKey;

public interface ServerReactorHandler extends ReactorHandler {
    // notifications from the reactor
    public void handleAccept(Reactor reactor, SelectionKey selKey);
}
