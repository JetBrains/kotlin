/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.konan

import com.intellij.util.containers.FactoryMap
import gnu.trove.THashMap
import org.jetbrains.kotlin.descriptors.commonizer.CommonizerTarget
import org.jetbrains.kotlin.descriptors.commonizer.LeafCommonizerTarget
import org.jetbrains.kotlin.descriptors.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.library.KotlinLibrary

fun interface TargetedNativeManifestDataProvider {
    fun getManifest(target: CommonizerTarget, libraryName: String): NativeSensitiveManifestData
}

internal interface NativeManifestDataProvider {
    fun getManifest(libraryName: String): NativeSensitiveManifestData
}

internal fun TargetedNativeManifestDataProvider(libraries: AllNativeLibraries): TargetedNativeManifestDataProvider {
    val cachedManifestProviders: Map<CommonizerTarget, NativeManifestDataProvider> = FactoryMap.create { target ->
        when (target) {
            is LeafCommonizerTarget -> libraries.librariesByTargets.getValue(target)
            is SharedCommonizerTarget -> CommonNativeManifestDataProvider(libraries.librariesByTargets.values)
        }
    }
    return TargetedNativeManifestDataProvider { target, libraryName ->
        cachedManifestProviders.getValue(target).getManifest(libraryName)
    }
}

/**
 * A separate Kotlin/Native library.
 */
internal class NativeLibrary(
    val library: KotlinLibrary
) {
    val manifestData = NativeSensitiveManifestData.readFrom(library)
}

/**
 * A collection of Kotlin/Native libraries for a certain Native target.
 */
internal class NativeLibrariesToCommonize(val libraries: List<NativeLibrary>) : NativeManifestDataProvider {
    private val manifestIndex: Map<String, NativeSensitiveManifestData> = buildManifestIndex()

    override fun getManifest(libraryName: String) = manifestIndex.getValue(libraryName)

    companion object {
        fun create(libraries: List<KotlinLibrary>) = NativeLibrariesToCommonize(libraries.map(::NativeLibrary))
    }
}

internal class AllNativeLibraries(
    val stdlib: NativeLibrary,
    val librariesByTargets: Map<LeafCommonizerTarget, NativeLibrariesToCommonize>
)

internal class CommonNativeManifestDataProvider(
    libraryGroups: Collection<NativeLibrariesToCommonize>
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

private fun NativeLibrariesToCommonize.buildManifestIndex(): MutableMap<String, NativeSensitiveManifestData> =
    libraries.map { it.manifestData }.associateByTo(THashMap()) { it.uniqueName }
