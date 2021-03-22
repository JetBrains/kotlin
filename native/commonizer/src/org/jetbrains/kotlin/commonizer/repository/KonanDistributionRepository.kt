/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.repository

import org.jetbrains.kotlin.commonizer.*
import org.jetbrains.kotlin.commonizer.konan.NativeLibrary

internal class KonanDistributionRepository(
    konanDistribution: KonanDistribution,
    targets: Set<LeafCommonizerTarget>,
    libraryLoader: NativeLibraryLoader,
) : Repository {
    private val librariesByTarget: Map<LeafCommonizerTarget, Lazy<Set<NativeLibrary>>> =
        targets.associateWith { target ->
            lazy {
                konanDistribution.platformLibsDir
                    .resolve(target.name)
                    .takeIf { it.isDirectory }
                    ?.listFiles()
                    .orEmpty().toList()
                    .map { libraryLoader(it) }
                    .toSet()
            }
        }

    override fun getLibraries(target: CommonizerTarget): Set<NativeLibrary> {
        return when (target) {
            is LeafCommonizerTarget -> librariesByTarget[target]?.value ?: error("Missing target libraries for: $target")
            is SharedCommonizerTarget -> emptySet()
        }
    }
}
