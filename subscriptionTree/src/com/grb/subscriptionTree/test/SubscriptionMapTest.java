package com.grb.subscriptionTree.test;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.grb.subscriptionTree.SubscriptionHashMap;
import com.grb.subscriptionTree.SubscriptionMatchEntry;
import com.grb.subscriptionTree.SubscriptionRemoveResult;
import com.grb.util.BaseUnitTestCase;

public class SubscriptionMapTest  extends BaseUnitTestCase {
    
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(SubscriptionMapTest.class);
        return suite;
    }

    public SubscriptionMapTest() {
        super("SubscriptionMapTest");
    }

    public void testSubscriptionMap1() {
        try {
            Integer one = new Integer(1);
            Integer two = new Integer(2);
            SubscriptionHashMap<Integer> map = new SubscriptionHashMap<Integer>();
            assertTrue(map.getMap() == null);
            SubscriptionHashMap<Integer> newMap = map.put("gaga".getBytes(), one);
            assertTrue(map.getMap() == null);
            assertTrue(newMap.getMap() != null);
            List<SubscriptionMatchEntry<Integer>> entries = new ArrayList<SubscriptionMatchEntry<Integer>>();
            map.get("gaga".getBytes(), entries);
            assertEquals(0, entries.size());
            entries.clear();
            newMap.get("gaga".getBytes(), entries);
            assertEquals(1, entries.size());
            assertEquals(1, entries.get(0).getEntries().size());
            map = newMap;
            entries.clear();
            newMap = newMap.put("gaga".getBytes(), two);
            newMap.get("gaga".getBytes(), entries);
            assertEquals(1, entries.size());
            assertEquals(2, entries.get(0).getEntries().size());
            map = newMap;
            newMap = newMap.remove("gaga".getBytes(), one, new SubscriptionRemoveResult());
            entries.clear();
            newMap.get("gaga".getBytes(), entries);
            assertEquals(1, entries.size());
            assertEquals(1, entries.get(0).getEntries().size());
            map = newMap;
            newMap = newMap.remove("gaga".getBytes(), two, new SubscriptionRemoveResult());
            entries.clear();
            newMap.get("gaga".getBytes(), entries);
            assertEquals(0, entries.size());
        } catch(Exception e) {
            System.err.println(e.getMessage());
            fail(e);
        }
    }
}
