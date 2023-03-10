/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.jvm.JvmLibrary
import org.gradle.language.base.artifact.SourcesArtifact
import org.gradle.language.java.artifact.JavadocArtifact
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinResolvedBinaryDependency
import org.jetbrains.kotlin.gradle.idea.tcs.extras.documentationClasspath
import org.jetbrains.kotlin.gradle.idea.tcs.extras.sourcesClasspath
import org.jetbrains.kotlin.gradle.idea.tcs.isKotlinCompileBinaryType
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeAdditionalArtifactResolver
import org.jetbrains.kotlin.gradle.plugin.ide.IdeaKotlinBinaryCoordinates
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.jetbrains.kotlin.gradle.plugin.sources.internal

/**
 * Resolved sources.jar and javadoc.jar files using Gradle's artifact resolution query.
 * ⚠️: This resolution method is slow and shall be replaced by ArtifactViews. However,
 * before 1.8.20 Kotlin MPP did not publish sources as variants which requires us to keep this resolver
 * for compatibility with libraries published prior to 1.8.20
 *
 * cc Anton Lakotka, Sebastian Sellmair
 */
internal object IdeArtifactResolutionQuerySourcesAndDocumentationResolver : IdeAdditionalArtifactResolver {
    override fun resolve(sourceSet: KotlinSourceSet, dependencies: Set<IdeaKotlinDependency>) {
        val binaryDependencies = dependencies.filterIsInstance<IdeaKotlinResolvedBinaryDependency>()
            .filter { dependency -> dependency.isKotlinCompileBinaryType }
            .groupBy { dependency -> dependency.coordinates?.copy(sourceSetName = null) }

        val project = sourceSet.project
        val configuration = selectConfiguration(sourceSet)
        val resolutionResult = project.dependencies.createArtifactResolutionQuery()
            .forComponents(configuration.incoming.resolutionResult.allComponents.map { it.id })
            .withArtifacts(JvmLibrary::class.java, SourcesArtifact::class.java, JavadocArtifact::class.java)
            .execute()

        val sourcesArtifacts = resolutionResult.resolvedComponents.flatMap { resolved ->
            resolved.getArtifacts(SourcesArtifact::class.java).filterIsInstance<ResolvedArtifactResult>()
        }

        sourcesArtifacts.forEach { artifact ->
            val artifactId = artifact.id.componentIdentifier as? ModuleComponentIdentifier ?: return@forEach
            val artifactCoordinates = IdeaKotlinBinaryCoordinates(artifactId)
            binaryDependencies[artifactCoordinates]?.forEach { dependency ->
                dependency.sourcesClasspath.add(artifact.file)
            }
        }

        val javadocArtifacts = resolutionResult.resolvedComponents.flatMap { resolved ->
            resolved.getArtifacts(JavadocArtifact::class.java).filterIsInstance<ResolvedArtifactResult>()
        }

        javadocArtifacts.forEach { artifact ->
            val artifactId = artifact.id.componentIdentifier as? ModuleComponentIdentifier ?: return@forEach
            val artifactCoordinates = IdeaKotlinBinaryCoordinates(artifactId)
            binaryDependencies[artifactCoordinates]?.forEach { dependency ->
                dependency.documentationClasspath.add(artifact.file)
            }
        }
    }

    private fun selectConfiguration(sourceSet: KotlinSourceSet): Configuration {
        val platformCompilation = sourceSet.internal.compilations.singleOrNull { it.platformType != KotlinPlatformType.common }
        return platformCompilation?.internal?.configurations?.compileDependencyConfiguration
            ?: sourceSet.internal.resolvableMetadataConfiguration
    }
}
