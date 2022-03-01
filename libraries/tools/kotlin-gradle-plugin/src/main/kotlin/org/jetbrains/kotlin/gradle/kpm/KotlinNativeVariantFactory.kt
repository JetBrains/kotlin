/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.kpm

import org.jetbrains.kotlin.gradle.kpm.util.FragmentNameDisambiguation

fun <T : KotlinNativeVariantInternal> KotlinNativeVariantFactory(
    module: KotlinGradleModule,
    constructor: KotlinNativeVariantConstructor<T>,
    config: KotlinNativeVariantConfig<T> = KotlinNativeVariantConfig()
) = KotlinGradleFragmentFactory(
    fragmentInstantiator = KotlinNativeVariantInstantiator(module, constructor, config),
    fragmentConfigurator = KotlinNativeVariantConfigurator(config)
)

data class KotlinNativeVariantConfig<T : KotlinNativeVariantInternal>(
    val dependenciesConfigurationFactory: KotlinFragmentDependencyConfigurationsFactory =
        DefaultKotlinFragmentDependencyConfigurationsFactory,

    val compileDependencies: ConfigurationDefinition<T> =
        DefaultKotlinCompileDependenciesDefinition + KotlinFragmentKonanTargetAttribute,

    val apiElements: ConfigurationDefinition<T> =
        DefaultKotlinApiElementsDefinition
                + KotlinFragmentKonanTargetAttribute
                + FragmentConfigurationRelation { extendsFrom(dependencies.transitiveImplementationConfiguration) },

    val hostSpecificMetadataElements: ConfigurationDefinition<T> =
        DefaultKotlinHostSpecificMetadataElementsDefinition,

    val compileTaskConfigurator: KotlinCompileTaskConfigurator<T> =
        KotlinNativeCompileTaskConfigurator,

    val sourceArchiveTaskConfigurator: KotlinSourceArchiveTaskConfigurator<T> =
        DefaultKotlinSourceArchiveTaskConfigurator,

    val sourceDirectoriesConfigurator: KotlinSourceDirectoriesConfigurator<T> =
        DefaultKotlinSourceDirectoriesConfigurator,

    val publicationConfigurator: KotlinPublicationConfigurator<KotlinNativeVariantInternal> =
        KotlinPublicationConfigurator.NativeVariantPublication
)

class KotlinNativeVariantInstantiator<T : KotlinNativeVariantInternal>(
    private val module: KotlinGradleModule,
    private val kotlinNativeVariantConstructor: KotlinNativeVariantConstructor<T>,
    private val config: KotlinNativeVariantConfig<T>

) : KotlinGradleFragmentFactory.FragmentInstantiator<T> {

    override fun create(name: String): T {
        val names = FragmentNameDisambiguation(module, name)
        val dependencies = config.dependenciesConfigurationFactory.create(module, names)
        val context = KotlinGradleFragmentConfigurationContextImpl(module, dependencies, names)

        return kotlinNativeVariantConstructor.invoke(
            containingModule = module,
            fragmentName = name,
            dependencyConfigurations = dependencies,
            compileDependencyConfiguration = config.compileDependencies.provider.getConfiguration(context).also { configuration ->
                config.compileDependencies.relations.setExtendsFrom(configuration, context)
            },
            apiElementsConfiguration = config.apiElements.provider.getConfiguration(context).also { configuration ->
                config.apiElements.relations.setExtendsFrom(configuration, context)
            },
            hostSpecificMetadataElementsConfiguration =
            config.hostSpecificMetadataElements.provider.getConfiguration(context).also { configuration ->
                config.hostSpecificMetadataElements.relations.setExtendsFrom(configuration, context)
            }
        )
    }
}

class KotlinNativeVariantConfigurator<T : KotlinNativeVariantInternal>(
    private val config: KotlinNativeVariantConfig<T>
) : KotlinGradleFragmentFactory.FragmentConfigurator<T> {

    override fun configure(fragment: T) {
        fragment.compileDependenciesConfiguration.configure(config.compileDependencies, fragment)
        fragment.apiElementsConfiguration.configure(config.apiElements, fragment)
        fragment.hostSpecificMetadataElementsConfiguration?.configure(config.hostSpecificMetadataElements, fragment)

        config.sourceDirectoriesConfigurator.configure(fragment)
        config.compileTaskConfigurator.registerCompileTasks(fragment)
        config.sourceArchiveTaskConfigurator.registerSourceArchiveTask(fragment)
        config.publicationConfigurator.configure(fragment)
    }
}
