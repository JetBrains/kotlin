package test.collections

import kotlin.test.*
import org.junit.Test as test

class SetOperationsTest {
    test fun distinct() {
        assertEquals(listOf(1, 3, 5), listOf(1, 3, 3, 1, 5, 1, 3).distinct())
        assertTrue(listOf<Int>().distinct().isEmpty())
    }

    test fun distinctBy() {
        assertEquals(listOf("some", "cat", "do"), arrayOf("some", "case", "cat", "do", "dog", "it").distinctBy { it.length() })
        assertTrue(charArrayOf().distinctBy { it }.isEmpty())
    }

    test fun union() {
        assertEquals(listOf(1, 3, 5), listOf(1, 3).union(listOf(5)).toList())
        assertEquals(listOf(1), listOf<Int>().union(listOf(1)).toList())
    }

    test fun subtract() {
        assertEquals(listOf(1, 3), listOf(1, 3).subtract(listOf(5)).toList())
        assertEquals(listOf(1, 3), listOf(1, 3, 5).subtract(listOf(5)).toList())
        assertTrue(listOf(1, 3, 5).subtract(listOf(1, 3, 5)).none())
        assertTrue(listOf<Int>().subtract(listOf(1)).none())
    }

    test fun intersect() {
        assertTrue(listOf(1, 3).intersect(listOf(5)).none())
        assertEquals(listOf(5), listOf(1, 3, 5).intersect(listOf(5)).toList())
        assertEquals(listOf(1, 3, 5), listOf(1, 3, 5).intersect(listOf(1, 3, 5)).toList())
        assertTrue(listOf<Int>().intersect(listOf(1)).none())
    }
}