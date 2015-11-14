package com.grb.transport;

import java.nio.ByteBuffer;

public interface TransportReadListener {
    // Return true to continue reading. This is the way to suspend reading from the socket.
    // if numBytesRead equals 0 then the transport has been closed. and return false.
    boolean onTransportRead(ByteBuffer readBuffer, int numBytesRead);
}
