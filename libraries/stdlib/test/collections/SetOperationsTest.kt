package test.collections

import kotlin.test.*
import org.junit.Test

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