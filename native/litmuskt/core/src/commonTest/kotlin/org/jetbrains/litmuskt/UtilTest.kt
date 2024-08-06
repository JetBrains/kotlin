package org.jetbrains.litmuskt

import kotlin.test.Test
import kotlin.test.assertEquals

class UtilTest {

    @Test
    fun testRangeSplitEqual() {
        assertEquals(
            listOf(0..<4, 4..<8, 8..<11),
            (0..<11).splitEqual(3)
        )
        assertEquals(
            listOf(0..<3, 3..<5, 5..<7, 7..<9, 9..<11),
            (0..<11).splitEqual(5)
        )
        assertEquals(
            listOf(0..<500, 500..<1000),
            (0..<1000).splitEqual(2)
        )
        assertEquals(
            listOf(1..1, 2..2),
            (1..2).splitEqual(2)
        )
    }

}
