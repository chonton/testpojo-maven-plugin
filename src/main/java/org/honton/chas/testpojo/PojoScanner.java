package org.honton.chas.testpojo;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.reflections.adapters.MetadataAdapter;
import org.reflections.scanners.AbstractScanner;

import lombok.RequiredArgsConstructor;

/**
 * Reflections scanner that finds pojos.  A pojo is defined here as any class with 
 * an equals and hashCode method. 
 */
@RequiredArgsConstructor
public class PojoScanner extends AbstractScanner {
    final private Collection<String> collector;

    @SuppressWarnings("unchecked")
    @Override
    public void scan(Object cls) {
        @SuppressWarnings("rawtypes")
        MetadataAdapter meta = getConfiguration().getMetadataAdapter();
        String className = meta.getClassName(cls);
        if (hasEqualsAndHashCode(meta, cls)) {
            collector.add(className);
        }
    }

    private static final List<String> NO_ARGS = Collections.emptyList();
    private static final List<String> SINGLE_OBJECT_ARG = Arrays.asList("java.lang.Object");

    private static <C, F, M> boolean hasEqualsAndHashCode(MetadataAdapter<C, F, M> meta, C cls) {
        boolean equals = false;
        boolean hashCode = false;
        for (M method : meta.getMethods(cls)) {
            equals |= isMethod(meta, method, "boolean", "equals", SINGLE_OBJECT_ARG);
            hashCode |= isMethod(meta, method, "int", "hashCode", NO_ARGS);
            if (equals && hashCode) {
                return true;
            }
        }
        return false;
    }

    private static <C, F, M> boolean isMethod(MetadataAdapter<C, F, M> meta, M method, String returnType, String methodName,
            List<String> parameterNames) {
        return meta.getReturnTypeName(method).equals(returnType) && meta.getMethodName(method).equals(methodName)
                && meta.getParameterNames(method).equals(parameterNames);
    }
}