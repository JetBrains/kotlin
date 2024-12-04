/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections

import test.*
import test.collections.behaviors.iteratorBehavior
import test.collections.behaviors.listIteratorBehavior
import test.collections.behaviors.listIteratorProperties
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.test.*

class ArrayDequeTest {
    @Test
    fun arrayDequeInit() {
        listOf(ArrayDeque<Int>(), ArrayDeque(10)).forEach { deque ->
            assertTrue(deque.isEmpty())
            assertNull(deque.firstOrNull())
            assertNull(deque.lastOrNull())
            assertNull(deque.removeFirstOrNull())
            assertNull(deque.removeLastOrNull())
        }

        ArrayDeque(listOf(0, 1, 2, 3, 4)).let { deque ->
            assertFalse(deque.isEmpty())
            assertEquals(0, deque.firstOrNull())
            assertEquals(4, deque.lastOrNull())
            assertEquals(0, deque.removeFirstOrNull())
            assertEquals(4, deque.removeLastOrNull())
        }
    }

    @Test
    fun size() {
        listOf(ArrayDeque<Int>(), ArrayDeque(10)).forEach { deque ->
            // head == tail
            assertEquals(0, deque.size)
            // head > tail
            deque.addFirst(-1)
            assertEquals(1, deque.size)
            // buffer expansion, head < tail
            deque.addAll(listOf(1, 2, 3, 4, 5, 6, 7))
            assertEquals(8, deque.size)
        }

        ArrayDeque(listOf(0, 1, 2, 3, 4)).let { deque ->
            // head < tail
            assertEquals(5, deque.size)
            // head > tail
            deque.addFirst(-1)
            assertEquals(6, deque.size)
            // buffer expansion, head < tail
            deque.addAll(listOf(5, 6, 7))
            assertEquals(9, deque.size)
        }
    }

    @Test
    fun contains() {
        val originalItems = listOf(0, 1, 2, 3, 4)
        val extraItems = listOf(-1, 5)
        val deque = ArrayDeque(originalItems)
        // head < tail
        originalItems.forEach { assertTrue(deque.contains(it), "Expected $it in $deque") }
        extraItems.forEach { assertFalse(deque.contains(it), "Not expected $it in $deque") }

        // head > tail
        deque.addFirst(-1)
        deque.addLast(5)

        (originalItems + extraItems).forEach { assertTrue(deque.contains(it), "Expected $it in $deque") }

        // remove, head > tail
        deque.remove(2)

        assertTrue(deque.contains(1))
        assertFalse(deque.contains(2))
        assertTrue(deque.contains(3))

        // remove, head < tail
        deque.remove(-1)

        assertTrue(deque.contains(5))
        assertFalse(deque.contains(-1))
        assertTrue(deque.contains(0))
    }

    @Test
    fun clear() = testArrayDeque { bufferSize: Int, _: Int, head: Int, tail: Int ->
        val deque = generateArrayDeque(head, tail, bufferSize).apply { clear() }
        assertTrue(deque.isEmpty())
    }

    @Test
    fun removeElement() {
        val deque = ArrayDeque<Int>()
        deque.addLast(0)
        deque.addLast(1)
        deque.addLast(2)
        deque.addLast(3)
        assertTrue(deque.contains(2))
        deque.remove(2)
        assertFalse(deque.contains(2))

        deque.addFirst(-1)
        assertTrue(deque.containsAll(listOf(-1, 0, 1, 3)))
        deque.remove(-1)
        assertTrue(deque.containsAll(listOf(0, 1, 3)))
        deque.remove(0)
        assertTrue(deque.containsAll(listOf(1, 3)))
    }

    @Test
    fun iterator() {
        val deque = ArrayDeque<Int>()

        deque.addLast(0)
        deque.addLast(1)
        deque.addLast(2)
        deque.addLast(3)
        compare(deque.iterator(), listOf(0, 1, 2, 3).iterator()) { iteratorBehavior() }

        deque.addFirst(-1)
        compare(deque.iterator(), listOf(-1, 0, 1, 2, 3).iterator()) { iteratorBehavior() }
    }

