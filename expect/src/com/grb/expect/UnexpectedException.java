package com.grb.expect;

public class UnexpectedException extends Exception {
    private static final long serialVersionUID = -6600903685969967562L;

    public UnexpectedException(String message) {
        super(message);
    }
    
    public UnexpectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
