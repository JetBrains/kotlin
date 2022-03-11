/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.artifacts.Configuration
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.KotlinNameDisambiguation

interface KotlinFragmentDependencyConfigurationsFactory {
    fun create(module: KotlinGradleModule, names: KotlinNameDisambiguation): KotlinFragmentDependencyConfigurations
}

object DefaultKotlinFragmentDependencyConfigurationsFactory : KotlinFragmentDependencyConfigurationsFactory {

    override fun create(module: KotlinGradleModule, names: KotlinNameDisambiguation): KotlinFragmentDependencyConfigurations {
        val configurations = module.project.configurations
        val apiConfiguration = configurations.maybeCreate(names.disambiguateName("api"))
        val implementationConfiguration = configurations.maybeCreate(names.disambiguateName("implementation"))
        val compileOnlyConfiguration = configurations.maybeCreate(names.disambiguateName("compileOnly"))
        val runtimeOnlyConfiguration = configurations.maybeCreate(names.disambiguateName("runtimeOnly"))
        val transitiveApiConfiguration = configurations.maybeCreate(names.disambiguateName("transitiveApi"))
        val transitiveImplementationConfiguration = configurations.maybeCreate(names.disambiguateName("transitiveImplementation"))
        val transitiveRuntimeOnlyConfiguration = configurations.maybeCreate(names.disambiguateName("transitiveRuntimeOnly"))

        listOf(
            apiConfiguration,
            implementationConfiguration,
            compileOnlyConfiguration,
            runtimeOnlyConfiguration,
            transitiveApiConfiguration,
            transitiveImplementationConfiguration,
            transitiveRuntimeOnlyConfiguration
        ).forEach { configuration ->
            configuration.isCanBeConsumed = false
            configuration.isCanBeResolved = false
        }

        transitiveApiConfiguration.extendsFrom(apiConfiguration)
        transitiveImplementationConfiguration.extendsFrom(implementationConfiguration)
        transitiveRuntimeOnlyConfiguration.extendsFrom(runtimeOnlyConfiguration)

        return KotlinFragmentDependencyConfigurations.create(
            apiConfiguration = apiConfiguration,
            implementationConfiguration = implementationConfiguration,
            compileOnlyConfiguration = compileOnlyConfiguration,
            runtimeOnlyConfiguration = runtimeOnlyConfiguration,
            transitiveApiConfiguration = transitiveApiConfiguration,
            transitiveImplementationConfiguration = transitiveImplementationConfiguration,
            transitiveRuntimeOnlyConfiguration = transitiveRuntimeOnlyConfiguration
        )
    }
}

/**
 * A legacy-mapped variant reuses whatever dependency configurations that the [compilation] has: namely its
 * [KotlinCompilation.apiConfigurationName] (and the other scoped dependency configurations). However, a compilation doesn't have all the
 * configurations that are necessary for a fragment. So this factory creates the
 * [KotlinFragmentDependencyConfigurations.transitiveApiConfiguration] (and the other scoped transitive configurations)
 */
internal class LegacyMappedVariantDependencyConfigurationsFactory(private val compilation: KotlinCompilation<*>) :
    KotlinFragmentDependencyConfigurationsFactory {

    override fun create(module: KotlinGradleModule, names: KotlinNameDisambiguation): KotlinFragmentDependencyConfigurations {
        val configurations = module.project.configurations
        fun byName(configurationName: String): Configuration = compilation.target.project.configurations.getByName(configurationName)
        val apiConfiguration = byName(compilation.apiConfigurationName)
        val implementationConfiguration = byName(compilation.implementationConfigurationName)
        val compileOnlyConfiguration = byName(compilation.compileOnlyConfigurationName)
        val runtimeOnlyConfiguration = byName(compilation.runtimeOnlyConfigurationName)
        val transitiveApiConfiguration = configurations.maybeCreate(names.disambiguateName("transitiveApi"))
        val transitiveImplementationConfiguration = configurations.maybeCreate(names.disambiguateName("transitiveImplementation"))
        val transitiveRuntimeOnlyConfiguration = configurations.maybeCreate(names.disambiguateName("transitiveRuntimeOnly"))

        listOf(
            transitiveApiConfiguration,
            transitiveImplementationConfiguration,
            transitiveRuntimeOnlyConfiguration
        ).forEach { configuration ->
            configuration.isCanBeConsumed = false
            configuration.isCanBeResolved = false
        }

        transitiveApiConfiguration.extendsFrom(apiConfiguration)
        transitiveImplementationConfiguration.extendsFrom(implementationConfiguration)
        transitiveRuntimeOnlyConfiguration.extendsFrom(runtimeOnlyConfiguration)

        return KotlinFragmentDependencyConfigurations.create(
            apiConfiguration = apiConfiguration,
            implementationConfiguration = implementationConfiguration,
            compileOnlyConfiguration = compileOnlyConfiguration,
            runtimeOnlyConfiguration = runtimeOnlyConfiguration,
            transitiveApiConfiguration = transitiveApiConfiguration,
            transitiveImplementationConfiguration = transitiveImplementationConfiguration,
            transitiveRuntimeOnlyConfiguration = transitiveRuntimeOnlyConfiguration
        )
    }
}
