package com.grb.subscriptionTree.test;

import java.util.ArrayList;
import java.util.HashSet;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.grb.subscriptionTree.SubscriptionMatchEntry;
import com.grb.subscriptionTree.SubscriptionRemoveResult;
import com.grb.subscriptionTree.SubscriptionTree;
import com.grb.util.BaseUnitTestCase;

public class SubscriptionTreeTest extends BaseUnitTestCase {
    private SubscriptionTree<String> subscriptions;
    
    private class SubscriptionDoesNotExistException extends Exception {
        private static final long serialVersionUID = 1L;

        public SubscriptionDoesNotExistException(String subscription) {
            super("Subscription \"" + subscription + "\" does not exist");
        }
        
        public SubscriptionDoesNotExistException(HashSet<String> subscriptions) {
            super("Subscriptions \"" + subscriptions + "\" do not exist");
        }
    }

    private class UnexpectedMatchException extends Exception {
        private static final long serialVersionUID = 1L;

        public UnexpectedMatchException(HashSet<String> matches) {
            super("Unexpected matches for " + matches);
        }
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(SubscriptionTreeTest.class);
        return suite;
    }

    public SubscriptionTreeTest() {
        super("SubscriptionTreeTest");
    }

    protected void setUp() {
        subscriptions = new SubscriptionTree<String>();
    }
    
    protected void addSubscription(String subscription) {
        subscriptions.put(subscription.getBytes(), subscription);
    }

    protected void removeSubscription(String subscription) throws SubscriptionDoesNotExistException {
        SubscriptionRemoveResult result = new SubscriptionRemoveResult();
        subscriptions.remove(subscription.getBytes(), subscription, result);
        if (!result.isFound()) {
            throw new SubscriptionDoesNotExistException(subscription);
        }
    }

    protected void lookup(String lookupStr, String... matches) 
        throws SubscriptionDoesNotExistException, UnexpectedMatchException {
        ArrayList<SubscriptionMatchEntry<String>> entries = new ArrayList<SubscriptionMatchEntry<String>>();
        subscriptions.get(lookupStr.getBytes(), entries);
        HashSet<String> matchSet = null;
        if (matches.length == 0) {
            // look for trivial match
            matchSet = new HashSet<String>(1);
            matchSet.add(lookupStr);
        } else {
            matchSet = new HashSet<String>(matches.length);
            for(int i = 0; i < matches.length; i++) {
                matchSet.add(matches[i]);
            }
        }
        HashSet<String> returnSet = new HashSet<String>(entries.size());
        for(int i = 0; i < entries.size(); i++) {
            SubscriptionMatchEntry<String> entry = entries.get(i);
            if (entry.getEntries().size() < 1) {
                System.err.println("No entries for a match returned");
                fail("No entries for a match returned");
            } else if (entry.getEntries().size() > 1) {
                System.err.println("Multiple entries for the same match returned");
                fail("Multiple entries for the same match returned");
            }
            if (!matchSet.remove(entry.getEntries().get(0))) {
                returnSet.add(entry.getEntries().get(0));
            }
        }
        if (returnSet.size() > 0) {
            throw new UnexpectedMatchException(returnSet);
        }
        if (matchSet.size() > 0) {
            throw new SubscriptionDoesNotExistException(matchSet);
        }
    }

    protected void lookupNotExists(String lookupStr) throws UnexpectedMatchException {
        ArrayList<SubscriptionMatchEntry<String>> entries = new ArrayList<SubscriptionMatchEntry<String>>();
        subscriptions.get(lookupStr.getBytes(), entries);
        HashSet<String> returnSet = new HashSet<String>(entries.size());
        for(int i = 0; i < entries.size(); i++) {
            SubscriptionMatchEntry<String> entry = entries.get(i);
            if (entry.getEntries().size() > 1) {
                System.err.println("Multiple entries for the same match returned");
                fail("Multiple entries for the same match returned");
            }
        }
        if (entries.size() > 0) {
            throw new UnexpectedMatchException(returnSet);
        }
    }

    // TEST START
    
    public void testGlobalWildcard1() {
        try {
            addSubscription(">");
            lookup("gaga", ">");
        } catch(Exception e) {
            System.err.println(e.getMessage());
            fail(e);
        }
    }

    public void testWildcard1() {
        try {
            addSubscription("*");
            lookup("gaga", "*");
        } catch(Exception e) {
            System.err.println(e.getMessage());
            fail(e);
        }
    }

