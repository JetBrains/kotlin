/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.util.List;

public class KotlinLifecycleParticipant extends AbstractMavenLifecycleParticipant implements LogEnabled {
    private static final String KOTLIN_MAVEN_PLUGIN_GROUP_ID = "org.jetbrains.kotlin";
    private static final String KOTLIN_MAVEN_PLUGIN_ARTIFACT_ID = "kotlin-maven-plugin";
    private static final String KOTLIN_STDLIB_ARTIFACT_ID = "kotlin-stdlib";
    private static final String SMART_DEFAULTS_ENABLED_PROPERTY = "kotlin.smart.defaults.enabled";

    private Logger logger;

    @Override
    public void enableLogging(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        for (MavenProject project : session.getProjects()) {
            Plugin plugin = getKotlinMavenPlugin(project);
            if (plugin != null && isExtensionsEnabled(plugin) && isSmartDefaultsEnabled(project)) {
                configureSmartDefaults(project, plugin);
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

    private void configureSmartDefaults(MavenProject project, Plugin plugin) {
        if (logger != null) {
            logger.info("Kotlin smart defaults are enabled for " + project.getArtifactId());
        }

        addSourceRoots(project, plugin);
        addStdlibDependency(project, plugin.getVersion());
    }

    private void addSourceRoots(MavenProject project, Plugin plugin) {
        if (hasUserDefinedSourceDirs(plugin)) {
            return;
        }

        File baseDir = project.getBasedir();
        
        File mainKotlinSource = new File(baseDir, "src/main/kotlin");
        if (mainKotlinSource.exists()) {
            project.addCompileSourceRoot(mainKotlinSource.getAbsolutePath());
        }

        File testKotlinSource = new File(baseDir, "src/test/kotlin");
        if (testKotlinSource.exists()) {
            project.addTestCompileSourceRoot(testKotlinSource.getAbsolutePath());
        }
    }

    private boolean hasUserDefinedSourceDirs(Plugin plugin) {
        Object configuration = plugin.getConfiguration();
        if (configuration instanceof Xpp3Dom) {
            Xpp3Dom configDom = (Xpp3Dom) configuration;
            Xpp3Dom sourceDirs = configDom.getChild("sourceDirs");
            return sourceDirs != null && sourceDirs.getChildCount() > 0;
        }
        return false;
    }

    private void addStdlibDependency(MavenProject project, String version) {
        if (hasStdlibDependency(project)) {
            return;
        }

        Dependency stdlib = new Dependency();
        stdlib.setGroupId(KOTLIN_MAVEN_PLUGIN_GROUP_ID);
        stdlib.setArtifactId(KOTLIN_STDLIB_ARTIFACT_ID);
        stdlib.setVersion(version);

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
