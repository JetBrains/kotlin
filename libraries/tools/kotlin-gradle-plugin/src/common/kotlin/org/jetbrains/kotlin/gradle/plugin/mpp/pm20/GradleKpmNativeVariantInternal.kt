/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.artifacts.Configuration
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.sources.kpm.FragmentMappedKotlinSourceSet
import org.jetbrains.kotlin.konan.target.KonanTarget

abstract class GradleKpmNativeVariantInternal(
    containingModule: GradleKpmModule,
    fragmentName: String,
    val konanTarget: KonanTarget,
    dependencyConfigurations: GradleKpmFragmentDependencyConfigurations,
    compileDependencyConfiguration: Configuration,
    apiElementsConfiguration: Configuration,
    final override val hostSpecificMetadataElementsConfiguration: Configuration?
) : GradleKpmNativeVariant,
    GradleKpmVariantInternal(
        containingModule = containingModule,
        fragmentName = fragmentName,
        dependencyConfigurations = dependencyConfigurations,
        compileDependenciesConfiguration = compileDependencyConfiguration,
        apiElementsConfiguration = apiElementsConfiguration
    ),
    GradleKpmSingleMavenPublishedModuleHolder by GradleKpmDefaultSingleMavenPublishedModuleHolder(containingModule, fragmentName) {

    @Deprecated("Please declare explicit dependency on kotlinx-cli. This option is scheduled to be removed in 1.9.0")
    override var enableEndorsedLibraries: Boolean = false

    override val gradleVariantNames: Set<String>
        get() = listOf(apiElementsConfiguration.name).flatMap { listOf(it, publishedConfigurationName(it)) }.toSet()

    val cinterops = project.container(DefaultCInteropSettings::class.java) { cinteropName ->
        project.objects.newInstance(DefaultCInteropSettings::class.java, project, cinteropName, compilationData)
    }

    override val compilationData by lazy { GradleKpmNativeVariantCompilationData(this) }
}

interface KotlinNativeCompilationData<T : KotlinCommonOptions> : KotlinCompilationData<T> {
    val konanTarget: KonanTarget

    @Deprecated("Please declare explicit dependency on kotlinx-cli. This option is scheduled to be removed in 1.9.0")
    val enableEndorsedLibs: Boolean
}

internal class KotlinMappedNativeCompilationFactory(
    target: KotlinNativeTarget,
    private val variantClass: Class<out GradleKpmNativeVariantInternal>
) : KotlinNativeCompilationFactory(target) {
    override fun create(name: String): KotlinNativeCompilation {
        val module = target.project.kpmModules.maybeCreate(name)
        val variant = module.fragments.create(target.name, variantClass)

        return target.project.objects.newInstance(
            KotlinNativeCompilation::class.java,
            target.konanTarget,
            VariantMappedCompilationDetails<KotlinCommonOptions>(
                variant, target, getOrCreateDefaultSourceSet(name) as FragmentMappedKotlinSourceSet
            )
        )
    }
}
