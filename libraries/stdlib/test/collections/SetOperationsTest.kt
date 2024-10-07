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
        assertEquals(listOf(1, 3, 5), listOf(1, 3, 5).intersect(listOf(5, 3, 1)).toList())
        assertTrue(listOf<Int>().intersect(listOf(1)).none())
    }

    @Test fun intersectIntArray() {
        assertTrue(intArrayOf(1, 3).intersect(listOf(5)).none())
        assertEquals(setOf(5), intArrayOf(1, 3, 5).intersect(listOf(5)))
        assertEquals(listOf(1, 3, 5), intArrayOf(1, 3, 5).intersect(listOf(5, 3, 1)).toList())
        assertTrue(intArrayOf().intersect(listOf(1)).none())
    }

    @Test fun intersectLongArray() {
        assertTrue(longArrayOf(1L, 3L).intersect(listOf(5L)).none())
        assertEquals(setOf(5L), longArrayOf(1L, 3L, 5L).intersect(listOf(5L)))
        assertEquals(listOf(1L, 3L, 5L), longArrayOf(1L, 3L, 5L).intersect(listOf(5L, 3L, 1L)).toList())
        assertTrue(longArrayOf().intersect(listOf(1L)).none())
    }

    @Test fun intersectShortArray() {
        assertTrue(shortArrayOf(1, 3).intersect(listOf(5)).none())
        assertEquals(setOf(5.toShort()), shortArrayOf(1, 3, 5).intersect(listOf(5)))
        assertEquals(listOf(1, 3, 5).map(Int::toShort), shortArrayOf(1, 3, 5).intersect(listOf(5, 3, 1)).toList())
        assertTrue(shortArrayOf().intersect(listOf(1)).none())
    }

    @Test fun intersectByteArray() {
        assertTrue(byteArrayOf(1, 3).intersect(listOf(5)).none())
        assertEquals(setOf(5.toByte()), byteArrayOf(1, 3, 5).intersect(listOf(5)))
        assertEquals(listOf(1, 3, 5).map(Int::toByte), byteArrayOf(1, 3, 5).intersect(listOf(5, 3, 1)).toList())
        assertTrue(byteArrayOf().intersect(listOf(1)).none())
    }

    @Test fun intersectCharArray() {
        assertTrue(charArrayOf('a', 'c').intersect(listOf('b')).none())
        assertEquals(setOf('e'), charArrayOf('a', 'c', 'e').intersect(listOf('e')))
        assertEquals(listOf('a', 'c', 'e'), charArrayOf('a', 'c', 'e').intersect(listOf('e', 'c', 'a')).toList())
        assertTrue(charArrayOf().intersect(listOf('x')).none())
    }

    @Test fun intersectFloatArray() {
        assertTrue(floatArrayOf(1f, 3f).intersect(listOf(5f)).none())
        assertEquals(setOf(5f), floatArrayOf(1f, 3f, 5f).intersect(listOf(5f)))
        assertEquals(listOf(1f, 3f, 5f), floatArrayOf(1f, 3f, 5f).intersect(listOf(5f, 3f, 1f)).toList())
        assertTrue(floatArrayOf().intersect(listOf(1f)).none())
    }

    @Test fun intersectDoubleArray() {
        assertTrue(doubleArrayOf(1.0, 3.0).intersect(listOf(5.0)).none())
        assertEquals(setOf(5.0), doubleArrayOf(1.0, 3.0, 5.0).intersect(listOf(5.0)))
        assertEquals(listOf(1.0, 3.0, 5.0), doubleArrayOf(1.0, 3.0, 5.0).intersect(listOf(5.0, 3.0, 1.0)).toList())
        assertTrue(doubleArrayOf().intersect(listOf(1.0)).none())
    }

    @Test fun intersectArray() {
        data class O(val x: Int)

        assertTrue(arrayOf(O(1), O(3)).intersect(listOf(O(5))).none())
        assertEquals(setOf(O(5)), arrayOf(O(1), O(3), O(5)).intersect(listOf(O(5))))
        assertEquals(listOf(O(1), O(3), O(5)), arrayOf(O(1), O(3), O(5)).intersect(listOf(O(5), O(3), O(1))).toList())
        assertTrue(arrayOf<O>().intersect(listOf(1)).none())
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

    @Test fun intersectArrayWithIdentitySet() {
        data class Obj(val x: Int)

        val a = Obj(1)
        val b = Obj(2)
        val c = Obj(1)

        assertEquals(setOf(b, c), arrayOf(a, b, c).intersect(IdentitySet(b, c)))
        assertEquals(setOf(b, c), arrayOf(c, b, a).intersect(IdentitySet(b, c)))
        assertEquals(setOf(b), arrayOf(b, c).intersect(IdentitySet(b, a)))
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
