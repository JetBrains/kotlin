/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package test.collections

import org.junit.Assume
import org.junit.Test
import kotlin.test.*

class IndexOverflowJVMTest {

    @BeforeTest
    fun checkIsNotIgnored() {
        Assume.assumeTrue(System.getProperty("kotlin.stdlib.test.long.sequences")?.toBoolean() ?: false)
    }


    companion object {
        fun <T> repeatCounted(value: T, count: Long = Int.MAX_VALUE + 1L): Sequence<T> = Sequence {
            object : Iterator<T> {
                var counter = count
                override fun hasNext(): Boolean = counter > 0
                override fun next(): T = value.also { counter-- }
            }
        }

        val maxIndexSequence = repeatCounted("k", (Int.MAX_VALUE + 1L) + 1L) // here the last index is one greater than Int.MAX_VALUE
        val maxIndexIterable = maxIndexSequence.asIterable()


        val longCountSequence = Sequence {
            object : Iterator<Long> {
                var counter = 0L
                override fun hasNext(): Boolean = true
                override fun next(): Long = counter++
            }
        }

        fun assertIndexOverflow(f: () -> Unit) {
            val ex = assertFailsWith<ArithmeticException>(block = f)
            assertTrue(ex.message!!.contains("index", ignoreCase = true))
        }

        fun assertCountOverflow(f: () -> Unit) {
            val ex = assertFailsWith<ArithmeticException>(block = f)
            assertTrue(ex.message!!.contains("count", ignoreCase = true))
        }

        fun checkIndexPositive(index: Int) {
            if (index < 0) fail("Encountered negative index")
        }
    }

    @Test
    fun indexOfOverflowSequence() {
        assertIndexOverflow { maxIndexSequence.indexOf("j") }
        assertIndexOverflow { maxIndexSequence.lastIndexOf("k") }
        assertIndexOverflow { maxIndexSequence.indexOfFirst { false } }
        assertIndexOverflow { maxIndexSequence.indexOfLast { false } }
    }

    @Test
    fun indexOfOverflowIterable() {
        assertIndexOverflow { maxIndexIterable.indexOf("j") }
        assertIndexOverflow { maxIndexIterable.lastIndexOf("k") }
        assertIndexOverflow { maxIndexIterable.indexOfFirst { false } }
        assertIndexOverflow { maxIndexIterable.indexOfLast { false } }
    }


    @Test
    fun forEachIndexedOverflow() {
        assertIndexOverflow { maxIndexSequence.forEachIndexed { index, _ -> checkIndexPositive(index) } }
        assertIndexOverflow { maxIndexIterable.forEachIndexed { index, _ -> checkIndexPositive(index) } }
    }

    @Test
    fun withIndexOverflow() {
        assertIndexOverflow { maxIndexSequence.withIndex().forEach { (index, _) -> checkIndexPositive(index) } }
        assertIndexOverflow { maxIndexIterable.withIndex().forEach { (index, _) -> checkIndexPositive(index) } }
    }


    @Test
    fun countOverflow() {
        assertCountOverflow { repeatCounted("k").count() }
        assertCountOverflow { repeatCounted("k").count { true } }
        assertCountOverflow { repeatCounted("k").asIterable().count() }
        assertCountOverflow { repeatCounted("k").asIterable().count { true } }
    }

    @Test
    fun averageCountOverflow() {
        assertCountOverflow { repeatCounted(1.0).average() }
        assertCountOverflow { repeatCounted(1L).asIterable().average() }
    }


    private class CountingCollection<T> : AbstractMutableCollection<T>() {
        private var _size = 0

        override fun add(element: T): Boolean {
            if (_size < 0) error("Collection is too long")
            _size++
            return true
        }

        override val size: Int get() = _size

        override fun iterator(): MutableIterator<T> = error("not implemented")
    }

    @Test
    fun mapIndexedOverflow() {
        assertIndexOverflow { maxIndexSequence.mapIndexed { index, _ -> checkIndexPositive(index) }.forEach { } }
        assertIndexOverflow { maxIndexSequence.mapIndexedTo(CountingCollection()) { index, _ -> checkIndexPositive(index) } }
        assertIndexOverflow { maxIndexIterable.mapIndexedTo(CountingCollection()) { index, _ -> checkIndexPositive(index) } }
    }

    @Test
    fun mapNotNullIndexedOverflow() {
        assertIndexOverflow { maxIndexSequence.mapIndexedNotNull { index, _ -> checkIndexPositive(index) }.forEach { } }
        assertIndexOverflow { maxIndexSequence.mapIndexedNotNullTo(CountingCollection()) { index, _ -> checkIndexPositive(index) } }
        assertIndexOverflow { maxIndexIterable.mapIndexedNotNullTo(CountingCollection()) { index, _ -> checkIndexPositive(index) } }
    }

    @Test
    fun filterIndexedOverflow() {
        assertIndexOverflow { maxIndexSequence.filterIndexed { index, _ -> checkIndexPositive(index); true }.forEach { } }
        assertIndexOverflow { maxIndexSequence.filterIndexedTo(CountingCollection()) { index, _ -> checkIndexPositive(index); true } }
        assertIndexOverflow { maxIndexIterable.filterIndexedTo(CountingCollection()) { index, _ -> checkIndexPositive(index); true } }
    }


    @Test
    fun foldIndexedOverflow() {
        assertIndexOverflow { maxIndexSequence.foldIndexed("") { index, acc, s -> checkIndexPositive(index); s } }
        assertIndexOverflow { maxIndexIterable.foldIndexed("") { index, acc, s -> checkIndexPositive(index); s } }
    }

    @Test
    fun reduceIndexedOverflow() {
        assertIndexOverflow { maxIndexSequence.reduceIndexed { index, acc, s -> checkIndexPositive(index); s } }
        assertIndexOverflow { maxIndexIterable.reduceIndexed { index, acc, s -> checkIndexPositive(index); s } }
    }


    @Test
    fun dropTwiceMaxValue() {

        val halfMax = (1 shl 30) + 1

        val dropOnce = longCountSequence.drop(halfMax)
        val dropTwice = dropOnce.drop(halfMax)

        val expectedEnd = halfMax.toLong() * 2

        assertEquals(expectedEnd, dropTwice.first())

        val dropTake = dropOnce.take(halfMax + 1)

        assertEquals(expectedEnd, dropTake.last())
    }

}