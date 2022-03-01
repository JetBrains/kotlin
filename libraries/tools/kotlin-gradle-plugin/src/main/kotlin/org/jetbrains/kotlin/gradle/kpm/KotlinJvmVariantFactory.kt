/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.kpm

import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleModule
import org.jetbrains.kotlin.gradle.kpm.util.FragmentNameDisambiguationOmittingMain

typealias KotlinJvmVariantFactory = KotlinGradleFragmentFactory<KotlinJvmVariant>

fun KotlinJvmVariantFactory(
    module: KotlinGradleModule, config: KotlinJvmVariantConfig = KotlinJvmVariantConfig()
): KotlinJvmVariantFactory = KotlinJvmVariantFactory(
    KotlinJvmVariantInstantiator(module, config),
    KotlinJvmVariantConfigurator(config)
)

data class KotlinJvmVariantConfig(
    val dependenciesConfigurationFactory: KotlinFragmentDependencyConfigurationsFactory
    = DefaultKotlinFragmentDependencyConfigurationsFactory,

    val compileDependencies: KotlinGradleFragmentConfigurationDefinition<KotlinJvmVariant>
    = DefaultKotlinCompileDependenciesDefinition,

    val runtimeDependencies: KotlinGradleFragmentConfigurationDefinition<KotlinJvmVariant>
    = DefaultKotlinRuntimeDependenciesDefinition,

    val apiElements: KotlinGradleFragmentConfigurationDefinition<KotlinJvmVariant>
    = DefaultKotlinApiElementsDefinition + KotlinFragmentCompilationOutputsJarArtifact,

    val runtimeElements: KotlinGradleFragmentConfigurationDefinition<KotlinJvmVariant>
    = DefaultKotlinRuntimeElementsDefinition,

    val compileTaskConfigurator: KotlinCompileTaskConfigurator<KotlinJvmVariant>
    = KotlinJvmCompileTaskConfigurator,

    val sourceArchiveTaskConfigurator: KotlinSourceArchiveTaskConfigurator<KotlinJvmVariant>
    = DefaultKotlinSourceArchiveTaskConfigurator,

    val sourceDirectoriesConfigurator: KotlinSourceDirectoriesConfigurator<KotlinJvmVariant>
    = DefaultKotlinSourceDirectoriesConfigurator,

    val publicationConfigurator: KotlinPublicationConfigurator<KotlinJvmVariant>
    = KotlinPublicationConfigurator.SingleVariantPublication
)

class KotlinJvmVariantInstantiator internal constructor(
    private val module: KotlinGradleModule,
    private val config: KotlinJvmVariantConfig
) : KotlinGradleFragmentFactory.FragmentInstantiator<KotlinJvmVariant> {

    override fun create(name: String): KotlinJvmVariant {
        val names = FragmentNameDisambiguationOmittingMain(module, name)
        val context = KotlinGradleFragmentConfigurationContextImpl(
            module, config.dependenciesConfigurationFactory.create(module, names), names
        )

        return KotlinJvmVariant(
            containingModule = module,
            fragmentName = name,
            dependencyConfigurations = context.dependencies,
            compileDependenciesConfiguration = config.compileDependencies.provider.getConfiguration(context).also { configuration ->
                config.compileDependencies.relations.setExtendsFrom(configuration, context)
            },
            runtimeDependenciesConfiguration = config.runtimeDependencies.provider.getConfiguration(context).also { configuration ->
                config.runtimeElements.relations.setExtendsFrom(configuration, context)
            },
            apiElementsConfiguration = config.apiElements.provider.getConfiguration(context).also { configuration ->
                config.apiElements.relations.setExtendsFrom(configuration, context)
            },
            runtimeElementsConfiguration = config.runtimeElements.provider.getConfiguration(context).also { configuration ->
                config.runtimeElements.relations.setExtendsFrom(configuration, context)
            }
        )
    }
}

class KotlinJvmVariantConfigurator internal constructor(
    private val config: KotlinJvmVariantConfig
) : KotlinGradleFragmentFactory.FragmentConfigurator<KotlinJvmVariant> {

    override fun configure(fragment: KotlinJvmVariant) {
        fragment.compileDependenciesConfiguration.configure(config.compileDependencies, fragment)
        fragment.runtimeDependenciesConfiguration.configure(config.runtimeDependencies, fragment)
        fragment.apiElementsConfiguration.configure(config.apiElements, fragment)
        fragment.runtimeElementsConfiguration.configure(config.runtimeElements, fragment)

        config.sourceDirectoriesConfigurator.configure(fragment)
        config.compileTaskConfigurator.registerCompileTasks(fragment)
        config.sourceArchiveTaskConfigurator.registerSourceArchiveTask(fragment)
        config.publicationConfigurator.configure(fragment)
    }
}
