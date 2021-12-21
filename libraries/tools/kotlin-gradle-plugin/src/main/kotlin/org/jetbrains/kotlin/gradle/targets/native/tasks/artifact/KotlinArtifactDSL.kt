/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks.artifact

import groovy.lang.Closure
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.konan.target.KonanTarget
import javax.inject.Inject

open class ArtifactType<T : KotlinArtifact>(internal val create: () -> T)
object Native {
    val Library = object : ArtifactType<KotlinNativeLibrary>({ KotlinNativeLibrary() }) {}
    val Framework = object : ArtifactType<KotlinNativeFramework>({ KotlinNativeFramework() }) {}
    val FatFramework = object : ArtifactType<KotlinNativeFatFramework>({ KotlinNativeFatFramework() }) {}
    val XCFramework = object : ArtifactType<KotlinNativeXCFramework>({ KotlinNativeXCFramework() }) {}
}

@RequiresOptIn(
    message = "This API is experimental. It may be changed in the future.",
    level = RequiresOptIn.Level.WARNING
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class ExperimentalArtifactDsl

private val SAFE_NAME_PATTERN = """\W""".toRegex()

@ExperimentalArtifactDsl
fun <T : KotlinArtifact> Project.kotlinArtifact(type: ArtifactType<T>, configure: T.() -> Unit) =
    kotlinArtifact(name.replace(SAFE_NAME_PATTERN, "_"), type, configure)

@ExperimentalArtifactDsl
fun <T : KotlinArtifact> Project.kotlinArtifact(name: String, type: ArtifactType<T>, configure: T.() -> Unit) {
    val artifact = type.create()
    artifact.addModule(this)
    configure(artifact)
    if (artifact.validate(this, name)) {
        artifact.registerAssembleTask(this, name)
    }
}

//Groovy script DSL
class BitcodeEmbeddingModeDsl {
    val DISABLE = org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode.DISABLE
    val BITCODE = org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode.BITCODE
    val MARKER = org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode.MARKER
}

@OptIn(ExperimentalArtifactDsl::class)
class NativeArtifacts(private val project: Project) {
    fun Library(name: String, configure: KotlinNativeLibrary.() -> Unit) {
        project.kotlinArtifact(name, Native.Library, configure)
    }

    fun Library(configure: KotlinNativeLibrary.() -> Unit) {
        project.kotlinArtifact(Native.Library, configure)
    }

    fun Framework(name: String, configure: KotlinNativeFramework.() -> Unit) {
        project.kotlinArtifact(name, Native.Framework, configure)
    }

    fun Framework(configure: KotlinNativeFramework.() -> Unit) {
        project.kotlinArtifact(Native.Framework, configure)
    }

    fun FatFramework(name: String, configure: KotlinNativeFatFramework.() -> Unit) {
        project.kotlinArtifact(name, Native.FatFramework, configure)
    }

    fun FatFramework(configure: KotlinNativeFatFramework.() -> Unit) {
        project.kotlinArtifact(Native.FatFramework, configure)
    }

    fun XCFramework(name: String, configure: KotlinNativeXCFramework.() -> Unit) {
        project.kotlinArtifact(name, Native.XCFramework, configure)
    }

    fun XCFramework(configure: KotlinNativeXCFramework.() -> Unit) {
        project.kotlinArtifact(Native.XCFramework, configure)
    }

    fun Library(name: String, configure: Closure<KotlinNativeLibrary>) {
        project.kotlinArtifact(name, Native.Library) {
            configure.delegate = this
            configure.call()
        }
    }

    fun Library(configure: Closure<KotlinNativeLibrary>) {
        project.kotlinArtifact(Native.Library) {
            configure.delegate = this
            configure.call()
        }
    }

    fun Framework(name: String, configure: Closure<KotlinNativeFramework>) {
        project.kotlinArtifact(name, Native.Framework) {
            configure.delegate = this
            configure.call()
        }
    }

    fun Framework(configure: Closure<KotlinNativeFramework>) {
        project.kotlinArtifact(Native.Framework) {
            configure.delegate = this
            configure.call()
        }
    }

    fun FatFramework(name: String, configure: Closure<KotlinNativeFatFramework>) {
        project.kotlinArtifact(name, Native.FatFramework) {
            configure.delegate = this
            configure.call()
        }
    }

    fun FatFramework(configure: Closure<KotlinNativeFatFramework>) {
        project.kotlinArtifact(Native.FatFramework) {
            configure.delegate = this
            configure.call()
        }
    }

    fun XCFramework(name: String, configure: Closure<KotlinNativeXCFramework>) {
        project.kotlinArtifact(name, Native.XCFramework) {
            configure.delegate = this
            configure.call()
        }
    }

    fun XCFramework(configure: Closure<KotlinNativeXCFramework>) {
        project.kotlinArtifact(Native.XCFramework) {
            configure.delegate = this
            configure.call()
        }
    }
}

//Groovy script DSL
internal fun Project.registerKotlinArtifactsExtension() {
    extensions.create("kotlinArtifacts", KotlinArtifactsExtension::class.java, this)
}

//Groovy script DSL
abstract class KotlinArtifactsExtension @Inject constructor(private val project: Project) {
    @JvmField
    val DEBUG = NativeBuildType.DEBUG

    @JvmField
    val RELEASE = NativeBuildType.RELEASE

    @OptIn(ExperimentalArtifactDsl::class)
    @JvmField
    val Native = NativeArtifacts(project)

    @JvmField
    val EmbedBitcodeMode = BitcodeEmbeddingModeDsl()

    @JvmField
    val androidX64 = KonanTarget.ANDROID_X64

    @JvmField
    val androidX86 = KonanTarget.ANDROID_X86

    @JvmField
    val androidArm32 = KonanTarget.ANDROID_ARM32

    @JvmField
    val androidArm64 = KonanTarget.ANDROID_ARM64

    @JvmField
    val iosArm32 = KonanTarget.IOS_ARM32

    @JvmField
    val iosArm64 = KonanTarget.IOS_ARM64

    @JvmField
    val iosX64 = KonanTarget.IOS_X64

    @JvmField
    val iosSimulatorArm64 = KonanTarget.IOS_SIMULATOR_ARM64

    @JvmField
    val watchosArm32 = KonanTarget.WATCHOS_ARM32

    @JvmField
    val watchosArm64 = KonanTarget.WATCHOS_ARM64

    @JvmField
    val watchosX86 = KonanTarget.WATCHOS_X86

    @JvmField
    val watchosX64 = KonanTarget.WATCHOS_X64

    @JvmField
    val watchosSimulatorArm64 = KonanTarget.WATCHOS_SIMULATOR_ARM64

    @JvmField
    val tvosArm64 = KonanTarget.TVOS_ARM64

    @JvmField
    val tvosX64 = KonanTarget.TVOS_X64

    @JvmField
    val tvosSimulatorArm64 = KonanTarget.TVOS_SIMULATOR_ARM64

    @JvmField
    val linuxX64 = KonanTarget.LINUX_X64

    @JvmField
    val mingwX86 = KonanTarget.MINGW_X86

    @JvmField
    val mingwX64 = KonanTarget.MINGW_X64

    @JvmField
    val macosX64 = KonanTarget.MACOS_X64

    @JvmField
    val macosArm64 = KonanTarget.MACOS_ARM64

    @JvmField
    val linuxArm64 = KonanTarget.LINUX_ARM64

    @JvmField
    val linuxArm32Hfp = KonanTarget.LINUX_ARM32_HFP

    @JvmField
    val linuxMips32 = KonanTarget.LINUX_MIPS32

    @JvmField
    val linuxMipsel32 = KonanTarget.LINUX_MIPSEL32

    @JvmField
    val wasm32 = KonanTarget.WASM32
}