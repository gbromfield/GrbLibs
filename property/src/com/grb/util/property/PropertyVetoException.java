package com.grb.util.property;

public class PropertyVetoException extends Exception {
	private static final long serialVersionUID = 1L;

	private PropertySourceChangeEvent<?> mPropertySourceChangeEvent;
	
	public PropertyVetoException(PropertySourceChangeEvent<?> event) {
		mPropertySourceChangeEvent = event;
	}

	public PropertyVetoException(PropertySourceChangeEvent<?> event, String message) {
		super(message);
		mPropertySourceChangeEvent = event;
	}

	public PropertyVetoException(PropertySourceChangeEvent<?> event, Throwable cause) {
		super(cause);
		mPropertySourceChangeEvent = event;
	}

	public PropertyVetoException(PropertySourceChangeEvent<?> event, String message, Throwable cause) {
		super(message, cause);
		mPropertySourceChangeEvent = event;
	}
	
	public PropertySourceChangeEvent<?> getPropertySourceChangeEvent() {
		return mPropertySourceChangeEvent;
	}
}
