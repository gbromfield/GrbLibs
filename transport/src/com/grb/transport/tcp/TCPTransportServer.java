package com.grb.transport.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.grb.reactor.Reactor;
import com.grb.reactor.ServerReactorHandler;
import com.grb.transport.TransportClosedException;
import com.grb.transport.TransportException;
import com.grb.transport.TransportServerProperties;
import com.grb.util.logging.LoggingContext;

public class TCPTransportServer implements com.grb.transport.TransportServer, ServerReactorHandler, LoggingContext {
    private static final Log Trace = LogFactory.getLog(TCPTransportServer.class);
    
    protected TCPTransportServerProperties mProperties;
    protected TCPTransportServerConnectionListener mListener;
    protected ServerSocketChannel mServerChannel;
    protected LoggingContext mLoggingCtx;
    
    /**
     * Blocking Mode
     * 
     * @param props
     * @throws TransportException
     */
    public TCPTransportServer(TCPTransportServerProperties props) throws TransportException {
        this(props, null);
    }

    /**
     * Non-Blocking Mode
     * 
     * @param props
     * @param listener
     * @throws TransportException
     */
    public TCPTransportServer(TCPTransportServerProperties props, 
            TCPTransportServerConnectionListener listener) throws TransportException {
        try {
            mProperties = props;
            mListener = listener;
            mServerChannel = ServerSocketChannel.open();
            String bindAddr = mProperties.getAddr();
            SocketAddress addr;
            if (bindAddr == null) {
                addr = new InetSocketAddress(mProperties.getPort()) ;
            } else {
                addr = new InetSocketAddress(bindAddr, mProperties.getPort()) ;
            }
            mServerChannel.socket().bind(addr);
            mServerChannel.configureBlocking(false);
            mLoggingCtx = this;
        } catch(IOException e) {
            throw new TransportException(e);
        }
    }
    
    /**
     * Sets the logging context to be used in log messages.
     * 
     * @param loggingCtx Logging context to be used.
     */
    public void setLoggingContext(LoggingContext loggingCtx) {
        mLoggingCtx = loggingCtx;
    }

    public void close() {
        try {
            mServerChannel.close();
        } catch (IOException e) {
            if (mLoggingCtx.getLog().isErrorEnabled()) {
                mLoggingCtx.getLog().error(mLoggingCtx.formatLog("Error closing"), e);
            }
        }
    }

    public TransportServerProperties getProperties() throws TransportException {
        return mProperties;
    }

    public void startAccepting() throws TransportException {
        try {
            mProperties.getReactor().addAccept(mServerChannel, this);
        } catch (ClosedChannelException e) {
            throw new TransportClosedException(e);
        }        
    }

    public void stopAccepting() throws TransportException {
        try {
            mProperties.getReactor().removeAccept(mServerChannel, this);
        } catch (ClosedChannelException e) {
            throw new TransportClosedException(e);
        }
    }

    public void handleClose(Reactor reactor, SelectionKey selKey, Throwable t) {
        // do nothing
    }

    public void handleException(Reactor reactor, SelectionKey selKey,
            Throwable t) {
        if (mLoggingCtx.getLog().isErrorEnabled()) {
            mLoggingCtx.getLog().error(mLoggingCtx.formatLog("Error handling reactor callback"), t);
        }
    }

    public void handleAccept(Reactor reactor, SelectionKey selKey) {
        try {
            final SocketChannel sc = mServerChannel.accept();
            sc.configureBlocking(false);
            if (mProperties.getClientProperties() != null) {
                TCPTransportClientProperties clientProps = new TCPTransportClientProperties(
                        null, // remote address
                        mProperties.getClientProperties().getEventExecutor(),
                        mProperties.getClientProperties().getReactor(), 
                        new SocketChannelFactory() {    
                            public SocketChannel newSocketChannel() throws IOException {
                                return sc;
                            }
                        },
                        mProperties.getClientProperties().getWriteBuffer().duplicate(),
                        mProperties.getClientProperties().getWriteTimeout(), 
                        mProperties.getClientProperties().getWriteTimeoutUnit());
                TCPTransportClient newClient = new TCPTransportClient(clientProps);
                newClient.connect(0, TimeUnit.SECONDS);     // to change the state to connected
                if (mListener != null) {
                    mListener.onNewConnection(newClient);
                }
            }
        } catch (IOException e) {
            if (mLoggingCtx.getLog().isErrorEnabled()) {
                mLoggingCtx.getLog().error(mLoggingCtx.formatLog("Error handling accept"), e);
            }
        } catch (TransportException e) {
            if (mLoggingCtx.getLog().isErrorEnabled()) {
                mLoggingCtx.getLog().error(mLoggingCtx.formatLog("Error handling accept"), e);
            }
        }
    }

    public String formatLog(String msg) {
        return "Server(" + mProperties.getPort() + ") - " + msg;
    }

    public Log getLog() {
        return Trace;
    }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        ServerSocket s = mServerChannel.socket();
        if (s.getLocalSocketAddress() != null) {
            bldr.append(s.getLocalSocketAddress());
        }
        return bldr.toString();
    }
}
