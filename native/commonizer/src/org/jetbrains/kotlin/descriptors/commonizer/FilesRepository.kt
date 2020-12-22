/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

import org.jetbrains.kotlin.commonizer.api.LeafCommonizerTarget
import org.jetbrains.kotlin.commonizer.api.CommonizerTarget
import org.jetbrains.kotlin.descriptors.commonizer.konan.NativeLibrary
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

private fun konanTargetOrThrow(value: String): KonanTarget {
    return KonanTarget.predefinedTargets[value] ?: error("Unknown KonanTarget $value")
}

// TODO SELLMAIR NOW: Test
internal class FilesRepository(
    private val libraryFiles: Set<File>,
    private val libraryLoader: NativeLibraryLoader
) : Repository {

    private val librariesByTarget: Map<CommonizerTarget, Set<NativeLibrary>> by lazy {
        libraryFiles
            .map(libraryLoader::invoke)
            .groupBy { library -> CommonizerTarget(library.manifestData.nativeTargets.map(::konanTargetOrThrow)) }
            .mapValues { (_, list) -> list.toSet() }
    }

    override fun getLibraries(): Set<NativeLibrary> {
        return librariesByTarget.values.flatten().toSet()
    }

    override fun getLibraries(target: LeafCommonizerTarget): Set<NativeLibrary> {
        return librariesByTarget[target].orEmpty()
    }
}
