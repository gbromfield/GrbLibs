package com.grb.transport.tcp;

import java.io.IOException;
import java.nio.channels.SocketChannel;


public interface SocketChannelFactory {
    public SocketChannel newSocketChannel() throws IOException;
}
