/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20.configuration

import org.gradle.api.artifacts.Configuration
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.FragmentNameDisambiguation
import org.jetbrains.kotlin.gradle.utils.addExtendsFromRelation

interface KotlinRuntimeDependenciesConfigurationInstantiator : KotlinFragmentConfigurationInstantiator

object DefaultKotlinRuntimeDependenciesConfigurationInstantiator : KotlinRuntimeDependenciesConfigurationInstantiator {
    override fun create(
        module: KotlinGradleModule,
        names: FragmentNameDisambiguation,
        dependencies: KotlinDependencyConfigurations
    ): Configuration {
        return module.project.configurations.maybeCreate(names.disambiguateName("runtimeDependencies")).apply {
            isCanBeConsumed = false
            isCanBeResolved = true
            module.project.addExtendsFromRelation(name, dependencies.transitiveApiConfiguration.name)
            module.project.addExtendsFromRelation(name, dependencies.transitiveImplementationConfiguration.name)
        }
    }
}

val DefaultKotlinRuntimeDependenciesConfigurator = KotlinConfigurationsConfigurator(
    KotlinFragmentPlatformAttributesConfigurator,
    KotlinFragmentConsumerRuntimeUsageAttributesConfigurator
)
