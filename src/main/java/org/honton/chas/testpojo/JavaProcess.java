package org.honton.chas.testpojo;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.apache.maven.plugin.logging.Log;

import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Data
@RequiredArgsConstructor
public class JavaProcess {

    private final Log log;

    private List<String> javaArgs;
    private List<String> cmdArgs;

    @Setter(AccessLevel.NONE)
    private ExecutorService executor;

    /**
     * Start the process and wait for response.
     * pump the stdout and stderr streams from launched process.
     */
    public int execute() throws ExecutionException, IOException, TimeoutException {
        // buildJar();
        executor = Executors.newFixedThreadPool(2);
        try {
            return startAndWait();
        } finally {
            executor.shutdown();
        }
    }

    private int startAndWait() throws ExecutionException, IOException, TimeoutException {
        final Process process = startProcess();
        try {
            closeOutputStream(process.getOutputStream());
            Future<IOException> stderrFuture = startStreamPump(process.getErrorStream(), (line) -> {
                synchronized (log) {
                    log.error(line);
                }
            });
            Future<IOException> stdoutFuture = startStreamPump(process.getInputStream(), (line) -> {
                synchronized (log) {
                    log.info(line);
                }
            });

            stopStreamPump(stderrFuture);
            stopStreamPump(stdoutFuture);
            return checkProcessExit(process);
        } catch (ExecutionException | TimeoutException e) {
            process.destroy();
            throw e;
        }
    }

    private int checkProcessExit(Process process) {
        try {
            return process.exitValue();
        } catch (IllegalThreadStateException e) {
            process.destroy();
            return 0;
        }
    }

    private void closeOutputStream(OutputStream outputStream) {
        try {
            outputStream.close();
        } catch (IOException e) {
            log.info("failed to close output stream: " + e.getMessage());
        }
    }

    private String getJava() {
        String javaHome = System.getProperty("java.home");
        return javaHome + File.separator + "bin" + File.separator + "java";
    }

    /**
     * Create the cmd line and start the java process
     * cmd line consists of:<ul>
     * <li>java binary</li>
     * <li>javaArgs*</li>
     * <li>cmdArgs*</li>
     * </ul>
     * 
     * @return
     * @throws IOException
     */
    private Process startProcess() throws IOException {

        List<String> allArgs = new ArrayList<>();
        allArgs.add(getJava());
        if(javaArgs!=null) {
            allArgs.addAll(javaArgs);
        }

        if(cmdArgs!=null) {
            allArgs.addAll(cmdArgs);
        }

        return Runtime.getRuntime().exec(allArgs.toArray(new String[allArgs.size()]));
    }

    private Future<IOException> startStreamPump(final InputStream errorStream, Consumer<String> logLine) {
        return executor.submit(new Callable<IOException>() {
            @Override
            public IOException call() {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));) {
                    for (;;) {
                        String line = reader.readLine();
                        if (line == null) {
                            break;
                        }
                        logLine.accept(line);
                    }
                    return null;
                } catch (IOException e) {
                    return e;
                }
            }
        });
    }

    private void stopStreamPump(Future<IOException> future) throws ExecutionException, TimeoutException, IOException {
        try {
            IOException e = future.get(20, TimeUnit.SECONDS);
            if (e != null) {
                throw e;
            }
        } catch (InterruptedException ignore) {
            Thread.currentThread().interrupt();
        }
    }

}
