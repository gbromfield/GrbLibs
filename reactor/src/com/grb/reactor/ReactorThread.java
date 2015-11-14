package com.grb.reactor;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ReactorThread implements Reactor, Runnable {
    static private final Log Trace = LogFactory.getLog(Reactor.class);

    static public final String DefaultThreadName = "ReactorThread";
    static public long RequestTimeoutInMS = 10000;
    
    private java.nio.channels.Selector mSelector;
    private Thread mReactorThread;
    private LinkedBlockingQueue<FutureTask<?>> mRequestQueue;
    
    public ReactorThread() throws IOException {
        this(DefaultThreadName);
    }

    public ReactorThread(String name) throws IOException {
        mSelector = java.nio.channels.Selector.open();
        mReactorThread = new Thread(this, name); 
        mRequestQueue = new LinkedBlockingQueue<FutureTask<?>>();
    }

    public Selector getSelector() {
        return mSelector;
    }

    public Thread getThread() {
        return mReactorThread;
    }
    
    public void start() {
        mReactorThread.start();
    }

    public void startAsDaemon() {
        mReactorThread.setDaemon(true);
        mReactorThread.start();
    }

    public void close() {
        if (mReactorThread.isAlive()) {
            FutureTask<Object> future = new FutureTask<Object>(
                    new Callable<Object>() {
                        public Object call() throws Exception {
                            mSelector.close();
                            return null;
                        }
                    });
            mRequestQueue.add(future);
            mSelector.wakeup();
        }
    }
    
    public void addAccept(SelectableChannel channel, ReactorHandler handler)
            throws ClosedChannelException {
        addOperation(channel, handler, SelectionKey.OP_ACCEPT);
    }

    public void addConnect(SelectableChannel channel, ReactorHandler handler)
            throws ClosedChannelException {
        addOperation(channel, handler, SelectionKey.OP_CONNECT);
    }

    public void addRead(SelectableChannel channel, ReactorHandler handler)
            throws ClosedChannelException {
        addOperation(channel, handler, SelectionKey.OP_READ);
    }

    public void addWrite(SelectableChannel channel, ReactorHandler handler)
            throws ClosedChannelException {
        addOperation(channel, handler, SelectionKey.OP_WRITE);
    }

    public void removeAccept(SelectableChannel channel, ReactorHandler handler)
            throws ClosedChannelException {
        removeOperation(channel, handler, SelectionKey.OP_ACCEPT);
    }

    public void removeConnect(SelectableChannel channel, ReactorHandler handler)
            throws ClosedChannelException {
        removeOperation(channel, handler, SelectionKey.OP_CONNECT);
    }

    public void removeRead(SelectableChannel channel, ReactorHandler handler)
            throws ClosedChannelException {
        removeOperation(channel, handler, SelectionKey.OP_READ);
    }

    public void removeWrite(SelectableChannel channel, ReactorHandler handler)
            throws ClosedChannelException {
        removeOperation(channel, handler, SelectionKey.OP_WRITE);
    }

    public void addOperation(final SelectableChannel channel,
            final ReactorHandler handler, final int operation) throws ClosedChannelException {
        checkClosed();
        checkStarted();
        if (Thread.currentThread().getName().equals(mReactorThread.getName())) {
            ReactorNoThread.addOperation(this, channel, handler, operation);
        } else {
            final Reactor reactor = this;
            FutureTask<Object> future = new FutureTask<Object>(
                    new Callable<Object>() {
                        public Object call() throws Exception {
                            ReactorNoThread.addOperation(reactor, channel, handler, operation);
                            return null;
                        }
                    });            
            performRequest(future, handler);
        }
    }

    public void removeOperation(final SelectableChannel channel,
            final ReactorHandler handler, final int operation) throws ClosedChannelException {
        checkClosed();
        checkStarted();
        if (Thread.currentThread().getName().equals(mReactorThread.getName())) {
            ReactorNoThread.removeOperation(this, channel, handler, operation);
        } else {
            final Reactor reactor = this;
            FutureTask<Object> future = new FutureTask<Object>(
                    new Callable<Object>() {
                        public Object call() throws Exception {
                            ReactorNoThread.removeOperation(reactor, channel, handler, operation);
                            return null;
                        }
                    });
            performRequest(future, handler);
        }
    }

    public void run() {
        while(true) {
            try {
                ReactorNoThread.processEvents(this);
            } catch (IOException e) {
                if (Trace.isFatalEnabled()) {
                    Trace.fatal("Event Processing Error - Closing Selector", e);
                }
                try {
                    mSelector.close();
                } catch(IOException e1) {}
                return;
            }
            // process request queue
            FutureTask<?> future = null;
            while((future = mRequestQueue.poll()) != null) {
                future.run();
            }
            if (!mSelector.isOpen()) {
                return;
            }
        }
    }
    
    protected void checkStarted() {
        if (!mReactorThread.isAlive()) {
            throw new IllegalStateException("Reactor thread not started");
        }
    }
    
    protected void checkClosed() {
        if (!mSelector.isOpen()) {
            throw new ClosedSelectorException();
        }
    }

    protected void performRequest(FutureTask<Object> future, ReactorHandler handler) throws ClosedChannelException {
        mRequestQueue.add(future);
        mSelector.wakeup();
        try {
            future.get(RequestTimeoutInMS, TimeUnit.MILLISECONDS);
        } catch(TimeoutException e) {
            // something seriously wrong
            if (Trace.isErrorEnabled()) {
                Trace.error("Request to reactor thread timed out", e);
            }
            handler.handleException(this, null, e);
        } catch(ExecutionException e) {
            if (e.getCause() instanceof ClosedChannelException) {
                throw (ClosedChannelException)e.getCause();
            } else {
                if (Trace.isErrorEnabled()) {
                    Trace.error("Request to reactor thread errored", e.getCause());
                }
                handler.handleException(this, null, e.getCause());
            }
        } catch(InterruptedException e) {
            if (Trace.isErrorEnabled()) {
                Trace.error("Request to reactor thread errored", e);
            }
            handler.handleException(this, null, e);
        }
    }
    
    @Override
    public String toString() {
        return mReactorThread.getName();
    }    
}
