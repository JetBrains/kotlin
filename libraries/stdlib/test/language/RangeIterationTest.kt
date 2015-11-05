@file: Suppress("DEPRECATION_ERROR")
package language

import org.junit.Test as test
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
                increment = sequence.increment
            }
            is LongProgression -> {
                first = sequence.first
                last = sequence.last
                increment = sequence.increment
            }
            is CharProgression -> {
                first = sequence.first
                last = sequence.last
                increment = sequence.increment
            }
            // TODO: Drop this branch
            is Progression -> {
                first = sequence.start
                last = sequence.end
                increment = sequence.increment
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
    }

}

// Test data for codegen is generated from this class. If you change it, rerun GenerateTests
public class RangeIterationTest : RangeIterationTestBase() {

    @test fun emptyConstant() {
        doTest(IntRange.EMPTY, 1, 0, 1, listOf())
        doTest(ByteRange.EMPTY, 1.toByte(), 0.toByte(), 1, listOf())
        doTest(ShortRange.EMPTY, 1.toShort(), 0.toShort(), 1, listOf())
        doTest(LongRange.EMPTY, 1.toLong(), 0.toLong(), 1.toLong(), listOf())

        doTest(CharRange.EMPTY, 1.toChar(), 0.toChar(), 1, listOf())

        doTest(DoubleRange.EMPTY, 1.0, 0.0, 1.0, listOf())
        doTest(FloatRange.EMPTY, 1.0.toFloat(), 0.0.toFloat(), 1.0.toFloat(), listOf())
    }

    @test fun emptyRange() {
        doTest(10..5, 10, 5, 1, listOf())
        doTest(10.toByte()..(-5).toByte(), 10, (-5), 1, listOf())
        doTest(10.toShort()..(-5).toShort(), 10, (-5), 1, listOf())
        doTest(10.toLong()..-5.toLong(), 10.toLong(), -5.toLong(), 1.toLong(), listOf())

        doTest('z'..'a', 'z', 'a', 1, listOf())

        doTest(5.0..-1.0, 5.0, -1.0, 1.0, listOf())
        doTest(5.0.toFloat()..-1.0.toFloat(), 5.0.toFloat(), -1.0.toFloat(), 1.toFloat(), listOf())
    }

    @test fun oneElementRange() {
        doTest(5..5, 5, 5, 1, listOf(5))
        doTest(5.toByte()..5.toByte(), 5, 5, 1, listOf(5))
        doTest(5.toShort()..5.toShort(), 5, 5, 1, listOf(5))
        doTest(5.toLong()..5.toLong(), 5.toLong(), 5.toLong(), 1.toLong(), listOf(5.toLong()))

        doTest('k'..'k', 'k', 'k', 1, listOf('k'))

        doTest(5.0..5.0, 5.0, 5.0, 1.0, listOf(5.0))
        doTest(5.0.toFloat()..5.0.toFloat(), 5.0.toFloat(), 5.0.toFloat(), 1.toFloat(), listOf(5.0.toFloat()))
    }

    @test fun simpleRange() {
        doTest(3..9, 3, 9, 1, listOf(3, 4, 5, 6, 7, 8, 9))
        doTest(3.toByte()..9.toByte(), 3, 9, 1, listOf(3, 4, 5, 6, 7, 8, 9))
        doTest(3.toShort()..9.toShort(), 3, 9, 1, listOf(3, 4, 5, 6, 7, 8, 9))
        doTest(3.toLong()..9.toLong(), 3.toLong(), 9.toLong(), 1.toLong(), listOf<Long>(3, 4, 5, 6, 7, 8, 9))

        doTest('c'..'g', 'c', 'g', 1, listOf('c', 'd', 'e', 'f', 'g'))

        doTest(3.0..9.0, 3.0, 9.0, 1.0, listOf(3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0))
        doTest(3.0.toFloat()..9.0.toFloat(), 3.0.toFloat(), 9.0.toFloat(), 1.toFloat(),
                listOf<Float>(3.0.toFloat(), 4.0.toFloat(), 5.0.toFloat(), 6.0.toFloat(), 7.0.toFloat(), 8.0.toFloat(), 9.0.toFloat()))
    }


