/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.GradleKpmNameDisambiguation

interface GradleKpmFragmentDependencyConfigurationsFactory {
    fun create(module: GradleKpmModule, names: GradleKpmNameDisambiguation): GradleKpmFragmentDependencyConfigurations
}

object GradleKpmDefaultFragmentDependencyConfigurationsFactory : GradleKpmFragmentDependencyConfigurationsFactory {

    override fun create(module: GradleKpmModule, names: GradleKpmNameDisambiguation): GradleKpmFragmentDependencyConfigurations {
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

        return GradleKpmFragmentDependencyConfigurations.create(
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
