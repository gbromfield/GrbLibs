package com.grb.expect;

public interface ExceptionMatcher {
    public boolean exceptionMatches(Exception exception);
}
