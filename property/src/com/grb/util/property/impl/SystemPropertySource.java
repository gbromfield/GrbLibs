package com.grb.util.property.impl;

import com.grb.util.property.PropertyConversionException;
import com.grb.util.property.PropertyConverter;
import com.grb.util.property.PropertySource;
import com.grb.util.property.PropertyVetoException;

public class SystemPropertySource<T> extends PropertySource<T> {

    static final public String PROPERTY_NAME = "System Property";
    
	protected String mPropertyName;

	public SystemPropertySource(SystemPropertySource<T> source) {
	   super(source);
	   mPropertyName = source.mPropertyName;
	}
	
    public SystemPropertySource(String propertyName) {
        this(PROPERTY_NAME, propertyName, DEFAULT_PRIORITY, DEFAULT_NULL_VALID);
    }

    public SystemPropertySource(String propertyName, int priority) {
        this(PROPERTY_NAME, propertyName, priority, DEFAULT_NULL_VALID);
    }

	public SystemPropertySource(String propertyName, int priority,
			boolean nullValid) {
		this(PROPERTY_NAME, propertyName, priority, nullValid);
	}

	public SystemPropertySource(String propertyName, int priority,
			boolean nullValid, PropertyConverter converter) {
		this(PROPERTY_NAME, propertyName, priority, nullValid, converter);
	}

	public SystemPropertySource(String name, String propertyName, int priority,
			boolean nullValid) {
	    this(name, propertyName, priority, nullValid, null);
	}

	public SystemPropertySource(String name, String propertyName, int priority,
			boolean nullValid, PropertyConverter converter) {
		super(name, priority, nullValid, converter);
		if (propertyName == null) {
		    throw new IllegalArgumentException("property name cannot be null");
		}
		mPropertyName = propertyName;
	}

	@Override
	public void initialize() throws PropertyVetoException, PropertyConversionException {
		setObjectValue(System.getProperty(mPropertyName));
	}

    @Override
    public Object clone() throws CloneNotSupportedException {
        return new SystemPropertySource<T>(this);
    }
}
