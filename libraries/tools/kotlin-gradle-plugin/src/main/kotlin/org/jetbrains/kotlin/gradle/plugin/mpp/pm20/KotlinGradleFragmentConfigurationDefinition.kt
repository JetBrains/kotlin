/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.KotlinNameDisambiguation

/* Internal abbreviation */
internal typealias ConfigurationDefinition<T> = KotlinGradleFragmentConfigurationDefinition<T>

data class KotlinGradleFragmentConfigurationDefinition<in T : KotlinGradleFragment>(
    val provider: KotlinGradleFragmentConfigurationProvider,
    val attributes: KotlinGradleFragmentConfigurationAttributes<T> = KotlinGradleFragmentConfigurationAttributes.None,
    val artifacts: KotlinGradleFragmentConfigurationArtifacts<T> = KotlinGradleFragmentConfigurationArtifacts.None,
    val relations: KotlinGradleFragmentConfigurationRelation = KotlinGradleFragmentConfigurationRelation.None,
    @property:AdvancedKotlinGradlePluginApi
    val capabilities: KotlinGradleFragmentConfigurationCapabilities<T> = KotlinGradleFragmentConfigurationCapabilities.None,
)

/* Internal abbreviation */
internal typealias ConfigurationContext = KotlinGradleFragmentConfigurationContext

interface KotlinGradleFragmentConfigurationContext : KotlinNameDisambiguation {
    val project: Project get() = module.project
    val module: KotlinGradleModule
    val dependencies: KotlinFragmentDependencyConfigurations
}

internal class KotlinGradleFragmentConfigurationContextImpl(
    override val module: KotlinGradleModule,
    override val dependencies: KotlinFragmentDependencyConfigurations,
    names: KotlinNameDisambiguation
) : KotlinGradleFragmentConfigurationContext, KotlinNameDisambiguation by names

fun <T : KotlinGradleFragment> KotlinGradleFragmentConfigurationDefinition<T>.withConfigurationProvider(
    provider: KotlinGradleFragmentConfigurationContext.() -> Configuration
) = copy(provider = ConfigurationProvider(provider))

operator fun <T : KotlinGradleFragment> KotlinGradleFragmentConfigurationDefinition<T>.plus(other: FragmentAttributes<T>):
        KotlinGradleFragmentConfigurationDefinition<T> = copy(attributes = attributes + other)

operator fun <T : KotlinGradleFragment> KotlinGradleFragmentConfigurationDefinition<T>.plus(other: FragmentArtifacts<T>):
        KotlinGradleFragmentConfigurationDefinition<T> = copy(artifacts = artifacts + other)

operator fun <T : KotlinGradleFragment> KotlinGradleFragmentConfigurationDefinition<T>.plus(other: FragmentConfigurationRelation):
        KotlinGradleFragmentConfigurationDefinition<T> = copy(relations = relations + other)

@AdvancedKotlinGradlePluginApi
operator fun <T : KotlinGradleFragment> KotlinGradleFragmentConfigurationDefinition<T>.plus(other: FragmentCapabilities<T>):
        KotlinGradleFragmentConfigurationDefinition<T> = copy(capabilities = capabilities + other)
