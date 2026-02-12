package org.jetbrains.kotlin.maven;

import kotlin.io.TextStreamsKt;
import kotlin.text.StringsKt;
import org.apache.commons.io.file.PathUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.maven.plugin.test.MavenTestExecutionContext;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static org.jetbrains.kotlin.maven.MavenTestUtils.getNotNullSystemProperty;
import static org.jetbrains.kotlin.maven.plugin.test.MavenTestExecutionContextKt.createMavenTestExecutionContextFromEnvironment;
import static org.jetbrains.kotlin.maven.test.MavenSettingsXmlBuilderKt.checkOrWriteKotlinMavenTestSettingsXml;

class MavenProject {
    @NotNull
    private final File workingDir;

    @NotNull
    private final File mavenSettingsXml;

    @NotNull
    private final MavenTestExecutionContext mavenTestExecutionContext;

    public enum ExecutionStrategy {
        IN_PROCESS,
        DAEMON,
        ;
    }

    MavenProject(@NotNull String name) throws IOException {
        File originalProjectDir = new File("src/test/resources/" + name);
        workingDir = Files.createTempDirectory("maven-test-" + name).toFile();
        File[] filesToCopy = originalProjectDir.listFiles();

        for (File from : filesToCopy) {
            File to = new File(workingDir, from.getName());
            if (from.isDirectory()) {
                PathUtils.copyDirectory(from.toPath(), to.toPath());
            } else {
                PathUtils.copyFile(from.toURI().toURL(), to.toPath());
            }
        }

        mavenTestExecutionContext = createMavenTestExecutionContextFromEnvironment(
                // this argument is required but is not used later in the test, so I set it to a random value
                Paths.get(workingDir.getAbsolutePath(), "tmp")
        );
        mavenSettingsXml = new File(workingDir, "maven-settings.xml");
        checkOrWriteKotlinMavenTestSettingsXml(
                Paths.get(mavenSettingsXml.getAbsolutePath()),
                mavenTestExecutionContext.getKotlinBuildRepo()
        );
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
        try {
            String localRepoPath = mavenTestExecutionContext.getSharedMavenLocal().toAbsolutePath().toString();
            if (!StringsKt.isBlank(localRepoPath) && new File(localRepoPath).isDirectory()) {
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
