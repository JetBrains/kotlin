/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections

import kotlin.test.*

class SetOperationsTest {
    @Test fun distinct() {
        assertEquals(listOf(1, 3, 5), listOf(1, 3, 3, 1, 5, 1, 3).distinct())
        assertTrue(listOf<Int>().distinct().isEmpty())
    }

    @Test fun distinctBy() {
        assertEquals(listOf("some", "cat", "do"), arrayOf("some", "case", "cat", "do", "dog", "it").distinctBy { it.length })
        assertTrue(charArrayOf().distinctBy { it }.isEmpty())
    }

    @Test fun union() {
        assertEquals(listOf(1, 3, 5), listOf(1, 3).union(listOf(5)).toList())
        assertEquals(listOf(1), listOf<Int>().union(listOf(1)).toList())
    }

    @Test fun subtract() {
        assertEquals(listOf(1, 3), listOf(1, 3).subtract(listOf(5)).toList())
        assertEquals(listOf(1, 3), listOf(1, 3, 5).subtract(listOf(5)).toList())
        assertTrue(listOf(1, 3, 5).subtract(listOf(1, 3, 5)).none())
        assertTrue(listOf<Int>().subtract(listOf(1)).none())
    }

    @Test fun intersect() {
        assertTrue(listOf(1, 3).intersect(listOf(5)).none())
        assertEquals(listOf(5), listOf(1, 3, 5).intersect(listOf(5)).toList())
        assertEquals(listOf(1, 3, 5), listOf(1, 3, 5).intersect(listOf(1, 3, 5)).toList())
        assertTrue(listOf<Int>().intersect(listOf(1)).none())
    }

    @Test fun intersectWithIdentitySet() {
        data class Obj(val x: Int)

        val a = Obj(1)
        val b = Obj(2)
        val c = Obj(1)

        assertEquals(setOf(b, c), listOf(a, b, c).intersect(IdentitySet(b, c)))
        assertEquals(setOf(b, c), listOf(c, b, a).intersect(IdentitySet(b, c)))
        assertEquals(setOf(b), listOf(b, c).intersect(IdentitySet(b, a)))
    }

    fun testPlus(doPlus: (Set<String>) -> Set<String>) {
        val set = setOf("foo", "bar")
        val set2: Set<String> = doPlus(set)
        assertEquals(setOf("foo", "bar"), set)
        assertEquals(setOf("foo", "bar", "cheese", "wine"), set2)
    }

    @Test fun plusElement() = testPlus { it + "bar" + "cheese" + "wine" }
    @Test fun plusCollection() = testPlus { it + listOf("bar", "cheese", "wine") }
    @Test fun plusArray() = testPlus { it + arrayOf("bar", "cheese", "wine") }
    @Test fun plusSequence() = testPlus { it + sequenceOf("bar", "cheese", "wine") }

    @Test fun plusAssign() {
        // lets use a mutable variable
        var set = setOf("a")
        val setOriginal = set
        set += "foo"
        set += listOf("beer", "a")
        set += arrayOf("cheese", "beer")
        set += sequenceOf("bar", "foo")
        assertEquals(setOf("a", "foo", "beer", "cheese", "bar"), set)
        assertTrue(set !== setOriginal)

        val mset = mutableSetOf("a")
        mset += "foo"
        mset += listOf("beer", "a")
        mset += arrayOf("cheese", "beer")
        mset += sequenceOf("bar", "foo")
        assertEquals(set, mset)
    }

    private fun testMinus(doMinus: (Set<String>) -> Set<String>) {
        val a = setOf("foo", "bar")
        val b: Set<String> = doMinus(a)
        assertEquals(setOf("foo"), b)
    }

    @Test fun minusElement() = testMinus { it - "bar" - "zoo" }
    @Test fun minusCollection() = testMinus { it - listOf("bar", "zoo") }
    @Test fun minusArray() = testMinus { it - arrayOf("bar", "zoo") }
    @Test fun minusSequence() = testMinus { it - sequenceOf("bar", "zoo") }
}

private class IdentitySet<T : Any>(vararg val elements: T) : AbstractSet<T>() {
    init {
        for (idx1 in elements.indices) {
            for (idx2 in elements.indices) {
                require(idx1 == idx2 || elements[idx1] !== elements[idx2])
            }
        }
    }

    override val size: Int
        get() = elements.size

    override fun iterator(): Iterator<T> = elements.iterator()

    override fun contains(element: T): Boolean = elements.any { it === element }
}