    @test fun simpleRangeWithNonConstantEnds() {
        doTest((1 + 2)..(10 - 1), 3, 9, 1, listOf(3, 4, 5, 6, 7, 8, 9))
        doTest((1.toByte() + 2.toByte()).toByte()..(10.toByte() - 1.toByte()).toByte(), 3, 9, 1, listOf(3, 4, 5, 6, 7, 8, 9))
        doTest((1.toShort() + 2.toShort()).toShort()..(10.toShort() - 1.toShort()).toShort(), 3, 9, 1, listOf(3, 4, 5, 6, 7, 8, 9))
        doTest((1.toLong() + 2.toLong())..(10.toLong() - 1.toLong()), 3.toLong(), 9.toLong(), 1.toLong(), listOf<Long>(3, 4, 5, 6, 7, 8, 9))

        doTest(("ace"[1])..("age"[1]), 'c', 'g', 1, listOf('c', 'd', 'e', 'f', 'g'))

        doTest((1.5 * 2)..(3.0 * 3.0), 3.0, 9.0, 1.0, listOf(3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0))
        doTest((1.5.toFloat() * 2.toFloat())..(3.0.toFloat() * 3.0.toFloat()), 3.0.toFloat(), 9.0.toFloat(), 1.toFloat(),
                listOf<Float>(3.0.toFloat(), 4.0.toFloat(), 5.0.toFloat(), 6.0.toFloat(), 7.0.toFloat(), 8.0.toFloat(), 9.0.toFloat()))
    }

    @test fun openRange() {
        doTest(1 until 5, 1, 4, 1, listOf(1, 2, 3, 4))
        doTest(1.toByte() until 5.toByte(), 1, 4, 1, listOf(1, 2, 3, 4))
        doTest(1.toShort() until 5.toShort(), 1, 4, 1, listOf(1, 2, 3, 4))
        doTest(1.toLong() until 5.toLong(), 1L, 4L, 1L, listOf<Long>(1, 2, 3, 4))
        doTest('a' until 'd', 'a', 'c', 1, listOf('a', 'b', 'c'))
    }


    @test fun emptyDownto() {
        doTest(5 downTo 10, 5, 10, -1, listOf())
        doTest(5.toByte() downTo 10.toByte(), 5, 10, -1, listOf())
        doTest(5.toShort() downTo 10.toShort(), 5, 10, -1, listOf())
        doTest(5.toLong() downTo 10.toLong(), 5.toLong(), 10.toLong(), -1.toLong(), listOf())

        doTest('a' downTo 'z', 'a', 'z', -1, listOf())

        doTest(-1.0 downTo 5.0, -1.0, 5.0, -1.0, listOf())
        doTest(-1.0.toFloat() downTo 5.0.toFloat(), -1.0.toFloat(), 5.0.toFloat(), -1.0.toFloat(), listOf())
    }

    @test fun oneElementDownTo() {
        doTest(5 downTo 5, 5, 5, -1, listOf(5))
        doTest(5.toByte() downTo 5.toByte(), 5, 5, -1, listOf(5))
        doTest(5.toShort() downTo 5.toShort(), 5, 5, -1, listOf(5))
        doTest(5.toLong() downTo 5.toLong(), 5.toLong(), 5.toLong(), -1.toLong(), listOf(5.toLong()))

        doTest('k' downTo 'k', 'k', 'k', -1, listOf('k'))

        doTest(5.0 downTo 5.0, 5.0, 5.0, -1.0, listOf(5.0))
        doTest(5.0.toFloat() downTo 5.0.toFloat(), 5.0.toFloat(), 5.0.toFloat(), -1.0.toFloat(), listOf(5.0.toFloat()))
    }

