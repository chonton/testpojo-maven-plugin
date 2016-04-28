package org.honton.chas.testpojo;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import lombok.SneakyThrows;

/**
 * Find Builder associated with @Data class
 */
public class Builder {

    private Method publicStaticBuilderMethod;
    private Method publicBuildMethod;
    private Method publicToBuilderMethod;
    private Object builder;

    public Builder(Class<?> dtoClass) {
        if (findBuilderMethod(dtoClass)) {
            findInstanceBuilder(dtoClass);
        }
    }

    @SneakyThrows
    public boolean createBuilder(Object dto) {
        if (publicStaticBuilderMethod == null) {
            return false;
        }
        builder = (publicToBuilderMethod != null ? publicToBuilderMethod : publicStaticBuilderMethod).invoke(dto);
        builder.toString();
        return true;
    }

    public boolean isInstanceBuilder() {
        return publicToBuilderMethod != null;
    }

    @SneakyThrows
    public Object build() {
        return publicBuildMethod.invoke(builder);
    }

    public void setBuilderValues(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            invokeSetter(entry.getKey(), entry.getValue());
        }
    }

    private static final Map<Class<?>, Class<?>> PRIMATIVE_TO_WRAPPER = ImmutableMap.<Class<?>, Class<?>> builder()
            .put(Byte.TYPE, Byte.class)
            .put(Short.TYPE, Short.class)
            .put(Integer.TYPE, Integer.class)
            .put(Long.TYPE, Long.class)
            .put(Float.TYPE, Float.class)
            .put(Double.TYPE, Double.class)
            .put(Character.TYPE, Character.class)
            .put(Boolean.TYPE, Boolean.class)
            .build();

    private void invokeSetter(String name, Object value) {
        for (Method mth : builder.getClass().getDeclaredMethods()) {
            if (!Modifier.isStatic(mth.getModifiers())
                    && mth.getName().equals(name) 
                    && mth.getParameterTypes().length == 1) {
                Class<?> parameterClass = mth.getParameterTypes()[0];
                if (parameterClass.isPrimitive()) {
                    if(value==null) {
                        continue;
                    }
                    parameterClass = PRIMATIVE_TO_WRAPPER.get(parameterClass);
                }
                if (parameterClass.isAssignableFrom(value.getClass())) {
                    try {
                        mth.invoke(builder, value);
                        return;
                    } catch (Exception e) {
                        System.err.println("builder method " + name + ": " + e.getMessage());
                    }
                }
            }
        }
    }

    private boolean findBuilderMethod(Class<?> dtoClass) {
        for (Method method : dtoClass.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers()) 
                    && Modifier.isPublic(method.getModifiers())
                    && findBuildMethod(dtoClass, method.getReturnType())) {
                this.publicStaticBuilderMethod = method;
                return true;
            }
        }
        return false;
    }

    private boolean findBuildMethod(Class<?> dtoClass, Class<?> builderClass) {
        for (Method method : builderClass.getDeclaredMethods()) {
            if (method.getReturnType().equals(dtoClass) 
                    && method.getParameterTypes().length == 0
                    && !Modifier.isStatic(method.getModifiers()) 
                    && Modifier.isPublic(method.getModifiers())) {
                this.publicBuildMethod = method;
                return true;
            }
        }
        return false;
    }

    private void findInstanceBuilder(Class<?> dtoClass) {
        try {
            Method method = dtoClass.getDeclaredMethod("toBuilder");
            if (Modifier.isPublic(method.getModifiers()) 
                    && !Modifier.isStatic(method.getModifiers())) {
                this.publicToBuilderMethod = method;
            }
        } catch (NoSuchMethodException e) {
        }
    }
}
