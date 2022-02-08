package org.jetbrains.kotlin;

import groovy.lang.Closure;
import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.tasks.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionGenerator extends DefaultTask {
    @OutputDirectory
    public File getVersionSourceDirectory() {
        return getProject().file("build/generated");
    }

    @OutputFile
    public File getVersionFile() {
        return getProject().file(getVersionSourceDirectory().getPath() + "/org/jetbrains/kotlin/konan/CompilerVersionGenerated.kt");
    }

    @Input
    public String getKonanVersion() {
        return getProject().getProperties().get("konanVersion").toString();
    }


    // TeamCity passes all configuration parameters into a build script as project properties.
    // Thus we can use them here instead of environment variables.
    @Optional
    @Input
    public String getBuildNumber() {
        Object property = getProject().findProperty("build.number");
        if (property == null) return null;

        return property.toString();
    }

    @Input
    public String getMeta() {
        Object konanMetaVersionProperty = getProject().getProperties().get("konanMetaVersion");
        if (konanMetaVersionProperty == null) {
            return "MetaVersion.DEV";
        }

        return "MetaVersion." + konanMetaVersionProperty.toString().toUpperCase();
    }

    private final static Pattern versionPattern = Pattern.compile(
            "^(\\d+)\\.(\\d+)(?:\\.(\\d+))?(?:-M(\\p{Digit}))?(?:-(\\p{Alpha}\\p{Alnum}*))?(?:-(\\d+))?$"
    );

    @TaskAction
    public void generateVersion() {
        Matcher matcher = versionPattern.matcher(getKonanVersion());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Cannot parse Kotlin/Native version: $konanVersion");
        }

        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));

        String maintenanceStr = matcher.group(3);
        int maintenance = maintenanceStr != null ? Integer.parseInt(maintenanceStr) : 0;

        String milestoneStr = matcher.group(4);
        int milestone = milestoneStr != null ? Integer.parseInt(milestoneStr) : -1;

        String buildNumber = getBuildNumber();
        getProject().getLogger().info("BUILD_NUMBER: " + getBuildNumber());
        int build = -1;
        if (buildNumber != null) {
            String[] buildNumberSplit = buildNumber.split("-");
            build = Integer.parseInt(buildNumberSplit[buildNumberSplit.length - 1]); // //7-dev-buildcount
        }

        try (PrintWriter printWriter = new PrintWriter(getVersionFile())) {
            printWriter.println(
                    "package org.jetbrains.kotlin.konan\n" +
                    "\n" +
                    "internal val currentCompilerVersion: CompilerVersion =\n" +
                    "    CompilerVersionImpl(\n" +
                            getMeta() + ", " + major + ", " + minor + ",\n" +
                            maintenance + ", " + milestone + ", "+ build + ")\n" +
                    "\n" +
                    "val CompilerVersion.Companion.CURRENT: CompilerVersion\n" +
                    "    get() = currentCompilerVersion"
            );
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}