    @test fun simpleDownTo() {
        doTest(9 downTo 3, 9, 3, -1, listOf(9, 8, 7, 6, 5, 4, 3))
        doTest(9.toByte() downTo 3.toByte(), 9, 3, -1, listOf(9, 8, 7, 6, 5, 4, 3))
        doTest(9.toShort() downTo 3.toShort(), 9, 3, -1, listOf(9, 8, 7, 6, 5, 4, 3))
        doTest(9.toLong() downTo 3.toLong(), 9.toLong(), 3.toLong(), -1.toLong(), listOf<Long>(9, 8, 7, 6, 5, 4, 3))

        doTest('g' downTo 'c', 'g', 'c', -1, listOf('g', 'f', 'e', 'd', 'c'))

        doTest(9.0 downTo 3.0, 9.0, 3.0, -1.0, listOf(9.0, 8.0, 7.0, 6.0, 5.0, 4.0, 3.0))
        doTest(9.0.toFloat() downTo 3.0.toFloat(), 9.0.toFloat(), 3.0.toFloat(), -1.0.toFloat(),
                listOf<Float>(9.0.toFloat(), 8.0.toFloat(), 7.0.toFloat(), 6.0.toFloat(), 5.0.toFloat(), 4.0.toFloat(), 3.0.toFloat()))
    }


    @test fun simpleSteppedRange() {
        doTest(3..9 step 2, 3, 9, 2, listOf(3, 5, 7, 9))
        doTest(3.toByte()..9.toByte() step 2, 3, 9, 2, listOf(3, 5, 7, 9))
        doTest(3.toShort()..9.toShort() step 2, 3, 9, 2, listOf(3, 5, 7, 9))
        doTest(3.toLong()..9.toLong() step 2.toLong(), 3.toLong(), 9.toLong(), 2.toLong(), listOf<Long>(3, 5, 7, 9))

        doTest('c'..'g' step 2, 'c', 'g', 2, listOf('c', 'e', 'g'))

        doTest(4.0..6.0 step 0.5, 4.0, 6.0, 0.5, listOf(4.0, 4.5, 5.0, 5.5, 6.0))
        doTest(4.0.toFloat()..6.0.toFloat() step 0.5.toFloat(), 4.0.toFloat(), 6.0.toFloat(), 0.5.toFloat(),
                listOf<Float>(4.0.toFloat(), 4.5.toFloat(), 5.0.toFloat(), 5.5.toFloat(), 6.0.toFloat()))
    }

    @test fun simpleSteppedDownTo() {
        doTest(9 downTo 3 step 2, 9, 3, -2, listOf(9, 7, 5, 3))
        doTest(9.toByte() downTo 3.toByte() step 2, 9, 3, -2, listOf(9, 7, 5, 3))
        doTest(9.toShort() downTo 3.toShort() step 2, 9, 3, -2, listOf(9, 7, 5, 3))
        doTest(9.toLong() downTo 3.toLong() step 2.toLong(), 9.toLong(), 3.toLong(), -2.toLong(), listOf<Long>(9, 7, 5, 3))

        doTest('g' downTo 'c' step 2, 'g', 'c', -2, listOf('g', 'e', 'c'))

        doTest(6.0 downTo 4.0 step 0.5, 6.0, 4.0, -0.5, listOf(6.0, 5.5, 5.0, 4.5, 4.0))
        doTest(6.0.toFloat() downTo 4.0.toFloat() step 0.5.toFloat(), 6.0.toFloat(), 4.0.toFloat(), -0.5.toFloat(),
                listOf<Float>(6.0.toFloat(), 5.5.toFloat(), 5.0.toFloat(), 4.5.toFloat(), 4.0.toFloat()))
    }


