/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.*
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinArtifactsExtension.Companion.KOTLIN_NATIVE_ARTIFACTS_DEPRECATION
import org.jetbrains.kotlin.gradle.plugin.mpp.BITCODE_EMBEDDING_DEPRECATION_MESSAGE
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.konan.target.KonanTarget

/**
 * @suppress
 */
@Deprecated(KotlinArtifactsExtension.KOTLIN_NATIVE_ARTIFACTS_DEPRECATION)
interface KotlinArtifact : Named, ExtensionAware {
    val artifactName: String
    val modules: Set<Any>
    val taskName: String
    val outDir: String
    fun registerAssembleTask(project: Project)
}

/**
 * @suppress
 */
@Deprecated(KotlinArtifactsExtension.KOTLIN_NATIVE_ARTIFACTS_DEPRECATION)
interface KotlinNativeArtifact : KotlinArtifact {
    val modes: Set<NativeBuildType>
    val isStatic: Boolean
    val linkerOptions: List<String>
    @Suppress("DEPRECATION_ERROR")
    @Deprecated(
        "Please migrate to toolOptionsConfigure DSL. More details are here: https://kotl.in/u1r8ln"
    )
    val kotlinOptionsFn: KotlinCommonToolOptions.() -> Unit
    val toolOptionsConfigure: KotlinCommonCompilerToolOptions.() -> Unit
    val binaryOptions: Map<String, String>
}

/**
 * @suppress
 */
@Deprecated(KotlinArtifactsExtension.KOTLIN_NATIVE_ARTIFACTS_DEPRECATION)
interface KotlinNativeLibrary : KotlinNativeArtifact {
    val target: KonanTarget
}

/**
 * @suppress
 */
@Deprecated(KotlinArtifactsExtension.KOTLIN_NATIVE_ARTIFACTS_DEPRECATION)
interface KotlinNativeFramework : KotlinNativeArtifact {
    val target: KonanTarget

    @OptIn(InternalKotlinGradlePluginApi::class)
    @Suppress("DEPRECATION_ERROR")
    @Deprecated(BITCODE_EMBEDDING_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
    val embedBitcode: org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode?
}

/**
 * @suppress
 */
@Deprecated(KotlinArtifactsExtension.KOTLIN_NATIVE_ARTIFACTS_DEPRECATION)
interface KotlinNativeFatFramework : KotlinNativeArtifact {
    val targets: Set<KonanTarget>

    @OptIn(InternalKotlinGradlePluginApi::class)
    @Suppress("DEPRECATION_ERROR")
    @Deprecated(BITCODE_EMBEDDING_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
    val embedBitcode: org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode?
}

/**
 * @suppress
 */
@Deprecated(KotlinArtifactsExtension.KOTLIN_NATIVE_ARTIFACTS_DEPRECATION)
interface KotlinNativeXCFramework : KotlinNativeArtifact {
    val targets: Set<KonanTarget>

    @OptIn(InternalKotlinGradlePluginApi::class)
    @Suppress("DEPRECATION_ERROR")
    @Deprecated(BITCODE_EMBEDDING_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
    val embedBitcode: org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode?
}

/**
 * @suppress
 */
@Deprecated(KotlinArtifactsExtension.KOTLIN_NATIVE_ARTIFACTS_DEPRECATION)
interface KotlinArtifactConfig {
    val artifactName: String
    val modules: Set<Any>
    fun setModules(vararg project: Any)
    fun addModule(project: Any)
    fun createArtifact(extensions: ExtensionAware): KotlinArtifact
}

/**
 * @suppress
 */
@Deprecated(KotlinArtifactsExtension.KOTLIN_NATIVE_ARTIFACTS_DEPRECATION)
interface KotlinNativeArtifactConfig : KotlinArtifactConfig {
    var modes: Set<NativeBuildType>
    fun modes(vararg modes: NativeBuildType)
    var isStatic: Boolean
    var linkerOptions: List<String>
    @Deprecated(
        "Please migrate to toolOptions DSL. More details are here: https://kotl.in/u1r8ln"
    )
    fun kotlinOptions(@Suppress("DEPRECATION_ERROR") fn: Action<KotlinCommonToolOptions>)
    fun toolOptions(configure: Action<KotlinCommonCompilerToolOptions>)
    fun binaryOption(name: String, value: String)
}

/**
 * @suppress
 */
@Deprecated(KotlinArtifactsExtension.KOTLIN_NATIVE_ARTIFACTS_DEPRECATION)
interface KotlinNativeLibraryConfig : KotlinNativeArtifactConfig {
    var target: KonanTarget
}

/**
 * @suppress
 */
@Deprecated(KotlinArtifactsExtension.KOTLIN_NATIVE_ARTIFACTS_DEPRECATION)
interface KotlinNativeFrameworkConfig : KotlinNativeArtifactConfig {
    var target: KonanTarget

