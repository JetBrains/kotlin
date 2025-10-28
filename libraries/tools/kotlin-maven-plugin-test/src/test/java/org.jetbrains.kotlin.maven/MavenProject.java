package org.jetbrains.kotlin.maven;

import kotlin.io.TextStreamsKt;
import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.com.intellij.openapi.util.io.FileUtil;

import java.io.*;
import java.util.*;

import static org.jetbrains.kotlin.maven.MavenTestUtils.getNotNullSystemProperty;

class MavenProject {
    @NotNull
    private final File workingDir;

    private static final File mavenSettingsXml = new File("../../maven-settings.xml");

    public enum ExecutionStrategy {
        IN_PROCESS,
        DAEMON,
        ;
    }

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
        return exec(null, targets);
    }

    MavenExecutionResult exec(@Nullable ExecutionStrategy executionStrategy, String... targets) throws Exception {
        List<String> cmd = buildCmd(targets);
        if (executionStrategy != null) { // else use the default strategy
            boolean daemonEnabled;
            switch (executionStrategy) {
                case IN_PROCESS:
                    daemonEnabled = false;
                    break;
                case DAEMON:
                    daemonEnabled = true;
                    break;
                default: throw new IllegalArgumentException("Unknown execution strategy: " + executionStrategy);
            }
            cmd.add("-Dkotlin.compiler.daemon=" + daemonEnabled);
        }

        assert mavenSettingsXml.exists() : "Could not find Maven settings file: " + mavenSettingsXml.getAbsolutePath();
        cmd.add("--settings");
        cmd.add(mavenSettingsXml.getAbsolutePath());

        ProcessBuilder processBuilder = new ProcessBuilder(cmd);

        processBuilder.directory(workingDir);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String stdout = TextStreamsKt.readText(reader);
        int exitCode = process.waitFor();

        return new MavenExecutionResult(stdout, workingDir, exitCode);
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

        String mavenHome = getNotNullSystemProperty("maven.home");
        String mavenBin = mavenHome + File.separator + "bin/mvn";

        cmd.add(mavenBin);
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
