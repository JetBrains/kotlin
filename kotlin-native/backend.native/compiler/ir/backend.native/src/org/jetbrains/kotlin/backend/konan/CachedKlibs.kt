/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.library.KlibDAG
import org.jetbrains.kotlin.backend.konan.library.KlibDAGBuilder
import org.jetbrains.kotlin.backend.konan.library.deserialize
import org.jetbrains.kotlin.konan.library.SerializedKlibDAG
import org.jetbrains.kotlin.konan.library.isExplicitlySpecifiedByUserInCLIArgument
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.utils.DFS

class CachedKlibs private constructor(computeDag: () -> KlibDAG) {
    private val dag: KlibDAG by lazy { computeDag() }
    private val serializedDag: SerializedKlibDAG by lazy { dag.serialize() }

    val librariesReverseTopoSorted: List<KotlinLibrary> by lazy {
        DFS.topologicalOrder(dag.libraries) { library -> dag[library].directDependencies }.reversed()
    }

    fun getDirectDependencies(library: KotlinLibrary): Set<KotlinLibrary> = dag[library].directDependencies
    fun getAllDependencies(library: KotlinLibrary): Set<KotlinLibrary> = dag[library].allDependencies

    fun toSerializedDag(): SerializedKlibDAG = serializedDag

    companion object {
        fun buildNew(libraries: Collection<KotlinLibrary>) = CachedKlibs {
            KlibDAGBuilder(libraries) { it.isExplicitlySpecifiedByUserInCLIArgument }.build()
        }

        fun restoreFromSerializedDag(serializedKlibDAG: SerializedKlibDAG, libraries: Collection<KotlinLibrary>) = CachedKlibs {
            serializedKlibDAG.deserialize(libraries)
        }
    }
}
