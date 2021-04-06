/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.repository

import org.jetbrains.kotlin.commonizer.*
import org.jetbrains.kotlin.commonizer.NativeLibraryLoader
import org.jetbrains.kotlin.commonizer.konan.NativeLibrary

internal class CommonizerDependencyRepository(
    private val dependencies: Set<CommonizerDependency>,
    private val libraryLoader: NativeLibraryLoader
) : Repository {

    private val nonTargetedDependencyRepository = FilesRepository(
        libraryFiles = dependencies.filterIsInstance<NonTargetedCommonizerDependency>().map { it.file }.toSet(),
        libraryLoader = libraryLoader
    )

    private val targetedDependencies: Map<CommonizerTarget, Lazy<Set<NativeLibrary>>> by lazy {
        dependencies
            .filterIsInstance<TargetedCommonizerDependency>()
            .groupBy { it.target }
            .mapValues { (_, dependencies) -> lazy { dependencies.map { libraryLoader(it.file) }.toSet() } }
    }

    override fun getLibraries(target: CommonizerTarget): Set<NativeLibrary> {
        return targetedDependencies[target]?.value.orEmpty() + nonTargetedDependencyRepository.getLibraries(target)
    }
}
