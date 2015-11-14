package com.grb.reactor;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class ReactorNoThread implements Reactor {
    private static final Log Trace = LogFactory.getLog(ReactorNoThread.class);
    
    protected Selector mSelector;
    
    public void processEvents() throws IOException {
        // Wait for an event
        if (mSelector.select() > 0) {
            processSelector(this);
        }
    }

    public void processEvents(long timeout) throws IOException {
        // Wait for an event
        if (mSelector.select(timeout) > 0) {
            processSelector(this);
        }
    }
    
    public void addAccept(SelectableChannel channel, ReactorHandler handler)
            throws ClosedChannelException {
        addOperation(this, channel, handler, SelectionKey.OP_ACCEPT);
    }

    public void addConnect(SelectableChannel channel, ReactorHandler handler)
            throws ClosedChannelException {
        addOperation(this, channel, handler, SelectionKey.OP_CONNECT);
    }

    public void addRead(SelectableChannel channel, ReactorHandler handler)
            throws ClosedChannelException {
        addOperation(this, channel, handler, SelectionKey.OP_READ);
    }

    public void addWrite(SelectableChannel channel, ReactorHandler handler)
            throws ClosedChannelException {
        addOperation(this, channel, handler, SelectionKey.OP_WRITE);
    }

    public void close() throws IOException {
        mSelector.close();
    }

    public void removeAccept(SelectableChannel channel, ReactorHandler handler)
            throws ClosedChannelException {
        removeOperation(this, channel, handler, SelectionKey.OP_ACCEPT);
    }

    public void removeConnect(SelectableChannel channel, ReactorHandler handler)
            throws ClosedChannelException {
        removeOperation(this, channel, handler, SelectionKey.OP_CONNECT);
    }

    public void removeRead(SelectableChannel channel, ReactorHandler handler)
            throws ClosedChannelException {
        removeOperation(this, channel, handler, SelectionKey.OP_READ);
    }

    public void removeWrite(SelectableChannel channel, ReactorHandler handler)
            throws ClosedChannelException {
        removeOperation(this, channel, handler, SelectionKey.OP_WRITE);
    }

    public Selector getSelector() {
        return mSelector;
    }

    static public void addOperation(Reactor reactor, SelectableChannel channel, ReactorHandler handler, int operation) throws ClosedChannelException  {
        Selector selector = reactor.getSelector();
        int interestOps = 0;
        SelectionKey selKey = channel.keyFor(selector);
        if (selKey == null) {
            interestOps = operation;
        } else {
            interestOps = selKey.interestOps() | operation;                    
        }
        channel.register(selector, interestOps, handler);
    }

    static public void removeOperation(Reactor reactor, SelectableChannel channel, ReactorHandler handler, int operation) throws ClosedChannelException {
        Selector selector = reactor.getSelector();
        SelectionKey selKey = channel.keyFor(selector);
        if (selKey == null) {
            if (channel.isOpen()) {
                if (Trace.isInfoEnabled()) {
                    Trace.info("No SelectionKey for remove operation");
                }
            } else {
                throw new ClosedChannelException();
            }
        } else {
            int interestOps = selKey.interestOps() & ~operation;
            channel.register(selector, interestOps, handler);
        }
    }

    static public void processEvents(Reactor reactor) throws IOException {
        // Wait for an event
        if (reactor.getSelector().select() > 0) {
            processSelector(reactor);
        }
    }

    static public void processEvents(Reactor reactor, long timeout) throws IOException {
        // Wait for an event
        if (reactor.getSelector().select(timeout) > 0) {
            processSelector(reactor);
        }
    }
    
    static public void processSelector(Reactor reactor) {
        // Get list of selection keys with pending events
        Iterator<SelectionKey> it = reactor.getSelector().selectedKeys().iterator();

        // Process each key at a time
        while (it.hasNext()) {
            // Get the selection key
            SelectionKey selKey = it.next();

            // Remove it from the list to indicate that it is being processed
            it.remove();
            
            ReactorHandler handler = (ReactorHandler)selKey.attachment();
            if (handler != null) {
                try {
                    if (selKey.isValid() && selKey.isAcceptable()) {
                        ((ServerReactorHandler)handler).handleAccept(reactor, selKey);
                    }                
                    if (selKey.isValid() && selKey.isConnectable()) {
                        ((ClientReactorHandler)handler).handleConnect(reactor, selKey);
                    }
                    if (selKey.isValid() && selKey.isReadable()) {
                        ((ClientReactorHandler)handler).handleRead(reactor, selKey);
                    }
                    if (selKey.isValid() && selKey.isWritable()) {
                        ((ClientReactorHandler)handler).handleWrite(reactor, selKey);
                    }
                    // This check should be last to support keys that are cancelled in callbacks
                    if (!selKey.isValid()) {
                        handler.handleClose(reactor, selKey, null);
                    }
                } catch(Throwable t) {
                    handler.handleException(reactor, selKey, t);
                }
            }
        }
    }
}
