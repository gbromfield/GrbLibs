package com.grb.transport.tcp;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.grb.reactor.Reactor;
import com.grb.transport.TransportClientProperties;

public class TCPTransportClientProperties implements TransportClientProperties {
    protected InetSocketAddress mRemoteAddress;
    protected ExecutorService mEventExecutor;
    protected Reactor mReactor;
    protected SocketChannelFactory mSCFactory;
    protected ByteBuffer mWriteBuffer;
    protected int mDefaultWriteTimeout;
    protected TimeUnit mDefaultWriteTimeoutUnit;

    public TCPTransportClientProperties(
            InetSocketAddress remoteAddress, 
            ExecutorService eventExecutor,
            Reactor reactor, 
            SocketChannelFactory scFactory,
            ByteBuffer writeBuffer,
            int defaultWriteTimeout,
            TimeUnit defaultWriteTimeoutUnit) {
        mRemoteAddress = remoteAddress;
        mEventExecutor = eventExecutor;
        mReactor = reactor;
        mSCFactory = scFactory;
        mWriteBuffer = writeBuffer;
        mDefaultWriteTimeout = defaultWriteTimeout;
        mDefaultWriteTimeoutUnit = defaultWriteTimeoutUnit;
    }

    public InetSocketAddress getRemoteAddress() {
        return mRemoteAddress;
    }

    public ExecutorService getEventExecutor() {
        return mEventExecutor;
    }
    
    public Reactor getReactor() {
        return mReactor;
    }
    
    public SocketChannelFactory getSocketChannelFactory() {
        return mSCFactory;
    }
    
    public ByteBuffer getWriteBuffer() {
        return mWriteBuffer;
    }
    
    public int getWriteTimeout() {
        return mDefaultWriteTimeout;
    }

    public TimeUnit getWriteTimeoutUnit() {
        return mDefaultWriteTimeoutUnit;
    }
}
