/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

import org.jetbrains.kotlin.commonizer.api.LeafCommonizerTarget
import org.jetbrains.kotlin.descriptors.commonizer.konan.NativeLibrary
import org.jetbrains.kotlin.konan.target.KonanTarget

internal interface Repository {
    fun getLibraries(): Set<NativeLibrary>
    fun getLibraries(target: LeafCommonizerTarget): Set<NativeLibrary>
}

internal operator fun Repository.plus(other: Repository): Repository {
    if (this is CompositeRepository) {
        return CompositeRepository(this.repositories + other)
    }
    return CompositeRepository(listOf(this, other))
}

internal class KonanDistributionRepository(
    private val konanDistribution: KonanDistribution,
    private val targets: Set<KonanTarget>,
    private val libraryLoader: NativeLibraryLoader,
) : Repository {

    private val librariesByTarget: Map<LeafCommonizerTarget, Lazy<Set<NativeLibrary>>> = run {
        targets.map(::LeafCommonizerTarget).associateWith { target ->
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
    }

    override fun getLibraries(): Set<NativeLibrary> {
        return librariesByTarget.values.map { it.value }.flatten().toSet()
    }

    override fun getLibraries(target: LeafCommonizerTarget): Set<NativeLibrary> {
        return librariesByTarget[target]?.value ?: error("Missing target $target")
    }
}

private class CompositeRepository(val repositories: Iterable<Repository>) : Repository {
    override fun getLibraries(): Set<NativeLibrary> {
        return repositories.map { it.getLibraries() }.flatten().toSet()
    }

    override fun getLibraries(target: LeafCommonizerTarget): Set<NativeLibrary> {
        return repositories.map { it.getLibraries(target) }.flatten().toSet()
    }
}

internal object EmptyRepository : Repository {
    override fun getLibraries(target: LeafCommonizerTarget): Set<NativeLibrary> {
        return emptySet()
    }

    override fun getLibraries(): Set<NativeLibrary> {
        return emptySet()
    }
}

