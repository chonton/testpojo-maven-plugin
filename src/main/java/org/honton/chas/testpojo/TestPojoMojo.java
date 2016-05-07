package org.honton.chas.testpojo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

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

    @Parameter(property = "project.build.directory", readonly = true)
    private String buildDirectory;

    @Parameter(property = "project.model.properties", readonly = true)
    private Map<String, String> properties;

    @Override
    public void execute() throws MojoExecutionException {
        String argLine = properties.get("argLine");
        if (argLine == null) {
            getLog().warn("No argLine specifying javaagent - not continuing");
            return;
        }
        argLine = replaceProperties(argLine);

        try {
            File jarFile = new File(buildDirectory, "testPojo.jar");
            new BuildExecJar(jarFile, Main.class.getCanonicalName())
                .buildJar(getClassPath());

            JavaProcess proc = new JavaProcess(getLog());
            proc.setJavaArgs(Arrays.asList(argLine, "-jar", jarFile.getAbsolutePath()));
            proc.setCmdArgs(Arrays.asList(outputDirectory, createDependencyFile()));

            int errors = proc.execute();
            if (errors > 0) {
                throw new MojoExecutionException(errors + " pojos had errors");
            }
        } catch (Exception ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

    private String createDependencyFile() throws IOException {
        File dependencyFile = new File(buildDirectory, "testPojo.dependencies");
        BufferedWriter dependencies = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dependencyFile), StandardCharsets.UTF_8));
        try {
            write(dependencies, runtimeScope);
            write(dependencies, compileScope);
        }
        finally {
            dependencies.close();
        }
        return dependencyFile.getAbsolutePath();
    }

    private static void write(BufferedWriter dependencies, List<String> dependencyScope) throws IOException {
        for(String dependency : dependencyScope) {
            dependencies.write(dependency);
            dependencies.newLine();
        }
    }

    private Set<String> getClassPath() throws URISyntaxException {
        Set<String> classPath = new HashSet<>();
        URLClassLoader ucl = (URLClassLoader) getClass().getClassLoader();
        for(URL url : ucl.getURLs()) {
            classPath.add(url.toURI().getPath());
        }
        return classPath;
    }

    private final static Pattern ARG_PATTERN = Pattern.compile("@\\{([^}]+)\\}");

    private String replaceProperties(String argLine) {
        StringBuilder sb = new StringBuilder();
        int start = 0;
        for (Matcher m = ARG_PATTERN.matcher(argLine); m.find();) {
            String value = properties.get(m.group(1));
            if (value != null) {
                sb.append(argLine.substring(start, m.start()));
                sb.append(value);
                start = m.end();
            }
        }
        if (start != 0) {
            return sb.append(argLine.substring(start)).toString();
        }
        return argLine;
    }
}
