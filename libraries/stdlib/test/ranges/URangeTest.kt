/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.ranges

import test.ranges.testProgressionCollection
import kotlin.math.min
import kotlin.math.sign
import kotlin.test.*

public class URangeTest {

    @Test
    fun uIntProgressionCollection() {
        testProgressionCollection(
            0U..10U, -10..10,
            UIntProgression::fromClosedRange,
            { cur: UInt, step ->
                when {
                    step > 0 -> (cur + step.toUInt()).takeIf { cur <= UInt.MAX_VALUE - step.toUInt() }
                    step < 0 -> (cur - (-step).toUInt()).takeIf { cur >= UInt.MIN_VALUE + (-step).toUInt() }
                    else -> cur
                }
            },
            { it.sign }
        )
        //testing overflow
        for (start in 0U..100U)
            for (finish in (0U..10U).map { start + Int.MAX_VALUE.toUInt() + it } + ((UInt.MAX_VALUE - 100U)..UInt.MAX_VALUE))
                for (step in 1..10) {
                    val expected = min(((finish.toLong() - start.toLong()) / step.toLong()) + 1, Int.MAX_VALUE.toLong())
                    assertEquals(expected, (start..finish step step).size.toLong())
                    assertEquals(expected, (finish downTo start step step).size.toLong())
                }

    }

    @Test
    fun uLongProgressionCollection() {
        testProgressionCollection(
            0UL..10UL, -10L..10L,
            ULongProgression::fromClosedRange,
            { cur: ULong, step ->
                when {
                    step > 0L -> (cur + step.toULong()).takeIf { cur <= ULong.MAX_VALUE - step.toULong() }
                    step < 0L -> (cur - (-step).toULong()).takeIf { cur >= ULong.MIN_VALUE + (-step).toULong() }
                    else -> cur
                }
            },
            { it.sign }
        )
        //testing overflow
        for (start in 0UL..100UL) {
            for (finish in (0UL..10UL).map { start + Int.MAX_VALUE.toULong() + it }) {
                assertEquals(Int.MAX_VALUE, (start..finish).size)
                assertEquals(Int.MAX_VALUE, (finish downTo start).size)
            }
            for (finish in ((ULong.MAX_VALUE - 100UL)..ULong.MAX_VALUE)) {
                for (step in 1L..10L) {
                    assertEquals(Int.MAX_VALUE, (start..finish step step).size)
                    assertEquals(Int.MAX_VALUE, (finish downTo start step step).size)
                }
            }
        }
    }

    @Test
    fun uintRange() {
        val range = 9u..(-5).toUInt()
        assertFalse((-4).toUInt() in range)
        assertFalse((-1).toUInt() in range)
        assertFalse(0u in range)
        assertFalse(3u in range)
        assertFalse(8u in range)

        assertTrue(9u in range)
        assertTrue(10u in range)
        assertTrue(9000u in range)
        assertTrue((-1000).toUInt() in range)
        assertTrue((-6).toUInt() in range)
        assertTrue((-5).toUInt() in range)

        assertFalse(range.isEmpty())

        assertTrue(9u in (range as ClosedRange<UInt>))
        assertFalse((range as ClosedRange<UInt>).isEmpty())

        assertTrue(12.toUShort() in range)
        assertTrue(12.toUByte() in range)
        assertTrue(12.toULong() in range)

        assertFalse((-1).toULong() in range)
        assertFalse((-1000).toULong() in range)

        assertFalse(null in range)
        assertTrue(12u as UInt? in range)
        assertFalse((-3).toUInt() as UInt? in range)

        val openRange = 1u until 10u
        assertTrue(9u in openRange)
        assertFalse(10u in openRange)

        assertTrue((1u until UInt.MIN_VALUE).isEmpty())
    }

