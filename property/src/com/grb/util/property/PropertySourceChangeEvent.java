package com.grb.util.property;


public class PropertySourceChangeEvent<T> {

    protected PropertySource<T> mSource;
    protected boolean mWasSet;
    protected boolean mIsSet;
    protected T mOldValue;
    protected T mNewValue;
    protected int mOldPriority;
    protected int mNewPriority;
    protected Property<T> mProperty;
    
    public PropertySourceChangeEvent(PropertySource<T> source, boolean wasSet, boolean isSet, T oldValue, T newValue, int oldPriority, int newPriority) {
        mSource = source;
        mWasSet = wasSet;
        mIsSet = isSet;
        mOldValue = oldValue;
        mNewValue = newValue;
        mOldPriority = oldPriority;
        mNewPriority = newPriority;
        mProperty = null;
    }
    
    public PropertySource<T> getSource() {
        return mSource;
    }
    
    public boolean wasSet() {
        return mWasSet;
    }
    
    public boolean isSet() {
        return mIsSet;
    }
    
    public T getOldValue() {
        return mOldValue;
    }
    
    public T getNewValue() {
        return mNewValue;
    }

    public int getOldPriority() {
        return mOldPriority;
    }
    
    public int getNewPriority() {
        return mNewPriority;
    }

    public Property<T> getProperty() {
        return mProperty;
    }
    
    public void setProperty(Property<T> property) {
        mProperty = property;
    }
    
    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("source=");
        bldr.append(mSource.getName());
        bldr.append(", wasSet=");
        bldr.append(mWasSet);
        bldr.append(", isSet=");
        bldr.append(mIsSet);
        bldr.append(", oldValue=");
        bldr.append(mOldValue);
        bldr.append(", newValue=");
        bldr.append(mNewValue);
        bldr.append(", oldPriority=");
        bldr.append(mOldPriority);
        bldr.append(", newPriority=");
        bldr.append(mNewPriority);
        if (mProperty != null) {
            bldr.append(", property=");
            bldr.append(mProperty.getId());
        }
        return bldr.toString();
    }
}
