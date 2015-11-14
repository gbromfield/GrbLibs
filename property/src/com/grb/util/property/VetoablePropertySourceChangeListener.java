package com.grb.util.property;

public interface VetoablePropertySourceChangeListener<T> {
    public void vetoablePropertySourceChanged(PropertySourceChangeEvent<T> event) throws PropertyVetoException;
}
