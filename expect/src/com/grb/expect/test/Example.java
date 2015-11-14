package com.grb.expect.test;

import com.grb.expect.ConfigurationMatcher;
import com.grb.expect.DefaultExceptionMatcher;
import com.grb.expect.Expect;
import com.grb.expect.ExpectDatabase;
import com.grb.expect.Outcome;
import com.grb.expect.UnexpectedException;
import com.grb.expect.DefaultExceptionMatcher.ClassMatch;
import com.grb.expect.DefaultExceptionMatcher.StringMatch;

public class Example {

    static private final String IllegalArgument = "IllegalArgument";

    static private final ExpectDatabase ExpectDB = PopulateExpectDB();
    
    static private ExpectDatabase PopulateExpectDB() {
        ExpectDatabase db = new ExpectDatabase(new String[] {"com.grb.expect.test"});
        
        // Expect an IllegalArgumentException with text "gaga" in all configurations
        db.add(IllegalArgument, Outcome.Exception,
                new ConfigurationMatcher() {
                    public boolean configurationMatches(Object ctx) {
                        return true;    // matches in all conditions
                    }
                },
                new DefaultExceptionMatcher(IllegalArgumentException.class, ClassMatch.SameClass, "gaga", StringMatch.Exact));

        return db;
    }
    
    // always succeeds - no expectations
    public void test1() {
        try {
            int i = 0;
            i = i + 1;
            handleSuccess();
        } catch(Exception e) {
            handleException(e);
        }
    }

    // simulates a throw with no expectation of a throw - this fails
    @SuppressWarnings("unused")
    public void test2() {
        try {
            if (true) {
                throw new IllegalArgumentException("gaga");
            }
            handleSuccess();
        } catch(Exception e) {
            handleException(e);
        }
    }

    // same as test2 but now expects the illegal arrgument - so succeeds
    @SuppressWarnings("unused")
    @Expect(IllegalArgument)
    public void test3() {
        try {
            if (true) {
                throw new IllegalArgumentException("gaga");
            }
            handleSuccess();
        } catch(Exception e) {
            handleException(e);
        }
    }

    // same as test3 but now doesn't throw the illegal arrgument - so fails
    // because "IllegalArgument" is defined as matching in all conditions.
    // handleSuccess() finds that "IllegalArgument" matches but there was
    // no exception thrown.
    @Expect(IllegalArgument)
    public void test4() {
        try {
//            if (true) {
//                throw new IllegalArgumentException("gaga");
//            }
            handleSuccess();
        } catch(Exception e) {
            handleException(e);
        }
    }

    private void handleSuccess() {
        try {
            String expect = ExpectDB.handleSuccess(null);
            if (expect == null) {
                printOutcome("Pass", "No Conditions Matched");
            } else {
                printOutcome("Pass", "Condition Matched = \"" + expect + "\"");
            }
        } catch (UnexpectedException e) {
            printOutcome("Failed", e.getMessage());
        }
    }
    
    @SuppressWarnings("unused")
    private void handleFailure() {
        try {
            String expect = ExpectDB.handleFailure(null);
            printOutcome("Pass", "a failure occurred but was expected by \"" + expect + "\"");
        } catch (UnexpectedException e) {
            printOutcome("Failed", e.getMessage());
        }
    }
    
    private void handleException(Exception e) {
        try {
            String expect = ExpectDB.handleException(null, e);
            printOutcome("Pass", "an exception was thrown but was expected by \"" + expect + "\"");
        } catch (UnexpectedException e1) {
            printOutcome("Failed", e1.getMessage());
        }
    }
    
    private void printOutcome(String testOutcome, String message) {
        Throwable t = new Throwable();
        t.fillInStackTrace();
        StackTraceElement[] elements = t.getStackTrace();
        String methodName = "";
        if (elements.length > 2) {
            methodName = elements[2].getMethodName();
        }
        System.out.println(methodName + "(" + testOutcome + ") - " + message);
    }
 
    public static void main(String[] args) {
        Example example = new Example();
        example.test1();
        example.test2();
        example.test3();
        example.test4();
    }
}
