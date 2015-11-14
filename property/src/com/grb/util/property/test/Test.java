package com.grb.util.property.test;

import junit.framework.TestCase;

import com.grb.util.property.Property;
import com.grb.util.property.PropertySource;
import com.grb.util.property.impl.RangeConstraint;
import com.grb.util.property.impl.SystemPropertySource;
import com.grb.util.property.impl.ValueOfConverter;

public class Test extends TestCase {
	
	public void test1String() {
		try {
			Property<String> property = new Property<String>(getMethodName(1), (String)null);
			System.out.println(property);
			assertNull(property.getDefaultValue());
			assertEquals(getMethodName(1), property.getId());
			assertFalse(property.isSet());
			validate(property, null, null);
			assertFalse(property.isSet());
		} catch(Exception e) {
			fail(e.getMessage());
		}
	}

	public void test1Integer() {
		try {
			Property<Integer> property = new Property<Integer>(getMethodName(1), (Integer)null);
			System.out.println(property);
			assertNull(property.getDefaultValue());
			assertEquals(getMethodName(1), property.getId());
			assertFalse(property.isSet());
			validate(property, null, null);
			assertFalse(property.isSet());
		} catch(Exception e) {
			fail(e.getMessage());
		}
	}

	public void test2String() {
		try {
			Property<String> property = new Property<String>(getMethodName(1), null, (PropertySource<String>[])null);
			property.setValue(null);
			System.out.println(property);
			assertNull(property.getDefaultValue());
			assertEquals(getMethodName(1), property.getId());
			assertFalse(property.isSet());
			validate(property, null, null);
			assertFalse(property.isSet());
		} catch(Exception e) {
			fail(e.getMessage());
		}
	}

	public void test2Integer() {
		try {
			Property<Integer> property = new Property<Integer>(getMethodName(1), null, (PropertySource<Integer>[])null);
			property.setValue(null);
			System.out.println(property);
			assertNull(property.getDefaultValue());
			assertEquals(getMethodName(1), property.getId());
			assertFalse(property.isSet());
			validate(property, null, null);
			assertFalse(property.isSet());
		} catch(Exception e) {
			fail(e.getMessage());
		}
	}

	public void test3String() {
		try {
			Property<String> property = new Property<String>(getMethodName(1), null, (PropertySource<String>[])null);
			property.setObjectValue(null);
			System.out.println(property);
			assertNull(property.getDefaultValue());
			assertEquals(getMethodName(1), property.getId());
			assertFalse(property.isSet());
			validate(property, null, null);
			assertFalse(property.isSet());
		} catch(Exception e) {
			fail(e.getMessage());
		}
	}

	public void test3Integer() {
		try {
			Property<Integer> property = new Property<Integer>(getMethodName(1), null, (PropertySource<Integer>[])null);
			property.setObjectValue(null);
			System.out.println(property);
			assertNull(property.getDefaultValue());
			assertEquals(getMethodName(1), property.getId());
			assertFalse(property.isSet());
			validate(property, null, null);
			assertFalse(property.isSet());
		} catch(Exception e) {
			fail(e.getMessage());
		}
	}

	public void test4String() {
		try {
			Property<String> property = new Property<String>(getMethodName(1), "");
			System.out.println(property);
			assertEquals("", property.getDefaultValue());
			assertEquals(getMethodName(1), property.getId());
			assertFalse(property.isSet());
			validate(property, "", Property.DEFAULT_SOURCE);
			assertFalse(property.isSet());
		} catch(Exception e) {
			fail(e.getMessage());
		}
	}

	public void test4Integer() {
		try {
			Property<Integer> property = new Property<Integer>(getMethodName(1), 0);
			System.out.println(property);
			assertEquals(new Integer(0), property.getDefaultValue());
			assertEquals(getMethodName(1), property.getId());
			assertFalse(property.isSet());
			validate(property, 0, Property.DEFAULT_SOURCE);
			assertFalse(property.isSet());
		} catch(Exception e) {
			fail(e.getMessage());
		}
	}

