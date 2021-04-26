/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.util

/**
 * General purpose implementation of a transitive closure
 * - Recursion free
 * - Predictable amount of allocations
 * - Handles loops and self references gracefully
 * @param edges: Producer function from one node to all its children. This implementation can handle loops and self references gracefully.
 * @return Note: No guarantees given about the order ot this [Set]
 */
public inline fun <reified T> transitiveClosure(seed: T, edges: T.() -> Iterable<T>): Set<T> {
    // Fast path when initial edges are empty
    val initialEdges = seed.edges()
    if (initialEdges is Collection && initialEdges.isEmpty()) return emptySet()

    val queue = deque<T>()
    val results = mutableSetOf<T>()
    queue.addAll(initialEdges)
    while (queue.isNotEmpty()) {
        // ArrayDeque implementation will optimize this call to 'removeFirst'
        val resolved = queue.removeAt(0)
        if (resolved != seed && results.add(resolved)) {
            queue.addAll(resolved.edges())
        }
    }

    return results.toSet()
}

@OptIn(ExperimentalStdlibApi::class)
@PublishedApi
internal inline fun <reified T> deque(): MutableList<T> {
    return if (KotlinVersion.CURRENT.isAtLeast(1, 4)) ArrayDeque()
    else mutableListOf()
}
