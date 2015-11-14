package com.grb.util.property;

public interface PropertyChangeListener<T> {
    public void propertyChanged(PropertyChangeEvent<T> event);
}
