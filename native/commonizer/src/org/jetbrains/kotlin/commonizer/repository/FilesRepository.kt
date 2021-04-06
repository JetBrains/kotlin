/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.repository

import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.commonizer.NativeLibraryLoader
import org.jetbrains.kotlin.commonizer.konan.NativeLibrary
import org.jetbrains.kotlin.commonizer.konanTargets
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

internal class FilesRepository(
    private val libraryFiles: Set<File>,
    private val libraryLoader: NativeLibraryLoader
) : Repository {

    private val librariesByKonanTargets: Map<Set<KonanTarget>, Set<NativeLibrary>> by lazy {
        libraryFiles
            .map(libraryLoader::invoke)
            .groupBy { library -> library.manifestData.nativeTargets.map(::konanTargetOrThrow).toSet() }
            .mapValues { (_, list) -> list.toSet() }
    }

    override fun getLibraries(target: CommonizerTarget): Set<NativeLibrary> {
        return librariesByKonanTargets[target.konanTargets].orEmpty()
    }

    private fun konanTargetOrThrow(value: String): KonanTarget {
        return KonanTarget.predefinedTargets[value] ?: error("Unexpected KonanTarget $value")
    }
}

