/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package test.collections.js

import kotlin.test.*

class JsCollectionsTest {
    val TEST_LIST = arrayOf(2, 0, 9, 7, 1).toList()

    @Test fun collectionToArray() {
        val array = TEST_LIST.toTypedArray()
        assertEquals(array.toList(), TEST_LIST)
    }

    @Test fun toListDoesNotCreateArrayView() {
        snapshotDoesNotCreateView(arrayOf("first", "last"), { it.toList() })
        snapshotDoesNotCreateView(arrayOf<Any>("item", 1), { it.toList() })
    }

    @Test fun toMutableListDoesNotCreateArrayView() {
        snapshotDoesNotCreateView(arrayOf("first", "last"), { it.toMutableList() })
        snapshotDoesNotCreateView(arrayOf<Any>("item", 2), { it.toMutableList() })
    }

    @Test fun listOfDoesNotCreateView() {
        snapshotDoesNotCreateView(arrayOf("first", "last"), { listOf(*it) })
        snapshotDoesNotCreateView(arrayOf<Any>("item", 3), { listOf(*it) })
    }

    @Test fun mutableListOfDoesNotCreateView() {
        snapshotDoesNotCreateView(arrayOf("first", "last"), { mutableListOf(*it) })
        snapshotDoesNotCreateView(arrayOf<Any>("item", 4), { mutableListOf(*it) })
    }

    @Test fun arrayListDoesNotCreateArrayView() {
        snapshotDoesNotCreateView(arrayOf(1, 2), { arrayListOf(*it) })
        snapshotDoesNotCreateView(arrayOf<Any>("first", "last"), { arrayListOf(*it) })
    }

    @Suppress("USELESS_CAST")
    @Test fun asListHidesPrimitivenessOfArray() {
        assertTrue(intArrayOf(1).asList().toTypedArray() as Any is Array<*>, "IntArray primitiveness leaks")
        assertTrue(longArrayOf(1).asList().toTypedArray() as Any is Array<*>, "LongArray primitiveness leaks")
        assertTrue(charArrayOf(' ').asList().toTypedArray() as Any is Array<*>, "CharArray primitiveness leaks")
    }

    @Test fun arrayListCapacity() {
        val list = ArrayList<Any>(20)
        list.ensureCapacity(100)
        list.trimToSize()
        assertTrue(list.isEmpty())
    }

    @Test fun listEqualsOperatesOnAny() {
        assertFalse(listOf(1, 2, 3).equals(object {}))
    }

    @Test fun arrayListValidatesIndexRange() {
        val list = mutableListOf(1)
        for (index in listOf(-1, 1, 3)) {
            if (index != list.size) { // size is a valid position index
                assertFailsWith<IndexOutOfBoundsException> { list.add(index, 2) }
                assertFailsWith<IndexOutOfBoundsException> { list.addAll(index, listOf(3, 0)) }
                assertFailsWith<IndexOutOfBoundsException> { list.listIterator(index) }
            }
            assertFailsWith<IndexOutOfBoundsException> { list.removeAt(index) }
            assertFailsWith<IndexOutOfBoundsException> { list[index] }
            assertFailsWith<IndexOutOfBoundsException> { list.subList(index, index + 2) }  // tests ranges [-1, 1), [1, 3) and [3, 5)
        }
        assertEquals(listOf(1), list)
    }

    @Test fun mutableIteratorRemove() {
        val a = mutableListOf(1, 2, 3)
        val it = a.iterator()
        assertFailsWith<IllegalStateException> { it.remove() }
    }

    private fun <T> snapshotDoesNotCreateView(array: Array<T>, snapshot: (Array<T>) -> List<T>) {
        val first = array.first()
        val last = array.last()

        val list = snapshot(array)
        assertEquals(first, list[0])
        array[0] = last
        assertEquals(first, list[0])
    }
}
