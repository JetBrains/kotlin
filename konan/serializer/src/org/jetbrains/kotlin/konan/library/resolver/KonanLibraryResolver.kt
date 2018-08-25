/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library.resolver

import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.SearchPathResolverWithTarget

typealias DuplicatedLibraryLogger = (String) -> Unit

interface KonanLibraryResolver {

    val searchPathResolver: SearchPathResolverWithTarget
    val abiVersion: Int

    /**
     * Given the list of Kotlin/Native library names, ABI version and other parameters
     * resolves libraries and evaluates dependencies between them.
     */
    fun resolveWithDependencies(
        libraryNames: List<String>,
        noStdLib: Boolean = false,
        noDefaultLibs: Boolean = false,
        logger: DuplicatedLibraryLogger? = null
    ): KonanLibraryResolveResult
}

interface KonanLibraryResolveResult {

    fun filterRoots(predicate: (KonanResolvedLibrary) -> Boolean): KonanLibraryResolveResult

    fun getFullList(order: LibraryOrder? = null): List<KonanLibrary>

    fun forEach(action: (KonanLibrary, PackageAccessedHandler) -> Unit)
}


typealias LibraryOrder = (Iterable<KonanResolvedLibrary>) -> List<KonanResolvedLibrary>

val TopologicalLibraryOrder: LibraryOrder = { input ->
    val sorted = mutableListOf<KonanResolvedLibrary>()
    val visited = mutableSetOf<KonanResolvedLibrary>()
    val tempMarks = mutableSetOf<KonanResolvedLibrary>()

    fun visit(node: KonanResolvedLibrary, result: MutableList<KonanResolvedLibrary>) {
        if (visited.contains(node)) return
        if (tempMarks.contains(node)) error("Cyclic dependency in library graph.")
        tempMarks.add(node)
        node.resolvedDependencies.forEach {
            visit(it, result)
        }
        visited.add(node)
        result += node
    }

    input.forEach next@{
        if (visited.contains(it)) return@next
        visit(it, sorted)
    }

    sorted
}