/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ArtifactResolutionResult
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.component.Artifact
import org.gradle.jvm.JvmLibrary
import org.gradle.language.base.artifact.SourcesArtifact
import org.gradle.language.java.artifact.JavadocArtifact
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinResolvedBinaryDependency
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver
import org.jetbrains.kotlin.gradle.plugin.ide.IdeaKotlinBinaryCoordinates
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.plugin.sources.project

internal object IdeSlowSourcesAndDocumentationResolver : IdeDependencyResolver {
    override fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> {
        val project = sourceSet.project
        val configuration = sourceSet.internal.resolvableMetadataConfiguration
        val resolutionResult = project.dependencies.createArtifactResolutionQuery()
            .forComponents(configuration.incoming.resolutionResult.allComponents.map { it.id })
            .withArtifacts(JvmLibrary::class.java, SourcesArtifact::class.java, JavadocArtifact::class.java)
            .execute()

        return resolve(resolutionResult, SourcesArtifact::class.java, IdeaKotlinDependency.SOURCES_BINARY_TYPE) +
                resolve(resolutionResult, JavadocArtifact::class.java, IdeaKotlinDependency.DOCUMENTATION_BINARY_TYPE)
    }

    fun resolve(
        resolutionResult: ArtifactResolutionResult, artifactType: Class<out Artifact>, binaryType: String
    ): Set<IdeaKotlinResolvedBinaryDependency> {
        return resolutionResult.resolvedComponents.flatMap { resolved ->
            resolved.getArtifacts(artifactType)
                .filterIsInstance<ResolvedArtifactResult>()
                .mapNotNull { artifact ->
                    val id = artifact.id.componentIdentifier as? ModuleComponentIdentifier ?: return@mapNotNull null
                    IdeaKotlinResolvedBinaryDependency(
                        coordinates = IdeaKotlinBinaryCoordinates(id),
                        binaryType = binaryType,
                        binaryFile = artifact.file
                    )
                }
        }.toSet()
    }
}
