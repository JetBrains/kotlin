/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName", "DeprecatedCallableAddReplaceWith", "DuplicatedCode")

package org.jetbrains.kotlin.tooling.core

/**
 * General purpose implementation of a transitive closure
 * - Recursion free
 * - Predictable amount of allocations
 * - Handles loops and self references gracefully
 * @param edges: Producer function from one node to all its children. This implementation can handle loops and self references gracefully.
 * @return Note: The order of the set is guaranteed to be bfs
 */
inline fun <reified T> T.closure(edges: (T) -> Iterable<T>): Set<T> {
    val initialEdges = edges(this)

    val dequeue = if (initialEdges is Collection) {
        if (initialEdges.isEmpty()) return emptySet()
        createDequeue(initialEdges)
    } else createDequeueFromIterable(initialEdges)

    val results = createResultSet<T>(dequeue.size)

    while (dequeue.isNotEmpty()) {
        val element = dequeue.removeAt(0)
        if (element != this && results.add(element)) {
            dequeue.addAll(edges(element))
        }
    }
    return results
}

/**
 * Same as [closure], but returns a lazy sequence instead
 */
inline fun <reified T> T.closureSequence(crossinline edges: (T) -> Iterable<T>): Sequence<T> {
    val initialEdges = edges(this)

    val resolveDequeue = if (initialEdges is Collection && initialEdges.isEmpty()) return emptySequence()
    else createDequeue<T>()

    return sequence {
        val results = HashSet<T>()
        results.add(this@closureSequence)

        initialEdges.forEach { initialEdge ->
            if (results.add(initialEdge)) {
                yield(initialEdge)
                resolveDequeue.add(initialEdge)
            }
        }

        while (resolveDequeue.isNotEmpty()) {
            edges(resolveDequeue.removeAt(0)).forEach { edge ->
                if (results.add(edge)) {
                    yield(edge)
                    resolveDequeue.add(edge)
                }
            }
        }
    }
}


/**
 * Similar to [closure], but will also include the receiver(seed) of this function into the final set
 * @see closure
 */
inline fun <reified T> T.withClosure(edges: (T) -> Iterable<T>): Set<T> {
    val initialEdges = edges(this)

    val dequeue = if (initialEdges is Collection) {
        if (initialEdges.isEmpty()) return setOf(this)
        createDequeue(initialEdges)
    } else createDequeueFromIterable(initialEdges)

    val results = createResultSet<T>(dequeue.size)
    results.add(this)

    while (dequeue.isNotEmpty()) {
        val element = dequeue.removeAt(0)
        if (results.add(element)) {
            dequeue.addAll(edges(element))
        }
    }
    return results
}

/**
 * Similar to [closureSequence], but will also include the receiver(seed) of this function into the final set
 * @see closure
 */
inline fun <reified T> T.withClosureSequence(crossinline edges: (T) -> Iterable<T>): Sequence<T> {
    val initialEdges = edges(this)

    val resolveDequeue = if (initialEdges is Collection && initialEdges.isEmpty()) return sequenceOf(this)
    else createDequeue<T>()

    return sequence {
        val results = HashSet<T>()
        yield(this@withClosureSequence)
        results.add(this@withClosureSequence)

        initialEdges.forEach { initialEdge ->
            if (results.add(initialEdge)) {
                yield(initialEdge)
                resolveDequeue.add(initialEdge)
            }
        }

        while (resolveDequeue.isNotEmpty()) {
            edges(resolveDequeue.removeAt(0)).forEach { edge ->
                if (results.add(edge)) {
                    yield(edge)
                    resolveDequeue.add(edge)
                }
            }
        }
    }
}

/**
 * @see closure
 * @receiver: Will not be included in the return set
 */
