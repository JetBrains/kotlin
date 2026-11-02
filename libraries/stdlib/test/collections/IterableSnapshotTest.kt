/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections

import test.TestPlatform
import test.testExceptOn
import kotlin.test.*

class IterableSnapshotTest {
    @Test
    fun listToListWhenChangedFromSingletonToEmpty() = testExceptOn(::notTestOnPlatform) {
        val sut = ClearsAfterSizeRead()

        val snapshot = sut.toList()

        assertSnapshotIsOneOf(snapshot, emptyList(), listOf(42))
    }

    @Test
    fun collectionToListWhenChangedFromSingletonToEmpty() = testExceptOn(::notTestOnPlatform) {
        val sut = object : Collection<Int> by ClearsAfterSizeRead() {}

        val snapshot = sut.toList()

        assertSnapshotIsOneOf(snapshot, emptyList(), listOf(42))
    }

    @Test
    fun randomAccessListToListWhenChangedFromSingletonToEmpty() = testExceptOn(::notTestOnPlatform) {
        val sut = object : List<Int> by ClearsAfterSizeRead(), RandomAccess {}

        val snapshot = sut.toList()

        assertSnapshotIsOneOf(snapshot, emptyList(), listOf(42))
    }

    @Test
    fun setToListWhenChangedFromSingletonToEmpty() = testExceptOn(::notTestOnPlatform) {
        val sut = object : AbstractSet<Int>() {
            private val delegate = ClearsAfterSizeRead()

            override val size: Int
                get() = delegate.size

            override fun iterator(): Iterator<Int> = delegate.iterator()
        }

        val snapshot = sut.toList()

        assertSnapshotIsOneOf(snapshot, emptyList(), listOf(42))
    }

    @Test
    fun listToListWhenReplacedAfterSizeRead() = testExceptOn(::notTestOnPlatform) {
        val sut = ChangesAfterSizeRead(initialElements = listOf(42), changedElements = listOf(7, 8))

        val snapshot = sut.toList()

        assertSnapshotIsOneOf(snapshot, listOf(42), listOf(7, 8))
    }

    @Test
    fun collectionToListWhenReplacedAfterSizeRead() = testExceptOn(::notTestOnPlatform) {
        val sut = object : Collection<Int> by ChangesAfterSizeRead(
            initialElements = listOf(42),
            changedElements = listOf(7, 8)
        ) {}

        val snapshot = sut.toList()

        assertSnapshotIsOneOf(snapshot, listOf(42), listOf(7, 8))
    }

    @Test
    fun randomAccessListToListWhenReplacedAfterSizeRead() = testExceptOn(::notTestOnPlatform) {
        val sut = object : List<Int> by ChangesAfterSizeRead(
            initialElements = listOf(42),
            changedElements = listOf(7, 8)
        ), RandomAccess {}

        val snapshot = sut.toList()

        assertSnapshotIsOneOf(snapshot, listOf(42), listOf(7, 8))
    }

    @Test
    fun setToListWhenReplacedAfterSizeRead() = testExceptOn(::notTestOnPlatform) {
        val sut = object : AbstractSet<Int>() {
            private val delegate = ChangesAfterSizeRead(
                initialElements = listOf(42),
                changedElements = listOf(7, 8)
            )

            override val size: Int
                get() = delegate.size

            override fun iterator(): Iterator<Int> = delegate.iterator()
        }

        val snapshot = sut.toList()

        assertSnapshotIsOneOf(snapshot, listOf(42), listOf(7, 8))
    }

    @Test
    fun listToListWhenSourceGrowsAfterSizeRead() = testExceptOn(::notTestOnPlatform) {
        val sut = ChangesAfterSizeRead(
            initialElements = listOf(1, 2, 3),
            changedElements = listOf(4, 5, 6, 7)
        )

        val snapshot = sut.toList()

        assertSnapshotIsOneOf(snapshot, listOf(1, 2, 3), listOf(4, 5, 6, 7))
    }

