package com.grb.util.property;

public interface PropertySourceChangeListener<T> {
    public void propertySourceChanged(PropertySourceChangeEvent<T> event);
}
