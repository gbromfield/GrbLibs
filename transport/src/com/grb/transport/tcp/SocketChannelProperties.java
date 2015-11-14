package com.grb.transport.tcp;

import java.net.InetSocketAddress;

public class SocketChannelProperties {
    protected Boolean mIsBlocking;
    protected InetSocketAddress mLocalAddress;
    protected Boolean mTCPNoDelay;
    protected Integer mReceiveBufferSize;
    protected Integer mSendBufferSize;
    
    public SocketChannelProperties(
            Boolean isBlocking,
            InetSocketAddress localAddress,
            Boolean tcpNoDelay, 
            Integer receiveBufferSize,
            Integer sendBufferSize) {
        mIsBlocking = isBlocking;
        mLocalAddress = localAddress;
        mTCPNoDelay = tcpNoDelay;
        mReceiveBufferSize = receiveBufferSize;
        mSendBufferSize = sendBufferSize;
    }

    public Boolean isBlocking() {
        return mIsBlocking;
    }
    
    public InetSocketAddress getLocalAddress() {
        return mLocalAddress;
    }

    public Boolean getTCPNoDelay() {
        return mTCPNoDelay;
    }

    public Integer getReceiveBufferSize() {
        return mReceiveBufferSize;
    }

    public Integer getSendBufferSize() {
        return mSendBufferSize;
    }
}
