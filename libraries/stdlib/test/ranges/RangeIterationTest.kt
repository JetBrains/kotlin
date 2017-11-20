package test.ranges

import test.collections.behaviors.iteratorBehavior
import test.collections.compare
import kotlin.test.*

public open class RangeIterationTestBase {
    public fun <N : Any> doTest(
            sequence: Iterable<N>,
            expectedFirst: N,
            expectedLast: N,
            expectedIncrement: Number,
            expectedElements: List<N>
    ) {
        val first: Any
        val last: Any
        val increment: Number
        when (sequence) {
            is IntProgression -> {
                first = sequence.first
                last = sequence.last
                increment = sequence.step
            }
            is LongProgression -> {
                first = sequence.first
                last = sequence.last
                increment = sequence.step
            }
            is CharProgression -> {
                first = sequence.first
                last = sequence.last
                increment = sequence.step
            }
            else -> throw IllegalArgumentException("Unsupported sequence type: $sequence")
        }

        assertEquals(expectedFirst, first)
        assertEquals(expectedLast, last)
        assertEquals(expectedIncrement, increment)

        if (expectedElements.isEmpty())
            assertTrue(sequence.none())
        else
            assertEquals(expectedElements, sequence.toList())

        compare(expectedElements.iterator(), sequence.iterator()) {
            iteratorBehavior()
        }
    }

}

// Test data for codegen is generated from this class. If you change it, rerun GenerateTests
public class RangeIterationTest : RangeIterationTestBase() {

    @Test fun emptyConstant() {
        doTest(IntRange.EMPTY, 1, 0, 1, listOf())
        doTest(LongRange.EMPTY, 1.toLong(), 0.toLong(), 1.toLong(), listOf())

        doTest(CharRange.EMPTY, 1.toChar(), 0.toChar(), 1, listOf())
    }

    @Test fun emptyRange() {
        doTest(10..5, 10, 5, 1, listOf())
        doTest(10.toByte()..(-5).toByte(), 10, (-5), 1, listOf())
        doTest(10.toShort()..(-5).toShort(), 10, (-5), 1, listOf())
        doTest(10.toLong()..-5.toLong(), 10.toLong(), -5.toLong(), 1.toLong(), listOf())

        doTest('z'..'a', 'z', 'a', 1, listOf())
    }

    @Test fun oneElementRange() {
        doTest(5..5, 5, 5, 1, listOf(5))
        doTest(5.toByte()..5.toByte(), 5, 5, 1, listOf(5))
        doTest(5.toShort()..5.toShort(), 5, 5, 1, listOf(5))
        doTest(5.toLong()..5.toLong(), 5.toLong(), 5.toLong(), 1.toLong(), listOf(5.toLong()))

        doTest('k'..'k', 'k', 'k', 1, listOf('k'))
    }

    @Test fun simpleRange() {
        doTest(3..9, 3, 9, 1, listOf(3, 4, 5, 6, 7, 8, 9))
        doTest(3.toByte()..9.toByte(), 3, 9, 1, listOf(3, 4, 5, 6, 7, 8, 9))
        doTest(3.toShort()..9.toShort(), 3, 9, 1, listOf(3, 4, 5, 6, 7, 8, 9))
        doTest(3.toLong()..9.toLong(), 3.toLong(), 9.toLong(), 1.toLong(), listOf<Long>(3, 4, 5, 6, 7, 8, 9))

        doTest('c'..'g', 'c', 'g', 1, listOf('c', 'd', 'e', 'f', 'g'))
   }


    @Test fun simpleRangeWithNonConstantEnds() {
        doTest((1 + 2)..(10 - 1), 3, 9, 1, listOf(3, 4, 5, 6, 7, 8, 9))
        doTest((1.toByte() + 2.toByte()).toByte()..(10.toByte() - 1.toByte()).toByte(), 3, 9, 1, listOf(3, 4, 5, 6, 7, 8, 9))
        doTest((1.toShort() + 2.toShort()).toShort()..(10.toShort() - 1.toShort()).toShort(), 3, 9, 1, listOf(3, 4, 5, 6, 7, 8, 9))
        doTest((1.toLong() + 2.toLong())..(10.toLong() - 1.toLong()), 3.toLong(), 9.toLong(), 1.toLong(), listOf<Long>(3, 4, 5, 6, 7, 8, 9))

        doTest(("ace"[1])..("age"[1]), 'c', 'g', 1, listOf('c', 'd', 'e', 'f', 'g'))
    }

