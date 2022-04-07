/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks.artifact

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
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
    fun Library(name: String, configure: Action<KotlinNativeLibraryConfig>) {
        addKotlinArtifact(name, configure)
    }

    @ExperimentalArtifactDsl
    fun Library(configure: Action<KotlinNativeLibraryConfig>) {
        addKotlinArtifact(configure)
    }

    @ExperimentalArtifactDsl
    fun Framework(name: String, configure: Action<KotlinNativeFrameworkConfig>) {
        addKotlinArtifact(name, configure)
    }

    @ExperimentalArtifactDsl
    fun Framework(configure: Action<KotlinNativeFrameworkConfig>) {
        addKotlinArtifact(configure)
    }

    @ExperimentalArtifactDsl
    fun FatFramework(name: String, configure: Action<KotlinNativeFatFrameworkConfig>) {
        addKotlinArtifact(name, configure)
    }

    @ExperimentalArtifactDsl
    fun FatFramework(configure: Action<KotlinNativeFatFrameworkConfig>) {
        addKotlinArtifact(configure)
    }

    @ExperimentalArtifactDsl
    fun XCFramework(name: String, configure: Action<KotlinNativeXCFrameworkConfig>) {
        addKotlinArtifact(name, configure)
    }

    @ExperimentalArtifactDsl
    fun XCFramework(configure: Action<KotlinNativeXCFrameworkConfig>) {
        addKotlinArtifact(configure)
    }

    private inline fun <reified T : KotlinArtifactConfig> addKotlinArtifact(configure: Action<T>) {
        addKotlinArtifact(project.name.replace(UNSAFE_NAME_SYMBOLS, "_"), configure)
    }

    private inline fun <reified T : KotlinArtifactConfig> addKotlinArtifact(name: String, configure: Action<T>) {
        //create via newInstance for extensibility
        val config: T = project.objects.newInstance(T::class.java, name)
        project.kotlinArtifactsExtension.artifactConfigs.add(config)

        //current project is added by default
        config.addModule(project)

        //apply user configuration
        configure.execute(config)
        //create immutable artifact object
        val artifact = config.createArtifact(project, config as ExtensionAware)

        val isAdded = project.kotlinArtifactsExtension.artifacts.add(artifact)
        if (!isAdded) {
            error("Kotlin artifact with name '${artifact.name}' is already exists! Change the name, please!")
        }
    }
}

//Groovy script DSL
private const val KOTLIN_ARTIFACTS_EXTENSION_NAME = "kotlinArtifacts"
internal fun Project.registerKotlinArtifactsExtension() {
    val kotlinArtifactsExt = objects.newInstance(KotlinArtifactsExtension::class.java, this)
    extensions.add(KOTLIN_ARTIFACTS_EXTENSION_NAME, kotlinArtifactsExt)
    kotlinArtifactsExt.artifacts.all { it.registerAssembleTask() }
}

internal val Project.kotlinArtifactsExtension: KotlinArtifactsExtension
    get() = extensions.getByName(KOTLIN_ARTIFACTS_EXTENSION_NAME).castIsolatedKotlinPluginClassLoaderAware()

abstract class KotlinArtifactsExtension @Inject constructor(project: Project) {
    //Extending by external plugins:
    //
    //project.kotlinArtifactsExtension.apply {
    //    artifactConfigs.all {
    //      //add custom extension to artifact config DSL
    //      (it as ExtensionAware).extensions.create("myConfig", Config::class.java)
    //    }
    //    artifacts.all {
    //      val config = it.extensions.findByName("myConfig") as Config
    //      //configure additional tasks, etc
    //      //here we can use artifact parameters
    //    }
    //}
    val artifactConfigs = project.objects.domainObjectSet(KotlinArtifactConfig::class.java)
    val artifacts = project.objects.namedDomainObjectSet(KotlinArtifact::class.java)

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