/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.sourcesJarTask
import org.jetbrains.kotlin.gradle.utils.addExtendsFromRelation
import org.jetbrains.kotlin.project.model.refinesClosure

abstract class AbstractKotlinGradleVariantFactory<T : KotlinGradleVariant>(
    module: KotlinGradleModule
) : AbstractKotlinGradleFragmentFactory<T>(module) {

    override fun create(name: String): T = super.create(name).also { fragment ->
        createSourcesArchiveTask(fragment)
        configureKotlinCompilation(fragment)
        createElementsConfigurations(fragment)
        // TODO configure resources processing
    }

    protected open fun setPlatformAttributesInConfiguration(fragment: T, configuration: Configuration) {
        configuration.attributes.attribute(KotlinPlatformType.attribute, fragment.platformType)
    }

    open fun configureCompileResolvableConfiguration(fragment: T, configuration: Configuration) {
        setPlatformAttributesInConfiguration(fragment, configuration)
        configuration.attributes.attribute(Usage.USAGE_ATTRIBUTE, KotlinUsages.consumerApiUsage(project, fragment.platformType))
    }

    open fun configureApiElementsConfiguration(fragment: T, configuration: Configuration) {
        setPlatformAttributesInConfiguration(fragment, configuration)
        configuration.attributes.attribute(Usage.USAGE_ATTRIBUTE, KotlinUsages.producerApiUsage(project, fragment.platformType))
    }

    abstract fun configureKotlinCompilation(fragment: T)

    open fun createSourcesArchiveTask(fragment: T) {
        sourcesJarTask(
            project,
            lazy { fragment.refinesClosure.associate { it.disambiguateName("") to project.files(it.kotlinSourceRoots) } },
            fragment.name,
            fragment.name.toLowerCase()
        )
    }

    override fun createDependencyConfigurations(fragment: T) {
        super.createDependencyConfigurations(fragment)

        fragment.compileDependencyFiles = project.configurations.create(fragment.compileDependencyConfigurationName).apply {
            isCanBeConsumed = false
            isCanBeResolved = true
            configureCompileResolvableConfiguration(fragment, this@apply)
            project.addExtendsFromRelation(name, fragment.transitiveApiConfigurationName)
            project.addExtendsFromRelation(name, fragment.transitiveImplementationConfigurationName)
        }
        // FIXME runtime classpath if supported
    }

    open fun createElementsConfigurations(fragment: T) {
        project.configurations.maybeCreate(fragment.apiElementsConfigurationName).apply {
            isCanBeResolved = false
            isCanBeConsumed = false
            module.ifMadePublic {
                isCanBeConsumed = true
                // FIXME set capability
            }
            attributes.attribute<Usage>(Usage.USAGE_ATTRIBUTE, KotlinUsages.producerApiUsage(project, fragment.platformType))
            extendsFrom(project.configurations.getByName(fragment.transitiveApiConfigurationName))
            // FIXME + compileOnly
            configureApiElementsConfiguration(fragment, this@apply)
        }
    }
}

abstract class AbstractKotlinGradleVariantWithRuntimeDependenciesFactory<T : KotlinGradleVariantWithRuntimeDependencies>(module: KotlinGradleModule) :
    AbstractKotlinGradleVariantFactory<T>(module) {

    open fun configureRuntimeResolvableConfiguration(fragment: T, configuration: Configuration) {
        setPlatformAttributesInConfiguration(fragment, configuration)
        configuration.attributes.attribute(Usage.USAGE_ATTRIBUTE, KotlinUsages.consumerRuntimeUsage(project, fragment.platformType))
    }

    open fun configureRuntimeElementsConfiguration(fragment: T, configuration: Configuration) {
        setPlatformAttributesInConfiguration(fragment, configuration)
        configuration.attributes.attribute(Usage.USAGE_ATTRIBUTE, KotlinUsages.producerRuntimeUsage(project, fragment.platformType))
    }

    override fun createDependencyConfigurations(fragment: T) {
        super.createDependencyConfigurations(fragment)

        fragment.runtimeDependencyFiles = project.configurations.create(fragment.runtimeDependencyConfigurationName).apply {
            isCanBeConsumed = false
            isCanBeResolved = true
            configureCompileResolvableConfiguration(fragment, this@apply)
            project.addExtendsFromRelation(name, fragment.transitiveApiConfigurationName)
            project.addExtendsFromRelation(name, fragment.transitiveImplementationConfigurationName)
        }
    }

    override fun createElementsConfigurations(fragment: T) {
        super.createElementsConfigurations(fragment)

        project.configurations.maybeCreate(fragment.runtimeElementsConfigurationName).apply {
            isCanBeResolved = false
            isCanBeConsumed = false
            module.ifMadePublic {
                isCanBeConsumed = true
                // FIXME set capability
            }
            configureRuntimeElementsConfiguration(fragment, this@apply)
            extendsFrom(project.configurations.getByName(fragment.transitiveApiConfigurationName))
            extendsFrom(project.configurations.getByName(fragment.transitiveImplementationConfigurationName))
            // FIXME + runtimeOnly
        }
    }
}
