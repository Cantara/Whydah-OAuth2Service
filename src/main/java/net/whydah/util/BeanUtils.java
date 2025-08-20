// Create a new file: src/main/java/net/whydah/util/BeanUtils.java
package net.whydah.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class BeanUtils {
    
    public static void copyProperties(Object source, Object target) {
        if (source == null || target == null) {
            return;
        }
        
        Class<?> sourceClass = source.getClass();
        Class<?> targetClass = target.getClass();
        
        try {
            // Get all fields from source
            Field[] sourceFields = sourceClass.getDeclaredFields();
            
            for (Field sourceField : sourceFields) {
                String fieldName = sourceField.getName();
                
                try {
                    // Get getter method from source
                    String getterName = "get" + capitalize(fieldName);
                    Method getter = sourceClass.getMethod(getterName);
                    
                    // Get setter method from target
                    String setterName = "set" + capitalize(fieldName);
                    Method setter = targetClass.getMethod(setterName, sourceField.getType());
                    
                    // Copy value
                    Object value = getter.invoke(source);
                    setter.invoke(target, value);
                    
                } catch (Exception e) {
                    // Skip fields that don't have getter/setter or can't be copied
                    continue;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error copying properties", e);
        }
    }
    
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}