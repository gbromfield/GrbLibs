package com.grb.transport.tcp.test;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.grb.reactor.ReactorThread;
import com.grb.transport.TransportClosedException;
import com.grb.transport.TransportException;
import com.grb.transport.TransportReadListener;
import com.grb.transport.TransportSendResult;
import com.grb.transport.tcp.DefaultSocketChannelFactory;
import com.grb.transport.tcp.SocketChannelFactory;
import com.grb.transport.tcp.SocketChannelProperties;
import com.grb.transport.tcp.TCPTransportClient;
import com.grb.transport.tcp.TCPTransportClientProperties;
import com.grb.transport.tcp.TCPTransportServer;
import com.grb.transport.tcp.TCPTransportServerConnectionListener;
import com.grb.transport.tcp.TCPTransportServerProperties;
import com.grb.util.Reflection;

public class TCPTransportClientTest extends TestCase {
    private static final Log Trace = LogFactory.getLog(TCPTransportClientTest.class);

    private static final ExecutorService EventExecutor = Executors.newSingleThreadExecutor();
    
    public TCPTransportClientTest(String name) {
        super(name);
    }

    public static Test suite() throws Exception {
        return new TestSuite(TCPTransportClientTest.class);
    }   

    public class DoNothingTransportServerListener implements TCPTransportServerConnectionListener {
        public void onNewConnection(TCPTransportClient client) {
            // do nothing
        }    
    }

    public class SavedTransportServerListener implements TCPTransportServerConnectionListener {
        public TCPTransportClient client;
        public void onNewConnection(TCPTransportClient client) {
            this.client = client;
        }    
    }

    public class DoNothingTransportReadListener implements TransportReadListener {
        public boolean onTransportRead(ByteBuffer readBuffer, int numBytesRead) {
            return false;
        }        
    }

    public class CountingTransportReadListener implements TransportReadListener {
        public long totalRead = 0;
        public transient boolean cont = false;
        public boolean onTransportRead(ByteBuffer readBuffer, int numBytesRead) {
            if (numBytesRead > 0) {
                totalRead += numBytesRead;
            }
            return cont;
        }        
    }
    
