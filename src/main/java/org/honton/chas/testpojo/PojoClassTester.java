package org.honton.chas.testpojo;

import java.io.IOException;

public class PojoClassTester {

    static ClassLoader classLoader;

    public static void setClassLoader(ClassLoader classLoader) {
        PojoClassTester.classLoader = classLoader;
    }

    private final PojoClass pojoClass;

    public PojoClassTester(String pojoClassName) throws Exception {
        pojoClass = PojoClass.from(classLoader.loadClass(pojoClassName));
    }

    public boolean test() throws Exception {
        if (!pojoClass.isConstructable()) {
            return true;
        }

        final Object standard = createVariant(-1);
        if (standard == null) {
            return false;
        }

        final int variantCount = pojoClass.getVariationCount();
        if (variantCount > 0) {
            if (!testVariationInEquality(standard, variantCount)) {
                return false;
            }
        }

        if (!standard.equals(standard)) {
            System.err.println("this !== this");
            return false;
        }
        if (standard.equals(null)) {
            System.err.println("this == null");
            return false;
        }
        if (standard.equals(new Object())) {
            System.err.println("this == new Object()");
            return false;
        }
        return true;
    }

    private boolean testVariationInEquality(Object standard, int variantCount) throws Exception {
        Object prior = standard;
        for (int i = 0; i < variantCount; ++i) {
            Object variant = createVariant(i);
            if (variant == null) {
                continue;
            }
            if (areEqual(prior, variant)) {
                return false;
            }
            prior = variant;
        }
        return prior==standard || !areEqual(prior, standard);
    }

    private static boolean areEqual(Object prior, Object variant) {
        boolean equal = variant.equals(prior);
        if (equal) {
            System.err.println(variant + " == " + prior);
        }
        return equal;
    }

    private Object createVariant(int variantIdx) throws Exception {
        Object variant = pojoClass.createVariant(variantIdx);
        if(variant == null) {
            return null;
        }
        variant.toString();

        if (!testJacksonSerialization(variant)) {
            return null;
        }

        Object copy = pojoClass.createCopyThroughBuilder(variant);
        if (copy != null && !comparePojos(variant, copy)) {
            return null;
        }
        return variant;
    }

    private boolean testJacksonSerialization(Object variant) throws IOException {
        return !pojoClass.isJacksonSerializable()
                || (comparePojos(variant, pojoClass.createCopyThroughMap(variant))
                        && comparePojos(variant, pojoClass.createCopyThroughString(variant)));
    }

    private boolean comparePojos(Object pojo, Object copy) {
        if (pojo == copy) {
            System.err.println("pojo === copy");
            return false;
        }
        if (!pojo.equals(copy)) {
            System.err.println("jackson copy should be equal to original; original: " + pojo + ", copy: " + copy);
            return false;
        }
        if (pojo.hashCode() != copy.hashCode()) {
            System.err.println("jackson copy should produce same hashCode as original;  original: " +pojo.hashCode() + ", copy: " + copy.hashCode());
            return false;
        }
        return true;
    }

}
