package com.grb.util.property;

/**
 * All PropertyConverters should handle the noop case of getting
 * the destination class as an argument.
 */
public interface PropertyConverter {
	public Object convert(Object fromValue) throws PropertyConversionException;
}
