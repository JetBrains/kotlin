/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.konan

import org.jetbrains.kotlin.library.KotlinLibrary

internal interface NativeManifestDataProvider {
    fun getManifest(libraryName: String): NativeSensitiveManifestData
}

/**
 * A separate Kotlin/Native library from the distribution.
 */
internal class NativeDistributionLibrary(
    val library: KotlinLibrary
) {
    val manifestData = NativeSensitiveManifestData.readFrom(library)
}

/**
 * A collection of Kotlin/Native libraries for a certain Native target + stdlib from the distribution.
 */
internal class NativeDistributionLibraries(
    val stdlib: NativeDistributionLibrary,
    val platformLibs: List<NativeDistributionLibrary>
) : NativeManifestDataProvider {
    constructor(stdlib: KotlinLibrary, platformLibs: List<KotlinLibrary>) : this(
        NativeDistributionLibrary(stdlib),
        platformLibs.map(::NativeDistributionLibrary)
    )

    private val manifestIndex: Map<String, NativeSensitiveManifestData> = buildManifestIndex()

    override fun getManifest(libraryName: String) = manifestIndex.getValue(libraryName)
}

internal class CommonNativeManifestDataProvider(
    libraryGroups: Collection<NativeDistributionLibraries>
) : NativeManifestDataProvider {
    private val manifestIndex: Map<String, NativeSensitiveManifestData>

    init {
        val iterator = libraryGroups.iterator()
        val index = iterator.next().buildManifestIndex()

        while (iterator.hasNext()) {
            val otherIndex = iterator.next().buildManifestIndex()
            otherIndex.forEach { (libraryName, otherManifestData) ->
                val manifestData = index[libraryName]
                if (manifestData != null) {
                    // merge manifests
                    index[libraryName] = manifestData.mergeWith(otherManifestData)
                } else {
                    index[libraryName] = otherManifestData
                }
            }
        }

        manifestIndex = index
    }

    override fun getManifest(libraryName: String) = manifestIndex.getValue(libraryName)
}

private fun NativeDistributionLibraries.buildManifestIndex(): MutableMap<String, NativeSensitiveManifestData> =
    (platformLibs + stdlib).map { it.manifestData }.associateByTo(HashMap()) { it.uniqueName }
