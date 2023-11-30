/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedVariantResult
import org.jetbrains.kotlin.gradle.dsl.metadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.idea.tcs.*
import org.jetbrains.kotlin.gradle.idea.tcs.extras.sourcesClasspath
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.ide.IdeAdditionalArtifactResolver
import org.jetbrains.kotlin.gradle.plugin.ide.IdeaKotlinBinaryCapability
import org.jetbrains.kotlin.gradle.plugin.mpp.configureSourcesPublicationAttributes
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.jetbrains.kotlin.gradle.plugin.sources.disambiguateName
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.utils.createResolvable
import org.jetbrains.kotlin.gradle.utils.detachedResolvable

internal object IdeSourcesVariantsResolver : IdeAdditionalArtifactResolver {
    override fun resolve(sourceSet: KotlinSourceSet, dependencies: Set<IdeaKotlinDependency>) {
        val project = sourceSet.project
        val metadataTarget = project.multiplatformExtensionOrNull?.metadataTarget

        val binaryDependenciesByCoordinates = dependencies
            .filterIsInstance<IdeaKotlinResolvedBinaryDependency>()
            .filter { dependency -> dependency.isKotlinCompileBinaryType }
            .groupBy { Coordinates(it.coordinates) }

        val platformCompilation = sourceSet.internal
            .compilations
            .singleOrNull { it.platformType != KotlinPlatformType.common }


        // Shared source sets and platform source sets has different configurations for compile dependencies
        val configuration: Configuration
        val target: KotlinTarget
        if (platformCompilation == null) {
            target = metadataTarget ?: return // source set configured incorrectly, can't resolve artifact for that
            configuration = sourceSet.internal.resolvableMetadataConfiguration
        } else {
            target = platformCompilation.target
            configuration = platformCompilation.internal.configurations.compileDependencyConfiguration
        }

        val sourcesConfig = project.configurations.detachedResolvable()
        sourcesConfig.apply {
            configureSourcesPublicationAttributes(target)
            extendsFrom(configuration)
        }

        sourcesConfig.incoming.artifactView { it.isLenient = true }.artifacts.forEach { artifactDependency ->
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