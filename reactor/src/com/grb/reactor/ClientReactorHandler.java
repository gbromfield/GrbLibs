package com.grb.reactor;

import java.nio.channels.SelectionKey;


public interface ClientReactorHandler extends ReactorHandler {
    public void handleConnect(Reactor reactor, SelectionKey selKey);
    public void handleRead(Reactor reactor, SelectionKey selKey);
    public void handleWrite(Reactor reactor, SelectionKey selKey);
}
