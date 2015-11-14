package com.grb.expect;


public class DefaultExceptionMatcher implements ExceptionMatcher {

    public enum ClassMatch {
        SameClass,
        AndSubclasses;
        
        public boolean match(Class<?> src, Class<?> comparer) {
            if (comparer == null) {
                return false;
            }
            if (SameClass.name().equals(this.name())) {
                return src.equals(comparer);
            } else {
                return src.isAssignableFrom(comparer);
            }
        }        
    }
    
    public enum StringMatch {
        Exact,
        IgnoreCase,
        IndexOf,
        StartsWith;
        
        public boolean match(String src, String comparer) {
            if (comparer == null) {
                return false;
            }
            if (Exact.name().equals(this.name())) {
                return comparer.equals(src);
            } else if (IgnoreCase.name().equals(this.name())) {
                return comparer.equalsIgnoreCase(src);
            } else if (IndexOf.name().equals(this.name())) {
                return comparer.indexOf(src) != -1;
            } else {
                return comparer.startsWith(src);
            }
        }
    }
    
    protected Class<?> mClass;
    protected ClassMatch mClassMatch;
    protected String mMessage;
    protected StringMatch mMessageMatch;
  
    public DefaultExceptionMatcher(Class<?> exceptionClass, ClassMatch classMatch) {
        this(exceptionClass, classMatch, null, null);
    }
    
    public DefaultExceptionMatcher(Class<?> exceptionClass, ClassMatch classMatch, 
            String exceptionMessage, StringMatch messageMatch) {
        mClass = exceptionClass;
        mClassMatch = classMatch;
        mMessage = exceptionMessage;
        mMessageMatch = messageMatch;
    }

    public boolean exceptionMatches(Exception exception) {
        if (exception == null) {
            return false;
        }
        if (!mClassMatch.match(mClass, exception.getClass())) {
            return false;
        }
        if (mMessageMatch != null) {
            if (!mMessageMatch.match(mMessage, exception.getMessage()) ) {
                return false;
            }
        }
        return true;
    }
}
