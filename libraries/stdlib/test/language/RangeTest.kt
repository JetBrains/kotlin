package language

import org.junit.Test as test
import kotlin.test.*

class RangeTest {
    test fun upRange() {
        val range = 0.rangeTo(9)
        println("Have created up range: $range")
        assertTrue(range.contains(0))
        assertTrue(range.contains(1))
        assertTrue(range.contains(9))
        assertFalse(range.contains(10))
    }

    test fun downRange() {
        val range = 9.downTo(0)
        println("Have created down range: $range")
        assertTrue(range.contains(0))
        assertTrue(range.contains(1))
        assertTrue(range.contains(9))
        assertFalse(range.contains(10))
    }

    test fun reversedRanges() {
        val intRange = 0..9
        val reversedIntRange = intRange.reversed()
        assertEquals(9, reversedIntRange.start)
        assertEquals(0, reversedIntRange.end)
        assertEquals(intRange.toList(), reversedIntRange.reversed().toList())

        val doubleRange = 0.0..9.0
        val reversedDoubleRange = doubleRange.reversed()
        assertEquals(9.0, reversedDoubleRange.start)
        assertEquals(0.0, reversedDoubleRange.end)
        assertEquals(doubleRange.toList(), reversedDoubleRange.reversed().toList())
    }
}