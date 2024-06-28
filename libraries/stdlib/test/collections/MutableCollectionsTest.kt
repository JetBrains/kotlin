/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections

import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.test.*


class MutableCollectionTest {
    fun <T, C : MutableCollection<T>> testOperation(before: List<T>, after: List<T>, expectedModified: Boolean, toMutableCollection: (List<T>) -> C) =
        fun(operation: (C.() -> Boolean)) {
            val list = toMutableCollection(before)
            assertEquals(expectedModified, list.operation())
            assertEquals(toMutableCollection(after), list)
        }

    fun <T> testOperation(before: List<T>, after: List<T>, expectedModified: Boolean) =
        testOperation(before, after, expectedModified, { it.toMutableList() })


    @Test fun addAll() {
        val data = listOf("foo", "bar")

        testOperation(emptyList(), data, true).let { assertAdd ->
            assertAdd { addAll(data) }
            assertAdd { addAll(data.toTypedArray()) }
            assertAdd { addAll(data.toTypedArray().asIterable()) }
            assertAdd { addAll(data.asSequence()) }
        }

        testOperation(data, data, false, { it.toCollection(LinkedHashSet()) }).let { assertAdd ->
            assertAdd { addAll(data) }
            assertAdd { addAll(data.toTypedArray()) }
            assertAdd { addAll(data.toTypedArray().asIterable()) }
            assertAdd { addAll(data.asSequence()) }
        }
    }

    private fun <T> forAllStandardMutableLists(data: List<T>, block: (MutableList<T>) -> Unit) {
        block(data.toMutableList())
        block(ArrayDeque(data))
        buildList {
            addAll(data)
            block(this)
        }
    }

    @Test fun addAllAtIndex() {
        val original = List(15000) { Random.nextInt() }
        for (insertSize in listOf(0, 1, 10, 1000, 20000)) {
            val insertion = List(insertSize) { Random.nextInt() }
            for (index in listOf(0, original.size) + List(10) { Random.nextInt(original.indices) }) {
                forAllStandardMutableLists(original) { mutable ->
                    mutable.addAll(index, insertion)

                    assertEquals(original.size + insertSize, mutable.size)
                    val tailIndex = index + insertSize
                    for (i in 0..<index) {
                        assertEquals(original[i], mutable[i])
                    }
                    for (i in index..<tailIndex) {
                        assertEquals(insertion[i - index], mutable[i])
                    }
                    for (i in tailIndex..<mutable.size) {
                        assertEquals(original[i - insertSize], mutable[i])
                    }
                }
            }
        }
    }

    @Test fun removeFirst() {
        forAllStandardMutableLists(listOf("first", "second")) { list ->
            assertEquals("first", list.removeFirst())
            assertEquals("second", list.removeFirstOrNull())

            assertNull(list.removeFirstOrNull())
            assertFailsWith<NoSuchElementException> { list.removeFirst() }
        }
    }

    @Test fun removeLast() {
        forAllStandardMutableLists(listOf("first", "second")) { list ->
            assertEquals("second", list.removeLast())
            assertEquals("first", list.removeLastOrNull())

            assertNull(list.removeLastOrNull())
            assertFailsWith<NoSuchElementException> { list.removeLast() }
        }
    }

    @Test fun removeAll() {
        val content = listOf("foo", "bar", "bar")
        val data = listOf("bar")
        val expected = listOf("foo")

        testOperation(content, expected, true).let { assertRemove ->
            assertRemove { removeAll(data) }
            assertRemove { removeAll(data.toTypedArray()) }
            assertRemove { removeAll(data.toTypedArray().asIterable()) }
            assertRemove { removeAll { it in data } }
            assertRemove { (this as MutableIterable<String>).removeAll { it in data } }
            val predicate = { cs: CharSequence -> cs.first() == 'b' }
            assertRemove { removeAll(predicate) }
        }


        testOperation(content, content, false).let { assertRemove ->
            assertRemove { removeAll(emptyList()) }
            assertRemove { removeAll(emptyArray()) }
            assertRemove { removeAll(emptySequence()) }
            assertRemove { removeAll { false } }
            assertRemove { (this as MutableIterable<String>).removeAll { false } }
        }
    }

    @Test fun retainAll() {
        val content = listOf("foo", "bar", "bar")
        val expected = listOf("bar", "bar")

        testOperation(content, expected, true).let { assertRetain ->
            val data = listOf("bar")
            assertRetain { retainAll(data) }
            assertRetain { retainAll(data.toTypedArray()) }
            assertRetain { retainAll(data.toTypedArray().asIterable()) }
            assertRetain { retainAll(data.asSequence()) }
            assertRetain { retainAll { it in data } }
            assertRetain { (this as MutableIterable<String>).retainAll { it in data } }

            val predicate = { cs: CharSequence -> cs.first() == 'b' }
            assertRetain { retainAll(predicate) }
        }
        testOperation(content, emptyList(), true).let { assertRetain ->
            val data = emptyList<String>()
            assertRetain { retainAll(data) }
            assertRetain { retainAll(data.toTypedArray()) }
            assertRetain { retainAll(data.toTypedArray().asIterable()) }
            assertRetain { retainAll(data.asSequence()) }
            assertRetain { retainAll { it in data } }
            assertRetain { (this as MutableIterable<String>).retainAll { it in data } }
        }
        testOperation(emptyList<String>(), emptyList(), false).let { assertRetain ->
            val data = emptyList<String>()
            assertRetain { retainAll(data) }
            assertRetain { retainAll(data.toTypedArray()) }
            assertRetain { retainAll(data.toTypedArray().asIterable()) }
            assertRetain { retainAll(data.asSequence()) }
            assertRetain { retainAll { it in data } }
            assertRetain { (this as MutableIterable<String>).retainAll { it in data } }
        }
    }

    @Test fun listFill() {
        forAllStandardMutableLists(List(3) { it }) { list ->
            list.fill(42)
            assertEquals(listOf(42, 42, 42), list)
        }
    }

    @Test fun shuffle() {
        val list = List(100) { it }
        forAllStandardMutableLists(list) { shuffled ->
            shuffled.shuffle()
            assertNotEquals(list, shuffled)
            assertEquals(list.toSet(), shuffled.toSet())
            assertEquals(list.size, shuffled.distinct().size)
        }
    }

    @Test fun shufflePredictably() {
        val list = List(10) { it }
        repeat(2) {
            forAllStandardMutableLists(list) { shuffled1 ->
                shuffled1.shuffle(Random(1))
                assertEquals("[1, 4, 0, 6, 2, 8, 9, 7, 3, 5]", shuffled1.toString())
            }
        }

        forAllStandardMutableLists(list) { shuffled2 ->
            shuffled2.shuffle(Random(42))
            assertEquals("[5, 0, 4, 9, 2, 8, 1, 7, 6, 3]", shuffled2.toString())
        }
    }

}
