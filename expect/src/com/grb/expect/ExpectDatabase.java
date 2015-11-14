package com.grb.expect;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


public class ExpectDatabase {
    
    public class ExpectDatabaseEntry {
        public String name;
        public Outcome outcome;
        public ConfigurationMatcher configurationMatcher;
        public ExceptionMatcher exceptionMatcher;
        @Override
        public String toString() {
            return name;
        }
    }
    
    private HashMap<String, ExpectDatabaseEntry> mDB;
    private String[] mSearchPkgPrefixs;

    public ExpectDatabase(String[] searchPkgPrefixs) {
        mDB = new HashMap<String, ExpectDatabaseEntry>();
        mSearchPkgPrefixs = searchPkgPrefixs;
    }

    public void add(String name, Outcome outcome, ConfigurationMatcher configurationMatcher) {
        add(name, outcome, configurationMatcher, null);
    }
    
    public void add(String name, Outcome outcome, ConfigurationMatcher configurationMatcher, ExceptionMatcher exceptionMatcher) {
        if (((outcome == Outcome.Failure) || (outcome == Outcome.Success)) && (exceptionMatcher != null)) {
            throw new IllegalArgumentException("An exception matcher can only be specified with an outcome of exception");
        }
        if ((outcome == Outcome.Exception) && (exceptionMatcher == null)) {
            throw new IllegalArgumentException("An exception matcher must be specified for outcomes of exception");
        }
        ExpectDatabaseEntry entry = new ExpectDatabaseEntry();
        entry.name = name;
        entry.outcome = outcome;
        entry.configurationMatcher = configurationMatcher;
        entry.exceptionMatcher = exceptionMatcher;
        mDB.put(name, entry);
    }
    
    public void remove(String name) {
        mDB.remove(name);
    }
    
    public void clear() {
        mDB.clear();
    }

    /**
     * Returns the Expect configuration that matches. It may be null if no match.
     * 
     * @param ctx
     * @param stackDepth
     * @return The Expect configuration that matches. It may be null if no match.
     * @throws UnexpectedOutcomeException When a failure was supposed to happen.
     */
    public String handleSuccess(Object ctx) throws UnexpectedException {
        List<ExpectDatabaseEntry> expectEntries = getExpectDatabaseEntries();
        Iterator<ExpectDatabaseEntry> it = expectEntries.iterator();
        String matchingExpect = null;
        while(it.hasNext()) {
            ExpectDatabaseEntry expectEntry = it.next();
            if (expectEntry.configurationMatcher.configurationMatches(ctx)) {
                if (expectEntry.outcome == Outcome.Success) {
                    matchingExpect = expectEntry.name;
                } else {
                    throw new UnexpectedException("Matched \"" + expectEntry.name + "\", but outcome \"" + Outcome.Success + 
                            "\" didn't match expected outcome of \"" + expectEntry.outcome + "\"");
                }
            }
        }        
        return matchingExpect;
    }

    /**
     * Returns the Expect configuration that matches.
     * 
     * @param ctx
     * @param stackDepth
     * @return The Expect configuration that matches.
     * @throws UnexpectedOutcomeException When a failure was supposed to happen.
     * @throws UnexpectedFailureException 
     */
    public String handleFailure(Object ctx) throws UnexpectedException {
        List<ExpectDatabaseEntry> expectEntries = getExpectDatabaseEntries();
        Iterator<ExpectDatabaseEntry> it = expectEntries.iterator();
        while(it.hasNext()) {
            ExpectDatabaseEntry expectEntry = it.next();
            if (expectEntry.configurationMatcher.configurationMatches(ctx)) {
                if (expectEntry.outcome == Outcome.Failure) {
                    return expectEntry.name;
                } else {
                    throw new UnexpectedException("Matched \"" + expectEntry.name + "\", but outcome \"" + Outcome.Failure + 
                            "\" didn't match expected outcome of \"" + expectEntry.outcome + "\"");
                }
            }
        }        
        throw new UnexpectedException("Failure did not match any expectations");
    }

    /**
     * 
     * @param ctx
     * @param stackDepth
     * @param e
     * @return
     * @throws UnexpectedOutcomeException
     */
    public String handleException(Object ctx, Exception e) throws UnexpectedException {
        List<ExpectDatabaseEntry> expectEntries = getExpectDatabaseEntries();
        Iterator<ExpectDatabaseEntry> it = expectEntries.iterator();
        while(it.hasNext()) {
            ExpectDatabaseEntry expectEntry = it.next();
            if (expectEntry.configurationMatcher.configurationMatches(ctx)) {
                if (expectEntry.outcome == Outcome.Exception) {
                    if (expectEntry.exceptionMatcher.exceptionMatches(e)) {
                        return expectEntry.name;
                    } else {
                        throw new UnexpectedException("Matched \"" + expectEntry.name + "\", but thrown exception didn't match expected exception", e);
                    }
                } else {
                    throw new UnexpectedException("Matched \"" + expectEntry.name + "\", but outcome \"" + Outcome.Exception + 
                            "\" didn't match expected outcome of \"" + expectEntry.outcome + "\"", e);
                }
            }
        }        
        throw new UnexpectedException("Exception did not match any expectations", e);
    }

    public List<ExpectDatabaseEntry> getExpectDatabaseEntries() {
        ArrayList<ExpectDatabaseEntry> expectEntries = new ArrayList<ExpectDatabaseEntry>();
    	if (mSearchPkgPrefixs != null) {
            try {
                Throwable t = new Throwable();
                t.fillInStackTrace();
                StackTraceElement[] elements = t.getStackTrace();
                for(int i = 0; i < elements.length; i++) {
                	for(int j = 0; j < mSearchPkgPrefixs.length; j++) {
                		if (elements[i].getClassName().startsWith(mSearchPkgPrefixs[j])) {
                            Class<?> testClass = Class.forName(elements[i].getClassName());
                            Method[] methods = testClass.getMethods();
                            for(int k = 0; k < methods.length; k++) {
                                if (methods[k].getName().equals(elements[i].getMethodName())) {
                                    getExpectDatabaseEntries(methods[k].getAnnotation(Expect.class), expectEntries);
                                    if (expectEntries.size() == 0) {
                                        getExpectDatabaseEntries(testClass.getAnnotation(Expect.class), expectEntries);
                                    }                			
                                }
                            }
                            break;
                		}
                	}
                }
            } catch(Exception e) {
//                e.printStackTrace();
            }
    	}
        return expectEntries;
    }

    public void getExpectDatabaseEntries(Expect expect, List<ExpectDatabaseEntry> expectEntries) {
        if (expect != null) {
            String[] expectStrs = expect.value();
            if (expectStrs != null) {
                for(int i = 0; i < expectStrs.length; i++) {
                    ExpectDatabaseEntry entry = mDB.get(expectStrs[i]);
                    if (entry != null) {
                        expectEntries.add(entry);
                    }
                }
            }            
        }
    }

    @Override
    public String toString() {
        return mDB.keySet().toString();
    }
}
