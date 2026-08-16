/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.library.KlibDAG
import org.jetbrains.kotlin.backend.konan.library.KlibDAGBuilder
import org.jetbrains.kotlin.backend.konan.library.KlibDAGNode
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.utils.DFS

/**
 * The class represents all Klib libraries loaded on 2nd compilation stage of the Kotlin/Native
 * compiler together with their dependencies.
 *
 * @property klibsInArbitraryOrder All Klibs in the arbitrary order. Zero-cost property.
 * @property klibsReverseTopoSorted All Klibs in the reverse topological order.
 *
 * Functions:
 * [getDirectDependencies] Get the direct dependencies of the given library.
 * [getAllDependencies] Get all dependencies of the given library (i.e., direct + transitive deps).
 *
 * IMPORTANT: Accessing any of [klibsReverseTopoSorted], [getDirectDependencies] and [getAllDependencies]
 * triggers expensive computations to deduce DAG of library dependencies using the signature information
 * extracted from every library.
 */
class LoadedKlibsWithDependencies(val klibsInArbitraryOrder: Collection<KotlinLibrary>) {
    private val dag: KlibDAG by lazy { KlibDAGBuilder.build(klibsInArbitraryOrder) }

    val klibsReverseTopoSorted: List<KotlinLibrary> by lazy {
        DFS.topologicalOrder(dag.keys) { library -> dag[library]!!.directDependencies }.reversed()
    }

    fun getDirectDependencies(library: KotlinLibrary): Set<KotlinLibrary> = getDagNode(library).directDependencies
    fun getAllDependencies(library: KotlinLibrary): Set<KotlinLibrary> = getDagNode(library).allDependencies

    private fun getDagNode(library: KotlinLibrary): KlibDAGNode = dag[library] ?: error("Unexpected library in KLIB DAG: $library")
}