    @Test
    fun ubyteRange() {
        val range = 9.toUByte()..(-5).toUByte()
        assertFalse((-4).toUByte() in range)
        assertFalse((-1).toUByte() in range)
        assertFalse(0.toUByte() in range)
        assertFalse(3.toUByte() in range)
        assertFalse(8.toUByte() in range)

        assertTrue(9.toUByte() in range)
        assertTrue(10.toUByte() in range)
        assertTrue(111.toUByte() in range)
        assertTrue((-100).toUByte() in range)
        assertTrue((-6).toUByte() in range)
        assertTrue((-5).toUByte() in range)

        assertFalse(range.isEmpty())

        assertTrue(12.toUShort() in range)
        assertTrue(12.toUInt() in range)
        assertTrue(12.toULong() in range)

        assertFalse((-1).toUShort() in range)
        assertFalse((-1000).toUInt() in range)

//         assertTrue(1.toUByte() as UByte? in range) // expected not to compile

        val openRange = 1.toUByte() until 10.toUByte()
        assertTrue(9.toUByte() in openRange)
        assertFalse(10.toUByte() in openRange)

        assertTrue((UByte.MAX_VALUE until UByte.MIN_VALUE).isEmpty())
    }

    @Test
    fun ushortRange() {
        val range = 9.toUShort()..(-5).toUShort()
        assertFalse((-1).toUShort() in range)
        assertFalse((-4).toUShort() in range)
        assertFalse(0.toUShort() in range)
        assertFalse(3.toUShort() in range)
        assertFalse(8.toUShort() in range)

        assertTrue(9.toUShort() in range)
        assertTrue(10.toUShort() in range)
        assertTrue(239.toUShort() in range)
        assertTrue((-1000).toUShort() in range)
        assertTrue((-6).toUShort() in range)
        assertTrue((-5).toUShort() in range)

        assertFalse(range.isEmpty())

        assertTrue(12.toUByte() in range)
        assertTrue(12.toUInt() in range)
        assertTrue(12.toULong() in range)

        assertFalse((-1).toUInt() in range)
        assertFalse((-1000).toULong() in range)

//        assertTrue(1.toUShort() as UShort? in range) // expected not to compile

        val openRange = 1.toUShort() until 10.toUShort()
        assertTrue(9.toUShort() in openRange)
        assertFalse(10.toUShort() in openRange)

        assertTrue((0.toUShort() until UShort.MIN_VALUE).isEmpty())
    }

    @Test
    fun ulongRange() {
        val range = 9uL..(-5).toULong()
        assertFalse((-1).toULong() in range)
        assertFalse((-4).toULong() in range)
        assertFalse(0uL in range)
        assertFalse(3uL in range)
        assertFalse(8uL in range)

        assertTrue(9uL in range)
        assertTrue(10uL in range)
        assertTrue(10000000uL in range)
        assertTrue((-10000000).toULong() in range)
        assertTrue((-6).toULong() in range)
        assertTrue((-5).toULong() in range)

        assertFalse(range.isEmpty())

        assertTrue((-5).toULong() in (range as ClosedRange<ULong>))
        assertFalse((range as ClosedRange<ULong>).isEmpty())

        assertTrue(12.toUByte() in range)
        assertTrue(12.toUShort() in range)
        assertTrue(12.toUInt() in range)

        assertFalse(null in range)
        assertTrue(12uL as ULong? in range)
        assertFalse((-3).toULong() as ULong? in range)

        val openRange = 1uL until 10uL
        assertTrue(9uL in openRange)
        assertFalse(10uL in openRange)

        assertTrue((0uL until ULong.MIN_VALUE).isEmpty())
    }

    @Suppress("EmptyRange")
    @Test
    fun isEmpty() {
        assertTrue((2u..1u).isEmpty())
        assertTrue((2uL..0uL).isEmpty())
        assertTrue(((-1).toUShort()..1.toUShort()).isEmpty())
        assertTrue(((-5).toUByte()..0.toUByte()).isEmpty())

        assertTrue((1u downTo 2u).isEmpty())
        assertTrue((0uL downTo 2uL).isEmpty())
        assertFalse((2u downTo 1u).isEmpty())
        assertFalse((2uL downTo 0uL).isEmpty())
    }

