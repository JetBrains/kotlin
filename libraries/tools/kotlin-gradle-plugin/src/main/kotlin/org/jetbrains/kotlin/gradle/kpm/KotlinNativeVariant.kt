/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeCompileOptions
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.plugin.mpp.publishedConfigurationName
import org.jetbrains.kotlin.konan.target.KonanTarget

abstract class KotlinNativeVariantInternal(
    containingModule: KotlinGradleModule,
    fragmentName: String,
    val konanTarget: KonanTarget,
    dependencyConfigurations: KotlinFragmentDependencyConfigurations,
    compileDependencyConfiguration: Configuration,
    apiElementsConfiguration: Configuration,
    final override val hostSpecificMetadataElementsConfiguration: Configuration?
) : KotlinNativeVariant,
    KotlinGradleVariantInternal(
        containingModule = containingModule,
        fragmentName = fragmentName,
        dependencyConfigurations = dependencyConfigurations,
        compileDependenciesConfiguration = compileDependencyConfiguration,
        apiElementsConfiguration = apiElementsConfiguration
    ),
    SingleMavenPublishedModuleHolder by DefaultSingleMavenPublishedModuleHolder(containingModule, fragmentName) {

    override var enableEndorsedLibraries: Boolean = false

    override val gradleVariantNames: Set<String>
        get() = listOf(apiElementsConfiguration.name).flatMap { listOf(it, publishedConfigurationName(it)) }.toSet()

    val cinterops = project.container(DefaultCInteropSettings::class.java) { cinteropName ->
        DefaultCInteropSettings(project, cinteropName, compilationData)
    }

    override val compilationData by lazy { KotlinNativeVariantCompilationData(this) }
}

class KotlinNativeVariantConstructor<T : KotlinNativeVariantInternal>(
    val konanTarget: KonanTarget,
    val variantClass: Class<T>,
    private val constructor: (
        containingModule: KotlinGradleModule,
        fragmentName: String,
        dependencyConfigurations: KotlinFragmentDependencyConfigurations,
        compileDependencyConfiguration: Configuration,
        apiElementsConfiguration: Configuration,
        hostSpecificMetadataElementsConfiguration: Configuration?
    ) -> T
) {
    operator fun invoke(
        containingModule: KotlinGradleModule,
        fragmentName: String,
        dependencyConfigurations: KotlinFragmentDependencyConfigurations,
        compileDependencyConfiguration: Configuration,
        apiElementsConfiguration: Configuration,
        hostSpecificMetadataElementsConfiguration: Configuration?
    ): T = constructor(
        containingModule, fragmentName,
        dependencyConfigurations,
        compileDependencyConfiguration,
        apiElementsConfiguration,
        hostSpecificMetadataElementsConfiguration
    )
}

interface KotlinNativeCompilationData<T : KotlinCommonOptions> : KotlinCompilationData<T> {
    val konanTarget: KonanTarget
    val enableEndorsedLibs: Boolean
}

internal class KotlinNativeVariantCompilationData(
    val variant: KotlinNativeVariantInternal
) : KotlinVariantCompilationDataInternal<KotlinCommonOptions>, KotlinNativeCompilationData<KotlinCommonOptions> {
    override val konanTarget: KonanTarget
        get() = variant.konanTarget

    override val enableEndorsedLibs: Boolean
        get() = variant.enableEndorsedLibraries

    override val project: Project
        get() = variant.containingModule.project

    override val owner: KotlinNativeVariant
        get() = variant

    override val kotlinOptions: KotlinCommonOptions = NativeCompileOptions { variant.languageSettings }
}

internal class KotlinMappedNativeCompilationFactory(
    target: KotlinNativeTarget,
    private val variantClass: Class<out KotlinNativeVariantInternal>
) : KotlinNativeCompilationFactory(target) {
    override fun create(name: String): KotlinNativeCompilation {
        val module = target.project.kpmModules.maybeCreate(name)
        val variant = module.fragments.create(target.name, variantClass)

        return KotlinNativeCompilation(
            target.konanTarget,
            VariantMappedCompilationDetails(variant, target)
        )
    }
}