package org.honton.chas.testpojo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Main {

    /**
     * Run the tests against the pojos in classesDirectory
     * 
     * @param args
     *            [0] absolute directory name of classes directory
     * @throws IOException on various file problems 
     */
    public static void main(String[] args) throws IOException {
        System.exit(new Main(new File(args[0]), new File(args[1])).testPojos());
    }

    private final File classesDirectory;
    private final File dependencies;

    public int testPojos() throws IOException {
        PojoClassTester.setClassLoader(getDependencies());
        int errors = 0;
        for (String pojoName : getPojoNames()) {
            try {
                new PojoClassTester(pojoName).test();
            } catch (Throwable ex) {
                ++errors;
                System.err.println(pojoName + ": " + ex.getMessage());
                ex.printStackTrace(System.err);
            }
        }
        return errors;
    }

    private ClassLoader getDependencies() throws IOException {
        List<URL> jars = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(dependencies), StandardCharsets.UTF_8));
        try {
            for(;;) {
                String line = reader.readLine();
                if(line==null) {
                    return new URLClassLoader(jars.toArray(new URL[jars.size()]), getClass().getClassLoader());
                }
                jars.add(new File(line).toURI().toURL());
            }
        }
        finally {
            reader.close();
        }
    }

    private List<String> getPojoNames() throws MalformedURLException {
        final List<String> collector = new ArrayList<>();
        new Reflections(new ConfigurationBuilder().setUrls(Collections.singletonList(classesDirectory.toURI().toURL()))
                .setScanners(new PojoScanner(collector)));
        return collector;
    }
}
