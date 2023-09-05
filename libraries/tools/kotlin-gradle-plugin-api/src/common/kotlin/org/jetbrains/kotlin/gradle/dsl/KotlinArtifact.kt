/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.*
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.konan.target.KonanTarget

/**
 * @suppress
 */
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
interface KotlinNativeArtifact : KotlinArtifact {
    val modes: Set<NativeBuildType>
    val isStatic: Boolean
    val linkerOptions: List<String>
    val kotlinOptionsFn: KotlinCommonToolOptions.() -> Unit
    val toolOptionsConfigure: KotlinCommonCompilerToolOptions.() -> Unit
    val binaryOptions: Map<String, String>
}

/**
 * @suppress
 */
interface KotlinNativeLibrary : KotlinNativeArtifact {
    val target: KonanTarget
}

/**
 * @suppress
 */
interface KotlinNativeFramework : KotlinNativeArtifact {
    val target: KonanTarget
    val embedBitcode: BitcodeEmbeddingMode?
}

/**
 * @suppress
 */
interface KotlinNativeFatFramework : KotlinNativeArtifact {
    val targets: Set<KonanTarget>
    val embedBitcode: BitcodeEmbeddingMode?
}

/**
 * @suppress
 */
interface KotlinNativeXCFramework : KotlinNativeArtifact {
    val targets: Set<KonanTarget>
    val embedBitcode: BitcodeEmbeddingMode?
}

/**
 * @suppress
 */
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
interface KotlinNativeArtifactConfig : KotlinArtifactConfig {
    var modes: Set<NativeBuildType>
    fun modes(vararg modes: NativeBuildType)
    var isStatic: Boolean
    var linkerOptions: List<String>
    fun kotlinOptions(fn: Action<KotlinCommonToolOptions>)
    fun toolOptions(configure: Action<KotlinCommonCompilerToolOptions>)
    fun binaryOption(name: String, value: String)
}

/**
 * @suppress
 */
interface KotlinNativeLibraryConfig : KotlinNativeArtifactConfig {
    var target: KonanTarget
}

/**
 * @suppress
 */
interface KotlinNativeFrameworkConfig : KotlinNativeArtifactConfig {
    var target: KonanTarget
    var embedBitcode: BitcodeEmbeddingMode?
}

/**
 * @suppress
 */
interface KotlinNativeFatFrameworkConfig : KotlinNativeArtifactConfig {
    var targets: Set<KonanTarget>
    fun targets(vararg targets: KonanTarget)
    var embedBitcode: BitcodeEmbeddingMode?
}

/**
 * @suppress
 */
interface KotlinNativeXCFrameworkConfig : KotlinNativeArtifactConfig {
    var targets: Set<KonanTarget>
    fun targets(vararg targets: KonanTarget)
    var embedBitcode: BitcodeEmbeddingMode?
}

/**
 * @suppress
 */
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
}

/**
 * @suppress
 */
interface KotlinNativeArtifactDSL {
    @RequiresOptIn(
        message = "This API is experimental. It may be changed in the future.",
        level = RequiresOptIn.Level.WARNING
    )
    @Retention(AnnotationRetention.BINARY)
    @Target(AnnotationTarget.FUNCTION)
    annotation class ExperimentalArtifactDsl

    @ExperimentalArtifactDsl
    fun Library(name: String, configure: Action<KotlinNativeLibraryConfig>)

    @ExperimentalArtifactDsl
    fun Library(configure: Action<KotlinNativeLibraryConfig>)

    @ExperimentalArtifactDsl
    fun Framework(name: String, configure: Action<KotlinNativeFrameworkConfig>)

    @ExperimentalArtifactDsl
    fun Framework(configure: Action<KotlinNativeFrameworkConfig>)

    @ExperimentalArtifactDsl
    fun FatFramework(name: String, configure: Action<KotlinNativeFatFrameworkConfig>)

    @ExperimentalArtifactDsl
    fun FatFramework(configure: Action<KotlinNativeFatFrameworkConfig>)

    @ExperimentalArtifactDsl
    fun XCFramework(name: String, configure: Action<KotlinNativeXCFrameworkConfig>)

    @ExperimentalArtifactDsl
    fun XCFramework(configure: Action<KotlinNativeXCFrameworkConfig>)
}