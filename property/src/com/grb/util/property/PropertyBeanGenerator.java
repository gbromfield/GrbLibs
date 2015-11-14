package com.grb.util.property;

import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

public class PropertyBeanGenerator {
    static public final int GETTERS     = 0x01;
    static public final int SETTERS     = 0x02;
    static public final int ISSET       = 0x04;
        
    public static String generate(
            String classname,
            String packagename,
            Collection<Property<?>> properties,
            String source,
            int operations) {
        StringBuilder bldr = new StringBuilder();
        StringBuilder bodyBldr = new StringBuilder();
        TreeMap<String, Property<?>> propMap = new TreeMap<String, Property<?>>();
        {
            Iterator<Property<?>> it = properties.iterator();
            while(it.hasNext()) {
                Property<?> prop = it.next();
                if ((source == null) ||
                    ((source != null) &&
                    (prop.getSource(source) != null))) {
                    propMap.put(prop.getId().toString(), prop);
                }
            }
        }
        TreeSet<String> imports = new TreeSet<String>();
        imports.add("java.util.Collection");
        imports.add("java.util.Iterator");
        imports.add("java.util.Map");
        imports.add("java.util.TreeMap");
        imports.add("com.solacesystems.common.property.Property");
        if (source != null) {
            imports.add("com.solacesystems.common.property.PropertySource");
        }
        if ((operations & SETTERS) == SETTERS) {
            imports.add("com.solacesystems.common.property.PropertyConversionException");
            imports.add("com.solacesystems.common.property.PropertyVetoException");
        }
        {
            Iterator<String> it = propMap.keySet().iterator();
            while(it.hasNext()) {
                generate(bodyBldr, source, imports, 
                        propMap.get(it.next()), operations);
            }
        }

        generatePackage(bldr, packagename);
        generateImports(bldr, imports);
        generateClassSignature(bldr, classname);
        generateConstructor(bldr, classname, source);  
        generateGenericGets(bldr, source, operations);  
        bldr.append(bodyBldr.toString());
        generateToString(bldr, source);
        bldr.append("}\n");
        return bldr.toString();
    }

    private static void generatePackage(
            StringBuilder bldr, 
            String packagename) {
        bldr.append("package ");
        bldr.append(packagename);
        bldr.append(";\n\n");
    }

    private static void generateImports(
            StringBuilder bldr,
            TreeSet<String> imports) {
        Iterator<String> it = imports.iterator();
        while(it.hasNext()) {
            bldr.append("import ");
            bldr.append(it.next());
            bldr.append(";\n");
        }
        bldr.append("\n");
    }

    private static void generateClassSignature(
            StringBuilder bldr, 
            String classname) {
        bldr.append("/**\n");
        bldr.append(" * ");
        bldr.append(PropertyBeanGenerator.class.getSimpleName());
        bldr.append(" Generated Code\n");
        bldr.append(" */\n");
        bldr.append("public class ");
        bldr.append(classname);
        bldr.append(" {\n\n");
        bldr.append("    private TreeMap<String, Property<?>> mPropertyMap;\n\n");
    }

    private static void generateConstructor(
            StringBuilder bldr, 
            String classname,
            String source) {
        bldr.append("    public ");
        bldr.append(classname);
        bldr.append("(Collection<Property<?>> properties) {\n");
        bldr.append("        mPropertyMap = new TreeMap<String, Property<?>>();\n");
        bldr.append("        Iterator<Property<?>> it = properties.iterator();\n");
        bldr.append("        while(it.hasNext()) {\n");
        bldr.append("            Property<?> property = it.next();\n");
        if (source == null) {
            bldr.append("            mPropertyMap.put(property.getId(), property);\n");
        } else {
            bldr.append("            if (property.getSource(\"");
            bldr.append(source);
            bldr.append("\") != null) {\n");
            bldr.append("                mPropertyMap.put(property.getId(), property);\n");
            bldr.append("            }\n");
        }
        bldr.append("        }\n");
        bldr.append("    }\n\n");
    }
    
