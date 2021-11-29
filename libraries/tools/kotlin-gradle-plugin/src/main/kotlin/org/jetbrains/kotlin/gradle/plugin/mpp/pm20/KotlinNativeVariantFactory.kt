/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.configuration.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.FragmentNameDisambiguation

fun <T : KotlinNativeVariantInternal> KotlinNativeVariantFactory(
    nativeVariantInstantiator: KotlinNativeVariantInstantiator<T>,
    nativeVariantConfigurator: KotlinNativeVariantConfigurator<T> = KotlinNativeVariantConfigurator()
) = KotlinGradleFragmentFactory(
    fragmentInstantiator = nativeVariantInstantiator,
    fragmentConfigurator = nativeVariantConfigurator
)

class KotlinNativeVariantInstantiator<T : KotlinNativeVariantInternal>(
    private val module: KotlinGradleModule,

    private val kotlinNativeVariantConstructor: KotlinNativeVariantConstructor<T>,

    private val dependenciesConfigurationFactory: KotlinDependencyConfigurationsFactory =
        DefaultKotlinDependencyConfigurationsFactory,

    private val compileDependenciesConfigurationInstantiator: KotlinCompileDependenciesConfigurationInstantiator =
        DefaultKotlinCompileDependenciesConfigurationInstantiator,

    private val apiElementsConfigurationInstantiator: KotlinApiElementsConfigurationInstantiator =
        DefaultKotlinApiElementsConfigurationInstantiator,

    private val hostSpecificMetadataElementsConfigurationInstantiator: KotlinHostSpecificMetadataElementsConfigurationInstantiator? =
        DefaultKotlinHostSpecificMetadataElementsConfigurationInstantiator(kotlinNativeVariantConstructor.konanTarget)

) : KotlinGradleFragmentFactory.FragmentInstantiator<T> {

    override fun create(name: String): T {
        val names = FragmentNameDisambiguation(module, name)
        val dependencies = dependenciesConfigurationFactory.create(module, names)
        return kotlinNativeVariantConstructor.invoke(
            containingModule = module,
            fragmentName = name,
            dependencyConfigurations = dependencies,
            compileDependencyConfiguration = compileDependenciesConfigurationInstantiator.create(module, names, dependencies),
            apiElementsConfiguration = apiElementsConfigurationInstantiator.create(module, names, dependencies),
            hostSpecificMetadataElementsConfiguration =
            hostSpecificMetadataElementsConfigurationInstantiator?.create(module, names, dependencies)
        )
    }
}

class KotlinNativeVariantConfigurator<T : KotlinNativeVariantInternal>(
    private val compileTaskConfigurator: KotlinCompileTaskConfigurator<T> =
        KotlinNativeCompileTaskConfigurator,

    private val sourceArchiveTaskConfigurator: KotlinSourceArchiveTaskConfigurator<T> =
        DefaultKotlinSourceArchiveTaskConfigurator,

    private val sourceDirectoriesConfigurator: KotlinSourceDirectoriesConfigurator<T> =
        DefaultKotlinSourceDirectoriesConfigurator,

    private val compileDependenciesConfigurator: KotlinFragmentConfigurationsConfigurator<T> =
        DefaultKotlinCompileDependenciesConfigurator + KonanTargetAttributesConfigurator,

    private val apiElementsConfigurator: KotlinFragmentConfigurationsConfigurator<T> =
        DefaultKotlinApiElementsConfigurator + KonanTargetAttributesConfigurator,

    private val hostSpecificMetadataElementsConfigurator: KotlinFragmentConfigurationsConfigurator<T> =
        DefaultHostSpecificMetadataElementsConfigurator,

    private val hostSpecificMetadataArtifactConfigurator: KotlinGradleFragmentFactory.FragmentConfigurator<T> =
        KotlinHostSpecificMetadataArtifactConfigurator,

    private val publicationConfigurator: KotlinPublicationConfigurator<KotlinNativeVariantInternal> =
        KotlinPublicationConfigurator.NativeVariantPublication
) : KotlinGradleFragmentFactory.FragmentConfigurator<T> {

    override fun configure(fragment: T) {
        compileDependenciesConfigurator.configure(fragment, fragment.compileDependenciesConfiguration)
        apiElementsConfigurator.configure(fragment, fragment.apiElementsConfiguration)
        fragment.hostSpecificMetadataElementsConfiguration?.let { configuration ->
            hostSpecificMetadataElementsConfigurator.configure(fragment, configuration)
        }
        hostSpecificMetadataArtifactConfigurator.configure(fragment)
        sourceDirectoriesConfigurator.configure(fragment)
        compileTaskConfigurator.registerCompileTasks(fragment)
        sourceArchiveTaskConfigurator.registerSourceArchiveTask(fragment)
        publicationConfigurator.configure(fragment)
    }
}
