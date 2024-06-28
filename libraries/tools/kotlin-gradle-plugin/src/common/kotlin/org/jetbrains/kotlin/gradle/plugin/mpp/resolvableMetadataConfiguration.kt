/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.categoryByName
import org.jetbrains.kotlin.gradle.plugin.hierarchy.orNull
import org.jetbrains.kotlin.gradle.plugin.sources.*
import org.jetbrains.kotlin.gradle.plugin.sources.InternalKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.disambiguateName
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.gradle.utils.listProperty
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

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

    withDependsOnClosure.forAll { sourceSet ->
        val extenders = sourceSet.internal.compileDependenciesConfigurations
        configuration.extendsFrom(*extenders.toTypedArray())
    }

    /**
     * Adding dependencies from associate compilations using a listProvider, since we would like to defer
     * the call to 'getVisibleSourceSetsFromAssociateCompilations' as much as possible (changes to the model might significantly
     * change the result of this visible source sets)
     */
    configuration.dependencies.addAllLater(project.listProvider {
        getVisibleSourceSetsFromAssociateCompilations(this).flatMap { sourceSet ->
            sourceSet.internal.compileDependenciesConfigurations.flatMap { it.allDependencies }
        }
    })

    // needed for old IDEs
    configureLegacyMetadataDependenciesConfigurations(configuration)

    configuration
}

private val InternalKotlinSourceSet.compileDependenciesConfigurations: List<Configuration>
    get() = listOf(
        project.configurations.getByName(apiConfigurationName),
        project.configurations.getByName(implementationConfigurationName),
        project.configurations.getByName(compileOnlyConfigurationName),
    )

/**
Older IDEs still rely on resolving the metadata configurations explicitly.
Dependencies will be coming from extending the newer 'resolvableMetadataConfiguration'.

the intransitiveMetadataConfigurationName will not extend this mechanism, since it only
relies on dependencies being added explicitly by the Kotlin Gradle Plugin
 */
private fun InternalKotlinSourceSet.configureLegacyMetadataDependenciesConfigurations(resolvableMetadataConfiguration: Configuration) {
    @Suppress("DEPRECATION")
    listOf(
        apiMetadataConfigurationName,
        implementationMetadataConfigurationName,
        compileOnlyMetadataConfigurationName
    ).forEach { configurationName ->
        val configuration = project.configurations.getByName(configurationName)
        configuration.extendsFrom(resolvableMetadataConfiguration)
        configuration.shouldResolveConsistentlyWith(resolvableMetadataConfiguration)
    }
}

private fun Configuration.configureMetadataDependenciesAttribute(project: Project): Configuration = apply {
    usesPlatformOf(project.multiplatformExtension.metadata())
    attributes.setAttribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_METADATA))
    attributes.setAttribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
}

private inline fun <reified T> Project.listProvider(noinline provider: () -> List<T>): Provider<List<T>> {
    return project.objects.listProperty<T>().apply {
        set(project.provider(provider))
    }
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
        val configurationName = when(sourceSetTree) {
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