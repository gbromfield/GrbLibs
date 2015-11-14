package com.grb.transport;

import java.nio.ByteBuffer;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


/**
 * <p> no statistics gathering here - the number of bytes sent and received is known at the higher layer
 */
public interface TransportClient {
    public TransportClientProperties getProperties() throws TransportException;

    public void addEventListener(TransportEventListener listener) throws TransportException;
    public void removeEventListener(TransportEventListener listener) throws TransportException;

    public void connect(int timeout, TimeUnit unit) throws TransportException;
    public Future<Boolean> connectAsync(int timeout, TimeUnit unit) throws TransportException;

    public void close();

    public void startReading(TransportReadListener listener, ByteBuffer buffer) throws TransportException;
    public void stopReading() throws TransportException;
        
    public void write(ByteBuffer buffer) throws TransportException;
    public void write(ByteBuffer buffer, int timeout, TimeUnit unit) throws TransportException;

    public int writeAsync(ByteBuffer buffer, TransportSendResult result) throws TransportException;
    public int writeAsync(ByteBuffer buffer, int timeout, TimeUnit unit, TransportSendResult result) throws TransportException;

    public void write(ByteBuffer[] buffers) throws TransportException;
    public void write(ByteBuffer[] buffers, int timeout, TimeUnit unit) throws TransportException;

    public long writeAsync(ByteBuffer[] buffers, TransportSendResult result) throws TransportException;
    public long writeAsync(ByteBuffer[] buffers, int timeout, TimeUnit unit, TransportSendResult result) throws TransportException;

    public void write(ByteBuffer[] buffers, int offset, int length) throws TransportException;
    public void write(ByteBuffer[] buffers, int offset, int length, int timeout, TimeUnit unit) throws TransportException;

    public long writeAsync(ByteBuffer[] buffers, int offset, int length, TransportSendResult result) throws TransportException;
    public long writeAsync(ByteBuffer[] buffers, int offset, int length, int timeout, TimeUnit unit, TransportSendResult result) throws TransportException;
}
