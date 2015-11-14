package com.grb.util;

import junit.framework.TestCase;

public class BaseUnitTestCase extends TestCase {

    protected BaseUnitTestCase(String name) {
        super(name);
    }

    protected void printMethodName() {
        System.out.println(getMethodName(2));
    }

    protected String getMethodName(int index) {
        Throwable t = new Throwable();
        t.fillInStackTrace();
        StackTraceElement[] elements = t.getStackTrace();
        if (elements.length > index) {
            return elements[index].getMethodName();
        }
        return "";
    }
    
    /**
     * Fails a test with an exception.
     */
    protected void fail(Exception e) {
        String msg = String.format("%s: %s", e.getClass().getSimpleName(), e.getMessage());
        e.printStackTrace();
        super.fail(msg);
    }
}
