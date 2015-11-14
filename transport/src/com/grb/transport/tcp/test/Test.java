package com.grb.transport.tcp.test;

import com.grb.reactor.ReactorThread;
import com.grb.transport.tcp.TCPTransportClient;
import com.grb.transport.tcp.TCPTransportServer;
import com.grb.transport.tcp.TCPTransportServerConnectionListener;
import com.grb.transport.tcp.TCPTransportServerProperties;

public class Test implements TCPTransportServerConnectionListener {

    public void onNewConnection(TCPTransportClient client) {
        System.out.println("Accepted Connection!");
    }    

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            ReactorThread reactor = new ReactorThread();
            reactor.startAsDaemon();
            TCPTransportServerProperties serverProps = new TCPTransportServerProperties(4530, reactor, null);
            TCPTransportServer server = new TCPTransportServer(serverProps, new Test());
            server.startAccepting();
            Thread.sleep(360000);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}
