/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

abstract class AbstractKotlinGradleVariantFactory<T : KotlinGradleVariant>(
    module: KotlinGradleModule,
    private val compileTaskConfigurator: KotlinCompileTaskConfigurator<T>,

    protected val compileDependenciesConfigurationFactory: KotlinCompileDependenciesConfigurationFactory =
        DefaultKotlinCompileDependenciesConfigurationFactory,

    private val compileDependenciesSetup: KotlinConfigurationsSetup<T> =
        DefaultKotlinCompileDependenciesSetup,

    protected val apiElementsConfigurationFactory: KotlinApiElementsConfigurationFactory =
        DefaultKotlinApiElementsConfigurationFactory,

    private val apiElementsSetup: KotlinConfigurationsSetup<T> =
        DefaultKotlinApiElementsSetup,

    private val sourceArchiveTaskConfigurator: KotlinSourceArchiveTaskConfigurator<T> =
        DefaultKotlinSourceArchiveTaskConfigurator
) : AbstractKotlinGradleFragmentFactory<T>(module) {

    override fun create(name: String): T =
        super.create(name).also { fragment ->
            compileDependenciesSetup.configure(fragment, fragment.compileDependencyConfiguration)
            apiElementsSetup.configure(fragment, fragment.apiElementsConfiguration)
            sourceArchiveTaskConfigurator.registerSourceArchiveTask(fragment)
            compileTaskConfigurator.registerCompileTasks(fragment)
        }
}

abstract class AbstractKotlinGradleVariantWithRuntimeFactory<T : KotlinGradleVariantWithRuntime>(
    module: KotlinGradleModule,
    compileTaskConfigurator: KotlinCompileTaskConfigurator<T>,

    compileDependenciesConfigurationFactory: KotlinCompileDependenciesConfigurationFactory =
        DefaultKotlinCompileDependenciesConfigurationFactory,

    compileDependenciesSetup: KotlinConfigurationsSetup<T> =
        DefaultKotlinCompileDependenciesSetup,

    apiElementsConfigurationFactory: KotlinApiElementsConfigurationFactory =
        DefaultKotlinApiElementsConfigurationFactory,

    apiElementsSetup: KotlinConfigurationsSetup<T> =
        DefaultKotlinApiElementsSetup,

    sourceArchiveTaskConfigurator: KotlinSourceArchiveTaskConfigurator<T> =
        DefaultKotlinSourceArchiveTaskConfigurator,

    protected val runtimeDependenciesConfigurationFactory: KotlinRuntimeDependenciesConfigurationFactory =
        DefaultKotlinRuntimeDependenciesConfigurationFactory,

    private val runtimeDependenciesConfigurationSetup: KotlinConfigurationsSetup<T> =
        DefaultKotlinRuntimeDependenciesSetup,

    protected val runtimeElementsConfigurationFactory: KotlinRuntimeElementsConfigurationFactory =
        DefaultKotlinRuntimeElementsConfigurationFactory,

    private val runtimeElementsConfigurationSetup: KotlinConfigurationsSetup<T> =
        DefaultKotlinRuntimeElementsSetup,

    private val publicationSetup: PublicationSetup<T> = PublicationSetup.NoPublication

) : AbstractKotlinGradleVariantFactory<T>(
    module = module,
    compileTaskConfigurator = compileTaskConfigurator,
    compileDependenciesConfigurationFactory = compileDependenciesConfigurationFactory,
    compileDependenciesSetup = compileDependenciesSetup,
    apiElementsConfigurationFactory = apiElementsConfigurationFactory,
    apiElementsSetup = apiElementsSetup,
    sourceArchiveTaskConfigurator = sourceArchiveTaskConfigurator
) {
    override fun create(name: String): T {
        return super.create(name).also { variant ->
            runtimeDependenciesConfigurationSetup.configure(variant, variant.runtimeDependencyConfiguration)
            runtimeElementsConfigurationSetup.configure(variant, variant.runtimeElementsConfiguration)
            publicationSetup(variant)
        }
    }
}
