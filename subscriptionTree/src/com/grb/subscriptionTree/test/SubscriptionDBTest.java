package com.grb.subscriptionTree.test;

import java.util.ArrayList;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.grb.subscriptionTree.SubscriptionMap;
import com.grb.subscriptionTree.SubscriptionMatchEntry;
import com.grb.util.BaseUnitTestCase;

public class SubscriptionDBTest extends BaseUnitTestCase {

    public class NoMatchException extends Exception {
        private static final long serialVersionUID = 1L;
        
        public NoMatchException(ArrayList<String> matches) {
            super("No matches for " + matches);
        }
    }

    public class UnexpectedMatchException extends Exception {
        private static final long serialVersionUID = 1L;
        
        public UnexpectedMatchException(ArrayList<String> matches) {
            super("Unexpected matches for " + matches);
        }
    }

    public class SubscriptionTest {
        public String mTestId;
        public String[] mSubscriptions;
        public String mSubscriptionToLookup;
        public ArrayList<String> mMatches;

        public SubscriptionTest(String[][] args) {
            assertEquals(4, args.length);
            assertEquals(1, args[0].length);
            assertEquals(1, args[2].length);
            mTestId = args[0][0];
            mSubscriptions = args[1];
            mSubscriptionToLookup = args[2][0];
            if (args[3] == null) {
                mMatches = null;
            } else {
                mMatches = new ArrayList<String>();
                for(int i = 0; i < args[3].length; i++) {
                    mMatches.add(args[3][i]);
                }
            }
        }

        public void run() throws UnexpectedMatchException, NoMatchException {
            SubscriptionMap<String> db = new SubscriptionMap<String>();
            if (mSubscriptions != null) {
                db.startTransaction();
                try {
                    for(int i = 0; i < mSubscriptions.length; i++) {
                        db.put(mSubscriptions[i], mSubscriptions[i]);
                    }
                } finally {
                    db.commit();
                }
            }
            ArrayList<SubscriptionMatchEntry<String>> entries = new ArrayList<SubscriptionMatchEntry<String>>();
            db.get(mSubscriptionToLookup.getBytes(), entries);
            if (mMatches == null) {
                if (entries.size() > 0) {
                    ArrayList<String> matchList = new ArrayList<String>();
                    for(int i = 0; i < entries.size(); i++) {
                        matchList.add(new String(entries.get(i).getMatch()));
                    }
                    throw new UnexpectedMatchException(matchList);
                }
            } else {
                ArrayList<String> noMatchList = new ArrayList<String>();
                for (int i = 0; i < entries.size(); i++) {
                    SubscriptionMatchEntry<String> entry = entries.get(i);
                    String entryStr = new String(entry.getMatch());
                    for (int j = 0; j < entry.getEntries().size(); j++) {
                        boolean found = false;
                        for (int k = 0; k < mMatches.size(); k++) {
                            if (mMatches.get(k).equals(entryStr)) {
                                mMatches.remove(k);
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            noMatchList.add(entryStr);
                        }
                    }
                }
                if (noMatchList.size() > 0) {
                    throw new UnexpectedMatchException(noMatchList);
                }
                if (mMatches.size() > 0) {
                    throw new NoMatchException(mMatches);
                }
            }
        }
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(SubscriptionDBTest.class);
        return suite;
    }

    public SubscriptionDBTest() {
        super("SubscriptionDBTest");
    }

    protected void setUp() {
    }

    private void run(String[][] args) throws UnexpectedMatchException, NoMatchException {
        SubscriptionTest test = new SubscriptionTest(args);
        test.run();
    }
    
    public void testCode1() {
        String[][] testSetup = {
                {getMethodName(1)},     // id
                null,                   // subscriptions
                {"gaga"},               // to lookup
                {"gaga"}                // matches
        };
        try {
            try {
                run(testSetup);
                fail("should throw");
            } catch(NoMatchException e) {
                // expected
            }
        } catch(Exception e) {
            fail(e);
        }
    }

    public void testCode2() {
        String[][] testSetup = {
                {getMethodName(1)},     // id
                {">"},                  // subscriptions
                {"gaga"},               // to lookup
                null                    // matches
        };
        try {
            try {
                run(testSetup);
                fail("should throw");
            } catch(UnexpectedMatchException e) {
                // expected
            }
        } catch(Exception e) {
            fail(e);
        }
    }

    public void testCode3() {
        String[][] testSetup = {
                {getMethodName(1)},     // id
                {">", ">"},             // subscriptions
                {"gaga"},               // to lookup
                {">"}                   // matches
        };
        try {
            try {
                run(testSetup);
                fail("should throw");
            } catch(UnexpectedMatchException e) {
                // expected
            }
        } catch(Exception e) {
            fail(e);
        }
    }

