package com.grb.util.property;

import java.util.Collection;
import java.util.Iterator;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeDataSupport;

public class JMXUtils {
    static final public String JMX_SOURCE                             = "jmx";

    static public void setAttribute(Collection<Property<?>> properties, Attribute attribute) 
            throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        Iterator<Property<?>> it = properties.iterator();
        while(it.hasNext()) {
            Property<?> property = it.next();
            if (property.getId().equals(attribute.getName())) {
                setAttribute(property, attribute.getValue());
                return;
            }
        }
        throw new AttributeNotFoundException("Could not find attribute " + attribute);
    }
    
    static public void setAttribute(Property<?> property, Object value)
            throws InvalidAttributeValueException, MBeanException, ReflectionException {
        PropertySource<?> source = property.getSource(JMX_SOURCE);
        if (source != null) {
            try {
                source.setObjectValue(value);
                return;
            } catch (PropertyConversionException e) {
                throw new InvalidAttributeValueException(e.getMessage());
            } catch (PropertyVetoException e) {
                throw new InvalidAttributeValueException(e.getMessage());
            }
        }
    }

    static public Object getAttribute(Collection<Property<?>> properties, String attribute) 
            throws AttributeNotFoundException, MBeanException, ReflectionException {
        Iterator<Property<?>> it = properties.iterator();
        while(it.hasNext()) {
            Property<?> property = it.next();
            if (property.getId().equals(attribute)) {
                return property.getValue();
            }
        }
        throw new AttributeNotFoundException("Could not find attribute " + attribute);
    }

    static public MBeanAttributeInfo[] createAttrInfo(Collection<Property<?>> properties) {
        if (properties != null) {
            MBeanAttributeInfo[] attrs = new MBeanAttributeInfo[properties.size()];
            int index = 0;
            Iterator<Property<?>> it = properties.iterator();
            while(it.hasNext()) {
                Property<?> prop = it.next();
                boolean isReadable = prop.isPrintable();
                boolean isWritable = (prop.getSource(JMX_SOURCE) != null);
                // treat enums as strings
                String classname = prop.getParameterizedClass().getName();
                if (Enum.class.isAssignableFrom(prop.getParameterizedClass())) {
                    classname = String.class.getName();
                } else if (prop.getParameterizedClass().isArray()) {
                    classname = CompositeDataSupport.class.getName();
                }
                attrs[index] = new MBeanAttributeInfo(prop.getId(), classname, 
                        prop.getId(), isReadable, isWritable, false);
                index++;
            }
            return attrs;
        }
        return null;
    }
}
