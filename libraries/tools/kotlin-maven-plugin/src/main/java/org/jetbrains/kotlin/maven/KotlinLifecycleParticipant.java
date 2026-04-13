/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

public class KotlinLifecycleParticipant extends AbstractMavenLifecycleParticipant implements LogEnabled {
    private static final String KOTLIN_MAVEN_PLUGIN_GROUP_ID = "org.jetbrains.kotlin";
    private static final String KOTLIN_MAVEN_PLUGIN_ARTIFACT_ID = "kotlin-maven-plugin";
    private static final String KOTLIN_STDLIB_ARTIFACT_ID = "kotlin-stdlib";
    private static final String SMART_DEFAULTS_ENABLED_PROPERTY = "kotlin.smart.defaults.enabled";
    private static final String COMPILE_GOAL = "compile";
    private static final String TEST_COMPILE_GOAL = "test-compile";

    private static final String MAVEN_COMPILER_PLUGIN_GROUP_ID = "org.apache.maven.plugins";
    private static final String MAVEN_COMPILER_PLUGIN_ARTIFACT_ID = "maven-compiler-plugin";

    /** Minimum supported Java version for Kotlin's jvmTarget. */
    private static final int MIN_SUPPORTED_JAVA_VERSION = 8;

    private Logger logger;

