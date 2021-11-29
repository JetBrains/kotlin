/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.configuration.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.FragmentNameDisambiguation

typealias KotlinJvmVariantFactory = KotlinGradleFragmentFactory<KotlinJvmVariant>

fun KotlinJvmVariantFactory(module: KotlinGradleModule): KotlinJvmVariantFactory =
    KotlinJvmVariantFactory(KotlinJvmVariantInstantiator(module))

fun KotlinJvmVariantFactory(
    jvmVariantInstantiator: KotlinJvmVariantInstantiator,
    jvmVariantConfigurator: KotlinJvmVariantConfigurator = KotlinJvmVariantConfigurator()
): KotlinJvmVariantFactory = KotlinGradleFragmentFactory(
    fragmentInstantiator = jvmVariantInstantiator,
    fragmentConfigurator = jvmVariantConfigurator
)

class KotlinJvmVariantInstantiator(
    private val module: KotlinGradleModule,

    private val dependenciesConfigurationFactory: KotlinDependencyConfigurationsFactory =
        DefaultKotlinDependencyConfigurationsFactory,

    private val compileDependenciesConfigurationInstantiator: KotlinCompileDependenciesConfigurationInstantiator =
        DefaultKotlinCompileDependenciesConfigurationInstantiator,

    private val apiElementsConfigurationInstantiator: KotlinApiElementsConfigurationInstantiator =
        DefaultKotlinApiElementsConfigurationInstantiator,

    private val runtimeDependenciesConfigurationInstantiator: KotlinRuntimeDependenciesConfigurationInstantiator =
        DefaultKotlinRuntimeDependenciesConfigurationInstantiator,

    private val runtimeElementsConfigurationInstantiator: KotlinRuntimeElementsConfigurationFactory =
        DefaultKotlinRuntimeElementsConfigurationInstantiator,

    ) : KotlinGradleFragmentFactory.FragmentInstantiator<KotlinJvmVariant> {

    override fun create(name: String): KotlinJvmVariant {
        val names = FragmentNameDisambiguation(module, name)
        val dependencies = dependenciesConfigurationFactory.create(module, names)
        return KotlinJvmVariant(
            containingModule = module,
            fragmentName = name,
            dependencyConfigurations = dependencies,
            compileDependenciesConfiguration = compileDependenciesConfigurationInstantiator.locateOrRegister(module, names, dependencies),
            apiElementsConfiguration = apiElementsConfigurationInstantiator.locateOrRegister(module, names, dependencies),
            runtimeDependenciesConfiguration = runtimeDependenciesConfigurationInstantiator.locateOrRegister(module, names, dependencies),
            runtimeElementsConfiguration = runtimeElementsConfigurationInstantiator.locateOrRegister(module, names, dependencies)
        )
    }
}

class KotlinJvmVariantConfigurator(
    private val compileTaskConfigurator: KotlinCompileTaskConfigurator<KotlinJvmVariant> =
        KotlinJvmCompileTaskConfigurator,

    private val sourceArchiveTaskConfigurator: KotlinSourceArchiveTaskConfigurator<KotlinJvmVariant> =
        DefaultKotlinSourceArchiveTaskConfigurator,

    private val sourceDirectoriesConfigurator: KotlinSourceDirectoriesConfigurator<KotlinJvmVariant> =
        DefaultKotlinSourceDirectoriesConfigurator,

    private val compileDependenciesConfigurator: KotlinFragmentConfigurationsConfigurator<KotlinJvmVariant> =
        DefaultKotlinCompileDependenciesConfigurator,

    private val runtimeDependenciesConfigurator: KotlinFragmentConfigurationsConfigurator<KotlinJvmVariant> =
        DefaultKotlinRuntimeDependenciesConfigurator,

    private val apiElementsConfigurator: KotlinFragmentConfigurationsConfigurator<KotlinJvmVariant> =
        DefaultKotlinApiElementsConfigurator + KotlinCompilationOutputsJarArtifactConfigurator,

    private val runtimeElementsConfigurator: KotlinFragmentConfigurationsConfigurator<KotlinJvmVariant> =
        DefaultKotlinRuntimeElementsConfigurator,

    private val publicationConfigurator: KotlinPublicationConfigurator<KotlinJvmVariant> =
        KotlinPublicationConfigurator.SingleVariantPublication

) : KotlinGradleFragmentFactory.FragmentConfigurator<KotlinJvmVariant> {

    override fun configure(fragment: KotlinJvmVariant) {
        fragment.compileDependenciesConfiguration.configure { configuration ->
            compileDependenciesConfigurator.configure(fragment, configuration)
        }

        fragment.runtimeDependenciesConfiguration.configure { configuration ->
            runtimeDependenciesConfigurator.configure(fragment, configuration)
        }

        fragment.apiElementsConfiguration.configure { configuration ->
            apiElementsConfigurator.configure(fragment, configuration)
        }

        fragment.runtimeElementsConfiguration.configure { configuration ->
            runtimeElementsConfigurator.configure(fragment, configuration)
        }

        sourceDirectoriesConfigurator.configure(fragment)

        compileTaskConfigurator.registerCompileTasks(fragment)
        sourceArchiveTaskConfigurator.registerSourceArchiveTask(fragment)
        publicationConfigurator.configure(fragment)
    }
}
