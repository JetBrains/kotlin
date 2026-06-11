package test.ranges

import test.collections.behaviors.iteratorBehavior
import test.collections.compare
import test.ranges.EnumRangeTest.Planet.*
import kotlin.test.*

public class EnumRangeTest {

    internal enum class Planet {
        MERCURY, VENUS, EARTH, MARS, JUPITER, SATURN, URANUS, NEPTUNE //, PLUTO <- Sorry, buddy!
    }

    internal enum class SingleValueEnum {
        ONE
    }

    @Test fun enumRange() {
        val range = EARTH..SATURN
        assertFalse(MERCURY in range)
        assertFalse(VENUS in range)

        assertTrue(EARTH in range)
        assertTrue(MARS in range)
        assertTrue(JUPITER in range)
        assertTrue(SATURN in range)

        assertFalse(URANUS in range)
        assertFalse(NEPTUNE in range)

        assertFalse(range.isEmpty())

        assertTrue(MARS in (range as ClosedRange<Planet>))
        assertFalse((range as ClosedRange<Planet>).isEmpty())

        val openRange = EARTH until SATURN
        assertTrue(JUPITER in openRange)
        assertFalse(SATURN in openRange)

        assertTrue((MARS until MERCURY).isEmpty())

    }

    @Test fun isEmpty() {
        assertTrue((NEPTUNE..SATURN).isEmpty())
        assertTrue((NEPTUNE..MERCURY step 2).isEmpty())
        assertFailsWithIllegalArgument {
            (SingleValueEnum.ONE until SingleValueEnum.ONE).toList()
        }
    }

    @Test fun emptyEquals() {
        assertEquals(NEPTUNE..SATURN, NEPTUNE..SATURN)
        assertEquals(URANUS..VENUS step 4, SATURN..MARS step 2)
        assertFalse((VENUS..MARS) == (VENUS..JUPITER))
        assertFalse((VENUS..MARS) == (EARTH..MARS))
    }

    @Test fun emptyHashCode() {
        assertEquals((NEPTUNE..SATURN).hashCode(), (JUPITER..EARTH).hashCode())
        assertEquals((MERCURY downTo VENUS).hashCode(), (EARTH downTo MARS).hashCode())
    }

    @Test
    fun rangeIterationTests() {
        // Mirroring tests for built-in progressions in RangeIterationTest...
        doTest(EnumRange.empty(), VENUS, MERCURY, 1, listOf()) // emptyConstant
        doTest(NEPTUNE..MERCURY, NEPTUNE, MERCURY, 1, listOf()) //  emptyRange
        doTest(EARTH..EARTH, EARTH, EARTH, 1, listOf(EARTH)) // oneElementRange
        doTest(EARTH..SATURN, EARTH, SATURN, 1, listOf(EARTH, MARS, JUPITER, SATURN)) // simpleRange
        doTest((Planet.values()[1])..(Planet.values()[3]), VENUS, MARS, 1, listOf(VENUS, EARTH, MARS)) // simpleRangeWithNonConstantEnds
        doTest(VENUS until JUPITER, VENUS, MARS, 1, listOf(VENUS, EARTH, MARS)) // openRange
        doTest(MERCURY downTo NEPTUNE, MERCURY, NEPTUNE, -1, listOf()) // emptyDownto
        doTest(URANUS downTo URANUS, URANUS, URANUS, -1, listOf(URANUS)) // oneElementDownTo
        doTest(SATURN downTo EARTH, SATURN, EARTH, -1, listOf(SATURN, JUPITER, MARS, EARTH)) // simpleDownTo
        doTest(VENUS..SATURN step 2, VENUS, SATURN, 2, listOf(VENUS, MARS, SATURN)) // simpleSteppedRange
        doTest(URANUS downTo EARTH step 2, URANUS, EARTH, -2, listOf(URANUS, JUPITER, EARTH)) // simpleSteppedDownTo
        doTest(VENUS..URANUS step 2, VENUS, SATURN, 2, listOf(VENUS, MARS, SATURN)) // inexactSteppedRange
        doTest(URANUS downTo VENUS step 2, URANUS, EARTH, -2, listOf(URANUS, JUPITER, EARTH)) //inexactSteppedDownTo
        doTest((MARS..VENUS).reversed(), VENUS, MARS, -1, listOf()) // reversedEmptyRange
        doTest((VENUS downTo MARS).reversed(), MARS, VENUS, 1, listOf()) // reversedEmptyBackSequence
        doTest((VENUS..MARS).reversed(), MARS, VENUS, -1, listOf(MARS, EARTH, VENUS)) // reversedRange
        doTest((MARS downTo VENUS).reversed(), VENUS, MARS, 1, listOf(VENUS, EARTH, MARS)) // reversedBackSequence
        doTest((VENUS..SATURN step 2).reversed(), SATURN, VENUS, -2, listOf(SATURN, MARS, VENUS)) // reversedSimpleSteppedRange
        doTest((URANUS downTo VENUS step 2).reversed(), EARTH, URANUS, 2, listOf(EARTH, JUPITER, URANUS)) // reversedIneaxctSteppedDownTo

        // Mirroring tests for built-in progressions in RangeIterationJVMTest...
        doTest(MERCURY..NEPTUNE, MERCURY, NEPTUNE, 1, Planet.values().toList()) // min value to max value
        doTest(NEPTUNE..NEPTUNE, NEPTUNE, NEPTUNE, 1, listOf(NEPTUNE)) // maxValueToMaxValue
        doTest(SATURN..NEPTUNE, SATURN, NEPTUNE, 1, listOf(SATURN, URANUS, NEPTUNE)) // maxValueMinusTwoToMaxValue
        doTest(NEPTUNE..MERCURY, NEPTUNE, MERCURY, 1, listOf()) // maxValueToMinValue
        doTest(NEPTUNE..NEPTUNE step 1, NEPTUNE, NEPTUNE, 1, listOf(NEPTUNE)) // progressionMaxValueToMaxValue
        doTest(SATURN..NEPTUNE step 2, SATURN, NEPTUNE, 2, listOf(SATURN, NEPTUNE)) // progressionMaxValueMinusTwoToMaxValue
        doTest(NEPTUNE..MERCURY step 1, NEPTUNE, MERCURY, 1, listOf()) // progressionMaxValueToMinValue
        doTest(MERCURY..MERCURY step 1, MERCURY, MERCURY, 1, listOf(MERCURY)) // progressionMinValueToMinValue
        doTest(EARTH..NEPTUNE step 3, EARTH, SATURN, 3, listOf(EARTH, SATURN)) // inexactToMaxValue
        doTest(EARTH downTo MERCURY step 1, EARTH, MERCURY, -1, listOf(EARTH, VENUS, MERCURY)) // progressionDownToMinValue
        doTest(SATURN downTo MERCURY step 3, SATURN, EARTH, -3, listOf(SATURN, EARTH)) // inexactDownToMinValue
    }

