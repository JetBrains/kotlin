package test.collections

import kotlin.test.*
import org.junit.Test as test

class SetOperationsTest {
    test fun distinct() {
        assertEquals(listOf(1, 3, 5), listOf(1, 3, 3, 1, 5, 1, 3).distinct().toList())
        assertEquals(listOf<Int>(), listOf<Int>().distinct().toList())
    }

    test fun union() {
        assertEquals(listOf(1, 3, 5), listOf(1, 3).union(listOf(5)).toList())
        assertEquals(listOf(1), listOf<Int>().union(listOf(1)).toList())
    }

    test fun subtract() {
        assertEquals(listOf(1, 3), listOf(1, 3).subtract(listOf(5)).toList())
        assertEquals(listOf(1, 3), listOf(1, 3, 5).subtract(listOf(5)).toList())
        assertEquals(listOf<Int>(), listOf(1, 3, 5).subtract(listOf(1, 3, 5)).toList())
        assertEquals(listOf<Int>(), listOf<Int>().subtract(listOf(1)).toList())
    }

    test fun intersect() {
        assertEquals(listOf<Int>(), listOf(1, 3).intersect(listOf(5)).toList())
        assertEquals(listOf(5), listOf(1, 3, 5).intersect(listOf(5)).toList())
        assertEquals(listOf(1, 3, 5), listOf(1, 3, 5).intersect(listOf(1, 3, 5)).toList())
        assertEquals(listOf<Int>(), listOf<Int>().intersect(listOf(1)).toList())
    }
}