inline fun <reified T> Iterable<T>.closure(edges: (T) -> Iterable<T>): Set<T> {
    if (this is Collection && this.isEmpty()) return emptySet()
    val thisSet = this.toSet()

    val dequeue = createDequeue<T>()
    thisSet.forEach { seed -> dequeue.addAll(edges(seed)) }
    if (dequeue.isEmpty()) return emptySet()

    val results = createResultSet<T>()

    while (dequeue.isNotEmpty()) {
        val element = dequeue.removeAt(0)
        if (element !in thisSet && results.add(element)) {
            dequeue.addAll(edges(element))
        }
    }

    return results
}

/**
 * @see closure
 * @receiver: Will not be included in the return set
 */
inline fun <reified T> Iterable<T>.closureSequence(crossinline edges: (T) -> Iterable<T>): Sequence<T> {
    if (this is Collection && this.isEmpty()) return emptySequence()
    val thisSet = if (this is Set<T>) this else this.toSet()

    val results = HashSet<T>(thisSet)
    val resolveQueue = createDequeue<T>(thisSet)

    return sequence {
        while (resolveQueue.isNotEmpty()) {
            edges(resolveQueue.removeAt(0)).forEach { edge ->
                if (results.add(edge)) {
                    yield(edge)
                    resolveQueue.add(edge)
                }
            }
        }
    }
}

/**
 * @see closure
 * @receiver Initial sequence of elements will not be included in the return squence
 */
inline fun <reified T> Sequence<T>.closureSequence(crossinline edges: (T) -> Iterable<T>): Sequence<T> {
    return toSet().closureSequence(edges)
}

/**
 * @see closure
 * @receiver: Will be included in the return set
 */
inline fun <reified T> Iterable<T>.withClosure(edges: (T) -> Iterable<T>): Set<T> {
    val dequeue = if (this is Collection) {
        if (this.isEmpty()) return emptySet()
        createDequeue(this)
    } else createDequeueFromIterable(this)

    val results = createResultSet<T>()

    while (dequeue.isNotEmpty()) {
        val element = dequeue.removeAt(0)
        if (results.add(element)) {
            dequeue.addAll(edges(element))
        }
    }

    return results
}

/**
 * @see withClosure
 * @receiver: Will be included in the return set
 */
inline fun <reified T> Iterable<T>.withClosureSequence(crossinline edges: (T) -> Iterable<T>): Sequence<T> {
    return asSequence().withClosureSequence(edges)
}

/**
 * @see closure
 * @receiver: Will be included in the return set
 */
inline fun <reified T> Sequence<T>.withClosureSequence(crossinline edges: (T) -> Iterable<T>): Sequence<T> {
    val results = HashSet<T>()
    val resolveQueue = createDequeue<T>()

    return sequence {
        this@withClosureSequence.forEach { element ->
            if (results.add(element)) {
                yield(element)
                resolveQueue.add(element)
            }
        }

        while (resolveQueue.isNotEmpty()) {
            val seed = resolveQueue.removeAt(0)
            edges(seed).forEach { resolvedEdge ->
                if (results.add(resolvedEdge)) {
                    yield(resolvedEdge)
                    resolveQueue.add(resolvedEdge)
                }
            }
        }
    }
}

/**
 * @see closure
 * @receiver is not included in the return set
 */
inline fun <reified T : Any> T.linearClosure(next: (T) -> T?): Set<T> {
    val initial = next(this) ?: return emptySet()
    val results = createResultSet<T>()
    var enqueued: T? = initial
    while (enqueued != null) {
        if (enqueued != this && results.add(enqueued)) {
            enqueued = next(enqueued)
        } else break
    }

    return results
}

/**
 * @see closure
 * @receiver is not included in the return set
 */
inline fun <reified T : Any> T.linearClosureSequence(crossinline next: (T) -> T?): Sequence<T> {
    val initial = next(this) ?: return emptySequence()
    val results = HashSet<T>()

    return sequence {
        var enqueued: T? = initial
        while (enqueued != null) {
            if (enqueued != this@linearClosureSequence && results.add(enqueued)) {
                yield(enqueued)
                enqueued = next(enqueued)
            } else break
        }
    }
}

/**
 * @see closure
 * @receiver is included in the return set
 */
