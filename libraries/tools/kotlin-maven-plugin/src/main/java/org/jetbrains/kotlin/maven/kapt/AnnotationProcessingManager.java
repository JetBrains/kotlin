/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.maven.kapt;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.compiler.DependencyCoordinate;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class AnnotationProcessingManager {
    private final ArtifactHandlerManager artifactHandlerManager;
    private final MavenSession session;
    private final MavenProject project;
    private final RepositorySystem repositorySystem;
    private final ResolutionErrorHandler resolutionErrorHandler;

    AnnotationProcessingManager(@NotNull ArtifactHandlerManager artifactHandlerManager,
                                @NotNull MavenSession session,
                                @NotNull MavenProject project,
                                @NotNull RepositorySystem repositorySystem,
                                @NotNull ResolutionErrorHandler resolutionErrorHandler) {
        this.artifactHandlerManager = artifactHandlerManager;
        this.session = session;
        this.project = project;
        this.repositorySystem = repositorySystem;
        this.resolutionErrorHandler = resolutionErrorHandler;
    }

    private final static DependencyCoordinate KAPT_DEPENDENCY = new DependencyCoordinate();

    public final static String COMPILE_SOURCE_SET_NAME = "compile";
    public final static String TEST_SOURCE_SET_NAME = "test";

    static {
        KAPT_DEPENDENCY.setGroupId("org.jetbrains.kotlin");
        KAPT_DEPENDENCY.setArtifactId("kotlin-annotation-processing");
        try {
            KAPT_DEPENDENCY.setVersion(getMavenPluginVersion());
        }
        catch (MojoExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    public static File getGeneratedSourcesDirectory(@NotNull MavenProject project, @NotNull String sourceSetName) {
        return new File(getTargetDirectory(project), "generated-sources/kapt/" + sourceSetName);
    }

    @NotNull
    public static File getGeneratedKotlinSourcesDirectory(@NotNull MavenProject project, @NotNull String sourceSetName) {
        return new File(getTargetDirectory(project), "generated-sources/kaptKotlin/" + sourceSetName);
    }

    @NotNull
    static File getStubsDirectory(@NotNull MavenProject project, @NotNull String sourceSetName) {
        return new File(getTargetDirectory(project), "kaptStubs/" + sourceSetName);
    }

    @NotNull
    static File getGeneratedClassesDirectory(@NotNull MavenProject project, @NotNull String sourceSetName) {
        switch (sourceSetName) {
            case COMPILE_SOURCE_SET_NAME:
                return new File(project.getModel().getBuild().getOutputDirectory());
            case TEST_SOURCE_SET_NAME:
                return new File(project.getModel().getBuild().getTestOutputDirectory());
            default:
                throw new IllegalArgumentException("'" + COMPILE_SOURCE_SET_NAME + "' or '" +
                        TEST_SOURCE_SET_NAME + "' expected");
        }
    }

    @NotNull
    private static File getTargetDirectory(@NotNull MavenProject project) {
        return new File(project.getModel().getBuild().getDirectory());
    }

    @NotNull
    ResolvedArtifacts resolveAnnotationProcessors(@Nullable List<DependencyCoordinate> aptDependencies) throws Exception {
        if (aptDependencies == null) {
            aptDependencies = Collections.emptyList();
        }

        Set<Artifact> requiredArtifacts = new LinkedHashSet<>();

        requiredArtifacts.add(getArtifact(artifactHandlerManager, KAPT_DEPENDENCY));

        for (DependencyCoordinate dependency : aptDependencies) {
            requiredArtifacts.add(getArtifact(artifactHandlerManager, dependency));
        }

        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
                .setArtifact(requiredArtifacts.iterator().next())
                .setResolveRoot(true)
                .setResolveTransitively(true)
                .setArtifactDependencies(requiredArtifacts)
                .setLocalRepository(session.getLocalRepository())
                .setRemoteRepositories(project.getRemoteArtifactRepositories());

        ArtifactResolutionResult resolutionResult = repositorySystem.resolve(request);
        resolutionErrorHandler.throwErrors(request, resolutionResult);

        if (resolutionResult == null || resolutionResult.getArtifacts() == null) {
            throw new IllegalStateException("Annotation processing artifacts were not resolved.");
        }

        List<String> apClasspath = new ArrayList<>(resolutionResult.getArtifacts().size());
        String kaptCompilerPluginArtifact = null;

        for (Artifact artifact : resolutionResult.getArtifacts()) {
            if (artifact == null) {
                continue;
            }

            if (artifact.getGroupId().equals(KAPT_DEPENDENCY.getGroupId())
                    && artifact.getArtifactId().equals(KAPT_DEPENDENCY.getArtifactId())) {
                kaptCompilerPluginArtifact = artifact.getFile().getAbsolutePath();
            }
            else {
                apClasspath.add(artifact.getFile().getAbsolutePath());
            }
        }

        if (kaptCompilerPluginArtifact == null) {
            throw new IllegalStateException("Kapt compiler plugin artifact was not resolved.");
        }

        return new ResolvedArtifacts(apClasspath, kaptCompilerPluginArtifact);
    }

    @NotNull
    private Artifact getArtifact(
            @NotNull ArtifactHandlerManager artifactHandlerManager,
            @NotNull DependencyCoordinate dependency) throws Exception {
        ArtifactHandler handler = artifactHandlerManager.getArtifactHandler(dependency.getType());

        return new DefaultArtifact(
                dependency.getGroupId(),
                dependency.getArtifactId(),
                VersionRange.createFromVersionSpec(evaluateVersion(dependency)),
                Artifact.SCOPE_RUNTIME,
                dependency.getType(),
                dependency.getClassifier(),
                handler,
                false);
    }

    private String evaluateVersion(@NotNull DependencyCoordinate dependency) {
        String version = dependency.getVersion();
        if (version == null) {
            Optional<Dependency> sameButParentDependency = project.getDependencies().stream()
                    .filter(dep -> dep.getGroupId().equals(dependency.getGroupId())
                            && dep.getArtifactId().equals(dependency.getArtifactId())
                            && dep.getVersion() != null
                    ).findFirst();
            if (sameButParentDependency.isPresent()) {
                version = sameButParentDependency.get().getVersion();
            }
        }
        return version;
    }

    @NotNull
    private static String getMavenPluginVersion() throws MojoExecutionException {
        ClassLoader classLoader = AnnotationProcessingManager.class.getClassLoader();
        InputStream pomPropertiesIs = classLoader.getResourceAsStream(
                "META-INF/maven/org.jetbrains.kotlin/kotlin-maven-plugin/pom.properties");
        if (pomPropertiesIs == null) {
            throw new MojoExecutionException("Can't resolve the version of kotlin-maven-plugin");
        }

        Properties properties = new Properties();
        try {
            properties.load(pomPropertiesIs);
        }
        catch (IOException e) {
            throw new MojoExecutionException("Error while reading kotlin-maven-plugin/pom.properties", e);
        }

        return properties.getProperty("version");
    }

    class ResolvedArtifacts {
        @NotNull
        final List<String> annotationProcessingClasspath;

        @NotNull
        final String kaptCompilerPluginArtifact;

        ResolvedArtifacts(@NotNull List<String> annotationProcessingClasspath,
                          @NotNull String kaptCompilerPluginArtifact) {
            this.annotationProcessingClasspath = annotationProcessingClasspath;
            this.kaptCompilerPluginArtifact = kaptCompilerPluginArtifact;
        }
    }
}
