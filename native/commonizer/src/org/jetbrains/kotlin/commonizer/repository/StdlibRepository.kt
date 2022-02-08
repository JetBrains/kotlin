/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.repository

import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.commonizer.KonanDistribution
import org.jetbrains.kotlin.commonizer.NativeLibraryLoader
import org.jetbrains.kotlin.commonizer.konan.NativeLibrary
import org.jetbrains.kotlin.commonizer.stdlib

internal class StdlibRepository(
    private val konanDistribution: KonanDistribution,
    private val libraryLoader: NativeLibraryLoader,
) : Repository {

    private val stdlib by lazy {
        libraryLoader(konanDistribution.stdlib)
    }

    override fun getLibraries(target: CommonizerTarget): Set<NativeLibrary> {
        return setOf(stdlib)
    }
}