inline fun <reified T : Any> T.withLinearClosure(next: (T) -> T?): Set<T> {
    val initial = next(this) ?: return setOf(this)
    val results = createResultSet<T>()
    results.add(this)

    var enqueued: T? = initial
    while (enqueued != null) {
        if (results.add(enqueued)) {
            enqueued = next(enqueued)
        } else break
    }

    return results
}

/**
 * @see closure
 * @receiver is included in the return set
 */
inline fun <reified T : Any> T.withLinearClosureSequence(crossinline next: (T) -> T?): Sequence<T> {
    val initial = next(this) ?: return sequenceOf(this)
    val results = HashSet<T>()
    return sequence {
        results.add(this@withLinearClosureSequence)
        yield(this@withLinearClosureSequence)

        var enqueued: T? = initial
        while (enqueued != null) {
            if (results.add(enqueued)) {
                yield(enqueued)
                enqueued = next(enqueued)
            } else break
        }
    }
}

/**
 * Similar to [closure], but the result is returned in explicit BFS depth order starting with the receiver
 * @see closure
 */
inline fun <reified T> Iterable<T>.withClosureGroupingByDistance(edges: (T) -> Iterable<T>): List<Set<T>> {
    val dequeue = if (this is Collection) {
        if (this.isEmpty()) return emptyList()
        createDequeue(this)
    } else createDequeueFromIterable(this)

    val allElements = createResultSet<T>(dequeue.size)
    val results = mutableListOf<MutableSet<T>>()

    while (dequeue.isNotEmpty()) {
        val levelElements = createResultSet<T>(dequeue.size)
        var levelSize = dequeue.size
        while (levelSize != 0) {
            levelSize -= 1
            val element = dequeue.removeAt(0)
            if (allElements.add(element) && levelElements.add(element)) {
                dequeue.addAll(edges(element))
            }
        }
        if (levelElements.isNotEmpty()) {
            results.add(levelElements)
        }
    }
    return results
}

@PublishedApi
internal inline fun <reified T> createDequeue(initialSize: Int = 16): MutableList<T> {
    return if (KotlinVersion.CURRENT.isAtLeast(1, 4)) ArrayDeque(initialSize)
    else ArrayList(initialSize)
}

@PublishedApi
internal inline fun <reified T> createDequeue(elements: Collection<T>): MutableList<T> {
    return if (KotlinVersion.CURRENT.isAtLeast(1, 4)) ArrayDeque(elements)
    else ArrayList(elements)
}

@PublishedApi
internal inline fun <reified T> createDequeueFromIterable(elements: Iterable<T>): MutableList<T> {
    return createDequeue<T>().apply {
        elements.forEach { element -> add(element) }
    }
}


@PublishedApi
internal fun <T> createResultSet(initialSize: Int = 16): MutableSet<T> {
    return LinkedHashSet(initialSize)
}

//region Deprecations

@Suppress("unused")
@Deprecated("Scheduled for removal in 2.0", level = DeprecationLevel.ERROR)
@PublishedApi
internal fun <T> createResultSet(withValue: T, initialSize: Int = 16): MutableSet<T> {
    return LinkedHashSet<T>(initialSize).also { it.add(withValue) }
}

@Suppress("unused")
@Deprecated("Scheduled for removal in 2.0", level = DeprecationLevel.ERROR)
@PublishedApi
internal fun <T> createResultSet(withValues: Iterable<T>, initialSize: Int = 16): MutableSet<T> {
    return LinkedHashSet<T>(initialSize).also { it.addAll(withValues) }
}

@Suppress("unused")
@Deprecated("Scheduled for removal in 2.0", level = DeprecationLevel.ERROR)
@PublishedApi
internal inline fun <T> closureTo(
    destination: MutableSet<T>,
    exclude: Set<T>,
    dequeue: MutableList<T>,
    seed: T,
    enqueueNextElements: MutableList<T>.(value: T) -> Unit,
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

//endregion
