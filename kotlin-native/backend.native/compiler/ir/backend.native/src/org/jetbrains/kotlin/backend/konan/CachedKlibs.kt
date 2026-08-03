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

class CachedKlibs(libraries: Collection<KotlinLibrary>) {
    private val dag: KlibDAG by lazy { KlibDAGBuilder.build(libraries) }

    val librariesReverseTopoSorted: List<KotlinLibrary> by lazy {
        DFS.topologicalOrder(dag.keys) { library -> dag[library]!!.directDependencies }.reversed()
    }

    fun getDirectDependencies(library: KotlinLibrary): Set<KotlinLibrary> = getDagNode(library).directDependencies
    fun getAllTransitiveDependencies(library: KotlinLibrary): Set<KotlinLibrary> = getDagNode(library).allDependencies

    private fun getDagNode(library: KotlinLibrary): KlibDAGNode = dag[library] ?: error("Unexpected library in KLIB DAG: $library")
}