    @Test
    fun first() {
        val deque = ArrayDeque<Int>()
        assertFailsWith<NoSuchElementException> { deque.first() }

        deque.addLast(0)
        deque.addLast(1)
        assertEquals(0, deque.first())

        deque.addFirst(-1)
        assertEquals(-1, deque.first())

        deque.removeFirst()
        assertEquals(0, deque.first())

        deque.clear()
        assertFailsWith<NoSuchElementException> { deque.first() }
    }

    @Test
    fun firstOrNull() {
        val deque = ArrayDeque<Int>()
        assertNull(deque.firstOrNull())

        deque.addLast(0)
        deque.addLast(1)
        assertEquals(0, deque.firstOrNull())

        deque.addFirst(-1)
        assertEquals(-1, deque.firstOrNull())

        deque.removeFirst()
        assertEquals(0, deque.firstOrNull())

        deque.clear()
        assertNull(deque.firstOrNull())
    }

    @Test
    fun last() {
        val deque = ArrayDeque<Int>()
        assertFailsWith<NoSuchElementException> { deque.last() }

        deque.addLast(0)
        deque.addLast(1)
        assertEquals(1, deque.last())

        deque.addFirst(-1)
        assertEquals(1, deque.last())

        deque.removeLast()
        assertEquals(0, deque.last())

        deque.clear()
        assertFailsWith<NoSuchElementException> { deque.last() }
    }

    @Test
    fun lastOrNull() {
        val deque = ArrayDeque<Int>()
        assertNull(deque.lastOrNull())

        deque.addLast(0)
        deque.addLast(1)
        assertEquals(1, deque.lastOrNull())

        deque.addFirst(-1)
        assertEquals(1, deque.lastOrNull())

        deque.removeLast()
        assertEquals(0, deque.lastOrNull())

        deque.clear()
        assertNull(deque.lastOrNull())
    }

    @Test
    fun addFirst() {
        val deque = ArrayDeque<Int>()

        // head > tail
        listOf(-1, -2, -3).forEach {
            deque.addFirst(it)
            assertEquals(it, deque.first())
        }

        // head < tail
        listOf(-1, -2).forEach {
            assertEquals(it, deque.removeLast())
        }

        listOf(-4, -5, -6, -7, -8, -9).forEach {
            deque.addFirst(it)
            assertEquals(it, deque.first())
        }

        // buffer expansion, head < tail
        deque.addFirst(-10)
        assertEquals(-10, deque.first())
    }

    @Test
    fun addLast() {
        val deque = ArrayDeque<Int>()

        // head < tail
        listOf(0, 1, 2).forEach {
            deque.addLast(it)
            assertEquals(it, deque.last())
        }

        // head > tail
        listOf(0, 1).forEach {
            assertEquals(it, deque.removeFirst())
        }
        listOf(3, 4, 5, 6, 7, 8).forEach {
            deque.addLast(it)
            assertEquals(it, deque.last())
        }

        // buffer expansion, head < tail
        deque.addLast(9)
        assertEquals(9, deque.last())
    }

    @Test
    fun removeFirst() {
        val deque = ArrayDeque<Int>()
        assertFailsWith<NoSuchElementException> { deque.removeFirst() }

        deque.addLast(0)
        deque.addFirst(-1)
        deque.addFirst(-2)
        deque.addLast(1)

        assertEquals(-2, deque.removeFirst())
        assertEquals(-1, deque.removeFirst())
        assertEquals(0, deque.removeFirst())
        assertEquals(1, deque.removeFirst())

        assertFailsWith<NoSuchElementException> { deque.removeFirst() }
    }

