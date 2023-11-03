/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.commonizer.konan

import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.commonizer.UniqueLibraryName
import org.jetbrains.kotlin.commonizer.utils.CommonizerMap
import org.jetbrains.kotlin.library.KotlinLibrary

interface NativeManifestDataProvider {
    fun buildManifest(libraryName: UniqueLibraryName): NativeSensitiveManifestData
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
internal class NativeLibrariesToCommonize(
    private val target: CommonizerTarget,
    val libraries: List<NativeLibrary>
) : NativeManifestDataProvider {
    private val manifestIndex: Map<String, NativeSensitiveManifestData> = buildManifestIndex()

    override fun buildManifest(
        libraryName: String
    ): NativeSensitiveManifestData {
        return manifestIndex.getValue(libraryName).copy(
            commonizerTarget = target
        )
    }

    companion object {
        internal fun create(target: CommonizerTarget, libraries: List<KotlinLibrary>) = NativeLibrariesToCommonize(
            target, libraries.map(::NativeLibrary)
        )
    }
}

internal class CommonNativeManifestDataProvider(
    private val target: CommonizerTarget,
    private val manifests: Map<UniqueLibraryName, List<NativeSensitiveManifestData>>
) : NativeManifestDataProvider {

    override fun buildManifest(libraryName: UniqueLibraryName): NativeSensitiveManifestData {
        val rawManifests = manifests[libraryName] ?: error("Missing manifests for $libraryName")
        check(rawManifests.isNotEmpty()) { "No manifests for $libraryName" }

        val isInterop = rawManifests.all { it.isInterop }

        return NativeSensitiveManifestData(
            uniqueName = libraryName,
            versions = rawManifests.first().versions,
            dependencies = rawManifests.map { it.dependencies }.reduce { acc, list -> acc.intersect(list).toList() },
            isInterop = isInterop,
            packageFqName = rawManifests.first().packageFqName,
            exportForwardDeclarations = if (isInterop) rawManifests.map { it.exportForwardDeclarations }
                .reduce { acc, list -> acc.intersect(list).toList() } else emptyList(),
            includedForwardDeclarations = if (isInterop) rawManifests.map { it.includedForwardDeclarations }
                .reduce { acc, list -> acc.intersect(list).toList() } else emptyList(),
            nativeTargets = rawManifests.flatMapTo(mutableSetOf()) { it.nativeTargets },
            shortName = rawManifests.first().shortName,
            commonizerTarget = target
        )
    }
}

internal fun NativeManifestDataProvider(target: CommonizerTarget, libraries: List<NativeLibrariesToCommonize>): NativeManifestDataProvider {
    val manifestsByName = libraries
        .flatMap { it.libraries }
        .groupByTo(CommonizerMap()) { it.manifestData.uniqueName }
        .mapValues { (_, libraries) -> libraries.map { it.manifestData } }

    return CommonNativeManifestDataProvider(target, manifestsByName)
}

private fun NativeLibrariesToCommonize.buildManifestIndex(): MutableMap<UniqueLibraryName, NativeSensitiveManifestData> =
    libraries.map { it.manifestData }.associateByTo(CommonizerMap()) { it.uniqueName }
