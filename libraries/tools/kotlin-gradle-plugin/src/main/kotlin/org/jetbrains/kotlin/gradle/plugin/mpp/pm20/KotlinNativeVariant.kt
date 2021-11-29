/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultCInteropSettings
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeCompileOptions
import org.jetbrains.kotlin.gradle.plugin.mpp.publishedConfigurationName
import org.jetbrains.kotlin.konan.target.KonanTarget
import javax.inject.Inject

abstract class KotlinNativeVariantInternal(
    containingModule: KotlinGradleModule,
    fragmentName: String,
    val konanTarget: KonanTarget,
    dependencyConfigurations: KotlinDependencyConfigurations,
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
        dependencyConfigurations: KotlinDependencyConfigurations,
        compileDependencyConfiguration: Configuration,
        apiElementsConfiguration: Configuration,
        hostSpecificMetadataElementsConfiguration: Configuration?
    ) -> T
) {
    operator fun invoke(
        containingModule: KotlinGradleModule,
        fragmentName: String,
        dependencyConfigurations: KotlinDependencyConfigurations,
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

// FIXME codegen
open class KotlinLinuxX64Variant @Inject constructor(
    containingModule: KotlinGradleModule,
    fragmentName: String,
    dependencyConfigurations: KotlinDependencyConfigurations,
    compileDependencyConfiguration: Configuration,
    apiElementsConfiguration: Configuration,
    hostSpecificMetadataElementsConfiguration: Configuration?
) : KotlinNativeVariantInternal(
    containingModule = containingModule,
    fragmentName = fragmentName,
    konanTarget = KonanTarget.LINUX_X64,
    dependencyConfigurations = dependencyConfigurations,
    compileDependencyConfiguration = compileDependencyConfiguration,
    apiElementsConfiguration = apiElementsConfiguration,
    hostSpecificMetadataElementsConfiguration = hostSpecificMetadataElementsConfiguration
) {
    companion object {
        val constructor = KotlinNativeVariantConstructor(
            KonanTarget.LINUX_X64, KotlinLinuxX64Variant::class.java, ::KotlinLinuxX64Variant
        )
    }
}

open class KotlinMacosX64Variant @Inject constructor(
    containingModule: KotlinGradleModule,
    fragmentName: String,
    dependencyConfigurations: KotlinDependencyConfigurations,
    compileDependencyConfiguration: Configuration,
    apiElementsConfiguration: Configuration,
    hostSpecificMetadataElementsConfiguration: Configuration?
) : KotlinNativeVariantInternal(
    containingModule = containingModule,
    fragmentName = fragmentName,
    konanTarget = KonanTarget.MACOS_X64,
    dependencyConfigurations = dependencyConfigurations,
    compileDependencyConfiguration = compileDependencyConfiguration,
    apiElementsConfiguration = apiElementsConfiguration,
    hostSpecificMetadataElementsConfiguration = hostSpecificMetadataElementsConfiguration
) {
    companion object {
        val constructor = KotlinNativeVariantConstructor(
            KonanTarget.MACOS_X64, KotlinMacosX64Variant::class.java, ::KotlinMacosX64Variant
        )
    }
}

open class KotlinMacosArm64Variant @Inject constructor(
    containingModule: KotlinGradleModule, fragmentName: String,
    dependencyConfigurations: KotlinDependencyConfigurations,
    compileDependencyConfiguration: Configuration,
    apiElementsConfiguration: Configuration,
    hostSpecificMetadataElementsConfiguration: Configuration?
) : KotlinNativeVariantInternal(
    containingModule = containingModule,
    fragmentName = fragmentName,
    konanTarget = KonanTarget.MACOS_ARM64,
    dependencyConfigurations = dependencyConfigurations,
    compileDependencyConfiguration = compileDependencyConfiguration,
    apiElementsConfiguration = apiElementsConfiguration,
    hostSpecificMetadataElementsConfiguration = hostSpecificMetadataElementsConfiguration
) {
    companion object {
        val constructor = KotlinNativeVariantConstructor(
            KonanTarget.MACOS_ARM64, KotlinMacosArm64Variant::class.java, ::KotlinMacosArm64Variant
        )
    }
}

open class KotlinIosX64Variant @Inject constructor(
    containingModule: KotlinGradleModule,
    fragmentName: String,
    dependencyConfigurations: KotlinDependencyConfigurations,
    compileDependencyConfiguration: Configuration,
    apiElementsConfiguration: Configuration,
    hostSpecificMetadataElementsConfiguration: Configuration?
) : KotlinNativeVariantInternal(
    containingModule = containingModule,
    fragmentName = fragmentName,
    konanTarget = KonanTarget.IOS_X64,
    dependencyConfigurations = dependencyConfigurations,
    compileDependencyConfiguration = compileDependencyConfiguration,
    apiElementsConfiguration = apiElementsConfiguration,
    hostSpecificMetadataElementsConfiguration = hostSpecificMetadataElementsConfiguration
) {
    companion object {
        val constructor = KotlinNativeVariantConstructor(
            KonanTarget.IOS_X64, KotlinIosX64Variant::class.java, ::KotlinIosX64Variant
        )
    }
}

open class KotlinIosArm64Variant @Inject constructor(
    containingModule: KotlinGradleModule,
    fragmentName: String,
    dependencyConfigurations: KotlinDependencyConfigurations,
    compileDependencyConfiguration: Configuration,
    apiElementsConfiguration: Configuration,
    hostSpecificMetadataElementsConfiguration: Configuration?
) : KotlinNativeVariantInternal(
    containingModule = containingModule,
    fragmentName = fragmentName,
    konanTarget = KonanTarget.IOS_ARM64,
    dependencyConfigurations = dependencyConfigurations,
    compileDependencyConfiguration = compileDependencyConfiguration,
    apiElementsConfiguration = apiElementsConfiguration,
    hostSpecificMetadataElementsConfiguration = hostSpecificMetadataElementsConfiguration
) {
    companion object {
        val constructor = KotlinNativeVariantConstructor(
            KonanTarget.IOS_ARM64, KotlinIosArm64Variant::class.java, ::KotlinIosArm64Variant
        )
    }
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
