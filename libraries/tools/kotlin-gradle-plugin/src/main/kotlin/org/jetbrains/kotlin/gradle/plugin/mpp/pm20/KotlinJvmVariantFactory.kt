/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

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

    private val compileDependenciesConfigurationFactory: KotlinCompileDependenciesConfigurationFactory =
        DefaultKotlinCompileDependenciesConfigurationFactory,

    private val apiElementsConfigurationFactory: KotlinApiElementsConfigurationFactory =
        DefaultKotlinApiElementsConfigurationFactory,

    private val runtimeDependenciesConfigurationFactory: KotlinRuntimeDependenciesConfigurationFactory =
        DefaultKotlinRuntimeDependenciesConfigurationFactory,

    private val runtimeElementsConfigurationFactory: KotlinRuntimeElementsConfigurationFactory =
        DefaultKotlinRuntimeElementsConfigurationFactory,

    ) : KotlinGradleFragmentFactory.FragmentInstantiator<KotlinJvmVariant> {

    override fun create(name: String): KotlinJvmVariant {
        val names = FragmentNameDisambiguation(module, name)
        val dependencies = dependenciesConfigurationFactory.create(module, names)
        return KotlinJvmVariant(
            containingModule = module,
            fragmentName = name,
            dependencyConfigurations = dependencies,
            compileDependenciesConfiguration = compileDependenciesConfigurationFactory.create(module, names, dependencies),
            apiElementsConfiguration = apiElementsConfigurationFactory.create(module, names, dependencies),
            runtimeDependenciesConfiguration = runtimeDependenciesConfigurationFactory.create(module, names, dependencies),
            runtimeElementsConfiguration = runtimeElementsConfigurationFactory.create(module, names, dependencies)
        )
    }
}

class KotlinJvmVariantConfigurator(
    private val compileTaskConfigurator: KotlinCompileTaskConfigurator<KotlinJvmVariant> =
        KotlinJvmCompileTaskConfigurator,

    private val sourceArchiveTaskConfigurator: KotlinSourceArchiveTaskConfigurator<KotlinJvmVariant> =
        DefaultKotlinSourceArchiveTaskConfigurator,

    private val sourceDirectoriesSetup: KotlinSourceDirectoriesSetup<KotlinJvmVariant> =
        DefaultKotlinSourceDirectoriesSetup,

    private val compileDependenciesSetup: KotlinConfigurationsSetup<KotlinJvmVariant> =
        DefaultKotlinCompileDependenciesSetup,

    private val runtimeDependenciesSetup: KotlinConfigurationsSetup<KotlinJvmVariant> =
        DefaultKotlinRuntimeDependenciesSetup,

    private val apiElementsSetup: KotlinConfigurationsSetup<KotlinJvmVariant> =
        DefaultKotlinApiElementsSetup,

    private val runtimeElementsSetup: KotlinConfigurationsSetup<KotlinJvmVariant> =
        DefaultKotlinRuntimeElementsSetup,
    private val publicationSetup: PublicationSetup<KotlinJvmVariant> = PublicationSetup.SingleVariantPublication
) : KotlinGradleFragmentFactory.FragmentConfigurator<KotlinJvmVariant> {

    override fun configure(fragment: KotlinJvmVariant) {
        compileDependenciesSetup.configure(fragment, fragment.compileDependencyConfiguration)
        runtimeDependenciesSetup.configure(fragment, fragment.runtimeDependencyConfiguration)
        apiElementsSetup.configure(fragment, fragment.apiElementsConfiguration)
        runtimeElementsSetup.configure(fragment, fragment.runtimeElementsConfiguration)

        compileTaskConfigurator.registerCompileTasks(fragment)
        sourceArchiveTaskConfigurator.registerSourceArchiveTask(fragment)
        sourceDirectoriesSetup.configure(fragment)
        publicationSetup.configure(fragment)
    }
}
