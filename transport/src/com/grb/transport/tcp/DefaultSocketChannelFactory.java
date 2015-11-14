package com.grb.transport.tcp;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SocketChannel;

public class DefaultSocketChannelFactory implements SocketChannelFactory {
    protected SocketChannelProperties mProperties;
    protected SocketChannel mSocketChannel;
    
    public DefaultSocketChannelFactory() {
        mProperties = null;
        mSocketChannel = null;
    }

    public DefaultSocketChannelFactory(SocketChannel sc) {
        mProperties = null;
        mSocketChannel = sc;
    }

    public DefaultSocketChannelFactory(SocketChannelProperties properties) {
        mProperties = properties;
        mSocketChannel = null;
    }

    public SocketChannel newSocketChannel() throws IOException {
        if (mSocketChannel != null) {
            return mSocketChannel;
        }
        SocketChannel sc = SocketChannel.open();
        if (mProperties == null) {
            sc.configureBlocking(false);
        } else {
            if (mProperties.isBlocking() != null) {
                sc.configureBlocking(mProperties.isBlocking());
            }
            Socket s = sc.socket();
            if (mProperties.getReceiveBufferSize() != null) {
                s.setReceiveBufferSize(mProperties.getReceiveBufferSize());
            }
            if (mProperties.getSendBufferSize() != null) {
                s.setSendBufferSize(mProperties.getSendBufferSize());
            }
            if (mProperties.getTCPNoDelay() != null) {
                s.setTcpNoDelay(mProperties.getTCPNoDelay());
            }
            if (mProperties.getLocalAddress() != null) {
                s.bind(mProperties.getLocalAddress());
            }
        }
        return sc;
    }
}
