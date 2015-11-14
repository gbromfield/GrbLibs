package com.grb.util.property;

public class PropertyChangeEvent<T> {
	
	protected Property<T> mProperty;
	protected boolean mWasSet;
	protected boolean mIsSet;
	protected T mOldValue;
	protected T mNewValue;
	protected PropertySource<T> mOldMaster;
    protected PropertySource<T> mNewMaster;
	
	public PropertyChangeEvent(Property<T> property, 
	        boolean wasSet, boolean isSet, 
	        T oldValue, T newValue, 
	        PropertySource<T> oldMaster, PropertySource<T> newMaster) {
	    mProperty = property;
		mWasSet = wasSet;
		mIsSet = isSet;
		mOldValue = oldValue;
		mNewValue = newValue;
		mOldMaster = oldMaster;
		mNewMaster = newMaster;
	}
	
	public Property<T> getProperty() {
		return mProperty;
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

	public PropertySource<T> getOldMaster() {
	    return mOldMaster;
	}
	
	public PropertySource<T> getNewMaster() {
	    return mNewMaster;
	}
	
	@Override
	public String toString() {
		StringBuilder bldr = new StringBuilder();
		bldr.append("property=");
		bldr.append(mProperty.getId());
		bldr.append(", wasSet=");
		bldr.append(mWasSet);
		bldr.append(", isSet=");
		bldr.append(mIsSet);
		bldr.append(", oldValue=");
		bldr.append(mOldValue);
		bldr.append(", newValue=");
		bldr.append(mNewValue);
        bldr.append(", oldMaster=");
		if (mOldMaster == null) {
	        bldr.append("null");
		} else {
            bldr.append(mOldMaster.getName());
		}
        bldr.append(", newMaster=");
        if (mNewMaster == null) {
            bldr.append("null");
        } else {
            bldr.append(mNewMaster.getName());
        }
		return bldr.toString();
	}
}
