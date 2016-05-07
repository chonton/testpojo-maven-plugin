package org.honton.chas.testpojo;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PojoClass {

    static private final TypeReference<Map<String, Object>> MAP_STRING_TO_OBJECT_TYPE = new TypeReference<Map<String, Object>>() {
    };
    static private final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ClassLoader classLoader;
    private final Class<?> pojoClass;
    private final Constructor<?> constructor;
    private final List<Method> setters = new ArrayList<>();

    PojoClass(ClassLoader classLoader, String pojoClassName) throws ClassNotFoundException {
        this(classLoader, classLoader.loadClass(pojoClassName));
    }

    PojoClass(ClassLoader classLoader, Class<?> pojoClass) throws ClassNotFoundException {
        this.classLoader = classLoader;
        this.pojoClass = pojoClass;
        constructor = findConstructor();
        findSetters();
    }

    public boolean isTestable() {
        return constructor != null;
    }

    public Object createCopyThroughMap(Object pojo) {
        Map<String, Object> map = OBJECT_MAPPER.convertValue(pojo, MAP_STRING_TO_OBJECT_TYPE);
        return OBJECT_MAPPER.convertValue(map, pojoClass);
    }

    public int getVariationCount() {
        return Math.max(constructor.getParameterTypes().length, setters.size());
    }

    /**
     * @param varientIdx -1 for all 'default' values
     * @return a filled pojo
     * @throws Exception
     */
    public Object createVariant(int varientIdx) throws Exception {
        Object o = constructor.newInstance(createArgs(varientIdx));
        if(varientIdx>=constructor.getParameterTypes().length) {
            Method setter = setters.get(varientIdx);
            setter.invoke(o, createVariant(setter.getParameterTypes()[0], true));
        }
        return o;
    }

    private Object[] createArgs(int variantIdx) throws Exception {
        Class<?>[] argTypes = constructor.getParameterTypes();
        Object[] args = new Object[argTypes.length];
        for (int i = 0; i < argTypes.length; ++i) {
            args[i] = createVariant(argTypes[i], i==variantIdx);
        }
        return args;
    }

    private Object createVariant(Class<?> argType, boolean isVarient) throws Exception {
        if(isVarient) {
            Object rc= VARIENT_VALUES.get(argType);
            if(rc!=null) {
                return rc;
            }
            if(argType.isAssignableFrom(Collection.class) || argType.isAssignableFrom(List.class)) {
                return new ArrayList<Object>();
            }
            if(argType.isAssignableFrom(Map.class)) {
                return new HashMap<Object,Object>();
            }
            PojoClass pc = new PojoClass(classLoader, argType);
            if(pc.isTestable()) {
                return pc.createVariant(-1);
            }
        }
        return DEFAULT_VALUES.get(argType);
    }

    @SuppressWarnings("serial")
    private static final Map<Class<?>, Object> DEFAULT_VALUES = new HashMap<Class<?>, Object>() {
        {
            put(Boolean.TYPE, false);
            put(Byte.TYPE, (byte) 0);
            put(Character.TYPE, (char) 0);
            put(Short.TYPE, (short) 0);
            put(Integer.TYPE, (int) 0);
            put(Long.TYPE, (long) 0);
            put(Float.TYPE, (float) 0);
            put(Double.TYPE, (double) 0);
        }
    };

    @SuppressWarnings("serial")
    private static final Map<Class<?>, Object> VARIENT_VALUES = new HashMap<Class<?>, Object>() {
        {
            put(Boolean.TYPE, true);
            put(Byte.TYPE, (byte) 1);
            put(Character.TYPE, (char) 1);
            put(Short.TYPE, (short) 1);
            put(Integer.TYPE, (int) 1);
            put(Long.TYPE, (long) 1);
            put(Float.TYPE, (float) 1);
            put(Double.TYPE, (double) 1);
        }
    };

    private Constructor<?> findConstructor() {
        int leastNArgs = Integer.MAX_VALUE;
        Constructor<?> leastArgs = null;
        for (Constructor<?> constructor : pojoClass.getConstructors()) {
            if (Modifier.isPublic(constructor.getModifiers())) {
                int nArgs = constructor.getParameterTypes().length;
                if (nArgs == 0) {
                    return constructor;
                }
                if (nArgs < leastNArgs) {
                    nArgs = leastNArgs;
                    leastArgs = constructor;
                }
            }
        }
        return leastArgs;
    }

    private void findSetters() {
        for (Method method : pojoClass.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())
                    && Void.TYPE.equals(method.getReturnType())
                    && 1==method.getParameterTypes().length
                    && method.getName().startsWith("set")) {
                setters.add(method);
            }
        }
    }

    public Object createCopyThroughBuilder(Object pojo) {
        PojoBuilder builder = new PojoBuilder(pojoClass);
        if (!builder.createBuilder(pojo)) {
            return null;
        }
        if (!builder.isInstanceBuilder()) {
            Map<String, Object> map = OBJECT_MAPPER.convertValue(pojo, MAP_STRING_TO_OBJECT_TYPE);
            builder.setBuilderValues(map);
        }
        return builder.build();
    }
}
