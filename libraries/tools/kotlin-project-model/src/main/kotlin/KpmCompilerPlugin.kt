/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model

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
    fun forMetadataCompilation(fragment: KotlinModuleFragment): PluginData?

    /**
     * Returns [PluginData] when applicable for [fragment] compilation
     * Returns [null] if not applicable
     */
    fun forNativeMetadataCompilation(fragment: KotlinModuleFragment): PluginData?

    /**
     * Returns [PluginData] when applicable for [variant] compilation
     * Returns [null] if not applicable
     */
    fun forPlatformCompilation(variant: KotlinModuleVariant): PluginData?
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

    data class PluginOption(
        val key: String,
        val value: String
    )
}

// TODO: It should be part of "Compilation Process": KotlinModule.compilationRequestFor(METADATA | PLATFORM) -> CompilationRequest
//  But there is no such thing at the moment :)
fun KotlinModuleFragment.metadataCompilationPluginData(): List<PluginData> =
    containingModule
        .plugins
        .mapNotNull { plugin -> plugin.forMetadataCompilation(this) }

fun KotlinModuleFragment.nativeMetadataCompilationPluginData(): List<PluginData> =
    containingModule
        .plugins
        .mapNotNull { plugin -> plugin.forNativeMetadataCompilation(this) }

fun KotlinModuleVariant.platformCompilationPluginData(): List<PluginData> =
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

    protected abstract val pluginOptions: List<PluginData.PluginOption>


    override fun forMetadataCompilation(fragment: KotlinModuleFragment) = pluginDataOrNull(commonPluginArtifact())

    override fun forNativeMetadataCompilation(fragment: KotlinModuleFragment) = pluginDataOrNull(nativePluginArtifact())

    override fun forPlatformCompilation(variant: KotlinModuleVariant) =
        when (variant.platform) {
            KotlinPlatformTypeAttribute.NATIVE -> nativePluginArtifact()
            else -> commonPluginArtifact()
        }.let(::pluginDataOrNull)

    private fun pluginDataOrNull(artifact: PluginData.ArtifactCoordinates?) =
        if (artifact != null) PluginData(pluginId, artifact, pluginOptions)
        else null
}