    public void testConnectBadPort() {
        Trace.info("Test=" + Reflection.getMethodName());
        ReactorThread reactor = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            SocketChannelProperties scProps = new SocketChannelProperties(false, null, null, null, null);
            SocketChannelFactory scFactory = new DefaultSocketChannelFactory(scProps);
            InetSocketAddress remoteAddress = new InetSocketAddress(3456);
            TCPTransportClientProperties props = new TCPTransportClientProperties(remoteAddress, EventExecutor, reactor, scFactory, ByteBuffer.allocateDirect(10), 10, TimeUnit.SECONDS);
            TCPTransportClient client = new TCPTransportClient(props);
            try {
                client.connect(5, TimeUnit.SECONDS);
                fail("should throw");
            } catch(TransportException e) {
                assertTrue(ConnectException.class.isAssignableFrom(e.getCause().getClass()));
            }
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
        }
    }

    public void testConnectAsyncBadPort1() {
        Trace.info("Test=" + Reflection.getMethodName());
        ReactorThread reactor = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            SocketChannelProperties scProps = new SocketChannelProperties(false, null, null, null, null);
            SocketChannelFactory scFactory = new DefaultSocketChannelFactory(scProps);
            InetSocketAddress remoteAddress = new InetSocketAddress(3456);
            TCPTransportClientProperties props = new TCPTransportClientProperties(remoteAddress, EventExecutor, reactor, scFactory, ByteBuffer.allocateDirect(10), 10, TimeUnit.SECONDS);
            TCPTransportClient client = new TCPTransportClient(props);
            Future<Boolean> future = client.connectAsync(5, TimeUnit.SECONDS);
            Thread.sleep(3000);
            assertTrue(future.isDone());
            try {
                future.get();
            } catch(ExecutionException e) {
                assertTrue(TransportException.class.isAssignableFrom(e.getCause().getClass()));
            }
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
        }
    }

    public void testConnectAsyncBadPort2() {
        Trace.info("Test=" + Reflection.getMethodName());
        ReactorThread reactor = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            SocketChannelProperties scProps = new SocketChannelProperties(false, null, null, null, null);
            SocketChannelFactory scFactory = new DefaultSocketChannelFactory(scProps);
            InetSocketAddress remoteAddress = new InetSocketAddress(3456);
            TCPTransportClientProperties props = new TCPTransportClientProperties(remoteAddress, EventExecutor, reactor, scFactory, ByteBuffer.allocateDirect(10), 10, TimeUnit.SECONDS);
            TCPTransportClient client = new TCPTransportClient(props);
            Future<Boolean> future = client.connectAsync(5, TimeUnit.SECONDS);
            try {
                future.get(3, TimeUnit.SECONDS);
            } catch(ExecutionException e) {
                assertTrue(future.isDone());
                assertTrue(TransportException.class.isAssignableFrom(e.getCause().getClass()));
            }
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
        }
    }

    public void testConnectBadIP() {
        Trace.info("Test=" + Reflection.getMethodName());
        ReactorThread reactor = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            SocketChannelProperties scProps = new SocketChannelProperties(false, null, null, null, null);
            SocketChannelFactory scFactory = new DefaultSocketChannelFactory(scProps);
            InetSocketAddress remoteAddress = new InetSocketAddress("192.168.111.111", 3456);
            TCPTransportClientProperties props = new TCPTransportClientProperties(remoteAddress, EventExecutor, reactor, scFactory, ByteBuffer.allocateDirect(10), 10, TimeUnit.SECONDS);
            TCPTransportClient client = new TCPTransportClient(props);
            try {
                client.connect(2, TimeUnit.MINUTES);
                fail("should throw");
            } catch(TransportException e) {
                if (NoRouteToHostException.class.isAssignableFrom(e.getCause().getClass())) {
                    // ok
                } else {
                    e.printStackTrace();
                    fail("Failure: " + e.getMessage());
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
        }
    }

    public void testConnectBadIPTimeout() {
        Trace.info("Test=" + Reflection.getMethodName());
        ReactorThread reactor = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            SocketChannelProperties scProps = new SocketChannelProperties(false, null, null, null, null);
            SocketChannelFactory scFactory = new DefaultSocketChannelFactory(scProps);
            InetSocketAddress remoteAddress = new InetSocketAddress("192.168.111.111", 3456);
            TCPTransportClientProperties props = new TCPTransportClientProperties(remoteAddress, EventExecutor, reactor, scFactory, ByteBuffer.allocateDirect(10), 10, TimeUnit.SECONDS);
            TCPTransportClient client = new TCPTransportClient(props);
            try {
                client.connect(2, TimeUnit.SECONDS);
                fail("should throw");
            } catch(TransportException e) {
                assertTrue(TimeoutException.class.isAssignableFrom(e.getCause().getClass()));
            }
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
        }
    }

    public void testConnectAsyncBadIP() {
        Trace.info("Test=" + Reflection.getMethodName());
        ReactorThread reactor = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            SocketChannelProperties scProps = new SocketChannelProperties(false, null, null, null, null);
            SocketChannelFactory scFactory = new DefaultSocketChannelFactory(scProps);
            InetSocketAddress remoteAddress = new InetSocketAddress("192.168.111.111", 3456);
            TCPTransportClientProperties props = new TCPTransportClientProperties(remoteAddress, EventExecutor, reactor, scFactory, ByteBuffer.allocateDirect(10), 10, TimeUnit.SECONDS);
            TCPTransportClient client = new TCPTransportClient(props);
            Future<Boolean> future = client.connectAsync(2, TimeUnit.MINUTES);
            try {
                int i = 0;
                while(true) {
                    i++;
                    try {
                        future.get(1, TimeUnit.SECONDS);
                    } catch(TimeoutException e) {
                        assertTrue(i < 200);
                    }
                }
            } catch(ExecutionException e) {
                Trace.info("Exception=" + e.getCause().getCause().getClass().getName());
                assertTrue(NoRouteToHostException.class.isAssignableFrom(e.getCause().getCause().getClass()));
            }
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
        }
    }

    public void testConnectTwiceBadPort() {
        Trace.info("Test=" + Reflection.getMethodName());
        ReactorThread reactor = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            SocketChannelProperties scProps = new SocketChannelProperties(false, null, null, null, null);
            SocketChannelFactory scFactory = new DefaultSocketChannelFactory(scProps);
            InetSocketAddress remoteAddress = new InetSocketAddress(3456);
            TCPTransportClientProperties props = new TCPTransportClientProperties(remoteAddress, EventExecutor, reactor, scFactory, ByteBuffer.allocateDirect(10), 10, TimeUnit.SECONDS);
            TCPTransportClient client = new TCPTransportClient(props);
            try {
                client.connect(5, TimeUnit.SECONDS);
                fail("should throw");
            } catch(TransportException e) {
                assertTrue(ConnectException.class.isAssignableFrom(e.getCause().getClass()));
            }
            try {
                client.connect(5, TimeUnit.SECONDS);
                fail("should throw");
            } catch(TransportException e) {
                assertTrue(ClosedChannelException.class.isAssignableFrom(e.getCause().getClass()));
            }
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
        }
    }

    public void testConnectAsyncTwiceBadPort1() {
        Trace.info("Test=" + Reflection.getMethodName());
        ReactorThread reactor = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            SocketChannelProperties scProps = new SocketChannelProperties(false, null, null, null, null);
            SocketChannelFactory scFactory = new DefaultSocketChannelFactory(scProps);
            InetSocketAddress remoteAddress = new InetSocketAddress(3456);
            TCPTransportClientProperties props = new TCPTransportClientProperties(remoteAddress, EventExecutor, reactor, scFactory, ByteBuffer.allocateDirect(10), 10, TimeUnit.SECONDS);
            TCPTransportClient client = new TCPTransportClient(props);
            Future<Boolean> future = client.connectAsync(5, TimeUnit.SECONDS);
            Thread.sleep(3000);
            assertTrue(future.isDone());
            try {
                future.get();
            } catch(ExecutionException e) {
                assertTrue(TransportException.class.isAssignableFrom(e.getCause().getClass()));
            }
            try {
                future = client.connectAsync(5, TimeUnit.SECONDS);
                assertTrue(future.isDone());
            } catch(TransportException e) {
                assertTrue(ClosedChannelException.class.isAssignableFrom(e.getCause().getClass()));
            }
            try {
                future.get();
            } catch(ExecutionException e) {
                assertTrue(TransportException.class.isAssignableFrom(e.getCause().getClass()));
            }
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
        }
    }

    public void testConnectTwiceBadIP() {
        Trace.info("Test=" + Reflection.getMethodName());
        ReactorThread reactor = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            SocketChannelProperties scProps = new SocketChannelProperties(false, null, null, null, null);
            SocketChannelFactory scFactory = new DefaultSocketChannelFactory(scProps);
            InetSocketAddress remoteAddress = new InetSocketAddress("192.168.111.111", 3456);
            TCPTransportClientProperties props = new TCPTransportClientProperties(remoteAddress, EventExecutor, reactor, scFactory, ByteBuffer.allocateDirect(10), 10, TimeUnit.SECONDS);
            final TCPTransportClient client = new TCPTransportClient(props);
            Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
                        client.connect(2, TimeUnit.MINUTES);
                    } catch (TransportException e) {
                        assertTrue(NoRouteToHostException.class.isAssignableFrom(e.getCause().getClass()));
                    }
                }
            });
            t.start();
            Thread.sleep(1000);
            try {
                client.connect(2, TimeUnit.MINUTES);
                fail("should throw");
            } catch(TransportException e) {
                assertTrue(NoRouteToHostException.class.isAssignableFrom(e.getCause().getClass()));
            }
            Thread.sleep(1000);
            assertTrue(!t.isAlive());
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
        }
    }

    public void testConnectAsyncTwiceBadIP() {
        Trace.info("Test=" + Reflection.getMethodName());
        ReactorThread reactor = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            SocketChannelProperties scProps = new SocketChannelProperties(false, null, null, null, null);
            SocketChannelFactory scFactory = new DefaultSocketChannelFactory(scProps);
            InetSocketAddress remoteAddress = new InetSocketAddress("192.168.111.111", 3456);
            TCPTransportClientProperties props = new TCPTransportClientProperties(remoteAddress, EventExecutor, reactor, scFactory, ByteBuffer.allocateDirect(10), 10, TimeUnit.SECONDS);
            TCPTransportClient client = new TCPTransportClient(props);
            Future<Boolean> future = client.connectAsync(2, TimeUnit.MINUTES);
            future = client.connectAsync(2, TimeUnit.MINUTES);
            try {
                int i = 0;
                while(true) {
                    i++;
                    try {
                        future.get(1, TimeUnit.SECONDS);
                    } catch(TimeoutException e) {
                        assertTrue(i < 200);
                    }
                }
            } catch(ExecutionException e) {
                assertTrue(NoRouteToHostException.class.isAssignableFrom(e.getCause().getCause().getClass()));
            }
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
        }
    }

    public void testConnect() {
        Trace.info("Test=" + Reflection.getMethodName());
        ReactorThread reactor = null;
        TCPTransportServer server = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            TCPTransportServerProperties serverProps = new TCPTransportServerProperties(3456, reactor, null);
            server = new TCPTransportServer(serverProps, new DoNothingTransportServerListener());
            server.startAccepting();
            
            SocketChannelProperties scProps = new SocketChannelProperties(false, null, null, null, null);
            SocketChannelFactory scFactory = new DefaultSocketChannelFactory(scProps);
            
            InetSocketAddress remoteAddress = new InetSocketAddress(3456);
            TCPTransportClientProperties props = new TCPTransportClientProperties(remoteAddress, EventExecutor, reactor, scFactory, ByteBuffer.allocateDirect(10), 10, TimeUnit.SECONDS);
            TCPTransportClient client = new TCPTransportClient(props);
            client.connect(5, TimeUnit.SECONDS);
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
            if (server != null) {
                server.close();
            }
        }
    }

    public void testConnectAsync() {
        Trace.info("Test=" + Reflection.getMethodName());
        ReactorThread reactor = null;
        TCPTransportServer server = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            TCPTransportServerProperties serverProps = new TCPTransportServerProperties(3456, reactor, null);
            server = new TCPTransportServer(serverProps, new DoNothingTransportServerListener());
            server.startAccepting();
            
            SocketChannelProperties scProps = new SocketChannelProperties(false, null, null, null, null);
            SocketChannelFactory scFactory = new DefaultSocketChannelFactory(scProps);
            
            InetSocketAddress remoteAddress = new InetSocketAddress(3456);
            TCPTransportClientProperties props = new TCPTransportClientProperties(remoteAddress, EventExecutor, reactor, scFactory, ByteBuffer.allocateDirect(10), 10, TimeUnit.SECONDS);
            TCPTransportClient client = new TCPTransportClient(props);
            Future<Boolean> future = client.connectAsync(5, TimeUnit.SECONDS);
            future.get();
            future.get(10, TimeUnit.MINUTES);
            assertTrue(future.isDone());
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
            if (server != null) {
                server.close();
            }
        }
    }

    public void testConnectTwice() {
        Trace.info("Test=" + Reflection.getMethodName());
        ReactorThread reactor = null;
        TCPTransportServer server = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            TCPTransportServerProperties serverProps = new TCPTransportServerProperties(3456, reactor, null);
            server = new TCPTransportServer(serverProps, new DoNothingTransportServerListener());
            server.startAccepting();
            
            SocketChannelProperties scProps = new SocketChannelProperties(false, null, null, null, null);
            SocketChannelFactory scFactory = new DefaultSocketChannelFactory(scProps);
            
            InetSocketAddress remoteAddress = new InetSocketAddress(3456);
            TCPTransportClientProperties props = new TCPTransportClientProperties(remoteAddress, EventExecutor, reactor, scFactory, ByteBuffer.allocateDirect(10), 10, TimeUnit.SECONDS);
            TCPTransportClient client = new TCPTransportClient(props);
            client.connect(5, TimeUnit.SECONDS);
            client.connect(1, TimeUnit.SECONDS);
            client.connectAsync(5, TimeUnit.SECONDS);            
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
            if (server != null) {
                server.close();
            }
        }
    }

    public void testConnectAsyncTwice() {
        Trace.info("Test=" + Reflection.getMethodName());
        ReactorThread reactor = null;
        TCPTransportServer server = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            TCPTransportServerProperties serverProps = new TCPTransportServerProperties(3456, reactor, null);
            server = new TCPTransportServer(serverProps, new DoNothingTransportServerListener());
            server.startAccepting();
            
            SocketChannelProperties scProps = new SocketChannelProperties(false, null, null, null, null);
            SocketChannelFactory scFactory = new DefaultSocketChannelFactory(scProps);
            
            InetSocketAddress remoteAddress = new InetSocketAddress(3456);
            TCPTransportClientProperties props = new TCPTransportClientProperties(remoteAddress, EventExecutor, reactor, scFactory, ByteBuffer.allocateDirect(10), 10, TimeUnit.SECONDS);
            TCPTransportClient client = new TCPTransportClient(props);
            Future<Boolean> future = client.connectAsync(5, TimeUnit.SECONDS);
            future.get();
            future.get(10, TimeUnit.MINUTES);
            client.connectAsync(5, TimeUnit.SECONDS);
            assertTrue(future.isDone());
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
            if (server != null) {
                server.close();
            }
        }
    }

    public void testConnectClosed() {
        Trace.info("Test=" + Reflection.getMethodName());
        ReactorThread reactor = null;
        TCPTransportServer server = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            TCPTransportServerProperties serverProps = new TCPTransportServerProperties(3456, reactor, null);
            server = new TCPTransportServer(serverProps, new DoNothingTransportServerListener());
            server.startAccepting();
            
            SocketChannelProperties scProps = new SocketChannelProperties(false, null, null, null, null);
            SocketChannelFactory scFactory = new DefaultSocketChannelFactory(scProps);
            
            InetSocketAddress remoteAddress = new InetSocketAddress(3456);
            TCPTransportClientProperties props = new TCPTransportClientProperties(remoteAddress, EventExecutor, reactor, scFactory, ByteBuffer.allocateDirect(10), 10, TimeUnit.SECONDS);
            TCPTransportClient client = new TCPTransportClient(props);
            client.connect(5, TimeUnit.SECONDS);
            client.close();
            try {
                client.connect(1, TimeUnit.SECONDS);
            } catch(TransportException e) {
                assertTrue(e.getCause() instanceof ClosedChannelException);
            }
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
            if (server != null) {
                server.close();
            }
        }
    }

    public void testConnectAsyncClosed() {
        Trace.info("Test=" + Reflection.getMethodName());
        ReactorThread reactor = null;
        TCPTransportServer server = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            TCPTransportServerProperties serverProps = new TCPTransportServerProperties(3456, reactor, null);
            server = new TCPTransportServer(serverProps, new DoNothingTransportServerListener());
            server.startAccepting();
            
            SocketChannelProperties scProps = new SocketChannelProperties(false, null, null, null, null);
            SocketChannelFactory scFactory = new DefaultSocketChannelFactory(scProps);
            
            InetSocketAddress remoteAddress = new InetSocketAddress(3456);
            TCPTransportClientProperties props = new TCPTransportClientProperties(remoteAddress, EventExecutor, reactor, scFactory, ByteBuffer.allocateDirect(10), 10, TimeUnit.SECONDS);
            TCPTransportClient client = new TCPTransportClient(props);
            Future<Boolean> future = client.connectAsync(5, TimeUnit.SECONDS);
            future.get();
            future.get(10, TimeUnit.MINUTES);
            assertTrue(future.isDone());
            client.close();
            try {
                client.connectAsync(5, TimeUnit.SECONDS);
            } catch(TransportException e) {
                assertTrue(e.getCause() instanceof ClosedChannelException);
            }
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
            if (server != null) {
                server.close();
            }
        }
    }

    public void testCloseServer() {
        Trace.info("Test=" + Reflection.getMethodName());
        ReactorThread reactor = null;
        TCPTransportServer server = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            TCPTransportServerProperties serverProps = new TCPTransportServerProperties(3456, reactor, null);
            server = new TCPTransportServer(serverProps, new DoNothingTransportServerListener());
            server.startAccepting();
            
            SocketChannelProperties scProps = new SocketChannelProperties(false, null, null, null, null);
            SocketChannelFactory scFactory = new DefaultSocketChannelFactory(scProps);
            
            InetSocketAddress remoteAddress = new InetSocketAddress(3456);
            TCPTransportClientProperties props = new TCPTransportClientProperties(remoteAddress, EventExecutor, reactor, scFactory, ByteBuffer.allocateDirect(10), 10, TimeUnit.SECONDS);
            TCPTransportClient client = new TCPTransportClient(props);
            client.connect(5, TimeUnit.SECONDS);
            server.close();
            try {
                client.connect(1, TimeUnit.SECONDS);
            } catch(TransportException e) {
                assertTrue(e.getCause() instanceof ClosedChannelException);
            }
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
            if (server != null) {
                server.close();
            }
        }
    }

    public void testWriteTimeout() {
        Trace.info("Test=" + Reflection.getMethodName());
        ReactorThread reactor = null;
        TCPTransportServer server = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            TCPTransportServerProperties serverProps = new TCPTransportServerProperties(3456, reactor, null);
            server = new TCPTransportServer(serverProps, new DoNothingTransportServerListener());
            server.startAccepting();
            
            SocketChannelProperties scProps = new SocketChannelProperties(false, null, null, null, null);
            SocketChannelFactory scFactory = new DefaultSocketChannelFactory(scProps);
            
            InetSocketAddress remoteAddress = new InetSocketAddress(3456);
            TCPTransportClientProperties props = new TCPTransportClientProperties(remoteAddress, EventExecutor, reactor, scFactory, ByteBuffer.allocateDirect(10000000), 10, TimeUnit.SECONDS);
            TCPTransportClient client = new TCPTransportClient(props);
            client.connect(5, TimeUnit.SECONDS);
            byte[] ba = new byte[10000000];
            ByteBuffer buffer = ByteBuffer.wrap(ba); 
            buffer.position(10000000);
            client.write(buffer, 5, TimeUnit.SECONDS);
            buffer = ByteBuffer.wrap(ba); 
            buffer.position(10000000);
            try {
                client.write(buffer, 5, TimeUnit.SECONDS);
                fail("must throw");
            } catch(TransportException e) {
                assertTrue(e.getMessage().equalsIgnoreCase("write timeout"));
            }
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
            if (server != null) {
                server.close();
            }
        }
    }

    public void testWriteTimeoutAsync() {
        Trace.info("Test=" + Reflection.getMethodName());
        ReactorThread reactor = null;
        TCPTransportServer server = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            TCPTransportServerProperties serverProps = new TCPTransportServerProperties(3456, reactor, null);
            server = new TCPTransportServer(serverProps, new DoNothingTransportServerListener());
            server.startAccepting();
            
            SocketChannelProperties scProps = new SocketChannelProperties(false, null, null, null, null);
            SocketChannelFactory scFactory = new DefaultSocketChannelFactory(scProps);
            
            InetSocketAddress remoteAddress = new InetSocketAddress(3456);
            TCPTransportClientProperties props = new TCPTransportClientProperties(remoteAddress, EventExecutor, reactor, scFactory, ByteBuffer.allocateDirect(10000000), 10, TimeUnit.SECONDS);
            TCPTransportClient client = new TCPTransportClient(props);
            client.connect(5, TimeUnit.SECONDS);
            byte[] ba = new byte[10000000];
            ByteBuffer buffer = ByteBuffer.wrap(ba); 
            buffer.position(10000000);
            client.write(buffer, 5, TimeUnit.SECONDS);
            buffer = ByteBuffer.wrap(ba); 
            buffer.position(10000000);
            TransportSendResult result = new TransportSendResult();
            int numWritten = client.writeAsync(buffer, 5, TimeUnit.SECONDS, result);
            assertTrue(numWritten != -1);
            try {
                result.getFuture().get();
                fail("must throw");
            } catch(ExecutionException e) {
                assertTrue(e.getCause().getMessage().equalsIgnoreCase("write timeout"));
            }
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
            if (server != null) {
                server.close();
            }
        }
    }

    public void testWriteAndRead() {
        Trace.info("Test=" + Reflection.getMethodName());
        ReactorThread reactor = null;
        TCPTransportServer server = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            TCPTransportServerProperties serverProps = new TCPTransportServerProperties(3456, reactor, 
                    new TCPTransportClientProperties(null, EventExecutor, reactor, null, ByteBuffer.allocateDirect(10), 10, TimeUnit.SECONDS));
            SavedTransportServerListener serverListener = new SavedTransportServerListener();
            server = new TCPTransportServer(serverProps, serverListener);
            server.startAccepting();
            
            SocketChannelProperties scProps = new SocketChannelProperties(false, null, null, null, null);
            SocketChannelFactory scFactory = new DefaultSocketChannelFactory(scProps);
            
            InetSocketAddress remoteAddress = new InetSocketAddress(3456);
            TCPTransportClientProperties props = new TCPTransportClientProperties(remoteAddress, EventExecutor, reactor, scFactory, ByteBuffer.allocateDirect(10000000), 10, TimeUnit.SECONDS);
            TCPTransportClient client = new TCPTransportClient(props);
            client.connect(5, TimeUnit.SECONDS);
            
            Thread.sleep(500);
            TCPTransportClient serverClient = serverListener.client;
            ByteBuffer readBuffer = ByteBuffer.allocate(5);
            serverClient.startReading(new DoNothingTransportReadListener(), readBuffer);
            
            byte[] ba = new byte[] {(byte)0x00, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04}; 
            ByteBuffer writeBuffer = ByteBuffer.wrap(ba); 
            writeBuffer.position(writeBuffer.limit());
            client.write(writeBuffer, 5, TimeUnit.SECONDS);
            
            Thread.sleep(500);
            for(int i = 0; i < 5; i++) {
                assertEquals((byte)i, readBuffer.get(i));
            }
            serverClient.close();
            
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
            if (server != null) {
                server.close();
            }
        }
    }

    public void testReadAndWrite() {
        Trace.info("Test=" + Reflection.getMethodName());
        ReactorThread reactor = null;
        TCPTransportServer server = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            TCPTransportServerProperties serverProps = new TCPTransportServerProperties(3456, reactor, 
                    new TCPTransportClientProperties(null, EventExecutor, reactor, null, ByteBuffer.allocateDirect(10), 10, TimeUnit.SECONDS));
            SavedTransportServerListener serverListener = new SavedTransportServerListener();
            server = new TCPTransportServer(serverProps, serverListener);
            server.startAccepting();
            
            SocketChannelProperties scProps = new SocketChannelProperties(false, null, null, null, null);
            SocketChannelFactory scFactory = new DefaultSocketChannelFactory(scProps);
            
            InetSocketAddress remoteAddress = new InetSocketAddress(3456);
            TCPTransportClientProperties props = new TCPTransportClientProperties(remoteAddress, EventExecutor, reactor, scFactory, ByteBuffer.allocateDirect(10000000), 10, TimeUnit.SECONDS);
            TCPTransportClient client = new TCPTransportClient(props);
            client.connect(5, TimeUnit.SECONDS);
            
            Thread.sleep(500);
            TCPTransportClient serverClient = serverListener.client;
            ByteBuffer readBuffer = ByteBuffer.allocate(5);
            client.startReading(new DoNothingTransportReadListener(), readBuffer);
            
            byte[] ba = new byte[] {(byte)0x00, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04}; 
            ByteBuffer writeBuffer = ByteBuffer.wrap(ba); 
            writeBuffer.position(writeBuffer.limit());
            serverClient.write(writeBuffer, 5, TimeUnit.SECONDS);
            
            Thread.sleep(500);
            for(int i = 0; i < 5; i++) {
                assertEquals((byte)i, readBuffer.get(i));
            }
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
            if (server != null) {
                server.close();
            }
        }
    }

    public void testStopAccepting() {
        Trace.info("Test=" + Reflection.getMethodName());
        ReactorThread reactor = null;
        TCPTransportServer server = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            TCPTransportServerProperties serverProps = new TCPTransportServerProperties(3456, reactor, null);
            SavedTransportServerListener serverListener = new SavedTransportServerListener();
            server = new TCPTransportServer(serverProps, serverListener);
            server.stopAccepting(); // a log message is given
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
            if (server != null) {
                server.close();
            }
        }
    }

    public void testCloseStopAccepting() {
        Trace.info("Test=" + Reflection.getMethodName());
        ReactorThread reactor = null;
        TCPTransportServer server = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            TCPTransportServerProperties serverProps = new TCPTransportServerProperties(3456, reactor, null);
            SavedTransportServerListener serverListener = new SavedTransportServerListener();
            server = new TCPTransportServer(serverProps, serverListener);
            server.close();
            try {
                server.stopAccepting();
                fail("should throw");
            } catch(TransportClosedException e) {
                // ok
            }
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
            if (server != null) {
                server.close();
            }
        }
    }

    public void testCloseStartAccepting() {
        Trace.info("Test=" + Reflection.getMethodName());
        ReactorThread reactor = null;
        TCPTransportServer server = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            TCPTransportServerProperties serverProps = new TCPTransportServerProperties(3456, reactor, null);
            SavedTransportServerListener serverListener = new SavedTransportServerListener();
            server = new TCPTransportServer(serverProps, serverListener);
            server.close();
            try {
                server.startAccepting();
                fail("should throw");
            } catch(TransportClosedException e) {
                // ok
            }
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
            if (server != null) {
                server.close();
            }
        }
    }

    public void testMultipleClose() {
        Trace.info("Test=" + Reflection.getMethodName());
        ReactorThread reactor = null;
        TCPTransportServer server = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            TCPTransportServerProperties serverProps = new TCPTransportServerProperties(3456, reactor, null);
            SavedTransportServerListener serverListener = new SavedTransportServerListener();
            server = new TCPTransportServer(serverProps, serverListener);
            server.close();
            server.close();
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
            if (server != null) {
                server.close();
            }
        }
    }

    public void testConnectServerClient() {
        Trace.info("Test=" + Reflection.getMethodName());
        ReactorThread reactor = null;
        TCPTransportServer server = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            TCPTransportServerProperties serverProps = new TCPTransportServerProperties(3456, reactor, 
                    new TCPTransportClientProperties(null, EventExecutor, reactor, null, ByteBuffer.allocateDirect(10), 10, TimeUnit.SECONDS));
            SavedTransportServerListener serverListener = new SavedTransportServerListener();
            server = new TCPTransportServer(serverProps, serverListener);
            server.startAccepting();
            
            SocketChannelProperties scProps = new SocketChannelProperties(false, null, null, null, null);
            SocketChannelFactory scFactory = new DefaultSocketChannelFactory(scProps);
            
            InetSocketAddress remoteAddress = new InetSocketAddress(3456);
            TCPTransportClientProperties props = new TCPTransportClientProperties(remoteAddress, EventExecutor, reactor, scFactory, ByteBuffer.allocateDirect(10000000), 10, TimeUnit.SECONDS);
            TCPTransportClient client = new TCPTransportClient(props);
            client.connect(5, TimeUnit.SECONDS);
            
            Thread.sleep(500);
            TCPTransportClient serverClient = serverListener.client;
            serverClient.connect(10, TimeUnit.SECONDS);
            serverClient.connectAsync(10, TimeUnit.SECONDS);
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
            if (server != null) {
                server.close();
            }
        }
    }

    public void testConnectClosedServerClient() {
        Trace.info("Test=" + Reflection.getMethodName());
        ReactorThread reactor = null;
        TCPTransportServer server = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            TCPTransportServerProperties serverProps = new TCPTransportServerProperties(3456, reactor, 
                    new TCPTransportClientProperties(null, EventExecutor, reactor, null, ByteBuffer.allocateDirect(10), 10, TimeUnit.SECONDS));
            SavedTransportServerListener serverListener = new SavedTransportServerListener();
            server = new TCPTransportServer(serverProps, serverListener);
            server.startAccepting();
            
            SocketChannelProperties scProps = new SocketChannelProperties(false, null, null, null, null);
            SocketChannelFactory scFactory = new DefaultSocketChannelFactory(scProps);
            
            InetSocketAddress remoteAddress = new InetSocketAddress(3456);
            TCPTransportClientProperties props = new TCPTransportClientProperties(remoteAddress, EventExecutor, reactor, scFactory, ByteBuffer.allocateDirect(10000000), 10, TimeUnit.SECONDS);
            TCPTransportClient client = new TCPTransportClient(props);
            client.connect(5, TimeUnit.SECONDS);
            
            Thread.sleep(500);
            TCPTransportClient serverClient = serverListener.client;
            serverClient.close();
            try {
                serverClient.connect(10, TimeUnit.SECONDS);
                fail("should throw");
            } catch(TransportClosedException e) {
                // ok
            }
            try {
                serverClient.connectAsync(10, TimeUnit.SECONDS);
                fail("should throw");
            } catch(TransportClosedException e) {
                // ok
            }
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
            if (server != null) {
                server.close();
            }
        }
    }

    public void testWriteFuture() {
        Trace.info("Test=" + Reflection.getMethodName());
        final int chunkSize = 1000;
        ReactorThread reactor = null;
        TCPTransportServer server = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            TCPTransportServerProperties serverProps = new TCPTransportServerProperties(3456, reactor, 
                    new TCPTransportClientProperties(null, EventExecutor, reactor, null, ByteBuffer.allocateDirect(chunkSize), 10, TimeUnit.SECONDS));
            SavedTransportServerListener serverListener = new SavedTransportServerListener();
            server = new TCPTransportServer(serverProps, serverListener);
            server.startAccepting();
            
            SocketChannelProperties scProps = new SocketChannelProperties(false, null, null, null, null);
            SocketChannelFactory scFactory = new DefaultSocketChannelFactory(scProps);
            
            InetSocketAddress remoteAddress = new InetSocketAddress(3456);
            TCPTransportClientProperties props = new TCPTransportClientProperties(remoteAddress, EventExecutor, reactor, scFactory, ByteBuffer.allocateDirect(chunkSize), 10, TimeUnit.SECONDS);
            TCPTransportClient client = new TCPTransportClient(props);
            client.connect(5, TimeUnit.SECONDS);
            
            Thread.sleep(500);
            TCPTransportClient serverClient = serverListener.client;
            
            // send data to it back-pressures
            byte[] sendData = new byte[chunkSize];
            TransportSendResult result = new TransportSendResult();
            long totalSent = 0;
            Future<Boolean> future;
            while(true) {
                ByteBuffer writeBuffer = ByteBuffer.wrap(sendData);
                writeBuffer.position(chunkSize);
                int numWritten = client.writeAsync(writeBuffer, 100, TimeUnit.SECONDS, result);
                future = result.getFuture();
                if (numWritten == chunkSize) {
                    totalSent += chunkSize;
                    assertTrue(result.isDone());
                    assertNull(future);
                } else if (numWritten >= 0){
                    totalSent += chunkSize;
                    assertFalse(result.isDone());
                    assertNotNull(future);
                } else {
                    try {
                        // performing a blocking send when the previous send 
                        // hasn't completed throws
                        client.write(writeBuffer, 100, TimeUnit.SECONDS);
                        fail("should throw");
                    } catch(TransportException e) {
                        // ok
                    }
                    break;
                }
            }
            
            try {
                future.get(5, TimeUnit.SECONDS);
                fail("should throw");
            } catch(TimeoutException e) {
                // ok
            }
            
            // start the server reading
            ByteBuffer readBuffer = ByteBuffer.allocate(chunkSize);
            CountingTransportReadListener readListener = new CountingTransportReadListener();
            readListener.cont = true;
            serverClient.startReading(readListener, readBuffer);
            
            assertTrue(future.get(10, TimeUnit.SECONDS));
            Thread.sleep(1000);
            assertEquals(totalSent, readListener.totalRead);
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
            if (server != null) {
                server.close();
            }
        }
    }

    public void testVectoredWriteFuture() {
        Trace.info("Test=" + Reflection.getMethodName());
        final int chunkSize = 1000;
        final int numBuffers = 5;
        ReactorThread reactor = null;
        TCPTransportServer server = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            TCPTransportServerProperties serverProps = new TCPTransportServerProperties(3456, reactor,
                    new TCPTransportClientProperties(null, EventExecutor, reactor, null, ByteBuffer.allocateDirect(chunkSize * numBuffers), 10, TimeUnit.SECONDS));
            SavedTransportServerListener serverListener = new SavedTransportServerListener();
            server = new TCPTransportServer(serverProps, serverListener);
            server.startAccepting();
            
            SocketChannelProperties scProps = new SocketChannelProperties(false, null, null, null, null);
            SocketChannelFactory scFactory = new DefaultSocketChannelFactory(scProps);
            
            InetSocketAddress remoteAddress = new InetSocketAddress(3456);
            TCPTransportClientProperties props = new TCPTransportClientProperties(remoteAddress, EventExecutor, reactor, scFactory, 
                    ByteBuffer.allocateDirect(chunkSize * numBuffers), 10, TimeUnit.SECONDS);
            TCPTransportClient client = new TCPTransportClient(props);
            client.connect(5, TimeUnit.SECONDS);
            
            Thread.sleep(500);
            TCPTransportClient serverClient = serverListener.client;
            
            // send data to it back-pressures
            byte[] sendData = new byte[chunkSize];
            ByteBuffer[] writeBuffers = new ByteBuffer[numBuffers];
            TransportSendResult result = new TransportSendResult();
            long totalSent = 0;
            Future<Boolean> future;
            while(true) {
                for(int i = 0; i < numBuffers; i++) {
                    writeBuffers[i] = ByteBuffer.wrap(sendData);
                    writeBuffers[i].position(chunkSize);
                }
                long numWritten = client.writeAsync(writeBuffers, 100, TimeUnit.SECONDS, result);
                future = result.getFuture();
                if (numWritten == (chunkSize * numBuffers)) {
                    totalSent += chunkSize * numBuffers;
                    assertTrue(result.isDone());
                    assertNull(future);
                } else if (numWritten >= 0){
                    totalSent += chunkSize * numBuffers;
                    assertFalse(result.isDone());
                    assertNotNull(future);
                } else {
                    try {
                        // performing a blocking send when the previous send 
                        // hasn't completed throws
                        client.write(writeBuffers, 100, TimeUnit.SECONDS);
                        fail("should throw");
                    } catch(TransportException e) {
                        // ok
                    }
                    break;
                }
            }
            
            try {
                future.get(5, TimeUnit.SECONDS);
                fail("should throw");
            } catch(TimeoutException e) {
                // ok
            }
            
            // start the server reading
            ByteBuffer readBuffer = ByteBuffer.allocate(chunkSize);
            CountingTransportReadListener readListener = new CountingTransportReadListener();
            readListener.cont = true;
            serverClient.startReading(readListener, readBuffer);
            
            assertTrue(future.get(10, TimeUnit.SECONDS));
            Thread.sleep(1000);
            assertEquals(totalSent, readListener.totalRead);
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
            if (server != null) {
                server.close();
            }
        }
    }

    public void testChunkWriteFuture() {
        Trace.info("Test=" + Reflection.getMethodName());
        final int chunkSize = 1000000;
        ReactorThread reactor = null;
        TCPTransportServer server = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            TCPTransportServerProperties serverProps = new TCPTransportServerProperties(3456, reactor,
                    new TCPTransportClientProperties(null, EventExecutor, reactor, null, ByteBuffer.allocateDirect(chunkSize), 10, TimeUnit.SECONDS));
            SavedTransportServerListener serverListener = new SavedTransportServerListener();
            server = new TCPTransportServer(serverProps, serverListener);
            server.startAccepting();
            
            SocketChannelProperties scProps = new SocketChannelProperties(false, null, null, null, null);
            SocketChannelFactory scFactory = new DefaultSocketChannelFactory(scProps);
            
            InetSocketAddress remoteAddress = new InetSocketAddress(3456);
            TCPTransportClientProperties props = new TCPTransportClientProperties(remoteAddress, EventExecutor, reactor, scFactory, ByteBuffer.allocateDirect(chunkSize), 10, TimeUnit.SECONDS);
            TCPTransportClient client = new TCPTransportClient(props);
            client.connect(5, TimeUnit.SECONDS);
            
            Thread.sleep(500);
            TCPTransportClient serverClient = serverListener.client;
            
            // send data to it back-pressures
            byte[] sendData = new byte[chunkSize];
            TransportSendResult result = new TransportSendResult();
            long totalSent = 0;
            Future<Boolean> future;
            while(true) {
                totalSent += chunkSize;
                ByteBuffer writeBuffer = ByteBuffer.wrap(sendData);
                writeBuffer.position(chunkSize);
                int numWritten = client.writeAsync(writeBuffer, 100, TimeUnit.SECONDS, result);
                future = result.getFuture();
                if (numWritten == chunkSize) {
                    assertTrue(result.isDone());
                    assertNull(future);
                } else {
                    assertFalse(result.isDone());
                    assertNotNull(future);
                    break;
                }
            }
            
            try {
                future.get(5, TimeUnit.SECONDS);
                fail("should throw");
            } catch(TimeoutException e) {
                // ok
            }
            
            // start the server reading half
            ByteBuffer readBuffer = ByteBuffer.allocate(chunkSize/2);
            CountingTransportReadListener readListener = new CountingTransportReadListener();
            readListener.cont = false;
            serverClient.startReading(readListener, readBuffer);

            try {
                future.get(5, TimeUnit.SECONDS);
                fail("should throw");
            } catch(TimeoutException e) {
                // ok
            }

            // start the server reading everything
            readListener.cont = true;
            serverClient.startReading(readListener, readBuffer);
            
            assertTrue(future.get(10, TimeUnit.SECONDS));
            Thread.sleep(1000);
            assertEquals(totalSent, readListener.totalRead);
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
            if (server != null) {
                server.close();
            }
        }
    }

    public void testVectoredChunkWriteFuture() {
        Trace.info("Test=" + Reflection.getMethodName());
        final int chunkSize = 100000;
        final int numBuffers = 5;
        ReactorThread reactor = null;
        TCPTransportServer server = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            TCPTransportServerProperties serverProps = new TCPTransportServerProperties(3456, reactor,
                    new TCPTransportClientProperties(null, EventExecutor, reactor, null, ByteBuffer.allocateDirect(chunkSize * numBuffers), 10, TimeUnit.SECONDS));
            SavedTransportServerListener serverListener = new SavedTransportServerListener();
            server = new TCPTransportServer(serverProps, serverListener);
            server.startAccepting();
            
            SocketChannelProperties scProps = new SocketChannelProperties(false, null, null, null, null);
            SocketChannelFactory scFactory = new DefaultSocketChannelFactory(scProps);
            
            InetSocketAddress remoteAddress = new InetSocketAddress(3456);
            TCPTransportClientProperties props = new TCPTransportClientProperties(remoteAddress, EventExecutor, reactor, scFactory, ByteBuffer.allocateDirect(chunkSize * numBuffers), 10, TimeUnit.SECONDS);
            TCPTransportClient client = new TCPTransportClient(props);
            client.connect(5, TimeUnit.SECONDS);
            
            Thread.sleep(500);
            TCPTransportClient serverClient = serverListener.client;
            
            // send data to it back-pressures
            byte[] sendData = new byte[chunkSize];
            ByteBuffer[] writeBuffers = new ByteBuffer[numBuffers];
            TransportSendResult result = new TransportSendResult();
            long totalSent = 0;
            Future<Boolean> future;
            while(true) {
                for(int i = 0; i < numBuffers; i++) {
                    writeBuffers[i] = ByteBuffer.wrap(sendData);
                    writeBuffers[i].position(chunkSize);
                }
                long numWritten = client.writeAsync(writeBuffers, 100, TimeUnit.SECONDS, result);
                future = result.getFuture();
                if (numWritten == (chunkSize * numBuffers)) {
                    totalSent += chunkSize * numBuffers;
                    assertTrue(result.isDone());
                    assertNull(future);
                } else if (numWritten >= 0) {
                    totalSent += chunkSize * numBuffers;
                    assertFalse(result.isDone());
                    assertNotNull(future);
                } else {
                    try {
                        // performing a blocking send when the previous send 
                        // hasn't completed throws
                        client.write(writeBuffers, 100, TimeUnit.SECONDS);
                        fail("should throw");
                    } catch(TransportException e) {
                        // ok
                    }
                    break;
                }
            }
            
            try {
                future.get(5, TimeUnit.SECONDS);
                fail("should throw");
            } catch(TimeoutException e) {
                // ok
            }
            
            // start the server reading half
            ByteBuffer readBuffer = ByteBuffer.allocate(chunkSize/2);
            CountingTransportReadListener readListener = new CountingTransportReadListener();
            readListener.cont = false;
            serverClient.startReading(readListener, readBuffer);

            try {
                future.get(5, TimeUnit.SECONDS);
                fail("should throw");
            } catch(TimeoutException e) {
                // ok
            }

            // start the server reading everything
            readListener.cont = true;
            serverClient.startReading(readListener, readBuffer);
            
            assertTrue(future.get(10, TimeUnit.SECONDS));
            Thread.sleep(1000);
            assertEquals(totalSent, readListener.totalRead);
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
            if (server != null) {
                server.close();
            }
        }
    }

    public void testWriteFutureMultiple() {
        Trace.info("Test=" + Reflection.getMethodName());
        final int chunkSize = 1000000;
        ReactorThread reactor = null;
        TCPTransportServer server = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            TCPTransportServerProperties serverProps = new TCPTransportServerProperties(3456, reactor,
                    new TCPTransportClientProperties(null, EventExecutor, reactor, null, ByteBuffer.allocateDirect(chunkSize), 10, TimeUnit.SECONDS));
            SavedTransportServerListener serverListener = new SavedTransportServerListener();
            server = new TCPTransportServer(serverProps, serverListener);
            server.startAccepting();
            
            SocketChannelProperties scProps = new SocketChannelProperties(false, null, null, null, null);
            SocketChannelFactory scFactory = new DefaultSocketChannelFactory(scProps);
            
            InetSocketAddress remoteAddress = new InetSocketAddress(3456);
            TCPTransportClientProperties props = new TCPTransportClientProperties(remoteAddress, EventExecutor, reactor, scFactory, ByteBuffer.allocateDirect(chunkSize), 10, TimeUnit.SECONDS);
            TCPTransportClient client = new TCPTransportClient(props);
            client.connect(5, TimeUnit.SECONDS);
            
            Thread.sleep(500);
            TCPTransportClient serverClient = serverListener.client;
            
            // send data to it back-pressures
            byte[] sendData = new byte[chunkSize];
            TransportSendResult result = new TransportSendResult();
            long totalSent = 0;
            Future<Boolean> future;
            while(true) {
                totalSent += chunkSize;
                ByteBuffer writeBuffer = ByteBuffer.wrap(sendData);
                writeBuffer.position(chunkSize);
                int numWritten = client.writeAsync(writeBuffer, 100, TimeUnit.SECONDS, result);
                future = result.getFuture();
                if (numWritten == chunkSize) {
                    assertTrue(result.isDone());
                    assertNull(future);
                } else {
                    assertFalse(result.isDone());
                    assertNotNull(future);
                    break;
                }
            }
            
            try {
                future.get(5, TimeUnit.SECONDS);
                fail("should throw");
            } catch(TimeoutException e) {
                // ok
            }
            
            // start the server reading
            ByteBuffer readBuffer = ByteBuffer.allocate(chunkSize);
            CountingTransportReadListener readListener = new CountingTransportReadListener();
            readListener.cont = true;
            serverClient.startReading(readListener, readBuffer);
            
            assertTrue(future.get(10, TimeUnit.SECONDS));
            Thread.sleep(1000);
            assertEquals(totalSent, readListener.totalRead);
            
            serverClient.stopReading();

            // send data to it back-pressures
            while(true) {
                totalSent += chunkSize;
                ByteBuffer writeBuffer = ByteBuffer.wrap(sendData);
                writeBuffer.position(chunkSize);
                int numWritten = client.writeAsync(writeBuffer, 100, TimeUnit.SECONDS, result);
                future = result.getFuture();
                if (numWritten == chunkSize) {
                    assertTrue(result.isDone());
                    assertNull(future);
                } else {
                    assertFalse(result.isDone());
                    assertNotNull(future);
                    break;
                }
            }
            
            try {
                future.get(5, TimeUnit.SECONDS);
                fail("should throw");
            } catch(TimeoutException e) {
                // ok
            }

            // start the server reading
            readListener.cont = true;
            serverClient.startReading(readListener, readBuffer);
            
            assertTrue(future.get(10, TimeUnit.SECONDS));
            Thread.sleep(1000);
            assertEquals(totalSent, readListener.totalRead);
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
            if (server != null) {
                server.close();
            }
        }
    }

    public void testVectoredWriteFutureMultiple() {
        Trace.info("Test=" + Reflection.getMethodName());
        final int chunkSize = 100000;
        final int numBuffers = 5;
        ReactorThread reactor = null;
        TCPTransportServer server = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            TCPTransportServerProperties serverProps = new TCPTransportServerProperties(3456, reactor,
                    new TCPTransportClientProperties(null, EventExecutor, reactor, null, ByteBuffer.allocateDirect(chunkSize * numBuffers), 10, TimeUnit.SECONDS));
            SavedTransportServerListener serverListener = new SavedTransportServerListener();
            server = new TCPTransportServer(serverProps, serverListener);
            server.startAccepting();
            
            SocketChannelProperties scProps = new SocketChannelProperties(false, null, null, null, null);
            SocketChannelFactory scFactory = new DefaultSocketChannelFactory(scProps);
            
            InetSocketAddress remoteAddress = new InetSocketAddress(3456);
            TCPTransportClientProperties props = new TCPTransportClientProperties(remoteAddress, EventExecutor, reactor, scFactory, 
                    ByteBuffer.allocateDirect(chunkSize * numBuffers), 10, TimeUnit.SECONDS);
            TCPTransportClient client = new TCPTransportClient(props);
            client.connect(5, TimeUnit.SECONDS);
            
            Thread.sleep(500);
            TCPTransportClient serverClient = serverListener.client;
            
            // send data to it back-pressures
            byte[] sendData = new byte[chunkSize];
            ByteBuffer[] writeBuffers = new ByteBuffer[numBuffers];
            TransportSendResult result = new TransportSendResult();
            long totalSent = 0;
            Future<Boolean> future;
            while(true) {
                for(int i = 0; i < numBuffers; i++) {
                    writeBuffers[i] = ByteBuffer.wrap(sendData);
                    writeBuffers[i].position(chunkSize);
                }
                long numWritten = client.writeAsync(writeBuffers, 100, TimeUnit.SECONDS, result);
                future = result.getFuture();
                if (numWritten == (chunkSize * numBuffers)) {
                    totalSent += chunkSize * numBuffers;
                    assertTrue(result.isDone());
                    assertNull(future);
                } else if (numWritten >= 0) {
                    totalSent += chunkSize * numBuffers;
                    assertFalse(result.isDone());
                    assertNotNull(future);
                } else {
                    try {
                        // performing a blocking send when the previous send 
                        // hasn't completed throws
                        client.write(writeBuffers, 100, TimeUnit.SECONDS);
                        fail("should throw");
                    } catch(TransportException e) {
                        // ok
                    }
                    break;
                }
            }
            
            try {
                future.get(5, TimeUnit.SECONDS);
                fail("should throw");
            } catch(TimeoutException e) {
                // ok
            }
            
            // start the server reading
            ByteBuffer readBuffer = ByteBuffer.allocate(chunkSize);
            CountingTransportReadListener readListener = new CountingTransportReadListener();
            readListener.cont = true;
            serverClient.startReading(readListener, readBuffer);
            
            assertTrue(future.get(10, TimeUnit.SECONDS));
            Thread.sleep(1000);
            assertEquals(totalSent, readListener.totalRead);
            
            serverClient.stopReading();

            // send data to it back-pressures
            while(true) {
                for(int i = 0; i < numBuffers; i++) {
                    writeBuffers[i] = ByteBuffer.wrap(sendData);
                    writeBuffers[i].position(chunkSize);
                }
                long numWritten = client.writeAsync(writeBuffers, 100, TimeUnit.SECONDS, result);
                future = result.getFuture();
                if (numWritten == (chunkSize * numBuffers)) {
                    totalSent += chunkSize * numBuffers;
                    assertTrue(result.isDone());
                    assertNull(future);
                } else if (numWritten >= 0) {
                    totalSent += chunkSize * numBuffers;
                    assertFalse(result.isDone());
                    assertNotNull(future);
                } else {
                    try {
                        // performing a blocking send when the previous send 
                        // hasn't completed throws
                        client.write(writeBuffers, 100, TimeUnit.SECONDS);
                        fail("should throw");
                    } catch(TransportException e) {
                        // ok
                    }
                    break;
                }
            }
            
            try {
                future.get(5, TimeUnit.SECONDS);
                fail("should throw");
            } catch(TimeoutException e) {
                // ok
            }

            // start the server reading
            readListener.cont = true;
            serverClient.startReading(readListener, readBuffer);
            
            assertTrue(future.get(10, TimeUnit.SECONDS));
            Thread.sleep(1000);
            assertEquals(totalSent, readListener.totalRead);
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
            if (server != null) {
                server.close();
            }
        }
    }

    public void testWriteFutureTimeout() {
        Trace.info("Test=" + Reflection.getMethodName());
        final int chunkSize = 1000000;
        ReactorThread reactor = null;
        TCPTransportServer server = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            TCPTransportServerProperties serverProps = new TCPTransportServerProperties(3456, reactor,
                    new TCPTransportClientProperties(null, EventExecutor, reactor, null, ByteBuffer.allocateDirect(chunkSize), 10, TimeUnit.SECONDS));
            SavedTransportServerListener serverListener = new SavedTransportServerListener();
            server = new TCPTransportServer(serverProps, serverListener);
            server.startAccepting();
            
            SocketChannelProperties scProps = new SocketChannelProperties(false, null, null, null, null);
            SocketChannelFactory scFactory = new DefaultSocketChannelFactory(scProps);
            
            InetSocketAddress remoteAddress = new InetSocketAddress(3456);
            TCPTransportClientProperties props = new TCPTransportClientProperties(remoteAddress, EventExecutor, reactor, scFactory, ByteBuffer.allocateDirect(chunkSize), 10, TimeUnit.SECONDS);
            TCPTransportClient client = new TCPTransportClient(props);
            client.connect(5, TimeUnit.SECONDS);
                        
            // send data to it back-pressures
            byte[] sendData = new byte[chunkSize];
            TransportSendResult result = new TransportSendResult();
//            long totalSent = 0;
            Future<Boolean> future;
            while(true) {
//                totalSent += chunkSize;
                ByteBuffer writeBuffer = ByteBuffer.wrap(sendData);
                writeBuffer.position(chunkSize);
                int numWritten = client.writeAsync(writeBuffer, 10, TimeUnit.SECONDS, result);
                future = result.getFuture();
                if (numWritten == chunkSize) {
                    assertTrue(result.isDone());
                    assertNull(future);
                } else {
                    assertFalse(result.isDone());
                    assertNotNull(future);
                    break;
                }
            }
            
            try {
                future.get(30, TimeUnit.SECONDS);
                fail("should throw");
            } catch(ExecutionException e) {
                // ok
            }
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
            if (server != null) {
                server.close();
            }
        }
    }

    public void testVectoredWriteFutureTimeout() {
        Trace.info("Test=" + Reflection.getMethodName());
        final int chunkSize = 100000;
        final int numBuffers = 5;
        ReactorThread reactor = null;
        TCPTransportServer server = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            TCPTransportServerProperties serverProps = new TCPTransportServerProperties(3456, reactor,
                    new TCPTransportClientProperties(null, EventExecutor, reactor, null, ByteBuffer.allocateDirect(chunkSize * numBuffers), 10, TimeUnit.SECONDS));
            SavedTransportServerListener serverListener = new SavedTransportServerListener();
            server = new TCPTransportServer(serverProps, serverListener);
            server.startAccepting();
            
            SocketChannelProperties scProps = new SocketChannelProperties(false, null, null, null, null);
            SocketChannelFactory scFactory = new DefaultSocketChannelFactory(scProps);
            
            InetSocketAddress remoteAddress = new InetSocketAddress(3456);
            TCPTransportClientProperties props = new TCPTransportClientProperties(remoteAddress, EventExecutor, reactor, scFactory, 
                    ByteBuffer.allocateDirect(chunkSize * numBuffers), 10, TimeUnit.SECONDS);
            TCPTransportClient client = new TCPTransportClient(props);
            client.connect(5, TimeUnit.SECONDS);
                        
            // send data to it back-pressures
            byte[] sendData = new byte[chunkSize];
            ByteBuffer[] writeBuffers = new ByteBuffer[numBuffers];
            TransportSendResult result = new TransportSendResult();
//            long totalSent = 0;
            Future<Boolean> future;
            while(true) {
                for(int i = 0; i < numBuffers; i++) {
                    writeBuffers[i] = ByteBuffer.wrap(sendData);
                    writeBuffers[i].position(chunkSize);
                }
                long numWritten = client.writeAsync(writeBuffers, 10, TimeUnit.SECONDS, result);
                future = result.getFuture();
                if (numWritten == (chunkSize * numBuffers)) {
//                    totalSent += chunkSize * numBuffers;
                    assertTrue(result.isDone());
                    assertNull(future);
                } else if (numWritten >= 0) {
//                    totalSent += chunkSize * numBuffers;
                    assertFalse(result.isDone());
                    assertNotNull(future);
                } else {
                    try {
                        // performing a blocking send when the previous send 
                        // hasn't completed throws
                        client.write(writeBuffers, 100, TimeUnit.SECONDS);
                        fail("should throw");
                    } catch(TransportException e) {
                        // ok
                    }
                    break;
                }
            }
            
            try {
                future.get(30, TimeUnit.SECONDS);
                fail("should throw");
            } catch(ExecutionException e) {
                // ok
            }
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
            if (server != null) {
                server.close();
            }
        }
    }

    public void testWriteFutureServerClose() {
        Trace.info("Test=" + Reflection.getMethodName());
        final int chunkSize = 1000000;
        ReactorThread reactor = null;
        TCPTransportServer server = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            TCPTransportServerProperties serverProps = new TCPTransportServerProperties(3456, reactor,
                    new TCPTransportClientProperties(null, EventExecutor, reactor, null, ByteBuffer.allocateDirect(chunkSize), 10, TimeUnit.SECONDS));
            SavedTransportServerListener serverListener = new SavedTransportServerListener();
            server = new TCPTransportServer(serverProps, serverListener);
            server.startAccepting();
            
            SocketChannelProperties scProps = new SocketChannelProperties(false, null, null, null, null);
            SocketChannelFactory scFactory = new DefaultSocketChannelFactory(scProps);
            
            InetSocketAddress remoteAddress = new InetSocketAddress(3456);
            TCPTransportClientProperties props = new TCPTransportClientProperties(remoteAddress, EventExecutor, reactor, scFactory, ByteBuffer.allocateDirect(chunkSize), 10, TimeUnit.SECONDS);
            TCPTransportClient client = new TCPTransportClient(props);
            client.connect(5, TimeUnit.SECONDS);
            
            Thread.sleep(500);
            TCPTransportClient serverClient = serverListener.client;
            
            // send data to it back-pressures
            byte[] sendData = new byte[chunkSize];
            TransportSendResult result = new TransportSendResult();
//            long totalSent = 0;
            Future<Boolean> future;
            while(true) {
//                totalSent += chunkSize;
                ByteBuffer writeBuffer = ByteBuffer.wrap(sendData);
                writeBuffer.position(chunkSize);
                int numWritten = client.writeAsync(writeBuffer, 10, TimeUnit.SECONDS, result);
                future = result.getFuture();
                if (numWritten == chunkSize) {
                    assertTrue(result.isDone());
                    assertNull(future);
                } else {
                    assertFalse(result.isDone());
                    assertNotNull(future);
                    break;
                }
            }
            
            serverClient.close();
            
            try {
                future.get(15, TimeUnit.SECONDS);
                fail("should throw");
            } catch(ExecutionException e) {
                // ok
            }
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
            if (server != null) {
                server.close();
            }
        }
    }

    public void testVectoredWriteFutureServerClose() {
        Trace.info("Test=" + Reflection.getMethodName());
        final int chunkSize = 100000;
        final int numBuffers = 5;
        ReactorThread reactor = null;
        TCPTransportServer server = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            TCPTransportServerProperties serverProps = new TCPTransportServerProperties(3456, reactor,
                    new TCPTransportClientProperties(null, EventExecutor, reactor, null, ByteBuffer.allocateDirect(chunkSize * numBuffers), 10, TimeUnit.SECONDS));
            SavedTransportServerListener serverListener = new SavedTransportServerListener();
            server = new TCPTransportServer(serverProps, serverListener);
            server.startAccepting();
            
            SocketChannelProperties scProps = new SocketChannelProperties(false, null, null, null, null);
            SocketChannelFactory scFactory = new DefaultSocketChannelFactory(scProps);
            
            InetSocketAddress remoteAddress = new InetSocketAddress(3456);
            TCPTransportClientProperties props = new TCPTransportClientProperties(remoteAddress, EventExecutor, reactor, scFactory, 
                    ByteBuffer.allocateDirect(chunkSize * numBuffers), 10, TimeUnit.SECONDS);
            TCPTransportClient client = new TCPTransportClient(props);
            client.connect(5, TimeUnit.SECONDS);
            
            Thread.sleep(500);
            TCPTransportClient serverClient = serverListener.client;
            
            // send data to it back-pressures
            byte[] sendData = new byte[chunkSize];
            ByteBuffer[] writeBuffers = new ByteBuffer[numBuffers];
            TransportSendResult result = new TransportSendResult();
//            long totalSent = 0;
            Future<Boolean> future;
            while(true) {
                for(int i = 0; i < numBuffers; i++) {
                    writeBuffers[i] = ByteBuffer.wrap(sendData);
                    writeBuffers[i].position(chunkSize);
                }
                long numWritten = client.writeAsync(writeBuffers, 10, TimeUnit.SECONDS, result);
                future = result.getFuture();
                if (numWritten == (chunkSize * numBuffers)) {
 //                   totalSent += chunkSize * numBuffers;
                    assertTrue(result.isDone());
                    assertNull(future);
                } else if (numWritten >= 0) {
 //                   totalSent += chunkSize * numBuffers;
                    assertFalse(result.isDone());
                    assertNotNull(future);
                } else {
                    try {
                        // performing a blocking send when the previous send 
                        // hasn't completed throws
                        client.write(writeBuffers, 100, TimeUnit.SECONDS);
                        fail("should throw");
                    } catch(TransportException e) {
                        // ok
                    }
                    break;
                }
            }
            
            serverClient.close();
            
            try {
                future.get(15, TimeUnit.SECONDS);
                fail("should throw");
            } catch(ExecutionException e) {
                // ok
            }
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
            if (server != null) {
                server.close();
            }
        }
    }

    public void testWriteFutureClientClose() {
        Trace.info("Test=" + Reflection.getMethodName());
        final int chunkSize = 1000000;
        ReactorThread reactor = null;
        TCPTransportServer server = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            TCPTransportServerProperties serverProps = new TCPTransportServerProperties(3456, reactor,
                    new TCPTransportClientProperties(null, EventExecutor, reactor, null, ByteBuffer.allocateDirect(chunkSize), 10, TimeUnit.SECONDS));
            SavedTransportServerListener serverListener = new SavedTransportServerListener();
            server = new TCPTransportServer(serverProps, serverListener);
            server.startAccepting();
            
            SocketChannelProperties scProps = new SocketChannelProperties(false, null, null, null, null);
            SocketChannelFactory scFactory = new DefaultSocketChannelFactory(scProps);
            
            InetSocketAddress remoteAddress = new InetSocketAddress(3456);
            TCPTransportClientProperties props = new TCPTransportClientProperties(remoteAddress, EventExecutor, reactor, scFactory, ByteBuffer.allocateDirect(chunkSize), 10, TimeUnit.SECONDS);
            TCPTransportClient client = new TCPTransportClient(props);
            client.connect(5, TimeUnit.SECONDS);
                        
            // send data to it back-pressures
            byte[] sendData = new byte[chunkSize];
            TransportSendResult result = new TransportSendResult();
//            long totalSent = 0;
            Future<Boolean> future;
            while(true) {
//                totalSent += chunkSize;
                ByteBuffer writeBuffer = ByteBuffer.wrap(sendData);
                writeBuffer.position(chunkSize);
                int numWritten = client.writeAsync(writeBuffer, 100, TimeUnit.SECONDS, result);
                future = result.getFuture();
                if (numWritten == chunkSize) {
                    assertTrue(result.isDone());
                    assertNull(future);
                } else {
                    assertFalse(result.isDone());
                    assertNotNull(future);
                    break;
                }
            }
            
            client.close();
            
            try {
                future.get(15, TimeUnit.SECONDS);
                fail("should throw");
            } catch(ExecutionException e) {
                // ok
            }
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
            if (server != null) {
                server.close();
            }
        }
    }

    public void testVectoredWriteFutureClientClose() {
        Trace.info("Test=" + Reflection.getMethodName());
        final int chunkSize = 100000;
        final int numBuffers = 5;
        ReactorThread reactor = null;
        TCPTransportServer server = null;
        try {
            reactor = new ReactorThread();
            reactor.startAsDaemon();
            TCPTransportServerProperties serverProps = new TCPTransportServerProperties(3456, reactor,
                    new TCPTransportClientProperties(null, EventExecutor, reactor, null, ByteBuffer.allocateDirect(chunkSize * numBuffers), 10, TimeUnit.SECONDS));
            SavedTransportServerListener serverListener = new SavedTransportServerListener();
            server = new TCPTransportServer(serverProps, serverListener);
            server.startAccepting();
            
            SocketChannelProperties scProps = new SocketChannelProperties(false, null, null, null, null);
            SocketChannelFactory scFactory = new DefaultSocketChannelFactory(scProps);
            
            InetSocketAddress remoteAddress = new InetSocketAddress(3456);
            TCPTransportClientProperties props = new TCPTransportClientProperties(remoteAddress, EventExecutor, reactor, scFactory, 
                    ByteBuffer.allocateDirect(chunkSize * numBuffers), 10, TimeUnit.SECONDS);
            TCPTransportClient client = new TCPTransportClient(props);
            client.connect(5, TimeUnit.SECONDS);
                        
            // send data to it back-pressures
            byte[] sendData = new byte[chunkSize];
            ByteBuffer[] writeBuffers = new ByteBuffer[numBuffers];
            TransportSendResult result = new TransportSendResult();
//            long totalSent = 0;
            Future<Boolean> future;
            while(true) {
                for(int i = 0; i < numBuffers; i++) {
                    writeBuffers[i] = ByteBuffer.wrap(sendData);
                    writeBuffers[i].position(chunkSize);
                }
                long numWritten = client.writeAsync(writeBuffers, 100, TimeUnit.SECONDS, result);
                future = result.getFuture();
                if (numWritten == (chunkSize * numBuffers)) {
//                    totalSent += chunkSize * numBuffers;
                    assertTrue(result.isDone());
                    assertNull(future);
                } else if (numWritten >= 0) {
//                    totalSent += chunkSize * numBuffers;
                    assertFalse(result.isDone());
                    assertNotNull(future);
                } else {
                    try {
                        // performing a blocking send when the previous send 
                        // hasn't completed throws
                        client.write(writeBuffers, 100, TimeUnit.SECONDS);
                        fail("should throw");
                    } catch(TransportException e) {
                        // ok
                    }
                    break;
                }
            }
            
            client.close();
            
            try {
                future.get(15, TimeUnit.SECONDS);
                fail("should throw");
            } catch(ExecutionException e) {
                // ok
            }
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (reactor != null) {
                reactor.close();
            }
            if (server != null) {
                server.close();
            }
        }
    }

    public static void main(String[] args) {
    }
}
