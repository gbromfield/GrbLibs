package com.grb.transport.tcp;

import com.grb.reactor.Reactor;
import com.grb.transport.TransportServerProperties;

public class TCPTransportServerProperties implements TransportServerProperties {

    protected String mAddr;
    protected int mPort;
    protected Reactor mReactor;
    protected TCPTransportClientProperties mClientProps;

    public TCPTransportServerProperties(
            int port,
            Reactor reactor,
            TCPTransportClientProperties clientProps) {
        this(null, port, reactor, clientProps);
    }

    public TCPTransportServerProperties(
            String addr,
            int port,
            Reactor reactor,
            TCPTransportClientProperties clientProps) {
        mAddr = addr;
        mPort = port;
        mReactor = reactor;
        mClientProps = clientProps;
    }

    public String getAddr() {
        return mAddr;
    }
    
    public int getPort() {
        return mPort;
    }
    
    public Reactor getReactor() {
        return mReactor;
    }
    
    public TCPTransportClientProperties getClientProperties() {
        return mClientProps;
    }
}
