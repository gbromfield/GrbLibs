package com.grb.transport;

public class TransportException extends Exception {
    private static final long serialVersionUID = -374751666526770413L;

    public TransportException() {
        super();
    }

    public TransportException(String message) {
        super(message);
    }

    public TransportException(Throwable cause) {
        super(cause);
    }

    public TransportException(String message, Throwable cause) {
        super(message, cause);
    }
}
