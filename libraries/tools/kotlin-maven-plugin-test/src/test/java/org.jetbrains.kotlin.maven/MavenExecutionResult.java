package org.jetbrains.kotlin.maven;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class MavenExecutionResult {
    @NotNull
    private final String stdout;

    @NotNull
    private final File workingDir;

    private int exitCode;

    MavenExecutionResult(
            @NotNull String output,
            @NotNull File workingDir,
            int exitCode
    ) {
        this.stdout = output;
        this.workingDir = workingDir;
        this.exitCode = exitCode;
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
                Pattern kotlinCompileIteration = Pattern.compile("(?m)Kotlin compile iteration: (.*)$");
                Matcher m = kotlinCompileIteration.matcher(stdout);

                Set<String> normalizedActualPaths = new HashSet<String>();
                while (m.find()) {
                    String[] compiledFiles = m.group(1).split(",");
                    for (String path : compiledFiles) {
                        File file = new File(path.trim());
                        String relativePath = FileUtil.getRelativePath(workingDir, file);
                        normalizedActualPaths.add(FileUtil.normalize(relativePath));
                    }
                }
                String[] actualPaths = normalizedActualPaths.toArray(new String[normalizedActualPaths.size()]);
                Arrays.sort(actualPaths);

                for (int i = 0; i < expectedPaths.length; i++) {
                    expectedPaths[i] = FileUtil.normalize(expectedPaths[i]);
                }
                Arrays.sort(expectedPaths);

                String expected = StringUtil.join(expectedPaths, "\n");
                String actual = StringUtil.join(actualPaths, "\n");
                Assert.assertEquals("Compiled files differ", expected, actual);
            }
        });
    }
}
