/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation.analyzer.internal

import org.jetbrains.kotlin.documentation.analyzer.KdAnalyzerSourceSet

/**
 * The `dependsOn` graph of the source sets passed to `generateDocumentationBundle`, used to compute, for every
 * source set, which declarations should be visible in its [org.jetbrains.kotlin.documentation.KdFragment]
 */
internal class SourceSetGraph(sourceSets: List<KdAnalyzerSourceSet>) {
    private val dependsOn: Map<String, Set<String>> = sourceSets.associate { it.name to it.dependsOn }

    /** [name] together with every source set it (transitively) depends on. */
    private val ancestorsByName: Map<String, Set<String>> = sourceSets.associate { it.name to computeAncestors(it.name) }

    private fun computeAncestors(name: String): Set<String> {
        val result = linkedSetOf(name)
        val queue = ArrayDeque(dependsOn[name].orEmpty())
        while (queue.isNotEmpty()) {
            val next = queue.removeFirst()
            if (result.add(next)) {
                queue += dependsOn[next].orEmpty()
            }
        }
        return result
    }

    /** The set of source sets whose declarations are accessible from [name]: itself and all its dependencies. */
    fun reachableFrom(name: String): Set<String> = ancestorsByName[name].orEmpty()
}