    public void testWildcard2() {
        try {
            addSubscription("*/*/*");
            lookupNotExists("gaga");
            lookupNotExists("gaga/gaga");
            lookup("gaga/gaga/gaga", "*/*/*");            
            lookupNotExists("gaga/gaga/gaga/gaga");
        } catch(Exception e) {
            System.err.println(e.getMessage());
            fail(e);
        }
    }

    public void testWildcard3() {
        try {
            addSubscription("*");
            addSubscription("*/a");
            lookup("g", "*");
            lookup("g/a", "*/a");
            lookupNotExists("g/ab");
            lookupNotExists("g/ab");
            addSubscription("*/a/>");
            lookup("g/a/b", "*/a/>");
            lookup("g/a/b/c", "*/a/>");
            lookup("g/a/*", "*/a/>");
            lookup("g/a/*/g", "*/a/>");
        } catch(Exception e) {
            System.err.println(e.getMessage());
            fail(e);
        }
    }

    public void testWildcard4() {
        try {
            addSubscription("a");
            addSubscription("a*");
            addSubscription("ab");
            addSubscription("ab*");
            lookup("a", "a", "a*");            
            lookup("ab", "a*", "ab", "ab*");            
            lookup("abc", "a*", "ab*");            
            lookup("abcd", "a*", "ab*");            
        } catch(Exception e) {
            System.err.println(e.getMessage());
            fail(e);
        }
    }

    public void testWildcard5() {
        try {
            addSubscription("abc*");
            addSubscription("abc*/def");            
            addSubscription("abc*/def*");            
            lookup("abc", "abc*");            
            lookup("abc/def", "abc*/def", "abc*/def*");            
        } catch(Exception e) {
            System.err.println(e.getMessage());
            fail(e);
        }
    }

    public void testMerge1() {
        try {
            addSubscription("ab");
            addSubscription("ac");
            lookup("ab");
            lookup("ac");
            assertEquals(4, subscriptions.numNodes());
            removeSubscription("ab");
            assertEquals(2, subscriptions.numNodes());
            lookup("ac");
            lookupNotExists("ab");
            removeSubscription("ac");
            assertEquals(1, subscriptions.numNodes());
            lookupNotExists("ac");
        } catch(Exception e) {
            System.err.println(e.getMessage());
            fail(e);
        }
    }

    public void testMerge2() {
        try {
            addSubscription("*");
            addSubscription("*/*");
            addSubscription("*/*/*");
            lookup("gaga", "*");
            lookup("gaga/gaga", "*/*");
            lookup("gaga/gaga/gaga", "*/*/*");            

            removeSubscription("*");
            lookupNotExists("gaga");
            lookup("gaga/gaga", "*/*");
            lookup("gaga/gaga/gaga", "*/*/*"); 

            removeSubscription("*/*");
            lookupNotExists("gaga");
            lookupNotExists("gaga/gaga");
            lookup("gaga/gaga/gaga", "*/*/*"); 

            removeSubscription("*/*/*");
            lookupNotExists("gaga");
            lookupNotExists("gaga/gaga");
            lookupNotExists("gaga/gaga/gaga");
            assertEquals(1, subscriptions.numNodes());
        } catch(Exception e) {
            System.err.println(e.getMessage());
            fail(e);
        }
    }

    public void testMerge3() {
        try {
            addSubscription("*/*/*");
            addSubscription("*/*");
            addSubscription("*");
            lookup("gaga", "*");
            lookup("gaga/gaga", "*/*");
            lookup("gaga/gaga/gaga", "*/*/*");            

            removeSubscription("*/*/*");
            lookup("gaga", "*");
            lookup("gaga/gaga", "*/*");
            lookupNotExists("gaga/gaga/gaga"); 

            removeSubscription("*/*");
            lookup("gaga", "*");
            lookupNotExists("gaga/gaga");
            lookupNotExists("gaga/gaga/gaga"); 

            removeSubscription("*");
            lookupNotExists("gaga");
            lookupNotExists("gaga/gaga");
            lookupNotExists("gaga/gaga/gaga");
            assertEquals(1, subscriptions.numNodes());
        } catch(Exception e) {
            System.err.println(e.getMessage());
            fail(e);
        }
    }

