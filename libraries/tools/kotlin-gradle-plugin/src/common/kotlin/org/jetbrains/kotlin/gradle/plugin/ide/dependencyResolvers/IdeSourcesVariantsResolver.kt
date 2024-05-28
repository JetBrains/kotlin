/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedVariantResult
import org.jetbrains.kotlin.gradle.dsl.metadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.idea.tcs.*
import org.jetbrains.kotlin.gradle.idea.tcs.extras.sourcesClasspath
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeAdditionalArtifactResolver
import org.jetbrains.kotlin.gradle.plugin.ide.IdeaKotlinBinaryCapability
import org.jetbrains.kotlin.gradle.plugin.mpp.configureSourcesPublicationAttributes
import org.jetbrains.kotlin.gradle.plugin.sources.internal

internal object IdeSourcesVariantsResolver : IdeAdditionalArtifactResolver {
    override fun resolve(sourceSet: KotlinSourceSet, dependencies: Set<IdeaKotlinDependency>) {
        val project = sourceSet.project
        val multiplatformExtension = project.multiplatformExtensionOrNull ?: return // resolve sources only for KMP projects

        val sourceSetTarget = sourceSet.internal
            .compilations
            .map { it.target }
            .toSet()
            .singleOrNull() ?: multiplatformExtension.metadataTarget

        val binaryDependenciesByCoordinates = dependencies
            .filterIsInstance<IdeaKotlinResolvedBinaryDependency>()
            .filter { dependency -> dependency.isKotlinCompileBinaryType }
            .groupBy { Coordinates(it.coordinates) }

        val gradleDependencies = binaryDependenciesByCoordinates
            .keys
            .mapNotNull { coordinates -> coordinates?.toGradleDependency(project) }

        val dependencySourceConfiguration = project.configurations
            .detachedConfiguration(*gradleDependencies.toTypedArray())
            .apply { configureSourcesPublicationAttributes(sourceSetTarget) }

        dependencySourceConfiguration.incoming
            // not every dependency can be resolved into sources, and it is OK for IDE import
            .artifactView { it.isLenient = true }
            .artifacts
            .forEach { artifactDependency ->
                // now match originally requested dependencies with resolved sources artifacts
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
) {
    val gradleDependencyNotationString
        get() = buildString {
            append("$group:$module")
            if (version != null) append(":$version")
        }
}

private fun Coordinates.toGradleDependency(project: Project): Dependency {
    val dependency = project.dependencies.create(gradleDependencyNotationString)
    dependency as ExternalModuleDependency // this is safe since we create the dependency from "string" notation.
    if (capabilities.isNotEmpty()) {
        dependency.capabilities { handler ->
            capabilities.forEach {
                handler.requireCapability(it.gradleDependencyNotationString)
            }
        }
    }

    return dependency
}

private val IdeaKotlinBinaryCapability.gradleDependencyNotationString
    get() = buildString {
        append("$group:$name")
        if (version != null) append(":$version")
    }

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