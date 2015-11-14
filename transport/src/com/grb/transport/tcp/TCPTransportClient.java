package com.grb.transport.tcp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ConnectionPendingException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.grb.reactor.ClientReactorHandler;
import com.grb.reactor.Reactor;
import com.grb.service.listener.ListenerCallback;
import com.grb.service.listener.ListenerService;
import com.grb.service.state.StateChangeCallback;
import com.grb.service.state.StateChangeController;
import com.grb.service.state.StateChangeListener;
import com.grb.service.state.StateService;
import com.grb.service.timer.intervalTimer.IntervalTimer;
import com.grb.service.timer.intervalTimer.IntervalTimerTimeoutListener;
import com.grb.transport.TransportClient;
import com.grb.transport.TransportClientProperties;
import com.grb.transport.TransportClosedException;
import com.grb.transport.TransportEventListener;
import com.grb.transport.TransportException;
import com.grb.transport.TransportOperation;
import com.grb.transport.TransportReadListener;
import com.grb.transport.TransportSendResult;
import com.grb.transport.TransportState;
import com.grb.transport.TransportStateChangeEvent;
import com.grb.util.concurrent.BooleanFutureTask;
import com.grb.util.logging.LoggingContext;

public class TCPTransportClient implements TransportClient, ClientReactorHandler, 
    StateChangeController<TransportState>, StateChangeListener<TransportState>, LoggingContext {
    private static final Log Trace = LogFactory.getLog(TCPTransportClient.class);

    protected class TransportStateChangeEventUserData {
        public TransportOperation operation;
        public TransportException error;
        public TransportStateChangeEventUserData(
                TransportOperation operation,
                TransportException error) {
            this.operation = operation;
            this.error = error;
        }
        
        @Override
        public String toString() {
            StringBuilder bldr = new StringBuilder();
            bldr.append("operation=");
            bldr.append(operation);
            if (error != null) {
                bldr.append(",error=");
                bldr.append(error.getMessage());
            }
            return bldr.toString();
        }
    }
    
    protected class ConnectTimeoutListener implements IntervalTimerTimeoutListener {
        public void onIntervalTimeout() {
            final TransportException te = new TransportException("Connect Timeout", 
                    new TimeoutException());
            StateChangeController<TransportState> ctrller = new StateChangeController<TransportState>() {                
                public String onStateChangeRequested(TransportState currentState,
                        TransportState proposedState, Object userData) throws Exception {
                    if (currentState != TransportState.Connecting) {
                        return "Connect Timer";
                    }
                    return null;
                }
                
                public int getPriority() {
                    return StateChangeController.LOWEST_PRIORITY;
                }
            };
            close(TransportOperation.Connecting, te, ctrller);
        }    
    }

    protected class WriteTimeoutListener implements IntervalTimerTimeoutListener {
        final private long mMyWriteIndex;
        public WriteTimeoutListener(long writeIndex) {
            mMyWriteIndex = writeIndex;
        }
        public void onIntervalTimeout() {
            final TransportException te = new TransportException("Write Timeout", 
                    new TimeoutException());
            StateChangeController<TransportState> ctrller = new StateChangeController<TransportState>() {
                public String onStateChangeRequested(TransportState currentState,
                        TransportState proposedState, Object userData)
                        throws Exception {
                    if ((!mWriteBuffer.hasRemaining()) || (mMyWriteIndex != mWriteIndex)) {
                        return "Write Timer";
                    }
                    return null;
                }

                public int getPriority() {
                    return StateChangeController.LOWEST_PRIORITY;
                }                
            };
            close(TransportOperation.Sending, te, ctrller);
        }    
    }

    protected TCPTransportClientProperties mProperties;
    protected SocketChannel mSocketChannel;
    protected StateService<TransportState> mStateService;
    protected ListenerService<TransportEventListener, TransportStateChangeEvent> mListenerService;
    protected TransportReadListener mReadListener;
    protected ByteBuffer mReadBuffer;
    protected ByteBuffer mWriteBuffer;
    protected BooleanFutureTask mConnectFuture;
    protected BooleanFutureTask mWriteFuture;
    protected long mWriteIndex;
    protected LoggingContext mLoggingCtx;
    
    @SuppressWarnings("unchecked")
    public TCPTransportClient(TCPTransportClientProperties props) throws TransportException {
        try {
            mProperties = props;
            mSocketChannel = props.getSocketChannelFactory().newSocketChannel();
            mStateService = new StateService<TransportState>(TransportState.New, mProperties.getEventExecutor(), this);
            mStateService.setLoggingContext(this);
            mListenerService = new ListenerService<TransportEventListener, TransportStateChangeEvent>(
                    new ListenerCallback<TransportEventListener, TransportStateChangeEvent>() {
                        public void onListenerCallback(
                                TransportEventListener listener,
                                TransportStateChangeEvent event, Object userData) {
                            listener.onTransportEvent(event);
                        }
                    });
            mListenerService.setLoggingContext(this);
            mReadBuffer = null;
            mWriteBuffer = props.getWriteBuffer();
            mConnectFuture = new BooleanFutureTask();
            // if null remote address then socket factory
            // must give a connected socket.
            if (mProperties.getRemoteAddress() == null) {
                mConnectFuture.setDone();
            }
            mWriteFuture = null;
            mWriteIndex = 0;
            mLoggingCtx = this;
        } catch(IOException e) {
            throw new TransportException(e);
        }
    }

    public InetSocketAddress getLocalAddress() {
        return (InetSocketAddress)mSocketChannel.socket().getLocalSocketAddress();
    }

    public InetSocketAddress getRemoteAddress() {
        return (InetSocketAddress)mSocketChannel.socket().getRemoteSocketAddress();
    }

    public boolean isDisconnected() {
    	return mStateService.getState().equals(TransportState.Disconnected);
    }
    
    /**
     * Sets the logging context to be used in log messages.
     * 
     * @param loggingCtx Logging context to be used.
     */
    public void setLoggingContext(LoggingContext loggingCtx) {
        mLoggingCtx = loggingCtx;
    }

    public void addEventListener(TransportEventListener listener)
            throws TransportException {
        mListenerService.addListener(listener);
    }

    public void close() {
        close(TransportOperation.Closing, null);
    }

    public void close(TransportOperation operation, TransportException error) {
        close(operation, error, null);
    }
    
    /**
     * Close should never be called with mWriteLock locked.
     * @param event
     */
    @SuppressWarnings("unchecked")
    public void close(TransportOperation operation, TransportException error, 
            StateChangeController<TransportState> ctrller) {
        final TransportStateChangeEventUserData event = new TransportStateChangeEventUserData(operation, error);
        mStateService.changeStateNoThrow(TransportState.Disconnected, event,
                new StateChangeCallback<TransportState>() {
                    public void onStateChangeConfirmed(TransportState oldState, TransportState newState,
                            Object userData) {
                        try {
                            mSocketChannel.close();
                        } catch(IOException e) {}

                        if (event.error == null) {
                            mConnectFuture.setDone();   // do we need some indication that it was done by application close
                        } else {
                            mConnectFuture.setDone(event.error);
                        }
                        
                        if (mWriteFuture != null) {
                            if (event.error == null) {
                                mWriteFuture.setDone(new TransportClosedException("Transport closed by application"));
                            } else {
                                mWriteFuture.setDone(event.error);
                            }
                        }
                    }
                }, ctrller);
    }
    
    public void connect(int timeout, TimeUnit unit) throws TransportException {
        Future<Boolean> future = connectAsync(timeout, unit);        
        try {
            future.get();
        } catch(ExecutionException e) {
            throw (TransportException)e.getCause();
        } catch(InterruptedException e) {
            throw new TransportException("Error connecting", e);
        }
    }

    public Future<Boolean> connectAsync(int timeout, TimeUnit unit) throws TransportException {
        // if null remote address then socket factory
        // must give a connected socket.
        if (mProperties.getRemoteAddress() == null) {
            if (!mSocketChannel.isOpen()) {
                throw new TransportClosedException();
            }
            mStateService.changeStateNoThrow(TransportState.Connected, 
                    new TransportStateChangeEventUserData(
                            TransportOperation.Connecting, null));
            mConnectFuture.setDone();
            return mConnectFuture;
        }
        try {
            if (mSocketChannel.connect(mProperties.getRemoteAddress())) {
                mStateService.changeStateNoThrow(TransportState.Connecting, 
                        new TransportStateChangeEventUserData(
                                TransportOperation.Connecting, null));
                mStateService.changeStateNoThrow(TransportState.Connected, 
                    new TransportStateChangeEventUserData(
                            TransportOperation.Connecting, null));
                mConnectFuture.setDone();
            } else {
                mStateService.changeStateNoThrow(TransportState.Connecting, 
                        new TransportStateChangeEventUserData(
                                TransportOperation.Connecting, null));
                IntervalTimer.GetIntervalTimer(timeout, unit).schedule(new ConnectTimeoutListener());
                mProperties.getReactor().addConnect(mSocketChannel, this);                
            }
        } catch(AlreadyConnectedException e) {
            mConnectFuture.setDone();
        } catch(ConnectionPendingException e) {
            // do nothing
        } catch(IOException e) {
            final TransportException te = new TransportException("Error Connecting", e);
            close(TransportOperation.Connecting, te, null);
            throw te;
        }
        return mConnectFuture;
    }

    public TransportClientProperties getProperties() throws TransportException {
        return mProperties;
    }

    public void removeEventListener(TransportEventListener listener)
            throws TransportException {
        mListenerService.removeListener(listener);
    }

    public void startReading(TransportReadListener listener, ByteBuffer buffer)
            throws TransportException {
        mReadListener = listener;
        mReadBuffer = buffer;
        try {
            mProperties.getReactor().addRead(mSocketChannel, this);
        } catch (ClosedChannelException e) {
            throw new TransportClosedException(e);
        }
    }

    public void handleClose(Reactor reactor, SelectionKey selKey, Throwable t) {
        // do nothing - do closes in handlers
    }

    public void handleException(Reactor reactor, SelectionKey selKey,
            Throwable t) {
        close(TransportOperation.Unknown, new TransportException(t));
    }

    public void handleConnect(Reactor reactor, SelectionKey selKey) {
        SocketChannel channel = (SocketChannel)selKey.channel();
        try {
            if (!channel.finishConnect()) {
                // this should never happen
                return;
            }
            reactor.removeConnect(channel, this);
            mStateService.changeStateNoThrow(TransportState.Connected, 
                    new TransportStateChangeEventUserData(
                            TransportOperation.Connecting, null));
            mConnectFuture.setDone();
        } catch(IOException e) {
            selKey.cancel();
            close(TransportOperation.Connecting, 
                    new TransportException("Error connecting", e));
        }
    }

    public void handleRead(Reactor reactor, SelectionKey selKey) {
        try {
            SocketChannel channel = (SocketChannel)selKey.channel();
            int numBytesRead = channel.read(mReadBuffer);
            mReadBuffer.flip();
            if (!mReadListener.onTransportRead(mReadBuffer, numBytesRead)) {
                reactor.removeRead(channel, this);
            }
        } catch (IOException e) {
            selKey.cancel();
            close(TransportOperation.Reading, 
                    new TransportException("Error reading", e));
        }
    }

    public void handleWrite(Reactor reactor, SelectionKey selKey) {
        if (mLoggingCtx.getLog().isDebugEnabled()) {
            mLoggingCtx.getLog().debug(mLoggingCtx.formatLog("In handleWrite()"));
        }
        SocketChannel channel = (SocketChannel)selKey.channel();
        try {
            synchronized(mStateService.getStateChangeLock()) {
                channel.write(mWriteBuffer);
                if (!mWriteBuffer.hasRemaining()) {
                    mWriteFuture.setDone();
                    mWriteFuture = null;
                    mProperties.mReactor.removeWrite(channel, this);
                }
            }
        } catch(IOException e) {
            selKey.cancel();
            close(TransportOperation.Sending, 
                    new TransportException("Error writing", e));
        }
    }

    public TransportStateChangeEvent createStateChangeEvent(TransportState oldState, TransportState newState,
            Object userData) {
        TransportStateChangeEventUserData scUserData = (TransportStateChangeEventUserData)userData;
        return new TransportStateChangeEvent(this, oldState, newState, 
                scUserData.operation, scUserData.error);
    }
    
    public void write(ByteBuffer buffer) throws TransportException {
        write(buffer, mProperties.getWriteTimeout(), mProperties.getWriteTimeoutUnit());
    }

    /**
     * To get the maximum performance it is better to call 
     * the async interface directly and reuse the TransportSendResult.
     */
    public void write(ByteBuffer buffer, int timeout, TimeUnit unit)
            throws TransportException {
        TransportSendResult result = new TransportSendResult();
        int numWritten = writeAsync(buffer, timeout, unit, result);
        processSendResult(numWritten, result);
    }

    
    public int writeAsync(ByteBuffer buffer, TransportSendResult result)
            throws TransportException {
        return writeAsync(buffer, mProperties.getWriteTimeout(), mProperties.getWriteTimeoutUnit(), result);
    }

    /**
     * Buffer must be pre-flipped
     */
    public int writeAsync(ByteBuffer buffer, int timeout,
            TimeUnit unit, TransportSendResult result) throws TransportException {
        try {
            synchronized(mStateService.getStateChangeLock()) {
                if (mWriteFuture == null) {
                    result.setFuture(null);
                } else {
                    result.setFuture(mWriteFuture);
                    return -1;
                }
                int numWritten = mSocketChannel.write(buffer);
                if (buffer.hasRemaining()) {
                    mWriteFuture = new BooleanFutureTask();
                    result.setFuture(mWriteFuture);
                    mWriteBuffer.clear();
                    mWriteBuffer.put(buffer);
                    mWriteBuffer.flip();
                    mProperties.getReactor().addWrite(mSocketChannel, this); 
                    mWriteIndex++;
                    IntervalTimer.GetIntervalTimer(timeout, unit).schedule(new WriteTimeoutListener(mWriteIndex));
                }
                return numWritten;
            }
        } catch(ClosedChannelException e) {
            throw new TransportClosedException(e);
        } catch (IOException e) {
            throw new TransportException(e);
        }
    }

    
    public void write(ByteBuffer[] buffers) throws TransportException {
        write(buffers, mProperties.getWriteTimeout(), mProperties.getWriteTimeoutUnit());
    }

    public void write(ByteBuffer[] buffers, int timeout, TimeUnit unit)
        throws TransportException {
        TransportSendResult result = new TransportSendResult();
        long numWritten = writeAsync(buffers, timeout, unit, result);
        processSendResult(numWritten, result);
    }

    
    public long writeAsync(ByteBuffer[] buffers, TransportSendResult result)
            throws TransportException {
        return writeAsync(buffers, mProperties.getWriteTimeout(), mProperties.getWriteTimeoutUnit(), result);
    }

    public long writeAsync(ByteBuffer[] buffers, int timeout,
            TimeUnit unit, TransportSendResult result) throws TransportException {
        return writeAsync(buffers, 0, buffers.length, timeout, unit, result);
    }

    
    public void write(ByteBuffer[] buffers, int offset, int length)
            throws TransportException {
        write(buffers, offset, length, mProperties.getWriteTimeout(), mProperties.getWriteTimeoutUnit());
    }

    public void write(ByteBuffer[] buffers, int offset, int length,
            int timeout, TimeUnit unit)
        throws TransportException {
        TransportSendResult result = new TransportSendResult();
        long numWritten = writeAsync(buffers, offset, length, timeout, unit, result);
        processSendResult(numWritten, result);
    }

    
    public long writeAsync(ByteBuffer[] buffers, int offset, int length,
            TransportSendResult result) throws TransportException {
        return writeAsync(buffers, offset, length, mProperties.getWriteTimeout(), mProperties.getWriteTimeoutUnit(), result);
    }

    /**
     * 
     * @param buffers
     * @param offset
     * @param length
     * @param timeout
     * @param unit
     * @param result
     * @return -1 If blocked on previous write, else bytes written
     * @throws TransportException
     */
    public long writeAsync(ByteBuffer[] buffers, int offset,
            int length, int timeout,
            TimeUnit unit, TransportSendResult result) throws TransportException {
        try {
            synchronized(mStateService.getStateChangeLock()) {
                if (mWriteFuture == null) {
                    result.setFuture(null);
                } else {
                    result.setFuture(mWriteFuture);
                    return -1;
                }
                for (int i = offset; i < (offset + length); i++) {
                    buffers[i].flip();
                }
                long numWritten = mSocketChannel.write(buffers, offset, length);
                boolean init = false;
                for (int i = offset; i < (offset + length); i++) {
                    if (buffers[i].hasRemaining()) {
                        if (!init) {
                            mWriteFuture = new BooleanFutureTask();
                            result.setFuture(mWriteFuture);
                            mWriteBuffer.clear();
                            init = true;
                        }
                        mWriteBuffer.put(buffers[i]);
                    }
                }
                if (init) {
                    mWriteBuffer.flip();
                    mProperties.getReactor().addWrite(mSocketChannel, this); 
                    mWriteIndex++;
                    IntervalTimer.GetIntervalTimer(timeout, unit).schedule(new WriteTimeoutListener(mWriteIndex));
                }
                return numWritten;
            }
        } catch(ClosedChannelException e) {
            throw new TransportClosedException(e);
        } catch (IOException e) {
            throw new TransportException(e);
        }
    }

    public String formatLog(String msg) {
        InetAddress addr = null;
        if (mProperties.getRemoteAddress() == null) {
            if (mSocketChannel.socket().getInetAddress() != null) {
                addr = mSocketChannel.socket().getInetAddress();
            }
        } else {
            addr = mProperties.getRemoteAddress().getAddress();
        }
        if (addr == null) {
            return msg;
        }
        return addr.toString() + " - " + msg;
    }

    public Log getLog() {
        return Trace;
    }

    public String onStateChangeRequested(TransportState currentState,
            TransportState proposedState, Object userData) throws Exception {
        if (currentState.equals(TransportState.Disconnected)) {
            return "Already Disconnected";
        }
        return null;
    }

    
    public int getPriority() {
        return StateChangeController.HIGHEST_PRIORITY;
    }

    public void onStateChange(TransportState oldState, TransportState newState,
            Object userData) {
        TransportStateChangeEventUserData scUserData = (TransportStateChangeEventUserData)userData;
        TransportStateChangeEvent event = new TransportStateChangeEvent(this, oldState, newState, 
                scUserData.operation, scUserData.error);
        mListenerService.notifyListeners(event);
    }

    public void stopReading() throws TransportException {
        try {
            mProperties.getReactor().removeRead(mSocketChannel, this);
        } catch (ClosedChannelException e) {
            throw new TransportClosedException(e);
        }
    }

    @Override
    public String toString() {
        InetAddress addr = null;
        if (mProperties.getRemoteAddress() == null) {
            if (mSocketChannel.socket().getInetAddress() != null) {
                addr = mSocketChannel.socket().getInetAddress();
            }
        } else {
            addr = mProperties.getRemoteAddress().getAddress();
        }
        if (addr == null) {
            return null;
        }
        return addr.toString();
    }

    private void processSendResult(long numWritten, TransportSendResult result) throws TransportException {
        if (result.getFuture() != null) {
            if (numWritten == -1) {
                throw new TransportException("blocking send performed when previous send not complete");
            }
            try {
                result.getFuture().get();
            } catch (InterruptedException e) {
                throw new TransportException(e);
            } catch (ExecutionException e) {
                if (e.getCause() instanceof TransportException) {
                    throw (TransportException)e.getCause();
                }
                throw new TransportException(e);
            }
        }
    }
}
