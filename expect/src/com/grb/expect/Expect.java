package com.grb.expect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Expect provides a mechanism to alter the success/fail behaviour
 * of a unit test without modifying the code.
 * 
 * Expect handles the following 3 scenarios:
 * <li>Successful completion of a unit test.
 * <li>Failure of a unit test with an exception.
 * <li>Failure of a unit test by a failure to throw an exception
 * 
 * The setup of unit tests have the following 2 patterns:
 * 1)
 * @Expect({Condition1, Condition2, ....})
 * public void testExample1() {
 *     try {
 *         ...
 *         handleSuccess();
 *     } catch(Exception e) {
 *         handleException(e);
 *     }
 * }
 * 
 * 2)
 * @Expect({Condition1, Condition2, ....})
 * public void testExample2() {
 *     try {
 *         ...
 *         try {
 *             ... // this should throw
 *             handleFailure();
 *         } catch(SpecificException e) {
 *             handleSuccess();
 *         }
 *     } catch(Exception e) {
 *         handleException(e);
 *     }
 * }
 * 
 * The arguments to Expect are an array of strings. 
 * <p>handleSuccess() verifies that there was no exception that should have been thrown.
 * <p>handleFailure() verifies that no exception was correct for the configuration.
 * <p>handleException() verifies that the exception thrown was the correct one for the configuration.
 * <p>
 * For a unit test that always throws, the following pattern should be used:
 * <p>
 * 
 * <p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Expect {
    String[] value();
}
