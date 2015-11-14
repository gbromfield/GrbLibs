package com.grb.transport;

public interface TransportServer {
    public void close();
    public TransportServerProperties getProperties() throws TransportException;
    public void startAccepting() throws TransportException;
    public void stopAccepting() throws TransportException;
}
