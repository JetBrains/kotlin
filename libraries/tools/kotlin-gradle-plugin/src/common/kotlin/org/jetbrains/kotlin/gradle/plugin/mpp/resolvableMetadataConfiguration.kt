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
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.categoryByName
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.copyAttributes
import org.jetbrains.kotlin.gradle.plugin.sources.*
import org.jetbrains.kotlin.gradle.plugin.sources.InternalKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.disambiguateName
import org.jetbrains.kotlin.gradle.utils.markResolvable
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.plugin.usesPlatformOf
import org.jetbrains.kotlin.gradle.utils.getOrCreate
import org.jetbrains.kotlin.gradle.utils.listProperty
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.tooling.core.extrasLazyProperty

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
internal val InternalKotlinSourceSet.resolvableMetadataConfiguration: Configuration by extrasLazyProperty(
    "resolvableMetadataConfiguration"
) {
    assert(resolvableMetadataConfigurationName !in project.configurations.names)
    val configuration = project.configurations.maybeCreate(resolvableMetadataConfigurationName)
    configuration.markResolvable()

    withDependsOnClosure.forAll { sourceSet ->
        configuration.extendsFrom(project.configurations.getByName(sourceSet.apiConfigurationName))
        configuration.extendsFrom(project.configurations.getByName(sourceSet.implementationConfigurationName))
        configuration.extendsFrom(project.configurations.getByName(sourceSet.compileOnlyConfigurationName))
    }

    /**
     * Adding dependencies from associate compilations using a listProvider, since we would like to defer
     * the call to 'getVisibleSourceSetsFromAssociateCompilations' as much as possible (changes to the model might significantly
     * change the result of this visible source sets)
     */
    configuration.dependencies.addAllLater(project.listProvider {
        getVisibleSourceSetsFromAssociateCompilations(this).flatMap { sourceSet ->
            project.configurations.getByName(sourceSet.apiConfigurationName).allDependencies +
                    project.configurations.getByName(sourceSet.implementationConfigurationName).allDependencies +
                    project.configurations.getByName(sourceSet.compileOnlyConfigurationName).allDependencies
        }
    })

    val allCompileMetadataConfiguration = project.allCompileMetadataConfiguration

    /* Ensure consistent dependency resolution result within the whole module */
    configuration.shouldResolveConsistentlyWith(allCompileMetadataConfiguration)
    copyAttributes(allCompileMetadataConfiguration.attributes, configuration.attributes)

    configuration
}

/**
 * Configuration containing all compile dependencies from *all* source sets.
 * This configuration is used to provide a dependency 'consistency scope' for
 * the [InternalKotlinSourceSet.resolvableMetadataConfiguration]
 */
private val Project.allCompileMetadataConfiguration
    get(): Configuration = configurations.getOrCreate("allSourceSetsCompileDependenciesMetadata", invokeWhenCreated = { configuration ->
        configuration.markResolvable()
        configuration.usesPlatformOf(multiplatformExtension.metadata())
        configuration.attributes.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_METADATA))
        configuration.attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))

        kotlinExtension.sourceSets.all { sourceSet ->
            configuration.extendsFrom(configurations.getByName(sourceSet.apiConfigurationName))
            configuration.extendsFrom(configurations.getByName(sourceSet.implementationConfigurationName))
            configuration.extendsFrom(configurations.getByName(sourceSet.compileOnlyConfigurationName))
        }
    })

private inline fun <reified T> Project.listProvider(noinline provider: () -> List<T>): Provider<List<T>> {
    return project.objects.listProperty<T>().apply {
        set(project.provider(provider))
    }
}
