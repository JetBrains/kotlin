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
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryCoordinates
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinResolvedBinaryDependency
import org.jetbrains.kotlin.gradle.idea.tcs.extras.sourcesClasspath
import org.jetbrains.kotlin.gradle.idea.tcs.isKotlinCompileBinaryType
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeAdditionalArtifactResolver
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
internal object IdeArtifactResolutionQuerySourcesResolver : IdeAdditionalArtifactResolver {

    override fun resolve(sourceSet: KotlinSourceSet, dependencies: Set<IdeaKotlinDependency>) {
        val binaryDependencies = dependencies.filterIsInstance<IdeaKotlinResolvedBinaryDependency>()
            .filter { dependency -> dependency.isKotlinCompileBinaryType }
            .groupBy { dependency -> Coordinates(dependency.coordinates ?: return@groupBy null) }

        val project = sourceSet.project
        val configuration = selectConfiguration(sourceSet)

        val resolutionResult = project.dependencies.createArtifactResolutionQuery()
            .forComponents(configuration.incoming.resolutionResult.allComponents.map { it.id })
            .withArtifacts(JvmLibrary::class.java, SourcesArtifact::class.java)
            .execute()

        val sourcesArtifacts = resolutionResult.resolvedComponents.flatMap { resolved ->
            resolved.getArtifacts(SourcesArtifact::class.java).filterIsInstance<ResolvedArtifactResult>()
        }

        sourcesArtifacts.forEach { artifact ->
            binaryDependencies[Coordinates(artifact)]?.forEach { dependency ->
                dependency.sourcesClasspath.add(artifact.file)
            }
        }
    }

    private fun selectConfiguration(sourceSet: KotlinSourceSet): Configuration {
        val platformCompilation = sourceSet.internal.compilations.singleOrNull { it.platformType != KotlinPlatformType.common }
        return platformCompilation?.internal?.configurations?.compileDependencyConfiguration
            ?: sourceSet.internal.resolvableMetadataConfiguration
    }

    /**
     * Specific 'Coordinates' type used to match previously resolved dependencies with their
     * sources and javadoc artifact counterparts
     */
    private data class Coordinates(private val coordinates: String)

    private fun Coordinates(coordinates: IdeaKotlinBinaryCoordinates): Coordinates? = when {
        coordinates.capabilities.isEmpty() -> Coordinates("${coordinates.group}:${coordinates.module}:${coordinates.version}")
        coordinates.capabilities.size == 1 -> coordinates.capabilities.single().run { Coordinates("$group:$name:$version") }

        /*
        We do have a dependency that declares multiple capabilities. In this case we cannot use this resolver
        to find the sources as we can only specify the componentId and not the explicit artifact
         */
        else -> null
    }

    private fun Coordinates(artifact: ResolvedArtifactResult): Coordinates? {
        val id = artifact.id.componentIdentifier as? ModuleComponentIdentifier ?: return null
        return Coordinates("${id.group}:${id.module}:${id.version}")
    }
}