    @Test
    fun listToListWhenSourceShrinksAfterSizeRead() = testExceptOn(::notTestOnPlatform) {
        val sut = ChangesAfterSizeRead(
            initialElements = listOf(1, 2, 3),
            changedElements = listOf(4)
        )

        val snapshot = sut.toList()

        assertSnapshotIsOneOf(snapshot, listOf(1, 2, 3), listOf(4))
    }

    @Test
    fun listToListWhenSourceChangesAfterIteratorCapture() = testExceptOn(::notTestOnPlatform) {
        val sut = ChangesAfterIteratorCreation(
            initialElements = listOf(1, 2, 3),
            changedElements = listOf(4, 5)
        )

        assertSnapshotIsOneOf(sut.toList(),listOf(1, 2, 3), listOf(4, 5))
    }

    @Test
    fun collectionToListWhenIteratorIsEmptyButSizeIsPositive() = testExceptOn(::notTestOnPlatform) {
        val sut = object : AbstractCollection<Int>() {
            override val size: Int get() = 3

            override fun iterator(): Iterator<Int> = emptyList<Int>().iterator()
        }

        assertEquals(emptyList(), sut.toList())
    }

    @Test
    fun listToListIsIndependentWhenSourceChangesAfterReturn() = testExceptOn(::notTestOnPlatform) {
        val sut = MutableListSource(listOf(1, 2, 3))

        val snapshot = sut.toList()
        sut.replaceWith(listOf(7, 8))

        assertEquals(listOf(1, 2, 3), snapshot)
    }

    private fun notTestOnPlatform(platform: TestPlatform) =
        platform != TestPlatform.Jvm && platform != TestPlatform.Native
}

private fun assertSnapshotIsOneOf(snapshot: List<Int>, vararg expectedSnapshots: List<Int>) {
    val expected = expectedSnapshots.joinToString()
    assertTrue(expectedSnapshots.any { it == snapshot }, "Expected one of $expected, but was $snapshot.")
}

private class ClearsAfterSizeRead : AbstractList<Int>() {
    private val elements = mutableListOf(42)

    override val size: Int
        get() {
            val observedSize = elements.size
            elements.clear() // Simulate an intervening mutation.
            return observedSize
        }

    override fun get(index: Int): Int = elements[index]

    // Provide a snapshot iterator.
    override fun iterator(): Iterator<Int> =
        elements.toMutableList().iterator()
}

private class ChangesAfterSizeRead(
    initialElements: List<Int>,
    private val changedElements: List<Int>
) : AbstractList<Int>() {
    private val elements = initialElements.toMutableList()
    private var hasChanged = false

    override val size: Int
        get() {
            val observedSize = elements.size
            if (!hasChanged) {
                elements.clear()
                elements.addAll(changedElements)
                hasChanged = true
            }
            return observedSize
        }

    override fun get(index: Int): Int = elements[index]

    override fun iterator(): Iterator<Int> = elements.toMutableList().iterator()
}

private class ChangesAfterIteratorCreation(
    initialElements: List<Int>,
    private val changedElements: List<Int>
) : AbstractList<Int>() {
    private val elements = initialElements.toMutableList()

    override val size: Int
        get() = elements.size

    override fun get(index: Int): Int = elements[index]

    override fun iterator(): Iterator<Int> {
        val snapshot = elements.toMutableList()
        elements.clear()
        elements.addAll(changedElements)
        return snapshot.iterator()
    }
}

private class MutableListSource(initialElements: List<Int>) : AbstractList<Int>() {
    private val elements = initialElements.toMutableList()

    fun replaceWith(newElements: List<Int>) {
        elements.clear()
        elements.addAll(newElements)
    }

    override val size: Int
        get() = elements.size

    override fun get(index: Int): Int = elements[index]

    override fun iterator(): Iterator<Int> = elements.toMutableList().iterator()
}