    private static void generateGenericGets(
            StringBuilder bldr, 
            String source,
            int operations) {
        bldr.append("    public Map<String, Property<?>> getPropertyMap() {\n");
        bldr.append("        return mPropertyMap;\n");
        bldr.append("    }\n\n");

        bldr.append("    private Property<?> getProperty(String propertyName) {\n");
        bldr.append("        Property<?> property = mPropertyMap.get(propertyName);\n");
        bldr.append("        if (property == null) {\n");
        bldr.append("            throw new IllegalArgumentException(\"Property \\\"\" + propertyName + \"\\\" Not Found\");\n");
        bldr.append("        }\n");
        bldr.append("        return property;\n");
        bldr.append("    }\n\n");

        if (source != null) {
            bldr.append("    private PropertySource<?> getPropertySource(String propertyName) {\n");
            bldr.append("        Property<?> property = getProperty(propertyName);\n");
            bldr.append("        PropertySource<?> source = property.getSource(\"");
            bldr.append(source);
            bldr.append("\");\n");
            bldr.append("        if (source == null) {\n");
            bldr.append("            throw new IllegalArgumentException(\"Property \\\"\" + propertyName + \"\\\"");
            bldr.append(" Source \\\"");
            bldr.append(source);
            bldr.append("\\\" Not Found\");\n");
            bldr.append("        }\n");
            bldr.append("        return source;\n");
            bldr.append("    }\n\n");
        }

        bldr.append("    public Object getValue(String propertyName) {\n");
        if (source == null) {
            bldr.append("        return getProperty(propertyName).getValue();\n");
        } else {
            bldr.append("        return getPropertySource(propertyName).getValue();\n");
        }
        bldr.append("    }\n\n");

        if ((source != null) && ((operations & SETTERS) == SETTERS)) {
            bldr.append("    public void setValue(String propertyName, Object propertyValue) throws PropertyConversionException, PropertyVetoException {\n");
            bldr.append("        getPropertySource(propertyName).setObjectValue(propertyValue);\n");
            bldr.append("    }\n\n");
        }
    }

    private static void generateToString(
            StringBuilder bldr, 
            String source) {
        bldr.append("    @Override\n");
        bldr.append("    public String toString() {\n");
        bldr.append("        StringBuilder bldr = new StringBuilder();\n");
        bldr.append("        Iterator<String> it = mPropertyMap.keySet().iterator();\n");
        bldr.append("        while(it.hasNext()) {\n");
        bldr.append("            Property<?> property = mPropertyMap.get(it.next());\n");
        if (source == null) {
            bldr.append("            bldr.append(property);\n");
        } else {
            bldr.append("            bldr.append(\"property=\\\"\" + property.getId() + \"\\\", \");\n");
            bldr.append("            bldr.append(property.getSource(\"");
            bldr.append(source);
            bldr.append("\"));\n");
            bldr.append("            bldr.append(\"\\n\");\n");
        }
        bldr.append("        }\n");
        bldr.append("        return bldr.toString();\n");
        bldr.append("    }\n\n");
    }
    
    private static void generate(StringBuilder classBldr, 
            String source,
            TreeSet<String> imports, Property<?> property,
            int operations) {
        Class<?> clazz = property.getParameterizedClass();
        if (!clazz.getName().startsWith("java.lang.")) {
            imports.add(clazz.getName());
        }
        if ((operations & GETTERS) == GETTERS) {
            generateGetter(classBldr, property, source, clazz);
        }
        if ((operations & SETTERS) == SETTERS) {
            generateSetter(classBldr, property, source, clazz);
        }
        if ((operations & ISSET) == ISSET) {
            generateIsSetter(classBldr, property, source, clazz);
        }
    }

    private static void generateGetter(StringBuilder classBldr, 
            Property<?> property,
            String source,
            Class<?> clazz) {
        classBldr.append("    public ");
        classBldr.append(clazz.getSimpleName());
        classBldr.append(" get");
        classBldr.append(property.getId());
        classBldr.append("()");
        classBldr.append(" {\n");
        classBldr.append("        return (");
        classBldr.append(clazz.getSimpleName());
        classBldr.append(")");
        if (source == null) {
            classBldr.append("getProperty(\"");
        } else {
            classBldr.append("getPropertySource(\"");
        }
        classBldr.append(property.getId());
        classBldr.append("\").getValue();\n");
        classBldr.append("    }\n\n");
    }

    private static void generateSetter(StringBuilder classBldr, 
            Property<?> property,
            String source,
            Class<?> clazz) {
        classBldr.append("    public void ");
        classBldr.append("set");
        classBldr.append(property.getId());
        classBldr.append("(");
        classBldr.append(clazz.getSimpleName());
        classBldr.append(" ");
        classBldr.append(property.getId());
        classBldr.append(") throws PropertyConversionException, PropertyVetoException {\n");
        if (source == null) {
            classBldr.append("        getProperty(\"");
        } else {
            classBldr.append("        getPropertySource(\"");
        }
        classBldr.append(property.getId());
        classBldr.append("\")");
        classBldr.append(".setObjectValue(");
        classBldr.append(property.getId());
        classBldr.append(");\n");
        classBldr.append("    }\n\n");
    }

    private static void generateIsSetter(StringBuilder classBldr, 
            Property<?> property,
            String source,
            Class<?> clazz) {
        classBldr.append("    public boolean");
        classBldr.append(" isSet");
        classBldr.append(property.getId());
        classBldr.append("()");
        classBldr.append(" {\n");
        classBldr.append("        return ");
        if (source == null) {
            classBldr.append("getProperty(\"");
        } else {
            classBldr.append("getPropertySource(\"");
        }
        classBldr.append(property.getId());
        classBldr.append("\")");
        classBldr.append(".isSet();\n");
        classBldr.append("    }\n\n");
    }
}
