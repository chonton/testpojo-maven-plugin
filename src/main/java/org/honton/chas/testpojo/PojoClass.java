package org.honton.chas.testpojo;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.*;
import java.util.*;

public class PojoClass {

    static private final TypeReference<Map<String, Object>> MAP_STRING_TO_OBJECT_TYPE = new TypeReference<Map<String, Object>>() {
    };

    static private final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new GuavaModule())
            .registerModule(new JodaModule())
            .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .enable(SerializationFeature.WRITE_DATES_WITH_ZONE_ID);

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
            put(Byte.class, new Byte((byte) 1));
            put(Character.class, new Character((char) 1));
            put(Short.class, new Short((short) 1));
            put(Integer.class, new Integer(1));
            put(Long.class, new Long((long) 1));
            put(Float.class, new Float((float) 1));
            put(Double.class, new Double((double) 1));

            put(Duration.class, Duration.ZERO);
            put(Instant.class, Instant.now());
            put(LocalDate.class, LocalDate.now());
            put(LocalDateTime.class, LocalDateTime.now());
            put(LocalTime.class, LocalTime.now());
            put(MonthDay.class, MonthDay.now());
            put(OffsetDateTime.class, OffsetDateTime.now());
            put(OffsetTime.class, OffsetTime.now());
            put(Period.class, Period.ZERO);
            put(Year.class, Year.now());
            put(YearMonth.class, YearMonth.now());
            put(ZonedDateTime.class, ZonedDateTime.now());
            put(ZoneId.class, ZoneId.systemDefault());
            put(ZoneOffset.class, ZoneOffset.UTC);
        }
    };

    private static Map<Class<?>, PojoClass> cache = new HashMap<>();

    private final Class<?> pojoClass;
    private final Constructor<?> constructor;
    private final List<Method> setters = new ArrayList<>();
    private Argument[] arguments;

    private PojoClass(Class<?> pojoClass) {
        this.pojoClass = pojoClass;
        constructor = findConstructor();
        cache.put(pojoClass, this);

        if (constructor != null) {
            constructor.setAccessible(true);
            findSetters();
        }
    }

    static public PojoClass from(Class<?> pojoClass) {
        PojoClass pc = cache.get(pojoClass);
        return pc == null ? new PojoClass(pojoClass) : pc;
    }

    private Argument[] getArguments() {
        if (arguments == null) {
            Class<?>[] argTypes = constructor.getParameterTypes();
            if (argTypes.length > 0) {
                arguments = new Argument[argTypes.length];
                int i = 0;
                for (Class<?> argType : argTypes) {
                    arguments[i++] = new Argument(argType);
                }
            } else {
                arguments = new Argument[setters.size()];
                int i = 0;
                for (Method setter : setters) {
                    Class<?> argType = setter.getParameterTypes()[0];
                    arguments[i++] = new Argument(argType);
                }
            }
        }
        return arguments;
    }

    public boolean isConstructable() {
        return constructor != null;
    }

    public static boolean isJacksonSerializable(Object value) {
        try {
            return value.getClass().getAnnotation(JsonIgnoreType.class) == null
                    && createCopyThroughString(value) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static Object createCopyThroughMap(Object pojo) {
        Map<String, Object> map = OBJECT_MAPPER.convertValue(pojo, MAP_STRING_TO_OBJECT_TYPE);
        return OBJECT_MAPPER.convertValue(map, pojo.getClass());
    }

    public static Object createCopyThroughString(Object pojo) throws IOException {
        String json = OBJECT_MAPPER.writeValueAsString(pojo);
        return OBJECT_MAPPER.readValue(json, pojo.getClass());
    }

    /**
     * How many fields can be manipulated?
     *
     * @return The count of fields which can be changed.
     */
    public int getVariationCount() {
        return Math.max(constructor.getParameterCount(), setters.size());
    }

    public String getPojoClassSimpleName() {
        return pojoClass.getSimpleName();
    }

    /**
     * @param variantIdx -1 for all 'default' values
     * @return a filled pojo, or null on failures
     * @throws Exception
     */
    public Object createVariant(int variantIdx) throws Exception {
        Object[] args = createInstantiationArgs(variantIdx);
        if(args==null) {
            return null;
        }
        Object o = constructor.newInstance(args);
        if (variantIdx >= constructor.getParameterCount()) {
            Method setter = setters.get(variantIdx);
            Object variant = createInstance(setter.getParameterTypes()[0]);
            if (variant == null) {
                return null;
            }
            setter.invoke(o, variant);
        }
        return o;
    }

    private Object[] createInstantiationArgs(int variantIdx) {
        Argument[] arguments = getArguments();
        Object[] args = new Object[constructor.getParameterCount()];
        for (int i = 0; i < args.length; ++i) {
            boolean isVariantIdx = i == variantIdx;
            Object arg = arguments[i].getArgument(isVariantIdx);
            if( isVariantIdx && arg==null) {
                return null;
            }
            args[i] = arg;
        }
        return args;
    }

    private static Object createInstance(Class<?> argType) {
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
        if (argType.isAssignableFrom(ImmutableList.class)) {
            return ImmutableList.of();
        }
        if (argType.isEnum()) {
            return getFirstEnumValue(argType);
        }

        PojoClass pc = from(argType);
        if (pc.isConstructable()) {
            try {
                return pc.createVariant(-1);
            } catch (Exception e) {
                e.printStackTrace(System.err);
                return null;
            }
        }
        System.out.println("could not create instance of " + argType.getCanonicalName());
        return null;
    }

    private static Object getFirstEnumValue(Class<?> enumClass) {
        try {
            Object[] values = (Object[]) enumClass.getDeclaredMethod("values").invoke(null);
            return values.length > 0 ? values[0] : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private Constructor<?> findConstructor() {
        if (Modifier.isAbstract(pojoClass.getModifiers())) {
            System.out.println("Abstract class " + pojoClass.getCanonicalName());
            return null;
        }
        if (pojoClass.isMemberClass() && !Modifier.isStatic(pojoClass.getModifiers())) {
            System.out.println("Non-static inner class " + pojoClass.getCanonicalName());
            return null;
        }
        int leastNArgs = Integer.MAX_VALUE;
        Constructor<?> leastArgs = null;
        for (Constructor<?> constructor : pojoClass.getConstructors()) {
            if (Modifier.isPublic(constructor.getModifiers())) {
                int nArgs = constructor.getParameterCount();
                if (nArgs == 0) {
                    return constructor;
                }
                if (nArgs < leastNArgs) {
                    leastArgs = constructor;
                }
            }
        }
        if (leastArgs == null) {
            System.out.println("No public constructor for " + pojoClass.getCanonicalName());
        }
        return leastArgs;
    }

    private void findSetters() {
        for (Method method : pojoClass.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())
                    && 1 == method.getParameterCount()
                    && method.getName().startsWith("set")
                    && (Void.TYPE.equals(method.getReturnType())
                        || method.getReturnType().isAssignableFrom(pojoClass))) {
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
        final private Class<?> type;
        final private Object standard;
        private boolean variantCreated;
        private Object variant;

        public Argument(Class<?> type) {
            this.type = type;
            this.standard = STANDARD_VALUES.get(type);
        }

        public Object getArgument(boolean useVariant) {
            if (useVariant) {
                if (!variantCreated) {
                    variant = createInstance(type);
                    variantCreated = true;
                }
                return variant;
            }
            return standard;
        }
    }
}
