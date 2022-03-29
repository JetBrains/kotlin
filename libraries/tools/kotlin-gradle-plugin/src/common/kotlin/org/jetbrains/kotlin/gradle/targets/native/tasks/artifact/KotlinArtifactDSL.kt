/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks.artifact

import org.gradle.api.Action
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.utils.castIsolatedKotlinPluginClassLoaderAware
import org.jetbrains.kotlin.konan.target.KonanTarget
import javax.inject.Inject

//Groovy script DSL
abstract class NativeArtifacts @Inject constructor(private val project: Project) {
    companion object {
        private val UNSAFE_NAME_SYMBOLS = """\W""".toRegex()
    }

    @RequiresOptIn(
        message = "This API is experimental. It may be changed in the future.",
        level = RequiresOptIn.Level.WARNING
    )
    @Retention(AnnotationRetention.BINARY)
    @Target(AnnotationTarget.FUNCTION)
    annotation class ExperimentalArtifactDsl

    @ExperimentalArtifactDsl
    fun Library(name: String, configure: Action<KotlinNativeLibrary>) {
        addKotlinArtifact(name, configure)
    }

    @ExperimentalArtifactDsl
    fun Library(configure: Action<KotlinNativeLibrary>) {
        addKotlinArtifact(configure)
    }

    @ExperimentalArtifactDsl
    fun Framework(name: String, configure: Action<KotlinNativeFramework>) {
        addKotlinArtifact(name, configure)
    }

    @ExperimentalArtifactDsl
    fun Framework(configure: Action<KotlinNativeFramework>) {
        addKotlinArtifact(configure)
    }

    @ExperimentalArtifactDsl
    fun FatFramework(name: String, configure: Action<KotlinNativeFatFramework>) {
        addKotlinArtifact(name, configure)
    }

    @ExperimentalArtifactDsl
    fun FatFramework(configure: Action<KotlinNativeFatFramework>) {
        addKotlinArtifact(configure)
    }

    @ExperimentalArtifactDsl
    fun XCFramework(name: String, configure: Action<KotlinNativeXCFramework>) {
        addKotlinArtifact(name, configure)
    }

    @ExperimentalArtifactDsl
    fun XCFramework(configure: Action<KotlinNativeXCFramework>) {
        addKotlinArtifact(configure)
    }

    private inline fun <reified T : KotlinArtifact> addKotlinArtifact(configure: Action<T>) {
        addKotlinArtifact(project.name.replace(UNSAFE_NAME_SYMBOLS, "_"), configure)
    }

    private inline fun <reified T : KotlinArtifact> addKotlinArtifact(name: String, configure: Action<T>) {
        //create via newInstance for extensibility
        val artifact = project.objects.newInstance(T::class.java, project, name)

        //we should add artifact to collection BEFORE configuration
        //because other plugins can add extensions for artifacts which will be used in configuration block
        project.kotlinArtifactsExtension.artifacts.add(artifact)

        artifact.addModule(project)
        configure.execute(artifact)
        artifact.registerAssembleTask()
    }
}

//Groovy script DSL
private const val KOTLIN_ARTIFACTS_EXTENSION_NAME = "kotlinArtifacts"
internal fun Project.registerKotlinArtifactsExtension() {
    extensions.create(KOTLIN_ARTIFACTS_EXTENSION_NAME, KotlinArtifactsExtension::class.java, this)
}

internal val Project.kotlinArtifactsExtension: KotlinArtifactsExtension
    get() = extensions.getByName(KOTLIN_ARTIFACTS_EXTENSION_NAME).castIsolatedKotlinPluginClassLoaderAware()

abstract class KotlinArtifactsExtension @Inject constructor(project: Project) {
    val artifacts = project.objects.domainObjectSet(KotlinArtifact::class.java)

    val DEBUG = NativeBuildType.DEBUG
    val RELEASE = NativeBuildType.RELEASE

    class BitcodeEmbeddingModeDsl {
        val DISABLE = org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode.DISABLE
        val BITCODE = org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode.BITCODE
        val MARKER = org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode.MARKER
    }

    @JvmField
    val Native = project.objects.newInstance(NativeArtifacts::class.java, project)

    @JvmField
    val EmbedBitcodeMode = BitcodeEmbeddingModeDsl()

    val androidX64 = KonanTarget.ANDROID_X64
    val androidX86 = KonanTarget.ANDROID_X86
    val androidArm32 = KonanTarget.ANDROID_ARM32
    val androidArm64 = KonanTarget.ANDROID_ARM64
    val iosArm32 = KonanTarget.IOS_ARM32
    val iosArm64 = KonanTarget.IOS_ARM64
    val iosX64 = KonanTarget.IOS_X64
    val iosSimulatorArm64 = KonanTarget.IOS_SIMULATOR_ARM64
    val watchosArm32 = KonanTarget.WATCHOS_ARM32
    val watchosArm64 = KonanTarget.WATCHOS_ARM64
    val watchosX86 = KonanTarget.WATCHOS_X86
    val watchosX64 = KonanTarget.WATCHOS_X64
    val watchosSimulatorArm64 = KonanTarget.WATCHOS_SIMULATOR_ARM64
    val tvosArm64 = KonanTarget.TVOS_ARM64
    val tvosX64 = KonanTarget.TVOS_X64
    val tvosSimulatorArm64 = KonanTarget.TVOS_SIMULATOR_ARM64
    val linuxX64 = KonanTarget.LINUX_X64
    val mingwX86 = KonanTarget.MINGW_X86
    val mingwX64 = KonanTarget.MINGW_X64
    val macosX64 = KonanTarget.MACOS_X64
    val macosArm64 = KonanTarget.MACOS_ARM64
    val linuxArm64 = KonanTarget.LINUX_ARM64
    val linuxArm32Hfp = KonanTarget.LINUX_ARM32_HFP
    val linuxMips32 = KonanTarget.LINUX_MIPS32
    val linuxMipsel32 = KonanTarget.LINUX_MIPSEL32
    val wasm32 = KonanTarget.WASM32
}