	public void test4Double() {
		try {
			Property<Double> property = new Property<Double>(getMethodName(1), 0.0d);
			System.out.println(property);
			assertEquals(new Double(0.0), property.getDefaultValue());
			assertEquals(getMethodName(1), property.getId());
			assertFalse(property.isSet());
			validate(property, 0.0d, Property.DEFAULT_SOURCE);
			assertFalse(property.isSet());
		} catch(Exception e) {
			fail(e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
    public void testSystemPropertyString1() {
		try {
			String name = getMethodName(1);
			Property<String> property = new Property<String>(getMethodName(1), (String)null);
			SystemPropertySource<String> sysProp = new SystemPropertySource<String>(name, 1, false);
			property.addSources(sysProp);
			System.out.println(property);			
			assertFalse(property.isSet());
			validate(property, null, null);
		} catch(Exception e) {
			fail(e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
    public void testSystemPropertyString2() {
		try {
			String name = getMethodName(1);
			Property<String> property = new Property<String>(getMethodName(1), (String)null);
			SystemPropertySource<String> sysProp = new SystemPropertySource<String>(name, 1, true);
			property.addSources(sysProp);
			System.out.println(property);			
			assertTrue(property.isSet());
			validate(property, null, name);
		} catch(Exception e) {
			fail(e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
    public void testSystemPropertyString3() {
		try {
			String name = getMethodName(1);
			System.setProperty(name, "gaga");
			Property<String> property = new Property<String>(getMethodName(1), (String)null);
			SystemPropertySource<String> sysProp = new SystemPropertySource<String>(name, 1, false);
			property.addSources(sysProp);
			property.initialize();
			System.out.println(property);			
			assertTrue(property.isSet());
			validate(property, "gaga", name);
		} catch(Exception e) {
			fail(e.getMessage());
		}
	}

	public void testSystemPropertyString4() {
		try {
			String name = getMethodName(1);
			SystemPropertySource<String> sysProp = new SystemPropertySource<String>(name, 1, false);
			@SuppressWarnings("unchecked")
            Property<String> property = new Property<String>(getMethodName(1), (String)null, sysProp);
			System.out.println(property);			
			assertFalse(property.isSet());
			validate(property, null, null);
		} catch(Exception e) {
			fail(e.getMessage());
		}
	}

	public void testSystemPropertyString5() {
		try {
			String name = getMethodName(1);
			SystemPropertySource<String> sysProp = new SystemPropertySource<String>(name, 1, true);
			@SuppressWarnings("unchecked")
            Property<String> property = new Property<String>(getMethodName(1), (String)null, sysProp);
			System.out.println(property);			
			assertTrue(property.isSet());
			validate(property, null, name);
		} catch(Exception e) {
			fail(e.getMessage());
		}
	}

	public void testSystemPropertyString6() {
		try {
			String name = getMethodName(1);
			System.setProperty(name, "gaga");
			SystemPropertySource<String> sysProp = new SystemPropertySource<String>(name, 1, false);
			@SuppressWarnings("unchecked")
            Property<String> property = new Property<String>(getMethodName(1), (String)null, sysProp);
			property.initialize();
			System.out.println(property);			
			assertTrue(property.isSet());
			validate(property, "gaga", name);
		} catch(Exception e) {
			fail(e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
    public void testSystemPropertyInteger1() {
		try {
			String name = getMethodName(1);
			Property<Integer> property = new Property<Integer>(getMethodName(1), (Integer)null);
			SystemPropertySource<Integer> sysProp = new SystemPropertySource<Integer>(name, 1, false, ValueOfConverter.getConverter(Integer.class));
			property.addSources(sysProp);
			System.out.println(property);			
			assertFalse(property.isSet());
			validate(property, null, null);
		} catch(Exception e) {
			fail(e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
    public void testSystemPropertyInteger2() {
		try {
			String name = getMethodName(1);
			Property<Integer> property = new Property<Integer>(getMethodName(1), (Integer)null);
			SystemPropertySource<Integer> sysProp = new SystemPropertySource<Integer>(name, 1, true, ValueOfConverter.getConverter(Integer.class));
			property.addSources(sysProp);
			System.out.println(property);			
			assertTrue(property.isSet());
			validate(property, null, name);
		} catch(Exception e) {
			fail(e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
    public void testSystemPropertyInteger3() {
		try {
			String name = getMethodName(1);
			System.setProperty(name, "123");
			Property<Integer> property = new Property<Integer>(getMethodName(1), (Integer)null);
			SystemPropertySource<Integer> sysProp = new SystemPropertySource<Integer>(name, 1, false, ValueOfConverter.getConverter(Integer.class));
			property.addSources(sysProp);
			property.initialize();
			System.out.println(property);			
			assertTrue(property.isSet());
			validate(property, 123, name);
		} catch(Exception e) {
			fail(e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
    public void testSystemPropertyInteger4() {
		try {
			String name = getMethodName(1);
			System.setProperty(name, "123");
			RangeConstraint<Integer> con = new RangeConstraint<Integer>(100, 1000);
			Property<Integer> property = new Property<Integer>(getMethodName(1), (Integer)null);
			property.addVetoableListener(con);
			SystemPropertySource<Integer> sysProp = new SystemPropertySource<Integer>(name, 1, false, ValueOfConverter.getConverter(Integer.class));
			property.addSources(sysProp);
            property.initialize();
			System.out.println(property);			
			assertTrue(property.isSet());
			validate(property, 123, name);
		} catch(Exception e) {
			fail(e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
    public void testSystemPropertyInteger5() {
		try {
			String name = getMethodName(1);
			System.setProperty(name, "99");
			RangeConstraint<Integer> con = new RangeConstraint<Integer>(100, 1000);
			Property<Integer> property = new Property<Integer>(getMethodName(1), (Integer)null);
			property.addVetoableListener(con);
			SystemPropertySource<Integer> sysProp = new SystemPropertySource<Integer>(name, 1, false, ValueOfConverter.getConverter(Integer.class));
            property.addSources(sysProp);
			try {
	            property.initialize();
				fail("should throw");
			} catch(Exception e) {
//				System.out.println(e.getPropertySourceChangeEvent() + ", " + e.getMessage());
			}
			System.out.println(property);			
			assertFalse(property.isSet());
			validate(property, null, null);
		} catch(Exception e) {
			fail(e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
    public void testSystemPropertyInteger6() {
		try {
			String name = getMethodName(1);
			System.setProperty(name, "1001");
			RangeConstraint<Integer> con = new RangeConstraint<Integer>(100, 1000);
			Property<Integer> property = new Property<Integer>(getMethodName(1), (Integer)null);
			property.addVetoableListener(con);
			SystemPropertySource<Integer> sysProp = new SystemPropertySource<Integer>(name, 1, false, ValueOfConverter.getConverter(Integer.class));
            property.addSources(sysProp);
			try {
	            property.initialize();
				fail("should throw");
			} catch(Exception e) {
//				System.out.println(e.getPropertyChangeEvent() + ", " + e.getMessage());
			}
			System.out.println(property);			
			assertFalse(property.isSet());
			validate(property, null, null);
		} catch(Exception e) {
			fail(e.getMessage());
		}
	}

	protected void validate(Property<?> property, Object value, String masterName) {
		validate(property.getValue(), value);
		if (property.getMaster() == null) {
			assertNull(masterName);
		} else {
			assertNotNull(masterName);
			validate(property.getMaster().getValue(), value);
		}
	}

	protected void validate(Object value1, Object value2) {
		if (value1 == null) {
			assertNull(value2);
		} else {
			assertNotNull(value2);
			assertEquals(value1, value2);
		}
	}
	
    protected String getMethodName(int index) {
        Throwable t = new Throwable();
        t.fillInStackTrace();
        StackTraceElement[] elements = t.getStackTrace();
        if (elements.length > index) {
            return elements[index].getMethodName();
        }
        return "";
    }
    
    @SuppressWarnings("unchecked")
    protected void test1() {
//    	try {
        	Property<Boolean> test1 = new Property<Boolean>("test", true);
			test1.addSources(
					new PropertySource<Boolean>("x", 100, false),
					new PropertySource<Boolean>("y", 100, false));
//		} catch (PropertyVetoException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (PropertyConversionException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
    }
}
