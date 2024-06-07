/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedVariantResult
import org.jetbrains.kotlin.gradle.idea.tcs.*
import org.jetbrains.kotlin.gradle.idea.tcs.extras.sourcesClasspath
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeAdditionalArtifactResolver
import org.jetbrains.kotlin.gradle.plugin.ide.IdeaKotlinBinaryCapability
import org.jetbrains.kotlin.gradle.plugin.sources.internal

internal object IdeSourcesVariantsResolver : IdeAdditionalArtifactResolver {
    override fun resolve(sourceSet: KotlinSourceSet, dependencies: Set<IdeaKotlinDependency>) {
        val project = sourceSet.project

        val binaryDependenciesByCoordinates = dependencies
            .filterIsInstance<IdeaKotlinResolvedBinaryDependency>()
            .filter { dependency -> dependency.isKotlinCompileBinaryType }
            .groupBy { Coordinates(it.coordinates) }

        val dependencySourceConfiguration = project.configurations
            .findByName(sourceSet.internal.dependencySourcesConfigurationName) ?: return

        dependencySourceConfiguration.incoming.artifactView { it.isLenient = true }.artifacts.forEach { artifactDependency ->
            val coordinates = Coordinates(artifactDependency.variant)
            val binaryDependencies = binaryDependenciesByCoordinates[coordinates] ?: return@forEach
            binaryDependencies.forEach { dependency ->
                dependency.sourcesClasspath.add(artifactDependency.file)
            }
        }
    }
}

private data class Coordinates(
    val group: String,
    val module: String,
    val version: String?,
    val capabilities: Set<IdeaKotlinBinaryCapability>
)

private fun Coordinates(coordinates: IdeaKotlinBinaryCoordinates?): Coordinates? {
    if (coordinates == null) return null
    return Coordinates(
        group = coordinates.group,
        module = coordinates.module,
        version = coordinates.version,
        capabilities = coordinates.capabilities
    )
}

private fun Coordinates(variant: ResolvedVariantResult): Coordinates? {
    val id = (variant.owner as? ModuleComponentIdentifier) ?: return null
    return Coordinates(
        group = id.group,
        module = id.module,
        version = id.version,
        capabilities = variant.capabilities.map(::IdeaKotlinBinaryCapability).toSet()
    )
}