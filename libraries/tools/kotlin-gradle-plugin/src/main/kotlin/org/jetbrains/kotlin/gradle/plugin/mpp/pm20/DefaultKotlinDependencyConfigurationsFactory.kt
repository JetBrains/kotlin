/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.artifacts.Configuration
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.disambiguateName
import org.jetbrains.kotlin.gradle.utils.addExtendsFromRelation
import org.jetbrains.kotlin.project.model.KotlinModuleFragment

object DefaultKotlinDependencyConfigurationsFactory : KotlinDependencyConfigurations.Factory {

    override fun create(module: KotlinGradleModule, fragmentName: String): KotlinDependencyConfigurations {
        val apiConfiguration = maybeCreateConfiguration(module, fragmentName, "api")
        val implementationConfiguration = maybeCreateConfiguration(module, fragmentName, "implementation")
        val compileOnlyConfiguration = maybeCreateConfiguration(module, fragmentName, "compileOnly")
        val runtimeOnlyConfiguration = maybeCreateConfiguration(module, fragmentName, "runtimeOnly")
        val transitiveApiConfiguration = maybeCreateConfiguration(module, fragmentName, "transitiveApi")
        val transitiveImplementationConfiguration = maybeCreateConfiguration(module, fragmentName, "transitiveImplementation")

        listOf(
            apiConfiguration,
            implementationConfiguration,
            compileOnlyConfiguration,
            runtimeOnlyConfiguration,
            transitiveApiConfiguration,
            transitiveImplementationConfiguration
        ).forEach { configuration ->
            configuration.isCanBeConsumed = false
            configuration.isCanBeResolved = false
        }

        module.project.addExtendsFromRelation(
            transitiveApiConfiguration.name, apiConfiguration.name
        )

        module.project.addExtendsFromRelation(
            transitiveImplementationConfiguration.name, implementationConfiguration.name
        )

        return KotlinDependencyConfigurations.create(
            apiConfiguration = apiConfiguration,
            implementationConfiguration = implementationConfiguration,
            compileOnlyConfiguration = compileOnlyConfiguration,
            runtimeOnlyConfiguration = runtimeOnlyConfiguration,
            transitiveApiConfiguration = transitiveApiConfiguration,
            transitiveImplementationConfiguration = transitiveImplementationConfiguration
        )
    }

    private fun maybeCreateConfiguration(
        module: KotlinGradleModule, fragmentName: String, simpleConfigurationName: String
    ): Configuration = module.project.configurations.maybeCreate(
        KotlinModuleFragment.disambiguateName(module, fragmentName, simpleConfigurationName)
    )
}