    public void testCode4() {
        String[][] testSetup = {
                {getMethodName(1)},     // id
                {">", ">"},             // subscriptions
                {"gaga"},               // to lookup
                {">", ">"}              // matches
        };
        try {
            run(testSetup);
        } catch(Exception e) {
            fail(e);
        }
    }

    public void testCode5() {
        String[][] testSetup = {
                {getMethodName(1)},     // id
                {"gaga", ">"},          // subscriptions
                {"gaga"},               // to lookup
                {">", "gaga"}           // matches
        };
        try {
            run(testSetup);
        } catch(Exception e) {
            fail(e);
        }
    }

    public void testCode6() {
        String[][] testSetup = {
                {getMethodName(1)},     // id
                {"gaga", ">"},          // subscriptions
                {"blub"},               // to lookup
                {">"}                   // matches
        };
        try {
            run(testSetup);
        } catch(Exception e) {
            fail(e);
        }
    }
    
    public void testCode7() {
        String[][] testSetup = {
                {getMethodName(1)},     // id
                {"gaga", "blub", "gablub"}, // subscriptions
                {"blagaga"},            // to lookup
                {"abc"}                 // matches
        };
        try {
            try {
                run(testSetup);
                fail("should throw");
            } catch(NoMatchException e) {
                // expected
            }
        } catch(Exception e) {
            fail(e);
        }
    }

    public void testCode8() {
        String[][] testSetup = {
                {getMethodName(1)},     // id
                null,                   // subscriptions
                {"blagaga"},            // to lookup
                {"abc"}                 // matches
        };
        try {
            try {
                run(testSetup);
                fail("should throw");
            } catch(NoMatchException e) {
                // expected
            }
        } catch(Exception e) {
            fail(e);
        }
    }

    public void testCode9() {
        String[][] testSetup = {
                {getMethodName(1)},     // id
                null,                   // subscriptions
                {"blagaga"},            // to lookup
                null                    // matches
        };
        try {
            run(testSetup);
        } catch(Exception e) {
            fail(e);
        }
    }

    public void testCode10() {
        String[][] testSetup = {
                {getMethodName(1)},     // id
                {"t*"},                 // subscriptions
                {"t"},                  // to lookup
                {"t*"}                  // matches
        };
        try {
            run(testSetup);
        } catch(Exception e) {
            fail(e);
        }
    }

    public void testCode11() {
        String[][] testSetup = {
                {getMethodName(1)},     // id
                {"t", "t*"},            // subscriptions
                {"t"},                  // to lookup
                {"t", "t*"}             // matches
        };
        try {
            run(testSetup);
        } catch(Exception e) {
            fail(e);
        }
    }

    public void testCode12() {
        String[][] testSetup = {
                {getMethodName(1)},     // id
                {"t*/x"},               // subscriptions
                {"t/x"},                // to lookup
                {"t*/x"}                // matches
        };
        try {
            run(testSetup);
        } catch(Exception e) {
            fail(e);
        }
    }

    public void testCode13() {
        String[][] testSetup = {
                {getMethodName(1)},     // id
                {"t/>", "t/x", "t*/x", "t*/>", "t*/*", "t*/x*", "t/x*", "t/*", "t*/*/>"},// subscriptions
                {"t/x"},                // to lookup
                {"t/x", "t*/x", "t*/>", "t*/*", "t*/x*", "t/x*", "t/*"} // matches
        };
        try {
            run(testSetup);
        } catch(Exception e) {
            fail(e);
        }
    }

    public void testCode14() {
        String[][] testSetup = {
                {getMethodName(1)},     // id
                {"t/>", "t/x", "t*/x", "t*/>", "t*/*", "t*/x*", "t/x*", "t/*", "t*/*/>"},// subscriptions
                {"tx/x"},                // to lookup
                {"t*/x", "t*/>", "t*/*", "t*/x*"} // matches
        };
        try {
            run(testSetup);
        } catch(Exception e) {
            fail(e);
        }
    }

    public void testCode15() {
        String[][] testSetup = {
                {getMethodName(1)},     // id
                {"t/>", "t/x", "t*/x", "t*/>", "t*/*", "t*/x*", "t/x*", "t/*", "t*/*/>",
                 "tt/>", "tt/x", "tt*/x", "tt*/>", "tt*/*", "tt*/x*", "tt/x*", "tt/*", "tt*/*/>"
                }, // subscriptions
                {"tt/xx"},                // to lookup
                {"t*/>", "t*/*", "t*/x*", "tt/>", "tt*/>", "tt*/*", "tt*/x*", "tt/x*", "tt/*"} // matches
        };
        try {
            run(testSetup);
        } catch(Exception e) {
            fail(e);
        }
    }
}
