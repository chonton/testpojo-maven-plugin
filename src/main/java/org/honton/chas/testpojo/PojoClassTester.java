package org.honton.chas.testpojo;

public class PojoClassTester {

    private final PojoClass pojoClass;

    PojoClassTester(ClassLoader pojoClassLoader, String pojoClassName) throws ClassNotFoundException {
        pojoClass = new PojoClass(pojoClassLoader, pojoClassName);
    }

    public boolean test() throws Exception {
        if (!pojoClass.isTestable()) {
            return true;
        }

        final Object standard = createVariant(-1);
        if(standard==null) {
            return false;
        }

        final int variantCount = pojoClass.getVariationCount();
        if(variantCount>0) {
            if(!testVariationInEquality(standard, variantCount)) {
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
        for(int i = 0; i<variantCount; ++i) {
            Object variant = createVariant(i);
            if(variant==null || equal(prior, variant)) {
                return false;
            }
            prior = variant;
        }
        return !equal(prior, standard);
    }

    private static boolean equal(Object prior, Object variant) {
        boolean equal = variant.equals(prior);
        if(equal){
            System.err.println(variant + " == " + prior);
        }
        return equal;
    }

    private Object createVariant(int variantIdx) throws Exception {
        Object variant = pojoClass.createVariant(variantIdx);
        variant.toString();
        return testPojoToMapToPojo(variant) && testPojoToBuilderToPojo(variant) ? variant : null;
    }

    private boolean testPojoToBuilderToPojo(Object pojo) {
        Object copy = pojoClass.createCopyThroughBuilder(pojo);
        return copy == null || comparePojos(pojo, copy);
    }

    private boolean testPojoToMapToPojo(Object pojo) {
        return comparePojos(pojo, pojoClass.createCopyThroughMap(pojo));
    }

    private boolean comparePojos(Object pojo, Object copy) {
        if (pojo == copy) {
            System.err.println("pojo === copy");
            return false;
        }
        if (!pojo.equals(copy)) {
            System.err.println(pojo + " != " + copy);
            return false;
        }
        if (pojo.hashCode() != copy.hashCode()) {
            System.err.println(pojo.hashCode() + " != " + copy.hashCode());
            return false;
        }
        return true;
    }

}