    private fun <E : Enum<E>> doTest(
            sequence: EnumProgression<E>, expectedFirst: E, expectedLast: E, expectedIncrement: Number,
            expectedElements: List<E>
    ) {
        val first = sequence.first
        val last = sequence.last
        val increment = sequence.step

        assertEquals(expectedFirst, first)
        assertEquals(expectedLast, last)
        assertEquals(expectedIncrement, increment)

        if (expectedElements.isEmpty()) {
            assertTrue(sequence.none())
        } else {
            assertEquals(expectedElements, sequence.toList())
        }

        compare(expectedElements.iterator(), sequence.iterator()) {
            iteratorBehavior()
        }
    }

    @Test
    fun coercionsEnum() {
        expect(MARS) { MARS.coerceAtLeast(MERCURY) }
        expect(MARS) { MERCURY.coerceAtLeast(MARS) }
        expect(MERCURY) { MARS.coerceAtMost(MERCURY) }
        expect(MERCURY) { MERCURY.coerceAtMost(MARS) }

        for (value in MERCURY..NEPTUNE) {
            expect(value) { value.coerceIn(null, null) }
            val min = VENUS
            val max = SATURN
            val range = min..max
            expect(value.coerceAtLeast(min)) { value.coerceIn(min, null) }
            expect(value.coerceAtMost(max)) { value.coerceIn(null, max) }
            expect(value.coerceAtLeast(min).coerceAtMost(max)) { value.coerceIn(min, max) }
            expect(value.coerceAtMost(max).coerceAtLeast(min)) { value.coerceIn(range) }
            assertTrue((value.coerceIn(range)) in range)
        }

        assertFails { MERCURY.coerceIn(VENUS, MERCURY) }
        assertFails { MERCURY.coerceIn(VENUS..MERCURY) }
    }

    @Test fun illegalProgressionCreation() {
        // create Progression explicitly with increment = 0
        assertFailsWithIllegalArgument { EnumProgression.fromClosedRange(MERCURY, NEPTUNE, 0) }
        assertFailsWithIllegalArgument { MERCURY..NEPTUNE step 0 }
        assertFailsWithIllegalArgument { NEPTUNE downTo MERCURY step 0 }
        assertFailsWithIllegalArgument { MERCURY..NEPTUNE step -2 }
        assertFailsWithIllegalArgument { NEPTUNE downTo MERCURY step -2 }
    }

    private fun assertFailsWithIllegalArgument(f: () -> Unit) = assertFailsWith<IllegalArgumentException> { f() }
}
