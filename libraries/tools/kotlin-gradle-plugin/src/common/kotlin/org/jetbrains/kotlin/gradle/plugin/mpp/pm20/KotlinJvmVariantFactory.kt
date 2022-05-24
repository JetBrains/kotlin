/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.FragmentNameDisambiguationOmittingMain

typealias GradleKpmJvmVariantFactory = GradleKpmFragmentFactory<GradleKpmJvmVariant>

fun GradleKpmJvmVariantFactory(
    module: GradleKpmModule, config: GradleKpmJvmVariantConfig = GradleKpmJvmVariantConfig()
): GradleKpmJvmVariantFactory = GradleKpmJvmVariantFactory(
    GradleKpmJvmVariantInstantiator(module, config),
    GradleKpmJvmVariantConfigurator(config)
)

data class GradleKpmJvmVariantConfig(
    val dependenciesConfigurationFactory: GradleKpmFragmentDependencyConfigurationsFactory
    = GradleKpmDefaultFragmentDependencyConfigurationsFactory,

    val compileDependencies: GradleKpmConfigurationSetup<GradleKpmJvmVariant>
    = DefaultKotlinCompileDependenciesDefinition,

    val runtimeDependencies: GradleKpmConfigurationSetup<GradleKpmJvmVariant>
    = DefaultKotlinRuntimeDependenciesDefinition,

    val apiElements: GradleKpmConfigurationSetup<GradleKpmJvmVariant>
    = DefaultKotlinApiElementsDefinition + GradleKpmCompilationOutputsJarArtifact,

    val runtimeElements: GradleKpmConfigurationSetup<GradleKpmJvmVariant>
    = DefaultKotlinRuntimeElementsDefinition,

    val compileTaskConfigurator: GradleKpmCompileTaskConfigurator<GradleKpmJvmVariant>
    = GradleKpmJvmCompileTaskConfigurator,

    val sourceArchiveTaskConfigurator: GradleKpmSourceArchiveTaskConfigurator<GradleKpmJvmVariant>
    = GradleKpmDefaultKotlinSourceArchiveTaskConfigurator,

    val sourceDirectoriesConfigurator: GradleKpmSourceDirectoriesConfigurator<GradleKpmJvmVariant>
    = GradleKpmDefaultSourceDirectoriesConfigurator,

    val publicationConfigurator: GradleKpmPublicationConfigurator<GradleKpmJvmVariant>
    = GradleKpmPublicationConfigurator.SingleVariantPublication
)

class GradleKpmJvmVariantInstantiator internal constructor(
    private val module: GradleKpmModule,
    private val config: GradleKpmJvmVariantConfig
) : GradleKpmFragmentFactory.FragmentInstantiator<GradleKpmJvmVariant> {

    override fun create(name: String): GradleKpmJvmVariant {
        val names = FragmentNameDisambiguationOmittingMain(module, name)
        val context = GradleKpmFragmentConfigureContextImpl(
            module, config.dependenciesConfigurationFactory.create(module, names), names
        )

        return GradleKpmJvmVariant(
            containingModule = module,
            fragmentName = name,
            dependencyConfigurations = context.dependencies,
            compileDependenciesConfiguration = config.compileDependencies.provider.getConfiguration(context).also { configuration ->
                config.compileDependencies.relations.setupExtendsFromRelations(configuration, context)
            },
            runtimeDependenciesConfiguration = config.runtimeDependencies.provider.getConfiguration(context).also { configuration ->
                config.runtimeElements.relations.setupExtendsFromRelations(configuration, context)
            },
            apiElementsConfiguration = config.apiElements.provider.getConfiguration(context).also { configuration ->
                config.apiElements.relations.setupExtendsFromRelations(configuration, context)
            },
            runtimeElementsConfiguration = config.runtimeElements.provider.getConfiguration(context).also { configuration ->
                config.runtimeElements.relations.setupExtendsFromRelations(configuration, context)
            }
        )
    }
}

class GradleKpmJvmVariantConfigurator internal constructor(
    private val config: GradleKpmJvmVariantConfig
) : GradleKpmFragmentFactory.FragmentConfigurator<GradleKpmJvmVariant> {

    override fun configure(fragment: GradleKpmJvmVariant) {
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