    // 'inexact' means last element is not equal to sequence end
    @test fun inexactSteppedRange() {
        doTest(3..8 step 2, 3, 7, 2, listOf(3, 5, 7))
        doTest(3.toByte()..8.toByte() step 2, 3, 7, 2, listOf(3, 5, 7))
        doTest(3.toShort()..8.toShort() step 2, 3, 7, 2, listOf(3, 5, 7))
        doTest(3.toLong()..8.toLong() step 2.toLong(), 3.toLong(), 7.toLong(), 2.toLong(), listOf<Long>(3, 5, 7))

        doTest('a'..'d' step 2, 'a', 'c', 2, listOf('a', 'c'))

        doTest(4.0..5.8 step 0.5, 4.0, 5.8, 0.5, listOf(4.0, 4.5, 5.0, 5.5))
        doTest(4.0.toFloat()..5.8.toFloat() step 0.5.toFloat(), 4.0.toFloat(), 5.8.toFloat(), 0.5.toFloat(),
                listOf<Float>(4.0.toFloat(), 4.5.toFloat(), 5.0.toFloat(), 5.5.toFloat()))
    }

    // 'inexact' means last element is not equal to sequence end
    @test fun inexactSteppedDownTo() {
        doTest(8 downTo 3 step 2, 8, 4, -2, listOf(8, 6, 4))
        doTest(8.toByte() downTo 3.toByte() step 2, 8, 4, -2, listOf(8, 6, 4))
        doTest(8.toShort() downTo 3.toShort() step 2, 8, 4, -2, listOf(8, 6, 4))
        doTest(8.toLong() downTo 3.toLong() step 2.toLong(), 8.toLong(), 4.toLong(), -2.toLong(), listOf<Long>(8, 6, 4))

        doTest('d' downTo 'a' step 2, 'd', 'b', -2, listOf('d', 'b'))

        doTest(5.5 downTo 3.7 step 0.5, 5.5, 3.7, -0.5, listOf(5.5, 5.0, 4.5, 4.0))
        doTest(5.5.toFloat() downTo 3.7.toFloat() step 0.5.toFloat(), 5.5.toFloat(), 3.7.toFloat(), -0.5.toFloat(),
                listOf<Float>(5.5.toFloat(), 5.0.toFloat(), 4.5.toFloat(), 4.0.toFloat()))
    }


    @test fun reversedEmptyRange() {
        doTest((5..3).reversed(), 3, 5, -1, listOf())
        doTest((5.toByte()..3.toByte()).reversed(), 3, 5, -1, listOf())
        doTest((5.toShort()..3.toShort()).reversed(), 3, 5, -1, listOf())
        doTest((5.toLong()..3.toLong()).reversed(), 3.toLong(), 5.toLong(), -1.toLong(), listOf())

        doTest(('c'..'a').reversed(), 'a', 'c', -1, listOf())

        doTest((5.0..3.0).reversed(), 3.0, 5.0, -1.0, listOf())
        doTest((5.0.toFloat()..3.0.toFloat()).reversed(), 3.0.toFloat(), 5.0.toFloat(), -1.toFloat(), listOf())
    }

    @test fun reversedEmptyBackSequence() {
        doTest((3 downTo 5).reversed(), 5, 3, 1, listOf())
        doTest((3.toByte() downTo 5.toByte()).reversed(), 5, 3, 1, listOf())
        doTest((3.toShort() downTo 5.toShort()).reversed(), 5, 3, 1, listOf())
        doTest((3.toLong() downTo 5.toLong()).reversed(), 5.toLong(), 3.toLong(), 1.toLong(), listOf())

        doTest(('a' downTo 'c').reversed(), 'c', 'a', 1, listOf())

        doTest((3.0 downTo 5.0).reversed(), 5.0, 3.0, 1.0, listOf())
        doTest((3.0.toFloat() downTo 5.0.toFloat()).reversed(), 5.0.toFloat(), 3.0.toFloat(), 1.toFloat(), listOf())
    }