    @Suppress("ReplaceAssertBooleanWithAssertEquality", "EmptyRange")
    @Test
    fun emptyEquals() {
        assertTrue(UIntRange.EMPTY == UIntRange.EMPTY)
        assertEquals(UIntRange.EMPTY, UIntRange.EMPTY)
        assertEquals(0uL..42uL, 0uL..42uL)
        assertEquals(0uL..4200000042000000uL, 0uL..4200000042000000uL)
        assertEquals(3u downTo 0u, 3u downTo 0u)

        assertEquals(2u..1u, 1u..0u)
        assertEquals(2uL..1uL, 1uL..0uL)
        assertEquals(2u.toUShort()..1u.toUShort(), 1u.toUShort()..0u.toUShort())
        assertEquals(2u.toUByte()..1u.toUByte(), 1u.toUByte()..0u.toUByte())

        assertTrue(1u downTo 2u == 2u downTo 3u)
        assertTrue(0uL downTo (-1).toULong() == (-2).toULong() downTo (-1).toULong())

        assertFalse(0u..1u == UIntRange.EMPTY)
    }

    @Suppress("EmptyRange")
    @Test
    fun emptyHashCode() {
        assertEquals((0u..42u).hashCode(), (0u..42u).hashCode())

        assertEquals(((-1).toUInt()..0u).hashCode(), UIntRange.EMPTY.hashCode())
        assertEquals((2uL..1uL).hashCode(), (1uL..0uL).hashCode())
        assertEquals(((-1).toUShort()..(-2).toUShort()).hashCode(), (42.toUShort()..0.toUShort()).hashCode())
        assertEquals(((-1).toUByte()..(-2).toUByte()).hashCode(), (42.toUByte()..0.toUByte()).hashCode())

        assertEquals((1u downTo 2u).hashCode(), (2u downTo 3U).hashCode())
        assertEquals((1UL downTo 2uL).hashCode(), (2UL downTo 3UL).hashCode())
    }

    private fun assertFailsWithIllegalArgument(f: () -> Unit) = assertFailsWith<IllegalArgumentException> { f() }

    @Test
    fun illegalProgressionCreation() {
        // create Progression explicitly with increment = 0
        assertFailsWithIllegalArgument { UIntProgression.fromClosedRange(0u, 5u, 0) }
        assertFailsWithIllegalArgument { ULongProgression.fromClosedRange(0uL, 5uL, 0) }


        assertFailsWithIllegalArgument { 0u..5u step 0 }
        assertFailsWithIllegalArgument { 0.toUByte()..5.toUByte() step 0 }
        assertFailsWithIllegalArgument { 0.toUShort()..5.toUShort() step 0 }
        assertFailsWithIllegalArgument { 0uL..5uL step 0 }

        assertFailsWithIllegalArgument { (-5).toUInt() downTo 0u step 0 }
        assertFailsWithIllegalArgument { (-5).toUByte() downTo 0.toUByte() step 0 }
        assertFailsWithIllegalArgument { (-5).toUShort() downTo 0.toUShort() step 0 }
        assertFailsWithIllegalArgument { (-5).toULong() downTo 0uL step 0L }

        assertFailsWithIllegalArgument { 0u..5u step -2 }
        assertFailsWithIllegalArgument { 0.toUByte()..5.toUByte() step -2 }
        assertFailsWithIllegalArgument { 0.toUShort()..5.toUShort() step -2 }
        assertFailsWithIllegalArgument { 0uL..5uL step -2L }


        assertFailsWithIllegalArgument { (-5).toUInt() downTo 0u step -2 }
        assertFailsWithIllegalArgument { (-5).toUByte() downTo 0.toUByte() step -2 }
        assertFailsWithIllegalArgument { (-5).toUShort() downTo 0.toUShort() step -2 }
        assertFailsWithIllegalArgument { (-5).toULong() downTo 0uL step -2L }
    }

    @Test
    fun stepSizeIsTooLow() {
        assertFailsWithIllegalArgument { UIntProgression.fromClosedRange(0u, 1u, Int.MIN_VALUE) }
        assertFailsWithIllegalArgument { ULongProgression.fromClosedRange(0u, 1u, Long.MIN_VALUE) }
    }

    @Test
    fun randomInEmptyRange() {
        assertFailsWith<NoSuchElementException> { UIntRange.EMPTY.random() }
        assertFailsWith<NoSuchElementException> { ULongRange.EMPTY.random() }
    }
}
