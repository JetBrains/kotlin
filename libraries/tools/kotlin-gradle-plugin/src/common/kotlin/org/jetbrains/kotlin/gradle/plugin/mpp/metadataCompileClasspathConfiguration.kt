/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.dsl.awaitMetadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.hierarchy.orNull
import org.jetbrains.kotlin.gradle.plugin.sources.*
import org.jetbrains.kotlin.gradle.utils.*

/**
 * @see resolvableMetadataConfiguration
 */
internal val InternalKotlinSourceSet.resolvableMetadataConfigurationName: String
    get() = disambiguateName(lowerCamelCaseName("resolvable", METADATA_CONFIGURATION_NAME_SUFFIX))

/**
 * Represents a 'resolvable' configuration containing all dependencies in compile scope.
 * These dependencies are set up to resolve Kotlin Metadata (without transformation) and will resolve
 * consistently across the whole project.
 */
internal val InternalKotlinSourceSet.resolvableMetadataConfiguration: Configuration by extrasStoredProperty {
    assert(resolvableMetadataConfigurationName !in project.configurations.names)
    val configuration = project.configurations
        .maybeCreateResolvable(resolvableMetadataConfigurationName)
        .configureMetadataDependenciesAttribute(project)

    addDependsOnClosureConfigurationsTo(configuration)

    configuration
}

/**
 * Extends the given [configuration] from the configurations defined in the [withDependsOnClosure] of the source set.
 *
 * @param configuration The configuration to be extended.
 */
private fun InternalKotlinSourceSet.addDependsOnClosureConfigurationsTo(configuration: Configuration) {
    withDependsOnClosure.forAll { sourceSet ->
        val extenders = sourceSet.internal.compileDependenciesConfigurations
        configuration.extendsFrom(*extenders.toTypedArray())
    }

    // Extend compile-related configurations from associated compilations
    project.launch {
        val platformCompilations = internal.awaitPlatformCompilations()
        val visibleSourceSets = getVisibleSourceSetsFromAssociateCompilations(platformCompilations)
        for (visibleSourceSet in visibleSourceSets) {
            val compileDependenciesConfigurations = visibleSourceSet.internal.compileDependenciesConfigurations
            configuration.extendsFrom(*compileDependenciesConfigurations.toTypedArray())
        }
    }
}

private val InternalKotlinSourceSet.compileDependenciesConfigurations: List<Configuration>
    get() = listOf(
        project.configurations.getByName(apiConfigurationName),
        project.configurations.getByName(implementationConfigurationName),
        project.configurations.getByName(compileOnlyConfigurationName),
    )

internal fun Configuration.configureMetadataDependenciesAttribute(project: Project): Configuration = apply {
    if (project.multiplatformExtensionOrNull != null) {
        project.launch {
            usesPlatformOf(project.multiplatformExtension.awaitMetadataTarget())
        }
    }
    attributes.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_METADATA))
    attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
}

/**
 * Ensure a consistent dependencies resolution result between common source sets and actual
 * See [ResolvableMetadataConfigurationTest] for the cases where dependencies should resolve consistently
 */
internal val SetupConsistentMetadataDependenciesResolution = KotlinProjectSetupCoroutine {
    KotlinPluginLifecycle.Stage.AfterFinaliseRefinesEdges.await()

    val sourceSets = multiplatformExtension.awaitSourceSets()
    val sourceSetsBySourceSetTree = mutableMapOf<KotlinSourceSetTree?, MutableSet<KotlinSourceSet>>()
    for (sourceSet in sourceSets) {
        val trees = sourceSet.internal.compilations.map { KotlinSourceSetTree.orNull(it) }
        trees.forEach { tree -> sourceSetsBySourceSetTree.getOrPut(tree) { mutableSetOf() }.add(sourceSet) }
    }

    for ((sourceSetTree, sourceSetsOfTree) in sourceSetsBySourceSetTree) {
        val configurationName = when (sourceSetTree) {
            null -> continue // for unknown trees there should be no relation between source sets, so just skip
            KotlinSourceSetTree.main -> "allSourceSetsCompileDependenciesMetadata"
            else -> lowerCamelCaseName("all", sourceSetTree.name, "SourceSetsCompileDependenciesMetadata")
        }

        configureConsistentDependencyResolution(sourceSetsOfTree, configurationName)
    }
}

private fun Project.configureConsistentDependencyResolution(groupOfSourceSets: Collection<KotlinSourceSet>, configurationName: String) {
    if (groupOfSourceSets.isEmpty()) return
    val configuration = configurations.createResolvable(configurationName)
    configuration.configureMetadataDependenciesAttribute(project)
    val allVisibleSourceSets = groupOfSourceSets + groupOfSourceSets.flatMap { getVisibleSourceSetsFromAssociateCompilations(it) }
    val extenders = allVisibleSourceSets.flatMap { it.internal.compileDependenciesConfigurations }
    configuration.extendsFrom(*extenders.toTypedArray())
    groupOfSourceSets.forEach { it.internal.resolvableMetadataConfiguration.shouldResolveConsistentlyWith(configuration) }

    // FIXME: KT-66375 Make actual compilation classpaths/libraries configurations to have the same consistent dependencies
}