    @test fun reversedRange() {
        doTest((3..5).reversed(), 5, 3, -1, listOf(5, 4, 3))
        doTest((3.toByte()..5.toByte()).reversed(),5, 3, -1, listOf(5, 4, 3))
        doTest((3.toShort()..5.toShort()).reversed(), 5, 3, -1, listOf(5, 4, 3))
        doTest((3.toLong()..5.toLong()).reversed(), 5.toLong(), 3.toLong(), -1.toLong(), listOf<Long>(5, 4, 3))

        doTest(('a'..'c').reversed(), 'c', 'a', -1, listOf('c', 'b', 'a'))

        doTest((3.0..5.0).reversed(), 5.0, 3.0, -1.0, listOf(5.0, 4.0, 3.0))
        doTest((3.0.toFloat()..5.0.toFloat()).reversed(), 5.0.toFloat(), 3.0.toFloat(), -1.toFloat(),
                listOf<Float>(5.0.toFloat(), 4.0.toFloat(), 3.0.toFloat()))
    }

    @test fun reversedBackSequence() {
        doTest((5 downTo 3).reversed(), 3, 5, 1, listOf(3, 4, 5))
        doTest((5.toByte() downTo 3.toByte()).reversed(), 3, 5, 1, listOf(3, 4, 5))
        doTest((5.toShort() downTo 3.toShort()).reversed(), 3, 5, 1, listOf(3, 4, 5))
        doTest((5.toLong() downTo 3.toLong()).reversed(), 3.toLong(), 5.toLong(), 1.toLong(), listOf<Long>(3, 4, 5))

        doTest(('c' downTo 'a').reversed(), 'a', 'c', 1, listOf('a', 'b', 'c'))

        doTest((5.0 downTo 3.0).reversed(), 3.0, 5.0, 1.0, listOf(3.0, 4.0, 5.0))
        doTest((5.0.toFloat() downTo 3.0.toFloat()).reversed(), 3.0.toFloat(), 5.0.toFloat(), 1.toFloat(),
                listOf<Float>(3.0.toFloat(), 4.0.toFloat(), 5.0.toFloat()))
    }

    @test fun reversedSimpleSteppedRange() {
        doTest((3..9 step 2).reversed(), 9, 3, -2, listOf(9, 7, 5, 3))
        doTest((3.toByte()..9.toByte() step 2).reversed(), 9, 3, -2, listOf(9, 7, 5, 3))
        doTest((3.toShort()..9.toShort() step 2).reversed(), 9, 3, -2, listOf(9, 7, 5, 3))
        doTest((3.toLong()..9.toLong() step 2.toLong()).reversed(), 9.toLong(), 3.toLong(), -2.toLong(), listOf<Long>(9, 7, 5, 3))

        doTest(('c'..'g' step 2).reversed(), 'g', 'c', -2, listOf('g', 'e', 'c'))

        doTest((4.0..6.0 step 0.5).reversed(), 6.0, 4.0, -0.5, listOf(6.0, 5.5, 5.0, 4.5, 4.0))
        doTest((4.0.toFloat()..6.0.toFloat() step 0.5.toFloat()).reversed(), 6.0.toFloat(), 4.0.toFloat(), -0.5.toFloat(),
                listOf<Float>(6.0.toFloat(), 5.5.toFloat(), 5.0.toFloat(), 4.5.toFloat(), 4.0.toFloat()))
    }

    // invariant progression.reversed().toList() == progression.toList().reversed() is preserved
    // 'inexact' means that start of reversed progression is not the end of original progression, but the last element
    @test fun reversedInexactSteppedDownTo() {
        doTest((8 downTo 3 step 2).reversed(), 4, 8, 2, listOf(4, 6, 8))
        doTest((8.toByte() downTo 3.toByte() step 2).reversed(), 4, 8, 2, listOf(4, 6, 8))
        doTest((8.toShort() downTo 3.toShort() step 2).reversed(), 4, 8, 2, listOf(4, 6, 8))
        doTest((8.toLong() downTo 3.toLong() step 2.toLong()).reversed(), 4.toLong(), 8.toLong(), 2.toLong(), listOf<Long>(4, 6, 8))

        doTest(('d' downTo 'a' step 2).reversed(), 'b', 'd', 2, listOf('b', 'd'))
    }
}
