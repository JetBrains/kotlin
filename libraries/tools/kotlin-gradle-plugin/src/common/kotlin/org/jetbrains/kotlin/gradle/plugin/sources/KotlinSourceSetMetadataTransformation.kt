/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.GranularMetadataTransformation
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution
import org.jetbrains.kotlin.gradle.plugin.mpp.ModuleIds
import org.jetbrains.kotlin.gradle.plugin.mpp.projectDependency
import org.jetbrains.kotlin.gradle.targets.metadata.dependsOnClosureWithInterCompilationDependencies
import org.jetbrains.kotlin.tooling.core.extrasNullableLazyProperty

/**
 * Returns [GranularMetadataTransformation] for all requested compile dependencies
 * scopes: API, IMPLEMENTATION, COMPILE_ONLY; See [KotlinDependencyScope.compileScopes]
 *
 * Used only for IDE import (w/o KGP based dependency resolution).
 * Scheduled for removal after 1.9.20
 */
internal val InternalKotlinSourceSet.metadataTransformation: GranularMetadataTransformation? by extrasNullableLazyProperty lazy@{
    // Create only for source sets in multiplatform plugin
    project.multiplatformExtensionOrNull ?: return@lazy null

    val parentSourceSetVisibilityProvider = ParentSourceSetVisibilityProvider { componentIdentifier ->
        dependsOnClosureWithInterCompilationDependencies(this).filterIsInstance<DefaultKotlinSourceSet>()
            .mapNotNull { it.metadataTransformation }
            .flatMap { it.visibleSourceSetsByComponentId[componentIdentifier].orEmpty() }
            .toSet()
    }

    val granularMetadataTransformation = GranularMetadataTransformation(
        params = GranularMetadataTransformation.Params(project, this),
        parentSourceSetVisibilityProvider = parentSourceSetVisibilityProvider
    )

    @Suppress("DEPRECATION")
    /*
    Older IDEs still rely on resolving the metadata configurations explicitly.
    Dependencies will be coming from extending the newer 'resolvableMetadataConfiguration'.

    the intransitiveMetadataConfigurationName will not extend this mechanism, since it only
    relies on dependencies being added explicitly by the Kotlin Gradle Plugin
    */
    listOf(
        apiMetadataConfigurationName,
        implementationMetadataConfigurationName,
        compileOnlyMetadataConfigurationName
    ).forEach { configurationName ->
        val configuration = project.configurations.getByName(configurationName)
        project.applyTransformationToLegacyDependenciesMetadataConfiguration(configuration, granularMetadataTransformation)
    }

    granularMetadataTransformation
}

/**
 *
 * This method is only intended to be called on deprecated DependenciesMetadata configurations to ensure
 * correct behaviour in import.
 *
 * KGP based dependency resolution is therefore unaffected.
 *
 * Ensure that the [configuration] excludes the dependencies that are classified by this [GranularMetadataTransformation] as
 * [MetadataDependencyResolution.Exclude], and uses exactly the same versions as were resolved for the requested
 * dependencies during the transformation.
 */
private fun Project.applyTransformationToLegacyDependenciesMetadataConfiguration(
    configuration: Configuration, transformation: GranularMetadataTransformation
) {
    // Run this action immediately before the configuration first takes part in dependency resolution:
    configuration.withDependencies {
        val (unrequested, requested) = transformation.metadataDependencyResolutions
            .partition { it is MetadataDependencyResolution.Exclude }

        unrequested.forEach {
            val (group, name) = it.projectDependency(project)?.run {
                /** Note: the project dependency notation here should be exactly this, group:name,
                 * not from [ModuleIds.fromProjectPathDependency], as `exclude` checks it against the project's group:name  */
                ModuleDependencyIdentifier(group.toString(), name)
            } ?: ModuleIds.fromComponent(project, it.dependency)
            configuration.exclude(mapOf("group" to group, "module" to name))
        }

        requested.filter { it.dependency.currentBuildProjectIdOrNull == null }.forEach {
            val (group, name) = ModuleIds.fromComponent(project, it.dependency)
            val notation = listOfNotNull(group.orEmpty(), name, it.dependency.moduleVersion?.version).joinToString(":")
            configuration.resolutionStrategy.force(notation)
        }
    }
}
