package com.grb.util.property;

import java.util.ArrayList;
import java.util.HashMap;

public class PropertySource<T> implements Cloneable {
    /**
     * Convenience priority fields.
     * Looks better than just having "1" on the command line.
     */
    static final public int PRIORITY_1 = 1;
    static final public int PRIORITY_2 = 2;
    static final public int PRIORITY_3 = 3;
    static final public int PRIORITY_4 = 4;
    static final public int PRIORITY_5 = 5;
    static final public int PRIORITY_6 = 6;
    static final public int PRIORITY_7 = 7;

    static final public int HIGHEST_PRIORITY = PRIORITY_1;
    static final public int LOWEST_PRIORITY = Integer.MAX_VALUE;
    static final public int DEFAULT_PRIORITY = PRIORITY_1;
    
    /**
     * Convenience nullValid fields.
     * Looks better than just having "true" or "false" on the command line.
     */
    static final public boolean NULL_VALID = true;
    static final public boolean NULL_INVALID = false;

    static public boolean DEFAULT_NULL_VALID = NULL_INVALID;
        
    protected String mName;
    protected int mPriority;
    protected T mValue;
    protected boolean mIsSet;
    protected boolean mNullValid;
    protected ArrayList<PropertySourceChangeListener<T>> mListeners;
    protected ArrayList<VetoablePropertySourceChangeListener<T>> mVetoableListeners;
    protected PropertyConverter mConverter;
    protected HashMap<String, Object> mUserData;
    protected boolean mTrimStrings;
    protected boolean mPrintable;
    
    /**
     * Deep Copy Constructor
     * 
     * @param source PropertySource to be copied.
     */
    public PropertySource(PropertySource<T> source) {
        mName = source.mName;
        mPriority = source.mPriority;
        mValue = source.mValue;
        mIsSet = source.mIsSet;
        mNullValid = source.mNullValid;
        mListeners = null;
        mVetoableListeners = null;
        mConverter = source.mConverter;
        mUserData = source.mUserData;
        mTrimStrings = source.mTrimStrings;
        mPrintable = source.mPrintable;
    }
    
    public PropertySource(String name) {
        this(name, DEFAULT_PRIORITY, DEFAULT_NULL_VALID, null);
    }

    public PropertySource(String name, int priority) {
        this(name, priority, DEFAULT_NULL_VALID, null);
    }

    public PropertySource(String name, boolean nullValid) {
        this(name, DEFAULT_PRIORITY, nullValid, null);
    }

    public PropertySource(String name, PropertyConverter converter) {
        this(name, DEFAULT_PRIORITY, DEFAULT_NULL_VALID, converter);
    }

    public PropertySource(String name, int priority, boolean nullValid) {
    	this(name, priority, nullValid, null);
    }

    public PropertySource(String name, int priority, PropertyConverter converter) {
        this(name, priority, DEFAULT_NULL_VALID, converter);
    }
    
