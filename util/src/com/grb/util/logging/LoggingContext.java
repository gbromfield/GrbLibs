package com.grb.util.logging;

import org.apache.commons.logging.Log;

public interface LoggingContext {
    public String formatLog(String msg);
    public Log getLog();
}
