/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model

import java.io.File

/**
 * Adapts Kotlin Compiler Plugin for Multiplatform Kotlin Project Model
 * Build System uses this interface to identify applicable plugin artifacts and its options
 * before executing actual Kotlin Compilation
 */
interface KpmCompilerPlugin {

    /**
     * Returns [PluginData] when applicable for [fragment] compilation
     * Returns [null] if not applicable
     */
    fun forMetadataCompilation(fragment: KpmFragment): PluginData?

    /**
     * Returns [PluginData] when applicable for [fragment] compilation
     * Returns [null] if not applicable
     */
    fun forNativeMetadataCompilation(fragment: KpmFragment): PluginData?

    /**
     * Returns [PluginData] when applicable for [variant] compilation
     * Returns [null] if not applicable
     */
    fun forPlatformCompilation(variant: KpmVariant): PluginData?
}

/**
 * Plugin data can be used for changing some compilation request
 */
data class PluginData(
    val pluginId: String,
    val artifact: ArtifactCoordinates,
    val options: List<PluginOption>
) {
    // FIXME: (?) Is it common thing or gradle/maven centric?
    data class ArtifactCoordinates(
        val group: String,
        val artifact: String,
        val version: String? = null
    )
}

sealed class PluginOption {
    abstract val key: String

    /**
     * Indicates whether value of [PluginOption] should be stored for incremental build checks.
     * Value changes of non-transient [PluginOption] will invalidate incremental caches.
     */
    abstract val isTransient: Boolean
}

data class StringOption(
    override val key: String,
    val value: String,
    override val isTransient: Boolean = false
) : PluginOption()

data class FilesOption(
    override val key: String,
    val files: List<File>,
    /**
     * Indicates whether FilesOption is used as input or output during compilation
     * false means input
     * true means output
     */
    val isOutput: Boolean = false,
    override val isTransient: Boolean = false
) : PluginOption()

// TODO: It should be part of "Compilation Process": KotlinModule.compilationRequestFor(METADATA | PLATFORM) -> CompilationRequest
//  But there is no such thing at the moment :)
fun KpmFragment.metadataCompilationPluginData(): List<PluginData> =
    containingModule
        .plugins
        .mapNotNull { plugin -> plugin.forMetadataCompilation(this) }

fun KpmFragment.nativeMetadataCompilationPluginData(): List<PluginData> =
    containingModule
        .plugins
        .mapNotNull { plugin -> plugin.forNativeMetadataCompilation(this) }

fun KpmVariant.platformCompilationPluginData(): List<PluginData> =
    containingModule
        .plugins
        .mapNotNull { plugin -> plugin.forPlatformCompilation(this) }

/**
 * Represents trivial Compiler Plugin adapter for Kotlin Project Model
 * Where Compiler Plugin can have common and native artifacts
 */
abstract class BasicKpmCompilerPlugin : KpmCompilerPlugin {

    abstract val pluginId: String

    protected abstract fun commonPluginArtifact(): PluginData.ArtifactCoordinates?

    protected abstract fun nativePluginArtifact(): PluginData.ArtifactCoordinates?

    protected abstract val pluginOptions: List<PluginOption>

    override fun forMetadataCompilation(fragment: KpmFragment) = pluginDataOrNull(commonPluginArtifact())

    override fun forNativeMetadataCompilation(fragment: KpmFragment) = pluginDataOrNull(nativePluginArtifact())

    override fun forPlatformCompilation(variant: KpmVariant) =
        when (variant.platform) {
            KotlinPlatformTypeAttribute.NATIVE -> nativePluginArtifact()
            else -> commonPluginArtifact()
        }.let(::pluginDataOrNull)

    private fun pluginDataOrNull(artifact: PluginData.ArtifactCoordinates?) =
        if (artifact != null) PluginData(pluginId, artifact, pluginOptions)
        else null
}


