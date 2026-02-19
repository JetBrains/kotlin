/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections

import kotlin.test.*
import test.collections.behaviors.*

class AbstractMutableCollectionTest {
    private class TestCollection(private val storage: IntArray): AbstractMutableCollection<Int>() {
        private var len = 0

        override val size: Int
            get() = len

        override fun add(element: Int): Boolean {
            if (len >= storage.size) return false
            storage[len++] = element
            return true
        }

        override fun iterator(): MutableIterator<Int> = object: MutableIterator<Int> {
            var nextIndex = 0

            override fun hasNext() = nextIndex < len
            override fun next() = storage[nextIndex++]

            override fun remove() {
                if (nextIndex == 0) throw IllegalStateException()
                for (i in nextIndex..len - 1) {
                    storage[i - 1] = storage[i]
                }
                len--
                nextIndex--
            }
        }

        override fun clear() {
            len = 0
        }
    }

    @Test fun addAllSuccess() {
        val collection = TestCollection(IntArray(3))
        assertTrue(collection.addAll(listOf(1, 2, 3)))
        compare(listOf(1, 2, 3), collection) {
            collectionBehavior()
        }
    }

    @Test fun addAllFailure() {
        val collection = TestCollection(IntArray(3))
        assertTrue(collection.addAll(listOf(1, 2, 3)))
        assertFalse(collection.addAll(listOf(4, 5)))
        compare(listOf(1, 2, 3), collection) {
            collectionBehavior()
        }
    }

    @Test fun removeAll() {
        val collection = TestCollection(IntArray(7))
        assertTrue(collection.addAll(listOf(1, 2, 3, 2, 4, 5, 4)))
        assertTrue(collection.removeAll(listOf(1, 2)))
        compare(listOf(3, 4, 5, 4), collection) {
            collectionBehavior()
        }
    }

    @Test fun retainAll() {
        val collection = TestCollection(IntArray(7))
        assertTrue(collection.addAll(listOf(1, 2, 4, 3, 5, 2, 4)))
        assertTrue(collection.retainAll(listOf(4, 5)))
        compare(listOf(4, 5, 4), collection) {
            collectionBehavior()
        }
    }

    @Test fun remove() {
        val collection = TestCollection(IntArray(3))
        assertTrue(collection.addAll(listOf(1, 2, 3)))
        assertTrue(collection.remove(2))
        compare(listOf(1, 3), collection) {
            collectionBehavior()
        }
    }

    @Test fun clear() {
        val collection = TestCollection(IntArray(3))
        assertTrue(collection.addAll(listOf(1, 2, 3)))
        collection.clear()
        compare(emptyList(), collection) {
            collectionBehavior()
        }
    }
}