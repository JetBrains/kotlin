package language

import org.junit.Test as test
import kotlin.test.*

class RangeTest {
    test fun upRange() {
        val range = 0.upto(9)
        println("Have created up range: $range")
        assertEquals(10, range.size)
/*
        TODO runtime bug: java.lang.NoSuchMethodError: jet.IntRange.contains(I)Z
        at language.RangeTest.ranges(RangeTest.kt:11)

        see http://youtrack.jetbrains.com/issue/KT-2520

        assertTrue(range.contains(0))
        assertTrue(range.contains(1))
        assertTrue(range.contains(9))
        assertFalse(range.contains(10))
*/
    }

    test fun downRange() {
        val range = 9.downto(0)
        println("Have created down range: $range")
        assertEquals(10, range.size)
/*
        TODO runtime bug: java.lang.NoSuchMethodError: jet.IntRange.contains(I)Z
        at language.RangeTest.ranges(RangeTest.kt:11)

        see http://youtrack.jetbrains.com/issue/KT-2520

        assertTrue(range.contains(0))
        assertTrue(range.contains(1))
        assertTrue(range.contains(9))
        assertFalse(range.contains(10))
*/
    }
}