    @Test fun openRange() {
        doTest(1 until 5, 1, 4, 1, listOf(1, 2, 3, 4))
        doTest(1.toByte() until 5.toByte(), 1, 4, 1, listOf(1, 2, 3, 4))
        doTest(1.toShort() until 5.toShort(), 1, 4, 1, listOf(1, 2, 3, 4))
        doTest(1.toLong() until 5.toLong(), 1L, 4L, 1L, listOf<Long>(1, 2, 3, 4))
        doTest('a' until 'd', 'a', 'c', 1, listOf('a', 'b', 'c'))
    }


    @Test fun emptyDownto() {
        doTest(5 downTo 10, 5, 10, -1, listOf())
        doTest(5.toByte() downTo 10.toByte(), 5, 10, -1, listOf())
        doTest(5.toShort() downTo 10.toShort(), 5, 10, -1, listOf())
        doTest(5.toLong() downTo 10.toLong(), 5.toLong(), 10.toLong(), -1.toLong(), listOf())

        doTest('a' downTo 'z', 'a', 'z', -1, listOf())
    }

    @Test fun oneElementDownTo() {
        doTest(5 downTo 5, 5, 5, -1, listOf(5))
        doTest(5.toByte() downTo 5.toByte(), 5, 5, -1, listOf(5))
        doTest(5.toShort() downTo 5.toShort(), 5, 5, -1, listOf(5))
        doTest(5.toLong() downTo 5.toLong(), 5.toLong(), 5.toLong(), -1.toLong(), listOf(5.toLong()))

        doTest('k' downTo 'k', 'k', 'k', -1, listOf('k'))
    }

    @Test fun simpleDownTo() {
        doTest(9 downTo 3, 9, 3, -1, listOf(9, 8, 7, 6, 5, 4, 3))
        doTest(9.toByte() downTo 3.toByte(), 9, 3, -1, listOf(9, 8, 7, 6, 5, 4, 3))
        doTest(9.toShort() downTo 3.toShort(), 9, 3, -1, listOf(9, 8, 7, 6, 5, 4, 3))
        doTest(9.toLong() downTo 3.toLong(), 9.toLong(), 3.toLong(), -1.toLong(), listOf<Long>(9, 8, 7, 6, 5, 4, 3))

        doTest('g' downTo 'c', 'g', 'c', -1, listOf('g', 'f', 'e', 'd', 'c'))
    }


    @Test fun simpleSteppedRange() {
        doTest(3..9 step 2, 3, 9, 2, listOf(3, 5, 7, 9))
        doTest(3.toByte()..9.toByte() step 2, 3, 9, 2, listOf(3, 5, 7, 9))
        doTest(3.toShort()..9.toShort() step 2, 3, 9, 2, listOf(3, 5, 7, 9))
        doTest(3.toLong()..9.toLong() step 2.toLong(), 3.toLong(), 9.toLong(), 2.toLong(), listOf<Long>(3, 5, 7, 9))

        doTest('c'..'g' step 2, 'c', 'g', 2, listOf('c', 'e', 'g'))
    }

    @Test fun simpleSteppedDownTo() {
        doTest(9 downTo 3 step 2, 9, 3, -2, listOf(9, 7, 5, 3))
        doTest(9.toByte() downTo 3.toByte() step 2, 9, 3, -2, listOf(9, 7, 5, 3))
        doTest(9.toShort() downTo 3.toShort() step 2, 9, 3, -2, listOf(9, 7, 5, 3))
        doTest(9.toLong() downTo 3.toLong() step 2.toLong(), 9.toLong(), 3.toLong(), -2.toLong(), listOf<Long>(9, 7, 5, 3))

        doTest('g' downTo 'c' step 2, 'g', 'c', -2, listOf('g', 'e', 'c'))
    }


    // 'inexact' means last element is not equal to sequence end
    @Test fun inexactSteppedRange() {
        doTest(3..8 step 2, 3, 7, 2, listOf(3, 5, 7))
        doTest(3.toByte()..8.toByte() step 2, 3, 7, 2, listOf(3, 5, 7))
        doTest(3.toShort()..8.toShort() step 2, 3, 7, 2, listOf(3, 5, 7))
        doTest(3.toLong()..8.toLong() step 2.toLong(), 3.toLong(), 7.toLong(), 2.toLong(), listOf<Long>(3, 5, 7))

        doTest('a'..'d' step 2, 'a', 'c', 2, listOf('a', 'c'))
    }

