package org.jetbrains.kotlin.maven;

import com.intellij.openapi.util.io.FileUtil;
import kotlin.io.TextStreamsKt;
import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.*;

import static org.jetbrains.kotlin.maven.MavenTestUtils.getNotNullSystemProperty;

class MavenProject {
    @NotNull
    private final File workingDir;

    MavenProject(@NotNull String name) throws IOException {
        File originalProjectDir = new File("src/test/resources/" + name);
        workingDir = FileUtil.createTempDirectory("maven-test-" + name, null);
        File[] filesToCopy = originalProjectDir.listFiles();

        for (File from : filesToCopy) {
            File to = new File(workingDir, from.getName());
            FileUtil.copyFileOrDir(from, to);
        }
    }

    @NotNull
    File file(@NotNull String path) {
        return new File(workingDir, path);
    }

    MavenExecutionResult exec(String... targets) throws Exception {
        List<String> cmd = buildCmd(targets);
        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        setUpEnvVars(processBuilder.environment());

        processBuilder.directory(workingDir);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String stdout = TextStreamsKt.readText(reader);
        int exitCode = process.waitFor();

        return new MavenExecutionResult(stdout, workingDir, exitCode);
    }

    private void setUpEnvVars(Map<String, String> env) throws IOException {
        String mavenHome = getNotNullSystemProperty("maven.home");
        env.put("M2_HOME", mavenHome);
        String mavenPath = mavenHome + File.separator + "bin";
        env.put("PATH", env.get("PATH") + File.pathSeparator + mavenPath);
    }

    private List<String> buildCmd(String... args) {
        List<String> cmd = new ArrayList<String>();

        String osName = getNotNullSystemProperty("os.name");
        if (osName.contains("Windows")) {
            cmd.addAll(Arrays.asList("cmd", "/C"));
        }
        else {
            cmd.add("/bin/bash");
        }

        cmd.add("mvn");
        cmd.add("-Dkotlin.compiler.incremental.log.level=info");

        String kotlinVersionProperty = "kotlin.version";
        cmd.add("-D" + kotlinVersionProperty + "=" + getNotNullSystemProperty(kotlinVersionProperty));

        String mavenRepoLocalProperty = "maven.repo.local";
        String localRepoPath = System.getProperty(mavenRepoLocalProperty);
        try {
            if (localRepoPath != null && !StringsKt.isBlank(localRepoPath) && new File(localRepoPath).isDirectory()) {
                cmd.add("-D" + mavenRepoLocalProperty + "=" + localRepoPath);
            }
        }
        catch (SecurityException e) {
            e.printStackTrace();
        }

        cmd.addAll(Arrays.asList(args));

        return cmd;
    }
}

