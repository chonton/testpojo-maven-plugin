package org.honton.chas.testpojo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BuildExecJar {
    private final File jarLocation;
    private final String mainClass;

    public final void buildJar(Set<String> classPath) throws IOException {
 
        Manifest manifest = new Manifest();
        Attributes mainAttributes = manifest.getMainAttributes();
        mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mainAttributes.put(Attributes.Name.MAIN_CLASS, mainClass);
        mainAttributes.put(Attributes.Name.CLASS_PATH,  createClassPath(classPath));
        new JarOutputStream(new FileOutputStream(jarLocation), manifest).close();
    }

    private String createClassPath(Set<String> jars) {
        StringBuilder cp = new StringBuilder();
        for(String dependency : jars) {
            cp.append(dependency);
            if(!dependency.endsWith(".jar")) {
                cp.append('/');
            }
            cp.append(' ');
        }
        return cp.toString();
    }
}
