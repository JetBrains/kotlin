/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.artifacts.Configuration
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultCInteropSettings
import org.jetbrains.kotlin.gradle.plugin.mpp.GradleKpmDefaultCInteropSettingsFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.publishedConfigurationName
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

    val cinterops by lazy {
        project.container(
            DefaultCInteropSettings::class.java, GradleKpmDefaultCInteropSettingsFactory(compilationData)
        )
    }

    override val compilationData by lazy { GradleKpmNativeVariantCompilationData(this) }
}

interface GradleKpmNativeCompilationData<T : KotlinCommonOptions> : GradleKpmCompilationData<T> {
    val konanTarget: KonanTarget

    @Deprecated(
        "Please declare explicit dependency on kotlinx-cli. This option has no longer effect since 1.9.0",
        level = DeprecationLevel.ERROR
    )
    val enableEndorsedLibs: Boolean
}
