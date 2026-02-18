/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class KotlinLifecycleParticipantTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private KotlinLifecycleParticipant participant;

    @Before
    public void setUp() {
        participant = new KotlinLifecycleParticipant();
    }

    @Test
    public void testSmartDefaultsEnabledWithExtensions() throws Exception {
        File baseDir = tempFolder.newFolder("project");
        new File(baseDir, "src/main/kotlin").mkdirs();

        MavenProject project = createProject(baseDir, true, "2.0.0", new ArrayList<Dependency>());
        MavenSession session = createSession(project);

        participant.afterProjectsRead(session);

        assertTrue("src/main/kotlin should be added as compile source root",
                project.getCompileSourceRoots().contains(new File(baseDir, "src/main/kotlin").getAbsolutePath()));

        boolean hasStdlib = false;
        for (Dependency dep : project.getDependencies()) {
            if ("org.jetbrains.kotlin".equals(dep.getGroupId()) && "kotlin-stdlib".equals(dep.getArtifactId())) {
                hasStdlib = true;
                Assert.assertEquals("2.0.0", dep.getVersion());
                break;
            }
        }
        assertTrue("kotlin-stdlib should be added", hasStdlib);
    }

    @Test
    public void testSmartDefaultsDisabledWithoutExtensions() throws Exception {
        File baseDir = tempFolder.newFolder("project-no-ext");
        new File(baseDir, "src/main/kotlin").mkdirs();

        MavenProject project = createProject(baseDir, false, "2.0.0", new ArrayList<Dependency>());
        MavenSession session = createSession(project);

        int initialSourceRootsCount = project.getCompileSourceRoots().size();
        int initialDependenciesCount = project.getDependencies().size();

        participant.afterProjectsRead(session);

        Assert.assertEquals("Source roots should not change when extensions=false",
                initialSourceRootsCount, project.getCompileSourceRoots().size());
        Assert.assertEquals("Dependencies should not change when extensions=false",
                initialDependenciesCount, project.getDependencies().size());
    }

    @Test
    public void testStdlibNotAddedWhenAlreadyExists() throws Exception {
        File baseDir = tempFolder.newFolder("project-stdlib-exists");
        new File(baseDir, "src/main/kotlin").mkdirs();

        List<Dependency> dependencies = new ArrayList<>();
        Dependency existingStdlib = new Dependency();
        existingStdlib.setGroupId("org.jetbrains.kotlin");
        existingStdlib.setArtifactId("kotlin-stdlib");
        existingStdlib.setVersion("1.9.0");
        dependencies.add(existingStdlib);

        MavenProject project = createProject(baseDir, true, "2.0.0", dependencies);
        MavenSession session = createSession(project);

        participant.afterProjectsRead(session);

        int stdlibCount = 0;
        for (Dependency dep : project.getDependencies()) {
            if ("org.jetbrains.kotlin".equals(dep.getGroupId()) && "kotlin-stdlib".equals(dep.getArtifactId())) {
                stdlibCount++;
                Assert.assertEquals("Existing stdlib version should be preserved", "1.9.0", dep.getVersion());
            }
        }
        assertEquals("There should be only one kotlin-stdlib dependency", 1, stdlibCount);
    }

    @Test
    public void testSourceRootsAddedOnlyIfDirectoryExists() throws Exception {
        File baseDir = tempFolder.newFolder("project-no-kotlin-dir");
        // Don't create src/main/kotlin directory

        MavenProject project = createProject(baseDir, true, "2.0.0", new ArrayList<Dependency>());
        MavenSession session = createSession(project);

        int initialSourceRootsCount = project.getCompileSourceRoots().size();

        participant.afterProjectsRead(session);

        Assert.assertEquals("Source roots should not change when kotlin directory doesn't exist",
                initialSourceRootsCount, project.getCompileSourceRoots().size());
    }

    @Test
    public void testTestSourceRootsAdded() throws Exception {
        File baseDir = tempFolder.newFolder("project-with-tests");
        new File(baseDir, "src/main/kotlin").mkdirs();
        new File(baseDir, "src/test/kotlin").mkdirs();

        MavenProject project = createProject(baseDir, true, "2.0.0", new ArrayList<Dependency>());
        MavenSession session = createSession(project);

        participant.afterProjectsRead(session);

        assertTrue("src/main/kotlin should be added as compile source root",
                project.getCompileSourceRoots().contains(new File(baseDir, "src/main/kotlin").getAbsolutePath()));
        assertTrue("src/test/kotlin should be added as test compile source root",
                project.getTestCompileSourceRoots().contains(new File(baseDir, "src/test/kotlin").getAbsolutePath()));
    }

    @Test
    public void testNoKotlinPluginDoesNothing() throws Exception {
        File baseDir = tempFolder.newFolder("project-no-plugin");
        new File(baseDir, "src/main/kotlin").mkdirs();

        MavenProject project = new MavenProject();
        project.setFile(new File(baseDir, "pom.xml"));
        project.setBuild(new Build());
        project.setDependencies(new ArrayList<Dependency>());

        MavenSession session = createSession(project);

        int initialSourceRootsCount = project.getCompileSourceRoots().size();
        int initialDependenciesCount = project.getDependencies().size();

        participant.afterProjectsRead(session);

        Assert.assertEquals("Source roots should not change when no Kotlin plugin",
                initialSourceRootsCount, project.getCompileSourceRoots().size());
        Assert.assertEquals("Dependencies should not change when no Kotlin plugin",
                initialDependenciesCount, project.getDependencies().size());
    }

    @Test
    public void testSourceRootsNotAddedWhenUserSpecifiesSourceDirs() throws Exception {
        File baseDir = tempFolder.newFolder("project-custom-sources");
        new File(baseDir, "src/main/kotlin").mkdirs();

        MavenProject project = createProjectWithCustomSourceDirs(baseDir, true, "2.0.0", new ArrayList<Dependency>());
        MavenSession session = createSession(project);

        int initialSourceRootsCount = project.getCompileSourceRoots().size();

        participant.afterProjectsRead(session);

        Assert.assertEquals("Source roots should not be added when user specifies custom sourceDirs",
                initialSourceRootsCount, project.getCompileSourceRoots().size());
        assertFalse("src/main/kotlin should NOT be added when user specifies custom sourceDirs",
                project.getCompileSourceRoots().contains(new File(baseDir, "src/main/kotlin").getAbsolutePath()));
    }

    @Test
    public void testStdlibStillAddedWhenUserSpecifiesSourceDirs() throws Exception {
        File baseDir = tempFolder.newFolder("project-custom-sources-stdlib");

        MavenProject project = createProjectWithCustomSourceDirs(baseDir, true, "2.0.0", new ArrayList<Dependency>());
        MavenSession session = createSession(project);

        participant.afterProjectsRead(session);

        boolean hasStdlib = false;
        for (Dependency dep : project.getDependencies()) {
            if ("org.jetbrains.kotlin".equals(dep.getGroupId()) && "kotlin-stdlib".equals(dep.getArtifactId())) {
                hasStdlib = true;
                Assert.assertEquals("2.0.0", dep.getVersion());
                break;
            }
        }
        assertTrue("kotlin-stdlib should still be added even when user specifies custom sourceDirs", hasStdlib);
    }

    @Test
    public void testSmartDefaultsDisabledViaProjectProperty() throws Exception {
        File baseDir = tempFolder.newFolder("project-smart-defaults-disabled");
        new File(baseDir, "src/main/kotlin").mkdirs();

        MavenProject project = createProject(baseDir, true, "2.0.0", new ArrayList<Dependency>());
        // Disable smart defaults via project property
        project.getProperties().setProperty("kotlin.smart.defaults.enabled", "false");
        MavenSession session = createSession(project);

        int initialSourceRootsCount = project.getCompileSourceRoots().size();
        int initialDependenciesCount = project.getDependencies().size();

        participant.afterProjectsRead(session);

        Assert.assertEquals("Source roots should not change when smart defaults disabled",
                initialSourceRootsCount, project.getCompileSourceRoots().size());
        Assert.assertEquals("Dependencies should not change when smart defaults disabled",
                initialDependenciesCount, project.getDependencies().size());
    }

    @Test
    public void testSmartDefaultsDisabledViaSystemProperty() throws Exception {
        File baseDir = tempFolder.newFolder("project-smart-defaults-disabled-sys");
        new File(baseDir, "src/main/kotlin").mkdirs();

        MavenProject project = createProject(baseDir, true, "2.0.0", new ArrayList<Dependency>());
        MavenSession session = createSession(project);

        int initialSourceRootsCount = project.getCompileSourceRoots().size();
        int initialDependenciesCount = project.getDependencies().size();

        // Disable smart defaults via system property
        String oldValue = System.getProperty("kotlin.smart.defaults.enabled");
        try {
            System.setProperty("kotlin.smart.defaults.enabled", "false");
            participant.afterProjectsRead(session);
        } finally {
            if (oldValue != null) {
                System.setProperty("kotlin.smart.defaults.enabled", oldValue);
            } else {
                System.clearProperty("kotlin.smart.defaults.enabled");
            }
        }

        Assert.assertEquals("Source roots should not change when smart defaults disabled via system property",
                initialSourceRootsCount, project.getCompileSourceRoots().size());
        Assert.assertEquals("Dependencies should not change when smart defaults disabled via system property",
                initialDependenciesCount, project.getDependencies().size());
    }

    private MavenProject createProject(File baseDir, boolean extensionsEnabled, String kotlinVersion, List<Dependency> dependencies) {
        return createProject(baseDir, extensionsEnabled, kotlinVersion, dependencies, null);
    }

    private MavenProject createProjectWithCustomSourceDirs(File baseDir, boolean extensionsEnabled, String kotlinVersion, List<Dependency> dependencies) {
        Xpp3Dom configuration = new Xpp3Dom("configuration");
        Xpp3Dom sourceDirs = new Xpp3Dom("sourceDirs");
        Xpp3Dom sourceDir = new Xpp3Dom("sourceDir");
        sourceDir.setValue("custom/src");
        sourceDirs.addChild(sourceDir);
        configuration.addChild(sourceDirs);
        return createProject(baseDir, extensionsEnabled, kotlinVersion, dependencies, configuration);
    }

    private MavenProject createProject(File baseDir, boolean extensionsEnabled, String kotlinVersion, List<Dependency> dependencies, Xpp3Dom configuration) {
        MavenProject project = new MavenProject();
        project.setFile(new File(baseDir, "pom.xml"));
        project.setArtifactId("test-project");

        Build build = new Build();
        Plugin kotlinPlugin = new Plugin();
        kotlinPlugin.setGroupId("org.jetbrains.kotlin");
        kotlinPlugin.setArtifactId("kotlin-maven-plugin");
        kotlinPlugin.setVersion(kotlinVersion);
        kotlinPlugin.setExtensions(extensionsEnabled);
        if (configuration != null) {
            kotlinPlugin.setConfiguration(configuration);
        }

        List<Plugin> plugins = new ArrayList<>();
        plugins.add(kotlinPlugin);
        build.setPlugins(plugins);
        project.setBuild(build);

        project.setDependencies(dependencies);

        return project;
    }

    private MavenSession createSession(MavenProject project) {
        return new StubMavenSession(project);
    }

    /**
     * Stub implementation of MavenSession for testing purposes.
     */
    private static class StubMavenSession extends MavenSession {
        private List<MavenProject> projects;

        public StubMavenSession(MavenProject project) {
            super(null, null, null, null, null, null, null, null, null, null);
            this.projects = Collections.singletonList(project);
        }

        @Override
        public List<MavenProject> getProjects() {
            return projects;
        }

        @Override
        public void setProjects(List<MavenProject> projects) {
            this.projects = projects;
        }
    }
}
