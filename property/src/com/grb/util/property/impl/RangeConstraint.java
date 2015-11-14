package com.grb.util.property.impl;

import com.grb.util.property.PropertySourceChangeEvent;
import com.grb.util.property.PropertyVetoException;
import com.grb.util.property.VetoablePropertySourceChangeListener;

public class RangeConstraint<T> implements VetoablePropertySourceChangeListener<T> {

	protected Comparable<T> mMin;
	protected Comparable<T> mMax;
	
	public RangeConstraint(Comparable<T> min, Comparable<T> max) {
		mMin = min;
		mMax = max;
	}
	
	public void vetoablePropertySourceChanged(PropertySourceChangeEvent<T> event)
			throws PropertyVetoException {
		if ((event.isSet()) && (event.getNewValue() != null)) {
			if (mMin != null) {
				if (mMin.compareTo(event.getNewValue()) > 0) {
					throw new PropertyVetoException(event, "new value is below minimum value allowed");
				}
			}
			if (mMax != null) {
				if (mMax.compareTo(event.getNewValue()) < 0) {
					throw new PropertyVetoException(event, "new value is above maximum value allowed");
				}
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder bldr = new StringBuilder();
		bldr.append("min=");
		bldr.append(mMin);
		bldr.append(", max=");
		bldr.append(mMax);
		return bldr.toString();
	}
}
