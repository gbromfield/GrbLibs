package com.grb.transport;

public enum TransportOperation {
    Closing,
    Connecting,
    Reading,
    Sending,
    Unknown     // for internal errors
}