    public PropertySource(String name, int priority, boolean nullValid, PropertyConverter converter) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        if ((priority < HIGHEST_PRIORITY) || (priority > LOWEST_PRIORITY)) {
            throw new IllegalArgumentException("Priority value out of range - " + priority + 
                    ", must be in the range [" + HIGHEST_PRIORITY + "," + LOWEST_PRIORITY + "]");
        }
        mName = name;
        mPriority = priority;
        mValue = null;
        mIsSet = nullValid;
        mNullValid = nullValid;
        mListeners = null;
        mVetoableListeners = null;
        mConverter = converter;
        mUserData = null;
        mTrimStrings = false;
        mPrintable = true;
    }

    public String getName() {
        return mName;
    }
    
	public int getPriority() {
        return mPriority;
    }
    
    public T getValue() { 
        return mValue;
    }
    
    public boolean isSet() {
        return mIsSet;
    }

    public boolean isNullValid() {
        return mNullValid;
    }

    public void setTrimStrings(boolean trimStrings) {
        mTrimStrings = trimStrings;
    }
    
    public boolean getTrimStrings() {
        return mTrimStrings;
    }

    public void setPrintable(boolean printable) {
        mPrintable = printable;
    }
    
    public boolean getPrintable() {
        return mPrintable;
    }

    public PropertyConverter getConverter() {
    	return mConverter;
    }
            
    public void addListener(PropertySourceChangeListener<T> listener) {
    	if (mListeners == null) {
    		mListeners = new ArrayList<PropertySourceChangeListener<T>>();
    	}
    	mListeners.add(listener);
    }

    public void removeListener(PropertySourceChangeListener<T> listener) {
    	if (mListeners != null) {
    		mListeners.remove(listener);
    	}
    }

    public void addVetoableListener(VetoablePropertySourceChangeListener<T> listener) {
    	if (mVetoableListeners == null) {
    		mVetoableListeners = new ArrayList<VetoablePropertySourceChangeListener<T>>();
    	}
    	mVetoableListeners.add(listener);
    }

    public void removeVetoableListener(VetoablePropertySourceChangeListener<T> listener) {
    	if (mVetoableListeners != null) {
    		mVetoableListeners.remove(listener);
    	}
    }

    public HashMap<String, Object> getUserDataMap() {
        if (mUserData == null) {
            mUserData = new HashMap<String, Object>();
        }
        return mUserData;
    }

    public void setUserDataMap(HashMap<String, Object> map) {
        mUserData = map;
    }

    /**
     * Used for initializing the value of sources.
     * This should be called by the Property.initialize() method only.
     * Sources generally shouldn't be set before adding them to a property.
     * Initialize() gives a way of setting the value of the property source
     * when the logic for calculating the property is within the source itself.
     * This will ensure the set value will be checked by property vetoers.
     * If the source value is set in the constructor (of a subclass) the 
     * value will not be checked by vetoers.
     * 
     * @throws PropertyVetoException
     * @throws PropertyConversionException
     */
    public void initialize() throws PropertyVetoException, PropertyConversionException {	
    }
    
    @SuppressWarnings("unchecked")
    public void setValue(T value) throws PropertyVetoException {
    	boolean isSet = (!((value == null) && (!mNullValid)));
    	PropertySourceChangeEvent<T> event = new PropertySourceChangeEvent<T>(
    	        this, mIsSet, isSet, mValue, value, mPriority, mPriority);
    	notifyVetoableListeners(event);
    	mIsSet = isSet;
    	if ((value instanceof String) && (mTrimStrings)) {
            mValue = (T)((String)value).trim();    	    
    	} else {
            mValue = value;
    	}
        notifyListeners(event);
    }

    public void setPriority(int priority) throws PropertyVetoException {
        mPriority = priority;
        PropertySourceChangeEvent<T> event = new PropertySourceChangeEvent<T>(
                this, mIsSet, mIsSet, mValue, mValue, mPriority, priority);
        notifyVetoableListeners(event);
        notifyListeners(event);
    }

    @SuppressWarnings("unchecked")
	public void setObjectValue(Object value) throws PropertyConversionException, PropertyVetoException {
        try {
            if (value == null) {
                setValue(null);
            } else {
                if (mConverter == null) {
                    setValue((T)value);
                } else {
                    if ((value instanceof String) && (mTrimStrings)) {
                        setValue((T)mConverter.convert(((String)value).trim()));
                    } else {
                        setValue((T)mConverter.convert(value));
                    }
                }
            }
        } catch(PropertyConversionException e) {
            e.addUserData("Source", mName);
            throw e;
        }
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return new PropertySource<T>(this);
    }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("name=\"");
        bldr.append(mName);
        bldr.append("\", value=");
        if (mPrintable) {
            if (mValue instanceof String) {
                bldr.append("\"");
                bldr.append(mValue);
                bldr.append("\"");
            } else {
                bldr.append(mValue);
            }
        } else {
            if (mIsSet) {
                bldr.append(Property.NON_PRINTABLE_VALUE_STRING);
            }
        }
        bldr.append(", isSet=");
        bldr.append(mIsSet);
        bldr.append(", priority=");
        bldr.append(mPriority);
        if (mPriority == HIGHEST_PRIORITY) {
            bldr.append("(HIGHEST)");
        } else if (mPriority == LOWEST_PRIORITY) {
            bldr.append("(LOWEST)");
        }
        bldr.append(", nullValid=");
        bldr.append(mNullValid);
        bldr.append(", trimStrings=");
        bldr.append(mTrimStrings);
        return bldr.toString();
    }
    
	protected void notifyVetoableListeners(PropertySourceChangeEvent<T> event) throws PropertyVetoException {
    	if ((mVetoableListeners == null) || (mVetoableListeners.size() == 0)) {
    		return;
    	}
        for(int i = 0; i < mVetoableListeners.size(); i++) {
        	mVetoableListeners.get(i).vetoablePropertySourceChanged(event);
        }
    }

	protected void notifyListeners(PropertySourceChangeEvent<T> event) {
    	if ((mListeners == null) || (mListeners.size() == 0)) {
    		return;
    	}
        for(int i = 0; i < mListeners.size(); i++) {
        	mListeners.get(i).propertySourceChanged(event);
        }
    }
}