    // 'inexact' means last element is not equal to sequence end
    @Test fun inexactSteppedDownTo() {
        doTest(8 downTo 3 step 2, 8, 4, -2, listOf(8, 6, 4))
        doTest(8.toByte() downTo 3.toByte() step 2, 8, 4, -2, listOf(8, 6, 4))
        doTest(8.toShort() downTo 3.toShort() step 2, 8, 4, -2, listOf(8, 6, 4))
        doTest(8.toLong() downTo 3.toLong() step 2.toLong(), 8.toLong(), 4.toLong(), -2.toLong(), listOf<Long>(8, 6, 4))

        doTest('d' downTo 'a' step 2, 'd', 'b', -2, listOf('d', 'b'))
    }


    @Test fun reversedEmptyRange() {
        doTest((5..3).reversed(), 3, 5, -1, listOf())
        doTest((5.toByte()..3.toByte()).reversed(), 3, 5, -1, listOf())
        doTest((5.toShort()..3.toShort()).reversed(), 3, 5, -1, listOf())
        doTest((5.toLong()..3.toLong()).reversed(), 3.toLong(), 5.toLong(), -1.toLong(), listOf())

        doTest(('c'..'a').reversed(), 'a', 'c', -1, listOf())
    }

    @Test fun reversedEmptyBackSequence() {
        doTest((3 downTo 5).reversed(), 5, 3, 1, listOf())
        doTest((3.toByte() downTo 5.toByte()).reversed(), 5, 3, 1, listOf())
        doTest((3.toShort() downTo 5.toShort()).reversed(), 5, 3, 1, listOf())
        doTest((3.toLong() downTo 5.toLong()).reversed(), 5.toLong(), 3.toLong(), 1.toLong(), listOf())

        doTest(('a' downTo 'c').reversed(), 'c', 'a', 1, listOf())
    }

    @Test fun reversedRange() {
        doTest((3..5).reversed(), 5, 3, -1, listOf(5, 4, 3))
        doTest((3.toByte()..5.toByte()).reversed(),5, 3, -1, listOf(5, 4, 3))
        doTest((3.toShort()..5.toShort()).reversed(), 5, 3, -1, listOf(5, 4, 3))
        doTest((3.toLong()..5.toLong()).reversed(), 5.toLong(), 3.toLong(), -1.toLong(), listOf<Long>(5, 4, 3))

        doTest(('a'..'c').reversed(), 'c', 'a', -1, listOf('c', 'b', 'a'))
    }

    @Test fun reversedBackSequence() {
        doTest((5 downTo 3).reversed(), 3, 5, 1, listOf(3, 4, 5))
        doTest((5.toByte() downTo 3.toByte()).reversed(), 3, 5, 1, listOf(3, 4, 5))
        doTest((5.toShort() downTo 3.toShort()).reversed(), 3, 5, 1, listOf(3, 4, 5))
        doTest((5.toLong() downTo 3.toLong()).reversed(), 3.toLong(), 5.toLong(), 1.toLong(), listOf<Long>(3, 4, 5))

        doTest(('c' downTo 'a').reversed(), 'a', 'c', 1, listOf('a', 'b', 'c'))

     }

    @Test fun reversedSimpleSteppedRange() {
        doTest((3..9 step 2).reversed(), 9, 3, -2, listOf(9, 7, 5, 3))
        doTest((3.toByte()..9.toByte() step 2).reversed(), 9, 3, -2, listOf(9, 7, 5, 3))
        doTest((3.toShort()..9.toShort() step 2).reversed(), 9, 3, -2, listOf(9, 7, 5, 3))
        doTest((3.toLong()..9.toLong() step 2.toLong()).reversed(), 9.toLong(), 3.toLong(), -2.toLong(), listOf<Long>(9, 7, 5, 3))

        doTest(('c'..'g' step 2).reversed(), 'g', 'c', -2, listOf('g', 'e', 'c'))
    }

    // invariant progression.reversed().toList() == progression.toList().reversed() is preserved
    // 'inexact' means that start of reversed progression is not the end of original progression, but the last element
    @Test fun reversedInexactSteppedDownTo() {
        doTest((8 downTo 3 step 2).reversed(), 4, 8, 2, listOf(4, 6, 8))
        doTest((8.toByte() downTo 3.toByte() step 2).reversed(), 4, 8, 2, listOf(4, 6, 8))
        doTest((8.toShort() downTo 3.toShort() step 2).reversed(), 4, 8, 2, listOf(4, 6, 8))
        doTest((8.toLong() downTo 3.toLong() step 2.toLong()).reversed(), 4.toLong(), 8.toLong(), 2.toLong(), listOf<Long>(4, 6, 8))

        doTest(('d' downTo 'a' step 2).reversed(), 'b', 'd', 2, listOf('b', 'd'))
    }
}
