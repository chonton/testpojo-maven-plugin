package org.honton.chas.testpojo;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
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
            File jarFileLocation = new File(buildDirectory, "testPojo.jar");
            Set<String> dependencies = new HashSet<>();
            dependencies.addAll(runtimeScope);
            dependencies.addAll(compileScope);
            addMyDependencies(dependencies);
            
            new BuildExecJar(argLine, jarFileLocation, Main.class.getCanonicalName())
                .buildJar(dependencies);

            JavaProcess proc = new JavaProcess(getLog());
            proc.setJavaArgs(Arrays.asList(argLine, "-jar", jarFileLocation.getAbsolutePath()));
            proc.setCmdArgs(Arrays.asList(outputDirectory));

            int errors = proc.execute();
            if (errors > 0) {
                throw new MojoExecutionException(errors + " pojos had errors");
            }
        } catch (ExecutionException | IOException | TimeoutException | URISyntaxException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

    private void addMyDependencies(Set<String> dependencies) throws URISyntaxException {
        URLClassLoader ucl = (URLClassLoader) getClass().getClassLoader();
        for(URL url : ucl.getURLs()) {
            dependencies.add(url.toURI().getPath());
        }
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
