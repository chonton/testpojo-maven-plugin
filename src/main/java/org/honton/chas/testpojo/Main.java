package org.honton.chas.testpojo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;


@RequiredArgsConstructor
public class Main {

    /**
     * Run the tests against the pojos in classesDirectory
     * @param args 
     *  [0] absolute directory name of classes directory 
     */
    public static void main(String[] args) {
        System.exit(new Main(new File(args[0])).testPojos());
    }

    private final File classesDirectory;

    public int testPojos() {
        EnhancedRandom randomBuilder = EnhancedRandomBuilder.aNewEnhancedRandomBuilder().build();
        int errors = 0;
        for (String pojoName : getPojoNames() ) {
            try {
                Class<?> pojoClass = Class.forName(pojoName);
                testPojoInstance(pojoClass, pojoClass.newInstance());
                testPojoInstance(pojoClass, randomBuilder.nextObject(pojoClass));
            } catch (Throwable ex) {
                ex.printStackTrace();
                ++errors;
                System.err.println(pojoName + ": " + ex.getMessage());
                ex.printStackTrace(System.err);
            }
        }
        return errors;
    }

    @SneakyThrows
    private List<String> getPojoNames() {
        final List<String> collector = new ArrayList<>();
        new Reflections(new ConfigurationBuilder()
                .setUrls(Collections.singletonList(classesDirectory.toURI().toURL()))
                .setScanners(new PojoScanner(collector)));
        return collector;
    }

    private boolean testPojoInstance(Class<?> pojoClass, Object pojo) {
        pojo.toString();
        return testPojoToMapToPojo(pojoClass, pojo)  
                && testPojoToBuilderToPojo(pojoClass, pojo);
    }

    private boolean testPojoToBuilderToPojo(Class<?> pojoClass, Object pojo) {
        Builder helper = new Builder(pojoClass);
        if (!helper.createBuilder(pojo)) {
            return true;
        }
        if (!helper.isInstanceBuilder()) {
            Map<String, Object> map = OBJECT_MAPPER.convertValue(pojo, MAP_STRING_TO_OBJECT_TYPE);
            helper.setBuilderValues(map);
        }
        return comparePojos(pojo, helper.build());
    }

    static private final TypeReference<Map<String, Object>> MAP_STRING_TO_OBJECT_TYPE = new TypeReference<Map<String, Object>>() {
    };
    static private final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private boolean testPojoToMapToPojo(Class<?> pojoClass, Object pojo) {
        Map<String, Object> map = OBJECT_MAPPER.convertValue(pojo, MAP_STRING_TO_OBJECT_TYPE);
        Object copy = OBJECT_MAPPER.convertValue(map, pojoClass);
        return comparePojos(pojo, copy);
    }

    private boolean comparePojos(Object pojo, Object copy) {
        if(!pojo.equals(copy)) {
            System.err.println(pojo + " != " + copy);
            return false;
        }
        if(pojo.hashCode() != copy.hashCode()) {
            System.err.println(pojo.hashCode() + " != " + copy.hashCode());
            return false;
        }
        return true;
    }

}
