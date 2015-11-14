package com.grb.util.property;

import java.util.HashMap;
import java.util.Iterator;

public class PropertyConversionException extends Exception {

	private static final long serialVersionUID = 1L;

	private HashMap<String, String> mUserData = null;
	
	public PropertyConversionException() {
	}

	public PropertyConversionException(String message) {
		super(message);
	}

	public PropertyConversionException(Throwable cause) {
		super(cause);
	}

	public PropertyConversionException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public void addUserData(String key, String value) {
	    if (mUserData == null) {
	        mUserData = new HashMap<String, String>();
	    }
	    mUserData.put(key, value);
	}

    @Override
    public String getMessage() {
        if (mUserData == null) {
            return super.getMessage();
        }
        StringBuilder bldr = new StringBuilder(super.getMessage());
        bldr.append(" {");
        boolean first = true;
        Iterator<String> it = mUserData.keySet().iterator();
        while(it.hasNext()) {
            if (!first) {
                bldr.append(",");
            } else {
                first = false;
            }
            String key = it.next();
            bldr.append(key);
            bldr.append("=");
            bldr.append(mUserData.get(key));
        }
        bldr.append("}");
        return bldr.toString();
    }
}
