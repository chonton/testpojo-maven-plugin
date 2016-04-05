package org.honton.chas.testpojo;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.SneakyThrows;

/**
 * test pojo setters, getters, equals, hashCode
 */
@Mojo(name = "test", defaultPhase = LifecyclePhase.TEST, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class TestPojoMojo extends AbstractMojo {
    
    @Parameter(property = "project.runtimeClasspathElements", readonly = true)
    private List<String> runtimeScope;
    
    @Parameter(property = "project.compileClasspathElements", readonly = true)
    private List<String> compileScope;
    
    @Parameter(property = "project.build.outputDirectory", readonly = true)
    private String outputDirectory;

    @Override
    public void execute() throws MojoExecutionException {
        int errors = testPojos();
        if(errors>0) {
            throw new MojoExecutionException(errors + " pojos had errors");
        }
    }

    
    @SafeVarargs
    private final URL[] getUrls(String classes, List<String>... scopes) {
        Set<URL> urls = new HashSet<>();
        addUrl(urls, classes);
        for(List<String> elements : scopes) {
            addScope(urls, elements);
        }
        return urls.toArray(new URL[urls.size()]);
    }

    private void addScope(Set<URL> urls, List<String> elements) {
        for (String element : elements) {
            addUrl(urls, element);
        }
    }

    private void addUrl(Set<URL> urls, String element) {
        try {
            urls.add(new File(element).toURI().toURL());
        } catch (MalformedURLException e) {
            getLog().info(element + " not a valid location");
        }
    }

    private List<String> getPojoNames() {
        final List<String> collector = new ArrayList<>();
        new Reflections(new ConfigurationBuilder()
                .setUrls(getUrls(outputDirectory))
                .setScanners(new PojoScanner(getLog(), collector)));
        return collector;
    }
    
    private ClassLoader createClassLoader() {
        return new URLClassLoader(getUrls(outputDirectory, compileScope, runtimeScope), 
                Thread.currentThread().getContextClassLoader());
    }

    private List<Class<?>> getPojoClasses() {
        return ReflectionUtils.forNames(getPojoNames(), createClassLoader() );
    }

    public int testPojos() {
        EnhancedRandom randomBuilder = EnhancedRandomBuilder.aNewEnhancedRandomBuilder().build();
        int errors = 0;
        for (Class<?> dtoClass : getPojoClasses() ) {
            getLog().info("testing " + dtoClass.getCanonicalName());
            try {
                testPojoInstance(dtoClass, dtoClass.newInstance());
                testPojoInstance(dtoClass, randomBuilder.nextObject(dtoClass));
            } catch (Exception ex) {
                ++errors;
                getLog().error(dtoClass.getCanonicalName(), ex);
            }
        }
        return errors;
    }

    private boolean testPojoInstance(Class<?> dtoClass, Object dto) {
        dto.toString();
        return testPojoToMapToPojo(dtoClass, dto)  && testPojoToBuilderToPojo(dtoClass, dto);
    }

    @SneakyThrows
    private boolean testPojoToBuilderToPojo(Class<?> dtoClass, Object dto) {
        Builder helper = new Builder(getLog(), dtoClass);
        if (!helper.createBuilder(dto)) {
            return true;
        }
        if (!helper.isInstanceBuilder()) {
            Map<String, Object> map = OBJECT_MAPPER.convertValue(dto, MAP_STRING_TO_OBJECT_TYPE);
            helper.setBuilderValues(map);
        }
        return comparePojos(dto, helper.build());
    }

    static private final TypeReference<Map<String, Object>> MAP_STRING_TO_OBJECT_TYPE = new TypeReference<Map<String, Object>>() {
    };
    static private final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private boolean testPojoToMapToPojo(Class<?> dtoClass, Object dto) {
        Map<String, Object> map = OBJECT_MAPPER.convertValue(dto, MAP_STRING_TO_OBJECT_TYPE);
        Object copy = OBJECT_MAPPER.convertValue(map, dtoClass);
        return comparePojos(dto, copy);
    }

    private boolean comparePojos(Object dto, Object copy) {
        if(!dto.equals(copy)) {
            getLog().error(dto + " != " + copy);
            return false;
        }
        if(dto.hashCode() != copy.hashCode()) {
            getLog().error(dto.hashCode() + " != " + copy.hashCode());
            return false;
        }
        return true;
    }

}
