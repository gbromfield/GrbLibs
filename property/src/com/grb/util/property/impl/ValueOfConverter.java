package com.grb.util.property.impl;

import java.lang.reflect.Method;
import java.util.HashMap;

import com.grb.util.property.PropertyConversionException;
import com.grb.util.property.PropertyConverter;

public class ValueOfConverter implements PropertyConverter {
	static private HashMap<String, ValueOfConverter> converterMap = new HashMap<String, ValueOfConverter>();
	
	static public ValueOfConverter getConverter(Class<?> toClass) {
		ValueOfConverter converter = converterMap.get(toClass.getName());
		if (converter == null) {
            boolean found = false;
            Method[] methods = toClass.getDeclaredMethods();
            for(int i = 0; i < methods.length; i++) {
                if ((methods[i].getName().equals("valueOf")) &&
                    (methods[i].getParameterTypes().length == 1)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IllegalArgumentException("converter for " + toClass.getName() + " not found");
            }
			converter = new ValueOfConverter(toClass);
			converterMap.put(toClass.getName(), converter);
		}
		return converter;
	}
	
	private Class<?> mToClass;
	
	private ValueOfConverter(Class<?> toClass) {
		mToClass = toClass;
	}
	
	public Class<?> getDestinationClass() {
	    return mToClass;
	}
	
	public Object convert(Object fromValue) throws PropertyConversionException {
        if (fromValue.getClass().equals(mToClass)) {
            return fromValue;
        }
        try {
            Class<?>[] valueOfArgs = new Class[1];
            valueOfArgs[0] = fromValue.getClass();
            Method convertMethod = mToClass.getDeclaredMethod("valueOf", valueOfArgs);
            Object[] invokeArgs = new Object[1];
            invokeArgs[0] = fromValue;
            return convertMethod.invoke(null, invokeArgs);
        } catch(Exception e) {
        	throw new PropertyConversionException("failed to convert value \"" + fromValue + "\"", e);
        }
	}
}
