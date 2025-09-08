/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.compiler.DependencyCoordinate;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Set;

@Named
class KotlinArtifactResolver {
    private final RepositorySystem repoSystem;
    private final ArtifactHandlerManager artifactHandlerManager;
    private final MavenSession session;
    private final MavenProject project;
    private final ResolutionErrorHandler resolutionErrorHandler;

    @Inject
    KotlinArtifactResolver(
            RepositorySystem repoSystem,
            ArtifactHandlerManager artifactHandlerManager,
            MavenSession session,
            MavenProject project,
            ResolutionErrorHandler resolutionErrorHandler
    ) {
        this.repoSystem = repoSystem;
        this.artifactHandlerManager = artifactHandlerManager;
        this.session = session;
        this.project = project;
        this.resolutionErrorHandler = resolutionErrorHandler;
    }

    Set<Artifact> resolveArtifact(String group, String artifactId, String version) throws ArtifactResolutionException {
        DependencyCoordinate dependency = new DependencyCoordinate();
        dependency.setGroupId(group);
        dependency.setArtifactId(artifactId);
        dependency.setVersion(version);
        ArtifactHandler handler = artifactHandlerManager.getArtifactHandler(dependency.getType());
        DefaultArtifact
                artifact = new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), Artifact.SCOPE_RUNTIME, dependency.getType(), dependency.getClassifier(), handler);
        ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setArtifact(artifact)
                .setResolveRoot(true)
                .setResolveTransitively(true)
                .setLocalRepository(session.getLocalRepository())
                .setRemoteRepositories(project.getRemoteArtifactRepositories());
        ArtifactResolutionResult resolutionResult = repoSystem.resolve(request);
        resolutionErrorHandler.throwErrors(request, resolutionResult);
        return resolutionResult.getArtifacts();
    }
}
