/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.tooling.core

/**
 * General purpose implementation of a transitive closure
 * - Recursion free
 * - Predictable amount of allocations
 * - Handles loops and self references gracefully
 * @param edges: Producer function from one node to all its children. This implementation can handle loops and self references gracefully.
 * @return Note: The order of the set is guaranteed to be bfs
 */
inline fun <reified T> T.closure(edges: (T) -> Iterable<T>): Set<T> =
    closureTo(
        destination = createResultSet(),
        exclude = emptySet(),
        dequeue = createDequeue(),
        seed = this,
        enqueueNextElements = { element -> addAll(edges(element)) }
    )

/**
 * Similar to [closure], but will also include the receiver(seed) of this function into the final set
 * @see closure
 */
inline fun <reified T> T.withClosure(edges: (T) -> Iterable<T>): Set<T> =
    closureTo(
        destination = createResultSet(this),
        exclude = emptySet(),
        dequeue = createDequeue(),
        seed = this,
        enqueueNextElements = { element -> addAll(edges(element)) }
    )

/**
 * @see closure
 * @receiver: Will not be included in the return set
 */
inline fun <reified T> Iterable<T>.closure(edges: (T) -> Iterable<T>): Set<T> {
    val thisSet = this.toSet()
    return closureTo(
        destination = createResultSet(thisSet.size * 2),
        exclude = thisSet,
        dequeue = createDequeue(thisSet.size * 2), edges = edges
    )
}

/**
 * @see closure
 * @receiver: Will be included in the return set
 */
inline fun <reified T> Iterable<T>.withClosure(edges: (T) -> Iterable<T>): Set<T> {
    val size = this.count()
    return closureTo(
        destination = createResultSet(this, size * 2),
        exclude = emptySet(),
        dequeue = createDequeue(size * 2),
        edges = edges
    )
}

/**
 * @see closure
 * @receiver is not included in the return set
 */
inline fun <reified T : Any> T.linearClosure(next: (T) -> T?): Set<T> =
    closureTo(createResultSet(), emptySet(), createDequeue(), this) { element -> next(element)?.let { add(it) } }

/**
 * @see closure
 * @receiver is included in the return set
 */
inline fun <reified T : Any> T.withLinearClosure(next: (T) -> T?): Set<T> =
    closureTo(createResultSet(this), emptySet(), createDequeue(), this) { element -> next(element)?.let { add(it) } }

/* Implementation */

private typealias Results<T> = MutableSet<T>
private typealias Dequeue<T> = MutableList<T>

@PublishedApi
internal inline fun <reified T> Iterable<T>.closureTo(
    destination: Results<T>, exclude: Set<T>, dequeue: Dequeue<T>, edges: (T) -> Iterable<T>
): Set<T> {
    forEach { seed ->
        closureTo(
            destination = destination,
            exclude = exclude,
            dequeue = dequeue,
            seed = seed,
            enqueueNextElements = { element -> addAll(edges(element)) })
    }
    return destination
}

@PublishedApi
internal inline fun <T> closureTo(
    destination: Results<T>,
    exclude: Set<T>,
    dequeue: Dequeue<T>,
    seed: T,
    enqueueNextElements: Dequeue<T>.(value: T) -> Unit
): Set<T> {
    dequeue.enqueueNextElements(seed)
    while (dequeue.isNotEmpty()) {
        val element = dequeue.removeAt(0)
        if (element != seed && element !in exclude && destination.add(element)) {
            dequeue.enqueueNextElements(element)
        }
    }
    return destination
}

@PublishedApi
internal inline fun <reified T> createDequeue(initialSize: Int = 16): MutableList<T> {
    return if (KotlinVersion.CURRENT.isAtLeast(1, 4)) ArrayDeque(initialSize)
    else ArrayList(initialSize)
}

@PublishedApi
internal fun <T> createResultSet(initialSize: Int = 16): MutableSet<T> {
    return LinkedHashSet(initialSize)
}

@PublishedApi
internal fun <T> createResultSet(withValue: T, initialSize: Int = 16): MutableSet<T> {
    return LinkedHashSet<T>(initialSize).also { it.add(withValue) }
}

@PublishedApi
internal fun <T> createResultSet(withValues: Iterable<T>, initialSize: Int = 16): MutableSet<T> {
    return LinkedHashSet<T>(initialSize).also { it.addAll(withValues) }
}
