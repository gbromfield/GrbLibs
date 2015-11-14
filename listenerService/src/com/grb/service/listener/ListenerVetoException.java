package com.grb.service.listener;

public class ListenerVetoException extends Exception {
    private static final long serialVersionUID = 5329994870686302738L;

    public ListenerVetoException() {
    }

    public ListenerVetoException(String message) {
        super(message);
    }

    public ListenerVetoException(Throwable cause) {
        super(cause);
    }

    public ListenerVetoException(String message, Throwable cause) {
        super(message, cause);
    }
}
