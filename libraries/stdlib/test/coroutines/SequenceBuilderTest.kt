/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test.coroutines

import kotlin.test.*
import kotlin.coroutines.experimental.buildSequence
import kotlin.coroutines.experimental.buildIterator

class SequenceBuilderTest {
    @Test
    fun testSimple() {
        val result = buildSequence {
            for (i in 1..3) {
                yield(2 * i)
            }
        }

        assertEquals(listOf(2, 4, 6), result.toList())
        // Repeated calls also work
        assertEquals(listOf(2, 4, 6), result.toList())
    }

    @Test
    fun testCallHasNextSeveralTimes() {
        val result = buildSequence {
            yield(1)
        }

        val iterator = result.iterator()

        assertTrue(iterator.hasNext())
        assertTrue(iterator.hasNext())
        assertTrue(iterator.hasNext())

        assertEquals(1, iterator.next())

        assertFalse(iterator.hasNext())
        assertFalse(iterator.hasNext())
        assertFalse(iterator.hasNext())

        assertFailsWith<NoSuchElementException> { iterator.next() }
    }

    @Test
    fun testManualIteration() {
        val result = buildSequence {
            yield(1)
            yield(2)
            yield(3)
        }

        val iterator = result.iterator()

        assertTrue(iterator.hasNext())
        assertTrue(iterator.hasNext())
        assertEquals(1, iterator.next())

        assertTrue(iterator.hasNext())
        assertTrue(iterator.hasNext())
        assertEquals(2, iterator.next())

        assertEquals(3, iterator.next())

        assertFalse(iterator.hasNext())
        assertFalse(iterator.hasNext())

        assertFailsWith<NoSuchElementException> { iterator.next() }

        assertEquals(1, result.iterator().next())
    }

    @Test
    fun testEmptySequence() {
        val result = buildSequence<Int> {}
        val iterator = result.iterator()

        assertFalse(iterator.hasNext())
        assertFalse(iterator.hasNext())

        assertFailsWith<NoSuchElementException> { iterator.next() }
    }

    @Test
    fun testLaziness() {
        var sharedVar = -2
        val result = buildSequence {
            while (true) {
                when (sharedVar) {
                    -1 -> return@buildSequence
                    -2 -> error("Invalid state: -2")
                    else -> yield(sharedVar)
                }
            }
        }

        val iterator = result.iterator()

        sharedVar = 1
        assertTrue(iterator.hasNext())
        assertEquals(1, iterator.next())

        sharedVar = 2
        assertTrue(iterator.hasNext())
        assertEquals(2, iterator.next())

        sharedVar = 3
        assertTrue(iterator.hasNext())
        assertEquals(3, iterator.next())

        sharedVar = -1
        assertFalse(iterator.hasNext())
        assertFailsWith<NoSuchElementException> { iterator.next() }
    }

    @Test
    fun testExceptionInCoroutine() {
        var sharedVar = -2
        val result = buildSequence {
            while (true) {
                when (sharedVar) {
                    -1 -> return@buildSequence
                    -2 -> throw UnsupportedOperationException("-2 is unsupported")
                    else -> yield(sharedVar)
                }
            }
        }

        val iterator = result.iterator()

        sharedVar = 1
        assertEquals(1, iterator.next())

        sharedVar = -2
        assertFailsWith<UnsupportedOperationException> { iterator.hasNext() }
        assertFailsWith<IllegalStateException> { iterator.hasNext() }
        assertFailsWith<IllegalStateException> { iterator.next() }
    }

    @Test
    fun testParallelIteration() {
        var inc = 0
        val result = buildSequence {
            for (i in 1..3) {
                inc++
                yield(inc * i)
            }
        }

        assertEquals(listOf(Pair(1, 2), Pair(6, 8), Pair(15, 18)), result.zip(result).toList())
    }

    @Test
    fun testYieldAllIterator() {
        val result = buildSequence {
            yieldAll(listOf(1, 2, 3).iterator())
        }
        assertEquals(listOf(1, 2, 3), result.toList())
    }

    @Test
    fun testYieldAllSequence() {
        val result = buildSequence {
            yieldAll(sequenceOf(1, 2, 3))
        }
        assertEquals(listOf(1, 2, 3), result.toList())
    }

    @Test
    fun testYieldAllCollection() {
        val result = buildSequence {
            yieldAll(listOf(1, 2, 3))
        }
        assertEquals(listOf(1, 2, 3), result.toList())
    }

    @Test
    fun testYieldAllCollectionMixedFirst() {
        val result = buildSequence {
            yield(0)
            yieldAll(listOf(1, 2, 3))
        }
        assertEquals(listOf(0, 1, 2, 3), result.toList())
    }

    @Test
    fun testYieldAllCollectionMixedLast() {
        val result = buildSequence {
            yieldAll(listOf(1, 2, 3))
            yield(4)
        }
        assertEquals(listOf(1, 2, 3, 4), result.toList())
    }

    @Test
    fun testYieldAllCollectionMixedBoth() {
        val result = buildSequence {
            yield(0)
            yieldAll(listOf(1, 2, 3))
            yield(4)
        }
        assertEquals(listOf(0, 1, 2, 3, 4), result.toList())
    }

    @Test
    fun testYieldAllCollectionMixedLong() {
        val result = buildSequence {
            yield(0)
            yieldAll(listOf(1, 2, 3))
            yield(4)
            yield(5)
            yieldAll(listOf(6))
            yield(7)
            yieldAll(listOf())
            yield(8)
        }
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8), result.toList())
    }

    @Test
    fun testYieldAllCollectionOneEmpty() {
        val result = buildSequence<Int> {
            yieldAll(listOf())
        }
        assertEquals(listOf(), result.toList())
    }

    @Test
    fun testYieldAllCollectionManyEmpty() {
        val result = buildSequence<Int> {
            yieldAll(listOf())
            yieldAll(listOf())
            yieldAll(listOf())
        }
        assertEquals(listOf(), result.toList())
    }

    @Test
    fun testYieldAllSideEffects() {
        val effects = arrayListOf<Any>()
        val result = buildSequence {
            effects.add("a")
            yieldAll(listOf(1, 2))
            effects.add("b")
            yieldAll(listOf())
            effects.add("c")
            yieldAll(listOf(3))
            effects.add("d")
            yield(4)
            effects.add("e")
            yieldAll(listOf())
            effects.add("f")
            yield(5)
        }

        for (res in result) {
            effects.add("(") // marks step start
            effects.add(res)
            effects.add(")") // marks step end
        }
        assertEquals(
                listOf(
                        "a",
                        "(", 1, ")",
                        "(", 2, ")",
                        "b", "c",
                        "(", 3, ")",
                        "d",
                        "(", 4, ")",
                        "e", "f",
                        "(", 5, ")"
                ),
                effects.toList()
        )
    }

    @Test
    fun testInfiniteYieldAll() {
        val values = buildIterator {
            while (true) {
                yieldAll((1..5).map { it })
            }
        }

        var sum = 0
        repeat(10) {
            sum += values.next() //.also(::println)
        }
        assertEquals(30, sum)
    }
}