    @Override
    public void enableLogging(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void afterProjectsRead(MavenSession session) {
        for (MavenProject project : session.getProjects()) {
            Plugin kotlinMavenPlugin = getKotlinMavenPlugin(project);
            if (kotlinMavenPlugin != null && isExtensionsEnabled(kotlinMavenPlugin) && isSmartDefaultsEnabled(project)) {
                configureSmartDefaults(project, kotlinMavenPlugin);
            }
        }
    }

    private boolean isSmartDefaultsEnabled(MavenProject project) {
        // System property has priority
        String sysProp = System.getProperty(SMART_DEFAULTS_ENABLED_PROPERTY);
        if (sysProp != null) {
            return Boolean.parseBoolean(sysProp);
        }

        // Then check project properties
        String projectProp = project.getProperties().getProperty(SMART_DEFAULTS_ENABLED_PROPERTY);
        if (projectProp != null) {
            return Boolean.parseBoolean(projectProp);
        }

        // Enabled by default (when extensions=true)
        return true;
    }

    private void configureSmartDefaults(MavenProject project, Plugin kotlinMavenPlugin) {
        if (logger != null) {
            logger.info("Kotlin smart defaults are enabled for " + project.getArtifactId());
        }

        addSourceRoots(project, kotlinMavenPlugin);
        addStdlibDependency(project, kotlinMavenPlugin.getVersion());
        configureJvmTarget(project, kotlinMavenPlugin);
    }

    // -------------------------------------------------------------------------
    // jvmTarget auto-alignment
    // -------------------------------------------------------------------------

    private void configureJvmTarget(MavenProject project, Plugin kotlinMavenPlugin) {
        if (isJvmTargetOrJdkReleaseAlreadyConfigured(project, kotlinMavenPlugin)) {
            return;
        }

        JvmTargetResolution resolution = resolveJvmTarget(project);
        if (resolution == null) {
            return;
        }

        applyJvmTargetToProject(project, resolution.jvmTarget, resolution.useRelease);
        if (logger != null) {
            logger.info("Using jvmTarget=" + resolution.jvmTarget + " (derived from " + resolution.derivedFrom + ")");
        }
    }

    static boolean isJvmTargetOrJdkReleaseAlreadyConfigured(MavenProject project, Plugin kotlinMavenPlugin) {
        // Check project-level property
        // Equivalent to -Dkotlin.compiler.[jvmTarget/jdkRelease] or <kotlin.compiler.[jvmTarget/jdkRelease]>
        Properties projectProperties = project.getProperties();
        if (projectProperties.containsKey("kotlin.compiler.jvmTarget")
                || projectProperties.containsKey("kotlin.compiler.jdkRelease")) {
            return true;
        }

        // Check global plugin configuration
        if (configContains(kotlinMavenPlugin.getConfiguration(), "jvmTarget")
                || configContains(kotlinMavenPlugin.getConfiguration(), "jdkRelease")) {
            return true;
        }

        // Check per-execution configurations
        for (PluginExecution execution : kotlinMavenPlugin.getExecutions()) {
            if (configContains(execution.getConfiguration(), "jvmTarget")
                    || configContains(execution.getConfiguration(), "jdkRelease")) {
                return true;
            }
        }

        return false;
    }

    private static boolean configContains(Object configuration, String tagName) {
        if (configuration instanceof Xpp3Dom) {
            Xpp3Dom dom = (Xpp3Dom) configuration;
            return dom.getChild(tagName) != null;
        }
        return false;
    }

    /**
     * Resolves the jvmTarget value using the priority chain:
     * <ol>
     *   <li>{@code maven.compiler.release} (maven-compiler-plugin config or property)</li>
     *   <li>{@code maven.compiler.target}</li>
     *   <li>JDK version requirement from {@code maven-toolchains-plugin} configuration</li>
     * </ol>
     *
     * <p>Package-private for testing.
     *
     * @return resolution result, or {@code null} if jvmTarget cannot be derived
     */
    JvmTargetResolution resolveJvmTarget(MavenProject project) {
        Properties projectProperties = project.getProperties();
        Xpp3Dom compilerConfig = getMavenCompilerPluginConfig(project);

        // First priority: maven.compiler.release
        String release = getPluginConfigOrProperty(compilerConfig, "release", projectProperties, "maven.compiler.release");
        if (release != null) {
            String jvmTarget = normalizeToKotlinJvmTarget(release);
            if (jvmTarget != null) {
                return new JvmTargetResolution(jvmTarget, true, "maven.compiler.release=" + release);
            } else {
                if (logger != null) {
                    logger.warn("maven.compiler.release=" + release + " is not supported as a Kotlin jvmTarget. Skipping auto-detection.");
                }
                return null;
            }
        }

        // Priority 2: maven.compiler.target
        String target = getPluginConfigOrProperty(compilerConfig, "target", projectProperties, "maven.compiler.target");
        if (target != null) {
            String jvmTarget = normalizeToKotlinJvmTarget(target);
            if (jvmTarget != null) {
                return new JvmTargetResolution(jvmTarget, false, "maven.compiler.target=" + jvmTarget);
            } else {
                if (logger != null) {
                    logger.warn("maven.compiler.target=" + target + " is not supported as a Kotlin jvmTarget. Skipping auto-detection.");
                }
                return null;
            }
        }

        return null;
    }

    /**
     * Returns the value of configuration key {@code configKey} from the given plugin configuration (if present),
     * or the value of the given project property (if present).
     */
    static String getPluginConfigOrProperty(
            Xpp3Dom compilerPluginConfig, String configKey, Properties projectProperties,
            String propName
    ) {
        if (compilerPluginConfig != null) {
            Xpp3Dom child = compilerPluginConfig.getChild(configKey);
            if (child != null && child.getValue() != null && !child.getValue().trim().isEmpty()) {
                return child.getValue().trim();
            }
        }

        String propValue = projectProperties.getProperty(propName);
        if (propValue != null && !propValue.trim().isEmpty()) {
            return propValue.trim();
        }

        return null;
    }

    /**
     * Converts a Java version string to the Kotlin jvmTarget descriptor.
     *
     * <ul>
     *   <li>{@code "8"} or {@code "1.8"} → {@code "1.8"}</li>
     *   <li>{@code "9"}…{@code "26"} → the same string</li>
     *   <li>{@code "6"}/{@code "1.6"} or {@code "7"}/{@code "1.7"} → {@code null} (unsupported)</li>
     * </ul>
     */
    static String normalizeToKotlinJvmTarget(String javaVersion) {
        if (javaVersion == null) return null;
        String v = javaVersion.trim();
        switch (v) {
            case "1.6":
            case "6":
            case "1.7":
            case "7":
                return null; // Not supported
            case "1.8":
            case "8":
                return "1.8";
            default:
                try {
                    int version = Integer.parseInt(v);
                    if (version > MIN_SUPPORTED_JAVA_VERSION) {
                        return v;
                    }
                } catch (NumberFormatException ignored) {
                    // Not an integer, ignore
                }
                return null;
        }
    }

    /**
     * Retrieves the global {@code <configuration>} of the {@code maven-compiler-plugin} for the
     * given project, or {@code null} if the plugin is not present or has no configuration.
     */
    private static Xpp3Dom getMavenCompilerPluginConfig(MavenProject project) {
        for (Plugin plugin : project.getBuildPlugins()) {
            if (MAVEN_COMPILER_PLUGIN_GROUP_ID.equals(plugin.getGroupId())
                    && MAVEN_COMPILER_PLUGIN_ARTIFACT_ID.equals(plugin.getArtifactId())) {
                Object config = plugin.getConfiguration();
                if (config instanceof Xpp3Dom) {
                    return (Xpp3Dom) config;
                }
            }
        }
        return null;
    }


    private static void applyJvmTargetToProject(MavenProject project, String jvmTarget, boolean useRelease) {
        project.getProperties().setProperty("kotlin.compiler.jvmTarget", jvmTarget);
        if (useRelease) {
            project.getProperties().setProperty("kotlin.compiler.jdkRelease", jvmTarget);
        }
    }

    static class JvmTargetResolution {
        /** The resolved Kotlin jvmTarget string (e.g. {@code "1.8"}, {@code "17"}). */
        final String jvmTarget;
        /** Whether {@code -Xjdk-release} should be added (true when derived from {@code --release}). */
        final boolean useRelease;
        /** Human-readable description of the resolution source, used in the info log line. */
        final String derivedFrom;

        JvmTargetResolution(String jvmTarget, boolean useRelease, String derivedFrom) {
            this.jvmTarget = jvmTarget;
            this.useRelease = useRelease;
            this.derivedFrom = derivedFrom;
        }
    }

    // -------------------------------------------------------------------------
    // Source roots
    // -------------------------------------------------------------------------

    private void addSourceRoots(MavenProject project, Plugin kotlinMavenPlugin) {
        File baseDir = project.getBasedir();

        if (!hasUserDefinedSourceDirs(kotlinMavenPlugin, COMPILE_GOAL) && !hasMavenBuildSourceDirectoryOverride(project)) {
            File mainKotlinSource = new File(baseDir, "src/main/kotlin");
            if (mainKotlinSource.exists()) {
                project.addCompileSourceRoot(mainKotlinSource.getAbsolutePath());
            }
        }

        if (!hasUserDefinedSourceDirs(kotlinMavenPlugin, TEST_COMPILE_GOAL) && !hasMavenBuildTestSourceDirectoryOverride(project)) {
            File testKotlinSource = new File(baseDir, "src/test/kotlin");
            if (testKotlinSource.exists()) {
                project.addTestCompileSourceRoot(testKotlinSource.getAbsolutePath());
            }
        }
    }

    static boolean hasMavenBuildSourceDirectoryOverride(MavenProject project) {
        String sourceDir = project.getBuild().getSourceDirectory();
        if (sourceDir == null) return false;

        Path configured = Paths.get(sourceDir);
        if (!configured.isAbsolute()) {
            configured = Paths.get(project.getBasedir().getPath()).resolve(configured);
        }

        Path expected = Paths.get(project.getBasedir().getPath(), "src", "main", "java");

        return !configured.normalize().equals(expected.normalize());
    }

    static boolean hasMavenBuildTestSourceDirectoryOverride(MavenProject project) {
        String testSourceDir = project.getBuild().getTestSourceDirectory();
        if (testSourceDir == null) return false;

        Path configured = Paths.get(testSourceDir);
        if (!configured.isAbsolute()) {
            configured = Paths.get(project.getBasedir().getPath()).resolve(configured);
        }

        Path expected = Paths.get(project.getBasedir().getPath(), "src", "test", "java");

        return !configured.normalize().equals(expected.normalize());
    }

    private boolean hasUserDefinedSourceDirs(Plugin plugin, String goal) {
        if (hasNonEmptySourceDirs(plugin.getConfiguration())) {
            return true;
        }

        for (PluginExecution execution : plugin.getExecutions()) {
            if (execution.getGoals() != null && execution.getGoals().contains(goal) && hasNonEmptySourceDirs(execution.getConfiguration())) {
                return true;
            }
        }

        return false;
    }

    private boolean hasNonEmptySourceDirs(Object configuration) {
        if (configuration instanceof Xpp3Dom) {
            Xpp3Dom configDom = (Xpp3Dom) configuration;
            Xpp3Dom sourceDirs = configDom.getChild("sourceDirs");
            return sourceDirs != null && sourceDirs.getChildCount() > 0;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // stdlib dependency
    // -------------------------------------------------------------------------

    private void addStdlibDependency(MavenProject project, String version) {
        if (hasStdlibDependency(project)) {
            return;
        }

        Dependency stdlib = new Dependency();
        stdlib.setGroupId(KOTLIN_MAVEN_PLUGIN_GROUP_ID);
        stdlib.setArtifactId(KOTLIN_STDLIB_ARTIFACT_ID);
        stdlib.setVersion(version);
        stdlib.setScope("compile");

        project.getDependencies().add(stdlib);
        if (logger != null) {
            logger.info("Added kotlin-stdlib dependency, version " + version);
        }
    }

    private boolean hasStdlibDependency(MavenProject project) {
        List<Dependency> dependencies = project.getDependencies();
        for (Dependency dependency : dependencies) {
            if (KOTLIN_MAVEN_PLUGIN_GROUP_ID.equals(dependency.getGroupId()) &&
                KOTLIN_STDLIB_ARTIFACT_ID.equals(dependency.getArtifactId())) {
                return true;
            }
        }
        return false;
    }

    private Plugin getKotlinMavenPlugin(MavenProject project) {
        for (Plugin plugin : project.getBuildPlugins()) {
            if (KOTLIN_MAVEN_PLUGIN_GROUP_ID.equals(plugin.getGroupId()) &&
                KOTLIN_MAVEN_PLUGIN_ARTIFACT_ID.equals(plugin.getArtifactId())) {
                return plugin;
            }
        }
        return null;
    }

    private boolean isExtensionsEnabled(Plugin plugin) {
        return Boolean.parseBoolean(String.valueOf(plugin.getExtensions()));
    }
}
