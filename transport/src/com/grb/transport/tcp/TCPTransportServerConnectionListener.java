package com.grb.transport.tcp;

public interface TCPTransportServerConnectionListener {
    public void onNewConnection(TCPTransportClient client);
}
