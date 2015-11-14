package com.grb.util.property.impl;

import java.util.Map;

import com.grb.util.property.PropertyConversionException;
import com.grb.util.property.PropertyConverter;
import com.grb.util.property.PropertySource;
import com.grb.util.property.PropertyVetoException;

public class MapPropertySource<T> extends PropertySource<T> implements Cloneable {

    private Object mKey;
    private Map<?,?> mMap;
    
    public MapPropertySource(MapPropertySource<T> source) {
        super(source);
        mKey = source.mKey;
        mMap = source.mMap;
    }
    
    public MapPropertySource(String name, Object key, Map<?,?> map) {
        this(name, key, map, DEFAULT_PRIORITY);
    }

    public MapPropertySource(String name, Object key, Map<?,?> map, int priority) {
        this(name, key, map, priority, DEFAULT_NULL_VALID);
    }

    public MapPropertySource(String name, Object key, Map<?,?> map, boolean nullValid) {
        this(name, key, map, DEFAULT_PRIORITY, nullValid);
    }

    public MapPropertySource(String name, Object key, Map<?,?> map, PropertyConverter converter) {
        this(name, key, map, DEFAULT_PRIORITY, converter);
    }

    public MapPropertySource(String name, Object key, Map<?,?> map, int priority, boolean nullValid) {
        this(name, key, map, priority, nullValid, null);
    }

    public MapPropertySource(String name, Object key, Map<?,?> map, int priority,
            PropertyConverter converter) {
        this(name, key, map, priority, DEFAULT_NULL_VALID, converter);
    }

    public MapPropertySource(String name, Object key, Map<?,?> map, int priority, boolean nullValid,
            PropertyConverter converter) {
        super(name, priority, nullValid, converter);
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }
        mKey = key;
        mMap = map;
    }

    public Object getKey() {
        return mKey;
    }

    public Map<?,?> getMap() {
        return mMap;
    }
    
    @Override
    public void initialize() throws PropertyVetoException,
            PropertyConversionException {
        if (mMap != null) {
            if (mMap.containsKey(mKey)) {
                setObjectValue(mMap.get(mKey));
            }
        }
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return new MapPropertySource<T>(this);
    }
}