    public void testMatchIntegrity() {
        try {
            String first = "abcdef";
            String firstVerify = new String(first);
            byte[] firstBytes = first.getBytes();
            byte[] firstBytesVerify = first.getBytes();
            subscriptions.put(firstBytes, first);
            ArrayList<SubscriptionMatchEntry<String>> matches = new ArrayList<SubscriptionMatchEntry<String>>();
            subscriptions.get(firstBytes, matches);
            assertTrue(matches.size() == 1);
            SubscriptionMatchEntry<String> entry = matches.get(0);
            byte[] sub = entry.getMatch();
            for(int i = 0; i < firstBytesVerify.length; i++) {
                assertEquals(firstBytesVerify[i], sub[i]);
            }
            assertTrue(entry.getEntries().size() == 1);
            assertEquals(firstVerify, entry.getEntries().get(0));

            // add another subscription and retest the previous list
            String second = "abc";
//            String secondVerify = new String(second);
            byte[] secondBytes = second.getBytes();
//            byte[] secondBytesVerify = second.getBytes();
            subscriptions.put(secondBytes, second);

            assertTrue(matches.size() == 1);
            for(int i = 0; i < firstBytesVerify.length; i++) {
                assertEquals(firstBytesVerify[i], sub[i]);
            }
            assertTrue(entry.getEntries().size() == 1);
            assertEquals(firstVerify, entry.getEntries().get(0));

        } catch(Exception e) {
            System.err.println(e.getMessage());
            fail(e);
        }
    }

    public void testGreaterThan1() {
        try {
            addSubscription("asd>");
            lookup("asd>");
            assertEquals(2, subscriptions.numNodes());
            removeSubscription("asd>");
            assertEquals(1, subscriptions.numNodes());
        } catch(Exception e) {
            System.err.println(e.getMessage());
            fail(e);
        }
    }

    public void testGreaterThan2() {
        try {
            addSubscription(">asd");
            lookup(">asd");
            assertEquals(2, subscriptions.numNodes());
            removeSubscription(">asd");
            assertEquals(1, subscriptions.numNodes());            
        } catch(Exception e) {
            System.err.println(e.getMessage());
            fail(e);
        }
    }

    public void testGreaterThan3() {
        try {
            addSubscription("as>d");
            lookup("as>d");
            assertEquals(2, subscriptions.numNodes());
            removeSubscription("as>d");
            assertEquals(1, subscriptions.numNodes());            
        } catch(Exception e) {
            System.err.println(e.getMessage());
            fail(e);
        }
    }

    public void testGreaterThan4() {
        try {
            addSubscription("as>d*");
            lookup("as>dsdfsdf", "as>d*");
            assertEquals(3, subscriptions.numNodes());
            removeSubscription("as>d*");
            assertEquals(1, subscriptions.numNodes());            
        } catch(Exception e) {
            System.err.println(e.getMessage());
            fail(e);
        }
    }

    public void testGreaterThan5() {
        try {
            addSubscription("abba/as>d*");
            lookup("abba/as>dsdfsdf", "abba/as>d*");
            assertEquals(4, subscriptions.numNodes());
            removeSubscription("abba/as>d*");
            assertEquals(1, subscriptions.numNodes());            
        } catch(Exception e) {
            System.err.println(e.getMessage());
            fail(e);
        }
    }

    public void testGreaterThan6() {
        try {
            addSubscription("a>/>a");
            lookup("a>/>a", "a>/>a");
            assertEquals(3, subscriptions.numNodes());
            lookupNotExists("a>/a");
            removeSubscription("a>/>a");
            assertEquals(1, subscriptions.numNodes());            
        } catch(Exception e) {
            System.err.println(e.getMessage());
            fail(e);
        }
    }

    public void testGreaterThan7() {
        try {
            addSubscription("a>/>>");
            lookup("a>/>>", "a>/>>");
            assertEquals(3, subscriptions.numNodes());
            lookupNotExists("a>/gaga");
            removeSubscription("a>/>>");
            assertEquals(1, subscriptions.numNodes());            
        } catch(Exception e) {
            System.err.println(e.getMessage());
            fail(e);
        }
    }

    public void testGreaterThan8() {
        try {
            addSubscription(">>");
            lookup(">>", ">>");
            assertEquals(2, subscriptions.numNodes());
            lookupNotExists("gaga");
            removeSubscription(">>");
            assertEquals(1, subscriptions.numNodes());            
        } catch(Exception e) {
            System.err.println(e.getMessage());
            fail(e);
        }
    }

    public void testGreaterThan9() {
        try {
            addSubscription(">>>>>");
            lookup(">>>>>", ">>>>>");
            assertEquals(2, subscriptions.numNodes());
            lookupNotExists("gaga");
            removeSubscription(">>>>>");
            assertEquals(1, subscriptions.numNodes());   
        } catch(Exception e) {
            System.err.println(e.getMessage());
            fail(e);
        }
    }

}
