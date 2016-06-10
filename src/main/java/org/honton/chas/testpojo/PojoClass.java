package org.honton.chas.testpojo;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class PojoClass {

    static private final TypeReference<Map<String, Object>> MAP_STRING_TO_OBJECT_TYPE =
        new TypeReference<Map<String, Object>>() {};

    static private final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .registerModule(new GuavaModule())
        .registerModule(new JodaModule());

    @SuppressWarnings("serial")
    private static final Map<Class<?>, Object> STANDARD_VALUES = new HashMap<Class<?>, Object>() {
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
    private static final Map<Class<?>, Object> VARIANT_VALUES = new HashMap<Class<?>, Object>() {
        {
            put(Boolean.TYPE, true);
            put(Byte.TYPE, (byte) 1);
            put(Character.TYPE, (char) 1);
            put(Short.TYPE, (short) 1);
            put(Integer.TYPE, 1);
            put(Long.TYPE, (long) 1);
            put(Float.TYPE, (float) 1);
            put(Double.TYPE, (double) 1);

            put(Boolean.class, Boolean.TRUE);
            put(Byte.class, new Byte((byte)1));
            put(Character.class, new Character((char) 1));
            put(Short.class, new Short((short) 1));
            put(Integer.class, new Integer(1));
            put(Long.class, new Long((long) 1));
            put(Float.class, new Float((float) 1));
            put(Double.class, new Double((double) 1));
        }
    };

    private static Map<Class<?>,PojoClass> cache = new HashMap<>();

    private final ClassLoader classLoader;
    private final Class<?> pojoClass;
    private final Constructor<?> constructor;
    private final List<Method> setters = new ArrayList<>();
    private final boolean isTestable;
    private final Argument[] arguments;

    private PojoClass(ClassLoader classLoader, Class<?> pojoClass) {
        this.classLoader = classLoader;
        this.pojoClass = pojoClass;
        constructor = findConstructor();
        cache.put(pojoClass, this);

        if (constructor == null) {
            System.out.println("No public constructor for " + pojoClass.getCanonicalName());
            isTestable = false;
            arguments = null;
        }
        else {
            findSetters();
            arguments = createArguments();
            if(arguments == null) {
                System.out.println("No variant for " + pojoClass.getCanonicalName());
            }
            isTestable = arguments != null;
        }
    }

    static PojoClass from(ClassLoader classLoader, String pojoClassName) throws Exception {
        return from(classLoader, classLoader.loadClass(pojoClassName));
    }

    static PojoClass from(ClassLoader classLoader, Class<?> pojoClass) {
        PojoClass pc = cache.get(pojoClass);
        return pc == null ? new PojoClass(classLoader, pojoClass) : pc;
    }

    private Argument[] createArguments() {
        Class<?>[] argTypes = constructor.getParameterTypes();
        if (argTypes.length > 0) {
            return createContructorArguments(argTypes);
        }
        else {
            return createSetterArguments();
        }
    }

    private Argument[] createContructorArguments(Class<?>[] argTypes) {
        Argument[] arguments = new Argument[argTypes.length];
        int i = 0;
        for (Class<?> argType : argTypes) {
            Object variant = createVariant(argType);
            if(variant==null) {
                return null;
            }
            arguments[i++] = new Argument(argType, variant);
        }
        return arguments;
    }

    private Argument[] createSetterArguments() {
        Argument[] arguments = new Argument[setters.size()];
        int i = 0;
        for (Method setter : setters) {
            Class<?> argType = setter.getParameterTypes()[0];
            Object variant = createVariant(argType);
            if(variant==null) {
                return null;
            }
            arguments[i++] = new Argument(argType, variant);
        }
        return arguments;
    }

    public boolean isTestable() {
       return isTestable;
    }

    public boolean isJacksonSerializable() {
        return pojoClass.getAnnotation(JsonIgnoreType.class)==null;
    }

    public Object createCopyThroughMap(Object pojo) {
        Map<String, Object> map = OBJECT_MAPPER.convertValue(pojo, MAP_STRING_TO_OBJECT_TYPE);
        return OBJECT_MAPPER.convertValue(map, pojoClass);
    }

    public Object createCopyThroughString(Object pojo) throws IOException {
        String json = OBJECT_MAPPER.writeValueAsString(pojo);
        return OBJECT_MAPPER.readValue(json, pojoClass);
    }

    /**
     * How many fields can be manipulated?
     * @return The count of fields which can be changed.
     */
    public int getVariationCount() {
        return Math.max(constructor.getParameterTypes().length, setters.size());
    }

    /**
     * @param variantIdx -1 for all 'default' values
     * @return a filled pojo, or null on failures
     */
    public Object createVariant(int variantIdx) {
        try {
            Object[] args = createInstantiationArgs(variantIdx);
            Object o = constructor.newInstance(args);
            if (variantIdx >= constructor.getParameterTypes().length) {
                Method setter = setters.get(variantIdx);
                Object variant = createVariant(setter.getParameterTypes()[0]);
                setter.invoke(o, variant);
            }
            return o;
        }
        catch (Exception ex) {
            ex.printStackTrace(System.err);
            return null;
        }
    }

    private Object[] createInstantiationArgs(int variantIdx) {
        Object[] args = new Object[constructor.getParameterCount()];
        for(int i= 0; i<args.length; ++i) {
            args[i] = arguments[i].getArgument(i == variantIdx);
        }
        return args;
    }

    private Object createVariant(Class<?> argType) {
        Object rc = VARIANT_VALUES.get(argType);
        if (rc != null) {
            return rc;
        }
        if (argType.isAssignableFrom(Collection.class) || argType.isAssignableFrom(List.class)) {
            return new ArrayList<>();
        }
        if (argType.isAssignableFrom(Map.class)) {
            return new HashMap<>();
        }
        if( argType.isAssignableFrom(ImmutableList.class)) {
            return ImmutableList.of();
        }
        if( argType.isEnum() ) {
            Enum[] values = getEnumValues((Class<Enum>)argType);
            return values!=null && values.length>0 ?values[0] :null;
        }
        PojoClass pc = from(classLoader, argType);
        if (pc.isTestable()) {
            return pc.createVariant(-1);
        }
        return null;
    }

    private <E extends Enum<E>> E[] getEnumValues(Class<E> enumClass) {
        try {
            return (E[])enumClass.getDeclaredMethod("values").invoke(null);
        }
        catch (Exception ex) {
            return null;
        }
    }

    private Constructor<?> findConstructor() {
        if(Modifier.isAbstract(pojoClass.getModifiers())) {
            return null;
        }
        int leastNArgs = Integer.MAX_VALUE;
        Constructor<?> leastArgs = null;
        for (Constructor<?> constructor : pojoClass.getConstructors()) {
            if (Modifier.isPublic(constructor.getModifiers())) {
                int nArgs = constructor.getParameterTypes().length;
                if (nArgs == 0) {
                    return constructor;
                }
                if (nArgs < leastNArgs) {
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

    private static class Argument {
        Object standard;
        Object variant;

        public Argument(Class<?> standardClass, Object variant) {
            this.standard = STANDARD_VALUES.get(standardClass);
            this.variant = variant;
        }

        public Object getArgument(boolean useVariant) {
            return useVariant ?variant :standard;
        }
    }
}
