/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.repository

import org.jetbrains.kotlin.descriptors.commonizer.KonanDistribution
import org.jetbrains.kotlin.descriptors.commonizer.LeafCommonizerTarget
import org.jetbrains.kotlin.descriptors.commonizer.NativeLibraryLoader
import org.jetbrains.kotlin.descriptors.commonizer.konan.NativeLibrary
import org.jetbrains.kotlin.descriptors.commonizer.platformLibsDir
import org.jetbrains.kotlin.descriptors.commonizer.repository.KonanDistributionRepository.Filter

internal class KonanDistributionRepository(
    konanDistribution: KonanDistribution,
    targets: Set<LeafCommonizerTarget>,
    libraryLoader: NativeLibraryLoader,
    filter: Filter = Filter.none()
) : Repository {
    fun interface Filter {
        operator fun invoke(target: LeafCommonizerTarget, library: NativeLibrary): Boolean

        companion object Factory
    }

    private val librariesByTarget: Map<LeafCommonizerTarget, Lazy<Set<NativeLibrary>>> =
        targets.associateWith { target ->
            lazy {
                konanDistribution.platformLibsDir
                    .resolve(target.name)
                    .takeIf { it.isDirectory }
                    ?.listFiles()
                    .orEmpty().toList()
                    .map { libraryLoader(it) }
                    .filter { filter(target, it) }
                    .toSet()
            }
        }

    override fun getLibraries(target: LeafCommonizerTarget): Set<NativeLibrary> {
        return librariesByTarget[target]?.value ?: error("Missing target $target")
    }
}

internal fun Filter.Factory.onlyDependenciesOf(libraries: Repository): Filter {
    val requestedDependencies = mutableMapOf<LeafCommonizerTarget, Set<String>>()
    return Filter { target, dependency ->
        val requestedDependenciesForTarget = requestedDependencies.getOrPut(target) {
            libraries.getLibraries(target).flatMap { library -> library.manifestData.dependencies }.toSet()
        }
        dependency.manifestData.uniqueName in requestedDependenciesForTarget
    }
}

internal fun Filter.Factory.none() = Filter { _, _ -> true }
