/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultCInteropSettings
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeCompileOptions
import org.jetbrains.kotlin.gradle.plugin.mpp.publishedConfigurationName
import org.jetbrains.kotlin.konan.target.KonanTarget
import javax.inject.Inject

open class KotlinNativeVariantInternal(
    containingModule: KotlinGradleModule,
    fragmentName: String,
    val konanTarget: KonanTarget
) : KotlinNativeVariant,
    KotlinGradleVariantInternal(containingModule, fragmentName),
    SingleMavenPublicationHolder by DefaultSingleMavenPublicationHolder(containingModule, fragmentName) {

    override var enableEndorsedLibraries: Boolean = false

    override val gradleVariantNames: Set<String>
        get() = listOf(apiElementsConfigurationName).flatMap { listOf(it, publishedConfigurationName(it)) }.toSet()

    val cinterops = project.container(DefaultCInteropSettings::class.java) { cinteropName ->
        DefaultCInteropSettings(project, cinteropName, compilationData)
    }

    internal val compilationData by lazy { KotlinNativeVariantCompilationData(this) }
}

// FIXME codegen
open class KotlinLinuxX64Variant @Inject constructor(containingModule: KotlinGradleModule, fragmentName: String) :
    KotlinNativeVariantInternal(containingModule, fragmentName, KonanTarget.LINUX_X64)

open class KotlinMacosX64Variant @Inject constructor(containingModule: KotlinGradleModule, fragmentName: String) :
    KotlinNativeVariantInternal(containingModule, fragmentName, KonanTarget.MACOS_X64)

open class KotlinMacosArm64Variant @Inject constructor(containingModule: KotlinGradleModule, fragmentName: String) :
    KotlinNativeVariantInternal(containingModule, fragmentName, KonanTarget.MACOS_ARM64)

open class KotlinIosX64Variant @Inject constructor(containingModule: KotlinGradleModule, fragmentName: String) :
    KotlinNativeVariantInternal(containingModule, fragmentName, KonanTarget.IOS_X64)

open class KotlinIosArm64Variant @Inject constructor(containingModule: KotlinGradleModule, fragmentName: String) :
    KotlinNativeVariantInternal(containingModule, fragmentName, KonanTarget.IOS_ARM64)


internal val KotlinNativeVariant.hostSpecificMetadataConfigurationName
    get() = disambiguateName("MetadataElements")

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

    override val kotlinOptions: KotlinCommonOptions = NativeCompileOptions { languageSettings }
}
