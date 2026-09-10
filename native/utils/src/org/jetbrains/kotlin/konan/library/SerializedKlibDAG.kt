/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library

import org.jetbrains.kotlin.library.KotlinLibrary
import java.nio.file.Path

/**
 * The representation of a serialized [org.jetbrains.kotlin.backend.konan.library.KlibDAG],
 * where canonical library paths are stored instead of concrete [KotlinLibrary] instances.
 *
 * It is intended to be used for passing the DAG information from the main binary compilation
 * to spawned static cache compilations, so the DAG won't be re-computed multiple times.
 *
 * Note: The primary purpose of this class is to be used in the Kotlin/Native static caches machinery,
 * where we need to have the correct information about library dependencies even before the first launch
 * of IR linker. This class may not be needed in the future if we decide to move the caches orchestration
 * from the compiler to the BTA.
 */
data class SerializedKlibDAG(val dag: Map<Path, Set<Path>>) {
    init {
        // Sanity check.
        for ((libraryPath: Path, directDependencyPaths: Set<Path>) in dag.entries) {
            for (directDependency in directDependencyPaths) {
                check(directDependency in dag) {
                    "There is a direct dependency $directDependency of library $libraryPath that is not in DAG"
                }
            }
        }
    }
}
