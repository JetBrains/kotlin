/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalAtomicApi::class)

package collections

import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.native.concurrent.Lock
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.locked
import kotlin.random.Random
import kotlin.test.*

private const val ITERATIONS = 100_000
private const val MIN_SIZE = 1
private const val MAX_SIZE = 16

class IterableSnapshotStressTest {
    @Test
    fun copyOnWriteArrayListToList() = runStress { it.toList() }

    @Test
    fun copyOnWriteArrayListToArrayList() = runStress { ArrayList(it) }

    // `asList()` is a view, so the validation checks the array returned by `toTypedArray()` itself.
    @Test
    fun copyOnWriteArrayListToTypedArray() = runStress { it.toTypedArray().asList() }
}

/**
 * Takes snapshots of a collection with [takeSnapshot] on one worker while another worker keeps changing the list's size.
 *
 * The list is always a non-empty contiguous ascending range `[a, a + 1, ..., b]` of at most [MAX_SIZE] elements,
 * so every valid snapshot must also be one. A torn snapshot (e.g., the size read from one state and the elements
 * taken from another) shows up as `null`s, gaps, a wrong size, or an exception.
 */
private fun runStress(takeSnapshot: (Collection<Int>) -> List<Int>) {
    val source = CopyOnWriteArrayList(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9))
    val sourceRunning = AtomicBoolean(true)
    val sourceWorker = Worker.start(name = "source mutator")
    val snapshotWorker = Worker.start(name = "snapshot taker")
    try {
        val sourceFuture = sourceWorker.execute(TransferMode.SAFE, { Pair(source, sourceRunning) }) { [source, running] ->
            mutate(source, running)
        }
        val snapshotFuture = snapshotWorker.execute(TransferMode.SAFE, { Pair(source, takeSnapshot) }) { [source, takeSnapshot] ->
            takeSnapshots(source, takeSnapshot)
        }
        val failure = try {
            snapshotFuture.result
        } finally {
            sourceRunning.store(false)
            sourceFuture.result
        }
        assertNull(failure)
    } finally {
        sourceWorker.requestTermination().result
        snapshotWorker.requestTermination().result
    }
}

private fun mutate(source: CopyOnWriteArrayList<Int>, running: AtomicBoolean) {
    val random = Random(42)
    while (running.load() && source.isNotEmpty()) {
        val size = source.size
        val grow = when {
            size <= MIN_SIZE -> true
            size >= MAX_SIZE -> false
            else -> random.nextBoolean()
        }
        val atFront = random.nextBoolean()
        // This worker is the only mutator, so `first()` and `last()` are consistent with the subsequent mutation.
        when {
            grow && atFront -> source.add(0, source.first() - 1)
            grow -> source.add(source.last() + 1)
            atFront -> source.removeAt(0)
            else -> source.removeAt(source.lastIndex)
        }
    }
}

/**
 * Returns a description of the first invalid snapshot, or `null` if all snapshots were valid.
 */
private fun takeSnapshots(source: Collection<Int>, takeSnapshot: (Collection<Int>) -> List<Int>): String? {
    repeat(ITERATIONS) { iteration ->
        val snapshot = try {
            takeSnapshot(source)
        } catch (e: Throwable) {
            return "Iteration $iteration: taking a snapshot failed with $e"
        }
        if (!isValidSnapshot(snapshot)) return "Iteration $iteration: invalid snapshot $snapshot"
    }
    return null
}

// Accepts `List<Any?>` because a torn snapshot may contain `null`s despite its static type.
private fun isValidSnapshot(snapshot: List<Any?>): Boolean {
    if (snapshot.size !in MIN_SIZE..MAX_SIZE) return false
    val first = snapshot[0] as? Int ?: return false
    return snapshot.withIndex().all { [index, element] -> element == first + index }
}

/**
 * A thread-safe list that never mutates an array once published, so its iterators are snapshots.
 */
private class CopyOnWriteArrayList<E>(elements: Collection<E>) : AbstractMutableList<E>() {
    private val lock = Lock()
    private var store: Array<E> = arrayOfUninitializedElements(0)

    init {
        addAll(elements)
    }

    override fun add(index: Int, element: E): Unit = locked(lock) {
        if (index < 0 || index > store.size) {
            throw IndexOutOfBoundsException("Index $index is out of bounds")
        }
        val newStore = arrayOfUninitializedElements<E>(store.size + 1)
        store.copyInto(newStore, 0, 0, index)
        newStore[index] = element
        store.copyInto(newStore, index + 1, index, store.size)
        store = newStore
    }

    override fun addAll(elements: Collection<E>): Boolean = locked(lock) {
        val oldSize = store.size
        val newStore = arrayOfUninitializedElements<E>(oldSize + elements.size)
        store.copyInto(newStore)
        elements.forEachIndexed { index, element ->
            newStore[oldSize + index] = element
        }
        store = newStore
        elements.isNotEmpty()
    }

    override fun removeAt(index: Int): E = locked(lock) {
        checkIndex(index)
        val removedElement = store[index]
        val newStore = arrayOfUninitializedElements<E>(store.size - 1)
        store.copyInto(newStore, 0, 0, index)
        store.copyInto(newStore, index, index + 1, store.size)
        store = newStore
        removedElement
    }

    override fun set(index: Int, element: E): E = locked(lock) {
        checkIndex(index)
        val oldElement = store[index]
        val newStore = store.copyOf()
        newStore[index] = element
        store = newStore
        oldElement
    }

    override val size: Int
        get() = locked(lock) { store.size }

    override fun get(index: Int): E = locked(lock) {
        checkIndex(index)
        store[index]
    }

    override fun iterator(): MutableIterator<E> = IteratorImpl(locked(lock) { store })

    private fun checkIndex(index: Int) {
        if (index < 0 || index >= store.size) {
            throw IndexOutOfBoundsException("Index $index is out of bounds")
        }
    }

    private class IteratorImpl<E>(private val store: Array<E>) : MutableIterator<E> {
        private var index = 0

        override fun next(): E {
            if (index >= store.size) throw NoSuchElementException()
            return store[index++]
        }

        override fun hasNext(): Boolean = index < store.size

        override fun remove() {
            throw UnsupportedOperationException()
        }
    }
}
