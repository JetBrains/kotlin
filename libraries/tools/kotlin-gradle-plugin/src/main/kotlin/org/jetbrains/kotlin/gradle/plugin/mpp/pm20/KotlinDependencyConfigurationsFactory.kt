/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.FragmentNameDisambiguation
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.locateOrRegister
import org.jetbrains.kotlin.gradle.utils.addExtendsFromRelation
import org.jetbrains.kotlin.gradle.utils.extendsFrom

interface KotlinDependencyConfigurationsFactory {
    fun create(module: KotlinGradleModule, names: FragmentNameDisambiguation): KotlinDependencyConfigurations
}

object DefaultKotlinDependencyConfigurationsFactory : KotlinDependencyConfigurationsFactory {

    override fun create(module: KotlinGradleModule, names: FragmentNameDisambiguation): KotlinDependencyConfigurations {
        val configurations = module.project.configurations
        val apiConfiguration = configurations.locateOrRegister(names.disambiguateName("api"))
        val implementationConfiguration = configurations.locateOrRegister(names.disambiguateName("implementation"))
        val compileOnlyConfiguration = configurations.locateOrRegister(names.disambiguateName("compileOnly"))
        val runtimeOnlyConfiguration = configurations.locateOrRegister(names.disambiguateName("runtimeOnly"))
        val transitiveApiConfiguration = configurations.locateOrRegister(names.disambiguateName("transitiveApi"))
        val transitiveImplementationConfiguration = configurations.locateOrRegister(names.disambiguateName("transitiveImplementation"))

        listOf(
            apiConfiguration,
            implementationConfiguration,
            compileOnlyConfiguration,
            runtimeOnlyConfiguration,
            transitiveApiConfiguration,
            transitiveImplementationConfiguration
        ).forEach { configurationProvider ->
            configurationProvider.configure { configuration ->
                configuration.isCanBeConsumed = false
                configuration.isCanBeResolved = false
            }
        }

        transitiveApiConfiguration.extendsFrom(apiConfiguration)
        transitiveImplementationConfiguration.extendsFrom(implementationConfiguration)

        return KotlinDependencyConfigurations.create(
            apiConfiguration = apiConfiguration,
            implementationConfiguration = implementationConfiguration,
            compileOnlyConfiguration = compileOnlyConfiguration,
            runtimeOnlyConfiguration = runtimeOnlyConfiguration,
            transitiveApiConfiguration = transitiveApiConfiguration,
            transitiveImplementationConfiguration = transitiveImplementationConfiguration
        )
    }
}