    @Test
    fun removeFirstOrNull() {
        val deque = ArrayDeque<Int>()
        assertNull(deque.removeFirstOrNull())

        deque.addLast(0)
        deque.addFirst(-1)
        deque.addFirst(-2)
        deque.addLast(1)

        assertEquals(-2, deque.removeFirstOrNull())
        assertEquals(-1, deque.removeFirstOrNull())
        assertEquals(0, deque.removeFirstOrNull())
        assertEquals(1, deque.removeFirstOrNull())

        assertNull(deque.removeFirstOrNull())
    }

    @Test
    fun removeLast() {
        val deque = ArrayDeque<Int>()
        assertFailsWith<NoSuchElementException> { deque.removeLast() }

        deque.addLast(0)
        deque.addFirst(-1)
        deque.addFirst(-2)
        deque.addLast(1)

        assertEquals(1, deque.removeLast())
        assertEquals(0, deque.removeLast())
        assertEquals(-1, deque.removeLast())
        assertEquals(-2, deque.removeLast())

        assertFailsWith<NoSuchElementException> { deque.removeLast() }
    }

    @Test
    fun removeLastOrNull() {
        val deque = ArrayDeque<Int>()
        assertNull(deque.removeLastOrNull())

        deque.addLast(0)
        deque.addFirst(-1)
        deque.addFirst(-2)
        deque.addLast(1)

        assertEquals(1, deque.removeLastOrNull())
        assertEquals(0, deque.removeLastOrNull())
        assertEquals(-1, deque.removeLastOrNull())
        assertEquals(-2, deque.removeLastOrNull())

        assertNull(deque.removeLastOrNull())
    }