    @OptIn(InternalKotlinGradlePluginApi::class)
    @Suppress("DEPRECATION_ERROR")
    @Deprecated(BITCODE_EMBEDDING_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
    var embedBitcode: org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode?
}

/**
 * @suppress
 */
@Deprecated(KotlinArtifactsExtension.KOTLIN_NATIVE_ARTIFACTS_DEPRECATION)
interface KotlinNativeFatFrameworkConfig : KotlinNativeArtifactConfig {
    var targets: Set<KonanTarget>
    fun targets(vararg targets: KonanTarget)

    @OptIn(InternalKotlinGradlePluginApi::class)
    @Suppress("DEPRECATION_ERROR")
    @Deprecated(BITCODE_EMBEDDING_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
    var embedBitcode: org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode?
}

/**
 * @suppress
 */
@Deprecated(KotlinArtifactsExtension.KOTLIN_NATIVE_ARTIFACTS_DEPRECATION)
interface KotlinNativeXCFrameworkConfig : KotlinNativeArtifactConfig {
    var targets: Set<KonanTarget>
    fun targets(vararg targets: KonanTarget)

    @OptIn(InternalKotlinGradlePluginApi::class)
    @Suppress("DEPRECATION_ERROR")
    @Deprecated(BITCODE_EMBEDDING_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
    var embedBitcode: org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode?
}

/**
 * @suppress
 */
@Suppress("DEPRECATION")
@Deprecated(KotlinArtifactsExtension.KOTLIN_NATIVE_ARTIFACTS_DEPRECATION)
interface KotlinArtifactsExtension {
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
    val artifactConfigs: DomainObjectSet<KotlinArtifactConfig>
    val artifacts: NamedDomainObjectSet<KotlinArtifact>
    val Native: KotlinNativeArtifactDSL

    companion object {
        const val KOTLIN_NATIVE_ARTIFACTS_DEPRECATION = "kotlinArtifacts DSL is deprecated and will be removed in the future. Please migrate to another way to create Kotlin/Native binaries: https://kotl.in/kotlin-native-artifacts-gradle-dsl"
    }
}

/**
 * @suppress
 */
@Deprecated(KOTLIN_NATIVE_ARTIFACTS_DEPRECATION)
interface KotlinNativeArtifactDSL {
    @Deprecated(KotlinArtifactsExtension.KOTLIN_NATIVE_ARTIFACTS_DEPRECATION)
    fun Library(name: String, configure: Action<KotlinNativeLibraryConfig>)

    @Deprecated(KotlinArtifactsExtension.KOTLIN_NATIVE_ARTIFACTS_DEPRECATION)
    fun Library(configure: Action<KotlinNativeLibraryConfig>)

    @Deprecated(KotlinArtifactsExtension.KOTLIN_NATIVE_ARTIFACTS_DEPRECATION)
    fun Framework(name: String, configure: Action<KotlinNativeFrameworkConfig>)

    @Deprecated(KotlinArtifactsExtension.KOTLIN_NATIVE_ARTIFACTS_DEPRECATION)
    fun Framework(configure: Action<KotlinNativeFrameworkConfig>)

    @Deprecated(KotlinArtifactsExtension.KOTLIN_NATIVE_ARTIFACTS_DEPRECATION)
    fun FatFramework(name: String, configure: Action<KotlinNativeFatFrameworkConfig>)

    @Deprecated(KotlinArtifactsExtension.KOTLIN_NATIVE_ARTIFACTS_DEPRECATION)
    fun FatFramework(configure: Action<KotlinNativeFatFrameworkConfig>)

    @Deprecated(KotlinArtifactsExtension.KOTLIN_NATIVE_ARTIFACTS_DEPRECATION)
    fun XCFramework(name: String, configure: Action<KotlinNativeXCFrameworkConfig>)

    @Deprecated(KotlinArtifactsExtension.KOTLIN_NATIVE_ARTIFACTS_DEPRECATION)
    fun XCFramework(configure: Action<KotlinNativeXCFrameworkConfig>)
}
