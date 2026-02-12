package org.jetbrains.kotlin.maven;

import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class MavenExecutionResult {
    @NotNull
    private final String stdout;

    @NotNull
    private final File workingDir;

    @NotNull
    private final Path workingDirPath;

    private int exitCode;

    MavenExecutionResult(
            @NotNull String output,
            @NotNull File workingDir,
            int exitCode
    ) {
        this.stdout = output;
        this.workingDir = workingDir;
        this.exitCode = exitCode;
        try {
            this.workingDirPath = workingDir.toPath().toRealPath().toAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        assertArtifactsAreDownloadedFromCacheRedirector();
    }

    private void assertArtifactsAreDownloadedFromCacheRedirector() {
        final String lines[] = stdout.split(System.lineSeparator());
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.startsWith("[INFO] Downloading from")) {
                if (!line.contains("cache-redirector.jetbrains.com")) {
                    throw new AssertionError("cache-redirector is not enabled: " + line);
                }
            }
        }
    }

    MavenExecutionResult check(@NotNull Action<MavenExecutionResult> fn) throws Exception {
        try {
            fn.run(this);
        }
        catch (Throwable t) {
            System.out.println(stdout);
            throw new RuntimeException(t);
        }
        return this;
    }

    MavenExecutionResult succeeded() throws Exception {
        return check(new Action<MavenExecutionResult>() {
            @Override
            public void run(MavenExecutionResult execResult) {
                Assert.assertEquals("Maven process was expected to succeeded", 0, exitCode);
            }
        });
    }

    MavenExecutionResult failed() throws Exception {
        return check(new Action<MavenExecutionResult>() {
            @Override
            public void run(MavenExecutionResult execResult) {
                Assert.assertNotEquals("Maven process was expected to fail", 0, exitCode);
            }
        });
    }

    MavenExecutionResult contains(@NotNull final String str) throws Exception {
        return check(new Action<MavenExecutionResult>() {
            @Override
            public void run(MavenExecutionResult execResult) {
                if (!stdout.contains(str)) {
                    throw new AssertionError("Maven output should contain '" + str + "'");
                }
            }
        });
    }

    MavenExecutionResult notContains(@NotNull final String str) throws Exception {
        return check(new Action<MavenExecutionResult>() {
            @Override
            public void run(MavenExecutionResult execResult) {
                if (stdout.contains(str)) {
                    throw new AssertionError("Maven output should not contain '" + str + "'");
                }
            }
        });
    }

    MavenExecutionResult compiledKotlin(@NotNull final String... expectedPaths) throws Exception {
        return check(new Action<MavenExecutionResult>() {
            @Override
            public void run(MavenExecutionResult execResult) {
                Pattern kotlinCompileIteration = Pattern.compile("(?m)compile iteration: (.*)$");
                Matcher m = kotlinCompileIteration.matcher(stdout);

                Set<String> normalizedActualPaths = new HashSet<String>();
                while (m.find()) {
                    String[] compiledFiles = m.group(1).split(",");
                    for (String path : compiledFiles) {
                        if (StringsKt.isBlank(path)) continue;

                        File file = new File(path.trim());
                        Path relativePath = workingDirPath.relativize(file.toPath());
                        normalizedActualPaths.add(relativePath.normalize().toString());
                    }
                }
                String[] actualPaths = normalizedActualPaths.toArray(new String[normalizedActualPaths.size()]);
                Arrays.sort(actualPaths);

                for (int i = 0; i < expectedPaths.length; i++) {
                    expectedPaths[i] = Paths.get(expectedPaths[i]).normalize().toString();
                }
                Arrays.sort(expectedPaths);

                String expected = String.join("\n", expectedPaths);
                String actual = String.join("\n", actualPaths);
                Assert.assertEquals("Compiled files differ", expected, actual);
            }
        });
    }

    MavenExecutionResult filesExist(@NotNull final String... paths) throws Exception {
        return check(new Action<MavenExecutionResult>() {
            @Override
            public void run(MavenExecutionResult execResult) {
                for (String path : paths) {
                    File file = new File(workingDir, path);
                    Assert.assertTrue(file + " does not exist", file.exists());
                }
            }
        });
    }
}
