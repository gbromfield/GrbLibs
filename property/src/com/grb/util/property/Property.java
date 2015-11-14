package com.grb.util.property;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import com.grb.util.property.impl.ValueOfConverter;

public class Property<T> implements PropertySourceChangeListener<T>, 
    VetoablePropertySourceChangeListener<T>, Cloneable {    
    
    final static public String ANONYMOUS_SOURCE             = "_anonymous_";
    final static public String DEFAULT_SOURCE               = "_default_";
    final static public String NON_PRINTABLE_VALUE_STRING   = "******";
    
    private String mId;
    private T mValue;
    private boolean mIsSet;
    private PropertySource<T> mMaster;
    private HashMap<String, PropertySource<T>> mSources;
    private ArrayList<PropertyChangeListener<T>> mListeners;
    protected ArrayList<VetoablePropertySourceChangeListener<T>> mVetoableListeners;
    private HashMap<String, Object> mUserData;
    private Class<?> mType;
    private boolean mTrimStrings;
    private boolean mPrintable;
    
    /**
     * Deep Copy Constructor
     * 
     * @param property Property to be copied.
     * @throws CloneNotSupportedException 
     * @throws PropertyConversionException 
     * @throws PropertyVetoException 
     */
    @SuppressWarnings("unchecked")
    public Property(Property<T> property) {
        mId = property.mId;
        mValue = null;
        mIsSet = false;
        mMaster = null;
        mListeners = null;
        if (property.mListeners != null) {
            mListeners = (ArrayList<PropertyChangeListener<T>>)property.mListeners.clone();
        }
        mVetoableListeners = null;
        if (property.mVetoableListeners != null) {
            mVetoableListeners = (ArrayList<VetoablePropertySourceChangeListener<T>>)property.mVetoableListeners.clone();
        }
        mTrimStrings = property.mTrimStrings;
        mPrintable = property.mPrintable;
        mSources = new HashMap<String, PropertySource<T>>();
        Iterator<PropertySource<T>> it = property.mSources.values().iterator();
        while(it.hasNext()) {
            try {
                addSources((PropertySource<T>) it.next().clone());
            } catch (Exception e) {
                throw new IllegalArgumentException("error constructing property", e);
            }
        }
        mUserData = property.mUserData;
        mType = property.mType;
    }
    
    public Property(Object id) {
        this(id, (T)null);
    }

    /**
     * If you want to specify a null default value you have to call 
     * {@link #setDefaultValue(Object)} rather than specifying it in the
     * constructor since it will be taken as no default value.
     * 
     * @param id
     * @param defaultValue
     */
    public Property(Object id, T defaultValue) {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
        mId = id.toString();
        mValue = defaultValue;
        mIsSet = false;
        mMaster = null;
        mTrimStrings = false;
        mPrintable = true;
        mSources = new HashMap<String, PropertySource<T>>();
        mListeners = null;
        mVetoableListeners = null;
        mUserData = null;
        if (defaultValue != null) {
            setDefaultValue(defaultValue);
            mMaster= calculateNewMaster();
        }
        mType = null;
    }

    public Property(Object id, PropertySource<T> ... sources) {
        this(id, (T)null);
        if (sources != null) {
            addSources(sources);            
        }
    }

    /**
     * If you want to specify a null default value you have to call 
     * {@link #setDefaultValue(Object)} rather than specifying it in the
     * constructor since it will be taken as no default value.
     * 
     * @param id
     * @param defaultValue
     * @param sources
     */
    public Property(Object id, T defaultValue, PropertySource<T> ... sources) {
        this(id, defaultValue);
        if (sources != null) {
            addSources(sources);            
        }
    }

    /**
     * If you want to specify a null default value you have to call 
     * {@link #setDefaultValue(Object)} rather than specifying it in the
     * constructor since it will be taken as no default value.
     * 
     * @param id
     * @param defaultValue
     * @param trimStrings
     * @param sources
     */
    public Property(Object id, T defaultValue, boolean trimStrings, PropertySource<T> ... sources) {
    	this(id, defaultValue);
    	mTrimStrings = trimStrings;
        if (sources != null) {
            addSources(sources);            
        }
    }

    public String getId() {
        return mId;
    }
        
    public T getDefaultValue() {
        PropertySource<T> source = mSources.get(DEFAULT_SOURCE);
        if (source != null) {
            return source.getValue();
        }
        return null;
    }
    
    public void setDefaultValue(T value) {
        PropertySource<T> source = mSources.get(DEFAULT_SOURCE);
        if (source == null) {
            if (value == null) {
                source = new PropertySource<T>(DEFAULT_SOURCE, 
                        PropertySource.LOWEST_PRIORITY, 
                        PropertySource.NULL_VALID);
            } else {
                source = new PropertySource<T>(DEFAULT_SOURCE, 
                        PropertySource.LOWEST_PRIORITY, 
                        PropertySource.NULL_INVALID);
            }
            mSources.put(DEFAULT_SOURCE, source);
        }
        try {
            source.setValue(value);
        } catch (PropertyVetoException e) {
            // Default shouldn't be vetoed
        }
    }

    public boolean hasDefaultValue() {
        return (mSources.get(DEFAULT_SOURCE) != null);        
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
    
    public boolean isPrintable() {
        return mPrintable;
    }

    public void addListener(PropertyChangeListener<T> listener) {
    	if (mListeners == null) {
    		mListeners = new ArrayList<PropertyChangeListener<T>>();
    	}
    	mListeners.add(listener);
    }

    public void removeListener(PropertyChangeListener<T> listener) {
    	if (mListeners != null) {
    		mListeners.remove(listener);
    	}
    }

    public void clearListeners() {
        if (mListeners != null) {
            mListeners.clear();
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

    public void clearVetoableListener() {
        if (mVetoableListeners != null) {
            mVetoableListeners.clear();
        }
    }

    public void addSource(PropertySource<T> source) {
        Iterator<PropertySource<T>> it = mSources.values().iterator();
        while(it.hasNext()) {
            PropertySource<T> aSource = it.next();
            if (aSource.getPriority() == source.getPriority()) {
                throw new IllegalArgumentException("Duplicate Source Priority");
            }
        }
        mSources.put(source.getName(), source);
        source.setTrimStrings(mTrimStrings);
        source.setPrintable(mPrintable);
        source.addListener(this);
        source.addVetoableListener(this);

        if (source.isSet()) {
            update();
        }
    }
    
    public void addSources(PropertySource<T> ... sources) {
    	for(int i = 0; i < sources.length; i++) {
    	    addSource(sources[i]);
    	}
 	}

    public void addAllSources(Collection<PropertySource<T>> sources) {
        Iterator<PropertySource<T>> it = sources.iterator();
        while(it.hasNext()) {
            addSource(it.next());
        }
    }
    
    public void initialize() throws PropertyVetoException, PropertyConversionException {
        try {
            Iterator<PropertySource<T>> it = mSources.values().iterator();
            while(it.hasNext()) {
                PropertySource<T> aSource = it.next();
                aSource.initialize();
            }
        } catch(PropertyConversionException e) {
            e.addUserData("Property", mId);
            throw e;
        }
    }
    
    public Collection<PropertySource<T>> getSources() {
        return mSources.values();
    }
    
    public Collection<String> getSourceNames() {
        return mSources.keySet();
    }

    public PropertySource<T> getSource(String name) {
        return mSources.get(name);
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

    public T getSourceValue(String name) {
        PropertySource<T> source = mSources.get(name);
        if (source == null) {
            throw new IllegalArgumentException("Source " + name + " does not exist");
        }
        return source.getValue();
    }

    public void setSourceValue(String name, T value) throws PropertyVetoException {
        PropertySource<T> source = mSources.get(name);
        if (source == null) {
            throw new IllegalArgumentException("Source " + name + " does not exist");
        }
        source.setValue(value);
    }

    public HashMap<String, Object> getSourceUserDataMap(String name) {
        PropertySource<T> source = mSources.get(name);
        if (source == null) {
            throw new IllegalArgumentException("Source " + name + " does not exist");
        }
        return source.getUserDataMap();
    }

    public void setSourceUserDataMap(String name, HashMap<String, Object> map) {
        PropertySource<T> source = mSources.get(name);
        if (source == null) {
            throw new IllegalArgumentException("Source " + name + " does not exist");
        }
        source.setUserDataMap(map);
    }

    /**
     * Returns true if one of the property sources has been set.
     * This will return false if the default value is the only 
     * value that's been set (the master is the default value).
     * 
     * @return True if one of the property sources has been set.
     */
    public boolean isSet() {
    	return mIsSet;
    }

    public T getValue() {        
        return mValue;
    }
        
    /**
     * Returns the highest priority property source that has been set, 
     * or null if none of the sources are set.
     *  
     * @return The master property source.
     */
	public PropertySource<T> getMaster() {
	    return mMaster;
    }
    
    /**
     * Only to be used with a single sourced property. If multiple sources, 
     * the sources should be set directly.
     * 
     * @param value
     * @throws PropertyVetoException 
     * @throws PropertyConversionException 
     */
	public void setValue(T value) throws PropertyVetoException, PropertyConversionException {
    	PropertySource<T> source = getSingleSourced();
        source.setValue(value);
    }

    /**
     * Only to be used with a single sourced property. If multiple sources, 
     * the sources should be set directly.
     * 
     * @param value
     * @throws PropertyConversionException 
     * @throws PropertyVetoException 
     */
	public void setObjectValue(Object value) throws PropertyConversionException, PropertyVetoException {
    	PropertySource<T> source = getSingleSourced();
        source.setObjectValue(value);
    }

	public void propertySourceChanged(PropertySourceChangeEvent<T> event) {
		// ignore the changed values and just update
		update();
	}

	public void vetoablePropertySourceChanged(PropertySourceChangeEvent<T> event)
			throws PropertyVetoException {
		notifyVetoableListeners(event);
	}

	public void setType(Class<?> type) {
	    mType = type;
	}
	
	public Class<?> getParameterizedClass() {
	    if (mType != null) {
	        return mType;
	    }
        if (mValue != null) {
            return mValue.getClass();
        }
        if (getDefaultValue() != null) {
            return getDefaultValue().getClass();
        }
        Iterator<PropertySource<T>> it = mSources.values().iterator();
        while(it.hasNext()) {
            PropertySource<T> source = it.next();
            PropertyConverter conv = source.getConverter();
            if (conv instanceof ValueOfConverter) {
                return ((ValueOfConverter)conv).getDestinationClass();
            }
        }
        return Object.class;
	}
		
    @Override
    public Object clone() throws CloneNotSupportedException {
        return new Property<T>(this);
    }

    public String toKeyValueString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append(mId);
        bldr.append("=");
        if (mPrintable) {
            bldr.append(toValueString(mValue));
        } else {
            if (mIsSet) {
                bldr.append(NON_PRINTABLE_VALUE_STRING);
            }
        }
        return bldr.toString();
    }
    
    static public StackTraceElement[] getStackTraceForSet() {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        int startIndex = 0;
        for(int i = (trace.length-1); i >= 0; i--) {
            String traceElement = trace[i].toString();
            if (traceElement.contains("notifyListeners")) {
                startIndex = i + 1;
                break;
            }
        }
        if (startIndex == 0) {
            return trace;
        }
        int numElems = trace.length - startIndex;
        StackTraceElement[] returnTrace = new StackTraceElement[numElems];
        for(int i = 0; i < numElems; i++) {
            returnTrace[i] = trace[startIndex++];
        }
        return returnTrace;
    }

    static public String getStackTraceForSetString() {
        StringBuilder bldr = new StringBuilder();
        StackTraceElement[] trace = getStackTraceForSet();
        for(int i = (trace.length-1); i >= 0; i--) {
            if (bldr.length() > 0) {
                bldr.insert(0, "\n\t");
            }
            bldr.insert(0, trace[i].toString());
        }
        if (bldr.length() > 0) {
            bldr.insert(0, "\t");
        }
        return bldr.toString();
    }

    static private String toValueString(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            return "\"" + value + "\"";
        } else if (value.getClass().isArray()) {
            Object[] a = (Object[])value;
            StringBuilder bldr = new StringBuilder();
            bldr.append("[");
            for (int i = 0; i < a.length; i++) {
                if (i > 0) {
                    bldr.append(",");
                }
                bldr.append(toValueString(a[i]));
            }
            bldr.append("]");
            return bldr.toString();
        } else {
            return value.toString();
        }
    }

    @Override
    public String toString() {
        return toString(true);
    }

    public String toString(boolean inclID) {
        StringBuilder bldr = new StringBuilder();
        if (inclID) {
            bldr.append("id=\"");
            bldr.append(mId);
            bldr.append("\", value=");
        } else {
            bldr.append("value=");
        }
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
                bldr.append(NON_PRINTABLE_VALUE_STRING);
            }
        }
        bldr.append(", isSet=");
        bldr.append(mIsSet);
        bldr.append(", trimStrings=");
        bldr.append(mTrimStrings);

        PropertySource<T> master = getMaster();
        if (master == null) {
            bldr.append(", master=none, ");
        } else {
            bldr.append(", master=\"");
            bldr.append(master.getName());
            bldr.append("\", ");
        }
        if (mSources.size() == 0) {
            bldr.append("sources=none\r\n");
        } else {
            bldr.append("sources={\r\n");
            TreeSet<PropertySource<T>> sourceSet = new TreeSet<PropertySource<T>>(new Comparator<PropertySource<T>>() {
                public int compare(PropertySource<T> o1, PropertySource<T> o2) {
                    return o1.getPriority() - o2.getPriority();
                }
            });
            sourceSet.addAll(mSources.values());
            Iterator<PropertySource<T>> it = sourceSet.iterator();
            while(it.hasNext()) {
                bldr.append("\t");
                bldr.append(it.next());
                bldr.append("\r\n");
            }
            bldr.append("}\r\n");
        }
        return bldr.toString();
    }

    static public String mapToString(Map<String, Property<?>> propertyMap) {
        StringBuilder bldr = new StringBuilder();
        if (propertyMap == null) {
            bldr.append("null");
        } else {
            Iterator<Property<?>> it = propertyMap.values().iterator();
            while(it.hasNext()) {
                Property<?> prop = it.next();
                bldr.append(String.format("%s - %s\r\n", prop.getId(), prop.toString(false)));
            }
        }
        return bldr.toString();
    }
    
    @SuppressWarnings("unchecked")
    protected PropertySource<T> getSingleSourced() {
        PropertySource<T> source = getSource(ANONYMOUS_SOURCE);
        PropertySource<T> defaultSource = getSource(DEFAULT_SOURCE);
        if (source == null) {
            if (((defaultSource == null) && (mSources.size() > 0)) || 
                ((defaultSource != null) && (mSources.size() != 1))) {
                throw new IllegalStateException("This method to be used with a single sourced property only");
            }
            source = new PropertySource<T>(ANONYMOUS_SOURCE);
            addSources(source);            
        } else {
            if (((defaultSource == null) && (mSources.size() > 1)) || 
                ((defaultSource != null) && (mSources.size() != 2))) {
                throw new IllegalStateException("This method to be used with a single sourced property only");
            }
        }
        return source;
    }
    
    protected PropertySource<T> calculateNewMaster() {
        PropertySource<T> master = null;
        Iterator<String> it = mSources.keySet().iterator();
        while(it.hasNext()) {
            PropertySource<T> source = mSources.get(it.next());
            if (source.isSet()) {
                if (master == null) {
                    master = source;
                } else {
                    // compare with current master
                    if (master.getPriority() > source.getPriority()) {
                        master = source;
                    }
                }
            }            
        }
        return master;
    }

    protected void update() {
    	PropertySource<T> master = calculateNewMaster();
    	if (hasChanged(master)) {
        	if (master == null) {
        		PropertyChangeEvent<T> event = new PropertyChangeEvent<T>(this, mIsSet, false, mValue, null, mMaster, master);
            	mIsSet = false;
            	mValue = null;
            	mMaster = master;
        		notifyListeners(event);
            } else if (master.getName() == DEFAULT_SOURCE){
                PropertyChangeEvent<T> event = new PropertyChangeEvent<T>(this, mIsSet, false, mValue, master.getValue(), mMaster, master);
                mIsSet = false;
                mValue = master.getValue();
                mMaster = master;
                notifyListeners(event);             
        	} else {
        		PropertyChangeEvent<T> event = new PropertyChangeEvent<T>(this, mIsSet, master.isSet(), mValue, master.getValue(), mMaster, master);
            	mIsSet = master.isSet();
            	mValue = master.getValue();
                mMaster = master;
        		notifyListeners(event);        		
        	}
    	}
    }
    
    protected boolean hasChanged(PropertySource<T> source) {
    	boolean newIsSet = false;
    	T newValue = null;
    	if (source != null) {
    	    if (mMaster == null) {
    	        return true;
    	    } else {
                if (!source.getName().equals(mMaster.getName())) {
                    return true;
                }
    	    }
    		newIsSet = source.isSet();
    		newValue = source.getValue();
    	} else {
    	    if (mMaster != null) {
    	        return true;
    	    }
    	}
        if (mIsSet != newIsSet) {
        	return true;
        }
        if (!mIsSet) {
        	return false;
        }
        if (mValue == null) {
        	if (newValue == null) {
            	return false;
        	} else {
            	return true;
        	}
        }
        if (newValue == null) {
        	return true;
        }
        return (!mValue.equals(newValue));
    }
        
    protected void notifyListeners(PropertyChangeEvent<T> event) {
    	if ((mListeners == null) || (mListeners.size() == 0)) {
    		return;
    	}
    	for(int i = 0; i < mListeners.size(); i++) {
    		mListeners.get(i).propertyChanged(event);
    	}
    }
    
	protected void notifyVetoableListeners(PropertySourceChangeEvent<T> event) throws PropertyVetoException {
    	if ((mVetoableListeners == null) || (mVetoableListeners.size() == 0)) {
    		return;
    	}
    	event.setProperty(this);
        for(int i = 0; i < mVetoableListeners.size(); i++) {
        	mVetoableListeners.get(i).vetoablePropertySourceChanged(event);
        }
    }
}