    @Test
    fun bufferExpansion() {
        val deque = ArrayDeque<Int>()

        deque.addAll(listOf(0, 1, 2, 3, 4, 5, 6, 7))
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6, 7), deque.toList())

        listOf(-1, -2, -3, -4, -5, -6, -7, -8).forEach { deque.addFirst(it) }
        assertEquals(listOf(-8, -7, -6, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7), deque.toList())
    }

    private fun generateArrayDeque(head: Int, tail: Int, bufferSize: Int? = null): ArrayDeque<Int> {
        check(tail >= 0)

        val deque = if (bufferSize != null) ArrayDeque<Int>(bufferSize) else ArrayDeque()

        repeat(tail) {
            deque.addLast(it)
            if (it < head) deque.removeFirst()
        }
        repeat(-head) { deque.addFirst(-(it + 1)) }

        check(tail - head == deque.size)

        return deque
    }

    private fun testArrayDeque(test: (bufferSize: Int, dequeSize: Int, head: Int, tail: Int) -> Unit) {
        for (bufferSize in listOf(0, 2, 8, 15)) {
            for (dequeSize in 0..bufferSize) {
                for (tail in 0 until bufferSize) {
                    val head = tail - dequeSize
                    test(bufferSize, dequeSize, head, tail)
                }
            }
        }
    }

    // MutableList operations
    @Test
    fun insert() = testArrayDeque { bufferSize: Int, dequeSize: Int, head: Int, tail: Int ->
        for (index in 0..dequeSize) {
            val deque = generateArrayDeque(head, tail, bufferSize).apply { add(index, 100) }

            val expectedHead = when {
                dequeSize == bufferSize ->          // buffer expansion
                    when {
                        index * 2 < dequeSize -> -1 // shift preceding elements
                        // shift succeeding elements
                        dequeSize + 1 >= bufferSize + bufferSize / 2 -> -(dequeSize + 1) // tail == 0 -> head becomes negative (head > tail)
                        else -> 0
                    }
                index * 2 < dequeSize -> head - 1   // shift preceding elements
                else ->                             // shift succeeding last elements
                    if (tail == bufferSize - 1)
                        head - bufferSize       // tail == 0 -> head becomes negative (head > tail)
                    else
                        head
            }
            val expectedElements = (head until tail).toMutableList().apply { add(index, 100) }

            @Suppress("INVISIBLE_MEMBER")
            deque.internalStructure { actualHead, actualElements ->
                assertEquals(expectedHead, actualHead, "bufferSize: $bufferSize, head: $head, tail: $tail, index: $index")
                assertEquals(expectedElements, actualElements.toList())
            }
        }
    }

    @Test
    fun removeAt() = testArrayDeque { bufferSize: Int, dequeSize: Int, head: Int, tail: Int ->
        for (index in 0 until dequeSize) {
            val deque = generateArrayDeque(head, tail, bufferSize).apply { removeAt(index) }

            val expectedHead = when {
                index < dequeSize / 2 -> head + 1   // shift preceding elements
                else ->                             // shift succeeding elements
                    if (tail == 0)
                        head + bufferSize   // tail == bufferSize - 1 -> head becomes positive(head < tail)
                    else
                        head
            }
            val expectedElements = (head until tail).toMutableList().apply { removeAt(index) }

            @Suppress("INVISIBLE_MEMBER")
            deque.internalStructure { actualHead, actualElements ->
                assertEquals(expectedHead, actualHead, "bufferSize: $bufferSize, head: $head, tail: $tail, index: $index")
                assertEquals(expectedElements, actualElements.toList())
            }
        }
    }

    @Test
    fun indexOf() {
        // head < tail
        generateArrayDeque(0, 7).let { deque ->
            (0..6).forEach { assertEquals(it, deque.indexOf(it)) }
            assertEquals(-1, deque.indexOf(100))
        }

        // head > tail
        generateArrayDeque(-4, 3).let { deque ->
            (0..6).forEach { assertEquals(it, deque.indexOf(it - 4)) }
            assertEquals(-1, deque.indexOf(100))
        }
    }

    @Test
    fun addAll() {
        // head < tail
        generateArrayDeque(0, 3).let { deque ->
            deque.addAll(listOf(3, 4, 5))
            assertEquals(listOf(0, 1, 2, 3, 4, 5), deque.toList())

            deque.addAll(6..100)
            assertEquals((0..100).toList(), deque.toList())
        }

        generateArrayDeque(4, 6).let { deque ->
            deque.addAll(listOf(6, 7, 8))
            assertEquals(listOf(4, 5, 6, 7, 8), deque.toList())

            deque.addAll(9..100)
            assertEquals((4..100).toList(), deque.toList())
        }

        // head > tail
        generateArrayDeque(-3, 2).let { deque ->
            deque.addAll(listOf(2, 3))
            assertEquals(listOf(-3, -2, -1, 0, 1, 2, 3), deque.toList())

            deque.addAll(4..100)
            assertEquals((-3..100).toList(), deque.toList())
        }
    }

    @Test
    fun insertAll() = testArrayDeque { bufferSize: Int, dequeSize: Int, head: Int, tail: Int ->
        repeat(bufferSize.coerceAtMost(3)) {
            val insertCollectionSize = Random.nextInt(0..bufferSize)
            val listToInsert = (0 until insertCollectionSize).map { 100 + it }

            for (index in 0..dequeSize) {
                val deque = generateArrayDeque(head, tail, bufferSize).apply { addAll(index, listToInsert) }

                val expectedHead = when {
                    dequeSize + listToInsert.size > bufferSize ->       // buffer expansion
                        when {
                            index * 2 < dequeSize -> -listToInsert.size // shift preceding elements
                            // shift succeeding elements
                            dequeSize + listToInsert.size >= bufferSize + bufferSize / 2 -> -(dequeSize + listToInsert.size) // tail == 0 -> head becomes negative (head > tail)
                            else -> 0
                        }
                    index * 2 < dequeSize -> head - listToInsert.size   // shift preceding elements
                    else ->                                             // shift succeeding elements
                        if (tail + listToInsert.size >= bufferSize)
                            head - bufferSize       // tail >= 0 -> head becomes negative (head > tail)
                        else
                            head
                }
                val expectedElements = (head until tail).toMutableList().apply { addAll(index, listToInsert) }

                @Suppress("INVISIBLE_MEMBER")
                deque.internalStructure { actualHead, actualElements ->
                    assertEquals(expectedHead, actualHead, "bufferSize: $bufferSize, head: $head, tail: $tail, index: $index")
                    assertEquals(expectedElements, actualElements.toList())
                }
            }
        }
    }

    @Test
    fun listIterator() = testArrayDeque { bufferSize: Int, dequeSize: Int, head: Int, tail: Int ->

        val elements = (head until tail).toList()
        val deque = generateArrayDeque(head, tail, bufferSize)
        repeat(dequeSize.coerceAtMost(3)) {
            val index = Random.nextInt(0..dequeSize)
            val expectedIterator = elements.listIterator(index)
            val actualIterator = deque.listIterator(index)

            compare(expectedIterator, actualIterator) { listIteratorBehavior() }
        }

        repeat(dequeSize.coerceAtMost(3)) {
            val index = Random.nextInt(0..dequeSize)
            val expectedIterator = (head until tail).toMutableList().listIterator(index)
            val actualIterator = generateArrayDeque(head, tail, bufferSize).listIterator(index)

            for (element in listOf(100, 101)) {
                expectedIterator.add(element)
                actualIterator.add(element)
                compare(expectedIterator, actualIterator) { listIteratorProperties() }
            }
        }

        repeat(dequeSize.coerceAtMost(3)) {
            val index = Random.nextInt(0..dequeSize)
            val expectedIterator = (head until tail).toMutableList().listIterator(index)
            val actualIterator = generateArrayDeque(head, tail, bufferSize).listIterator(index)

            repeat(times = 2) {
                if (expectedIterator.hasNext()) {
                    expectedIterator.next()
                    actualIterator.next()
                } else if (expectedIterator.hasPrevious()) {
                    expectedIterator.previous()
                    actualIterator.previous()
                }
                if (dequeSize > it) {
                    expectedIterator.remove()
                    actualIterator.remove()
                }
                compare(expectedIterator, actualIterator) { listIteratorProperties() }
            }
        }
    }

    @Test
    fun removeAll() = testArrayDeque { bufferSize: Int, _: Int, head: Int, tail: Int ->
        generateArrayDeque(head, tail, bufferSize).let { deque ->
            deque.removeAll(emptyList())
            assertEquals((head until tail).toList(), deque)

            val absentElements = listOf(head - 1, tail, Random.nextInt(tail..Int.MAX_VALUE))
            deque.removeAll(absentElements)
            assertEquals((head until tail).toList(), deque)

            deque.removeAll((head until tail).toList())
            assertTrue(deque.isEmpty())
        }

        val listToRemove = (head - 1 until tail + 1).filter { Random.nextBoolean() }

        val elements = (head until tail).toMutableList().apply { removeAll(listToRemove) }
        val deque = generateArrayDeque(head, tail, bufferSize).apply { removeAll(listToRemove) }

        assertEquals(elements, deque)
    }

    @Test
    fun retainAll() = testArrayDeque { bufferSize: Int, _: Int, head: Int, tail: Int ->
        val listToRetain = (head..tail).filter { Random.nextBoolean() }

        val elements = (head until tail).toMutableList().apply { retainAll(listToRetain) }
        val deque = generateArrayDeque(head, tail, bufferSize).apply { retainAll(listToRetain) }

        assertEquals(elements, deque)
    }

    @Test
    fun set() = testArrayDeque { bufferSize: Int, dequeSize: Int, head: Int, tail: Int ->
        for (index in 0 until dequeSize) {
            val elements = (head until tail).toMutableList()
            val deque = generateArrayDeque(head, tail, bufferSize)

            elements[index] = 100
            deque[index] = 100

            assertEquals(elements, deque)
        }
    }

    @Test
    fun get() = testArrayDeque { bufferSize: Int, dequeSize: Int, head: Int, tail: Int ->
        val elements = (head until tail).toList()
        val deque = generateArrayDeque(head, tail, bufferSize)

        for (index in 0 until dequeSize) {
            assertEquals(elements[index], deque[index])
        }
    }

    @Test
    fun subList() = testArrayDeque { bufferSize: Int, dequeSize: Int, head: Int, tail: Int ->
        val elements = (head until tail).toList()
        val deque = generateArrayDeque(head, tail, bufferSize)

        for (fromIndex in 0 until dequeSize) {
            for (toIndex in fromIndex..dequeSize) {
                assertEquals(elements.subList(fromIndex, toIndex), deque.subList(fromIndex, toIndex))
            }
        }
    }

    @Test
    fun removeRange() = testArrayDeque { bufferSize: Int, dequeSize: Int, head: Int, tail: Int ->
        for (fromIndex in 0..dequeSize) {
            for (toIndex in fromIndex..dequeSize) {
                val deque = generateArrayDeque(head, tail, bufferSize).apply { testRemoveRange(fromIndex, toIndex) }

                val length = toIndex - fromIndex
                val expectedHead = when {
                    length == 0 -> head
                    length == dequeSize -> 0
                    fromIndex < dequeSize - toIndex -> head + length   // shift preceding elements
                    else ->                                            // shift succeeding elements
                        if (tail <= length - 1)
                            head + bufferSize   // head becomes positive(head < tail)
                        else
                            head
                }

                val expectedElements = (head until tail).toMutableList().apply {
                    repeat(length) { removeAt(fromIndex) }
                }

                deque.internalStructure { actualHead, actualElements ->
                    assertEquals(
                        expectedHead,
                        actualHead,
                        "bufferSize: $bufferSize, head: $head, tail: $tail, fromIndex: $fromIndex, toIndex: $toIndex"
                    )
                    assertEquals(expectedElements, actualElements.toList())
                }
            }
        }
    }

    @Suppress("INVISIBLE_MEMBER")
    @Test
    fun toArray() {
        val deque = ArrayDeque<Int>()

        // empty
        assertTrue(deque.testToArray().isEmpty())

        fun testContentEquals(expected: Array<Int>) {
            assertTrue(expected contentEquals deque.testToArray())
            assertTrue(expected contentEquals deque.testToArray(emptyArray()))

            val dest = Array(expected.size + 2) { it + 100 }

            val expectedDest = buildList {
                addAll(expected)
                if (TestPlatform.current == TestPlatform.Jvm) {
                    add(null)
                } else {
                    add(expected.size + 100)
                }
                add(expected.size + 101)
            }.toTypedArray()

            val actual = deque.testToArray(dest)
            assertTrue(
                expectedDest contentEquals actual,
                message = "Expected: ${expectedDest.contentToString()}, Actual: ${actual.contentToString()}"
            )
        }

        // head < tail
        deque.addAll(listOf(0, 1, 2, 3))
        deque.internalStructure { head, _ -> assertEquals(0, head) }
        testContentEquals(arrayOf(0, 1, 2, 3))
        deque.removeFirst()
        deque.internalStructure { head, _ -> assertEquals(1, head) }
        testContentEquals(arrayOf(1, 2, 3))

        // head > tail
        deque.addFirst(-1)
        deque.addFirst(-2)
        deque.addFirst(-3)
        deque.internalStructure { head, _ -> assertEquals(-2, head) } // deque min capacity is 10
        testContentEquals(arrayOf(-3, -2, -1, 1, 2, 3))

        // head == tail
        deque.addAll(listOf(4, 5, 6, 7))
        deque.internalStructure { head, _ -> assertEquals(-2, head) } // deque min capacity is 10
        testContentEquals(arrayOf(-3, -2, -1, 1, 2, 3, 4, 5, 6, 7))
    }
}