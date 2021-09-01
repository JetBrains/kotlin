/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.ranges

import test.collections.behaviors.iteratorBehavior
import test.collections.compare
import kotlin.test.*

const val MaxI = Int.MAX_VALUE
const val MinI = Int.MIN_VALUE
const val MaxB = Byte.MAX_VALUE
const val MinB = Byte.MIN_VALUE
const val MaxS = Short.MAX_VALUE
const val MinS = Short.MIN_VALUE
const val MaxL = Long.MAX_VALUE
const val MinL = Long.MIN_VALUE
const val MaxC = Char.MAX_VALUE
const val MinC = Char.MIN_VALUE

const val MaxUI = UInt.MAX_VALUE
const val MinUI = UInt.MIN_VALUE
const val MaxUL = ULong.MAX_VALUE
const val MinUL = ULong.MIN_VALUE
const val MaxUB = UByte.MAX_VALUE
const val MinUB = UByte.MIN_VALUE
const val MaxUS = UShort.MAX_VALUE
const val MinUS = UShort.MIN_VALUE

// Test data for codegen is generated from this class. If you change it, rerun generateTests task
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
            is UIntProgression -> {
                first = sequence.first
                last = sequence.last
                increment = sequence.step
            }
            is ULongProgression -> {
                first = sequence.first
                last = sequence.last
                increment = sequence.step
            }
            else -> throw IllegalArgumentException("Unsupported sequence type: $sequence")
        }

        assertEquals(expectedFirst, first)
        assertEquals(expectedLast, last)
        assertEquals(expectedIncrement, increment)

        assertEquals(expectedElements.isEmpty(), sequence.none())
        assertEquals(expectedElements, sequence.toList())

        compare(expectedElements.iterator(), sequence.iterator()) {
            iteratorBehavior()
        }
    }

}

// Test data for codegen is generated from this class. If you change it, rerun GenerateTests
@Suppress("EmptyRange")
public class RangeIterationTest : RangeIterationTestBase() {

    @Test fun emptyConstant() {
        doTest(IntRange.EMPTY, 1, 0, 1, listOf())
        doTest(LongRange.EMPTY, 1L, 0L, 1L, listOf())

        doTest(CharRange.EMPTY, 1.toChar(), 0.toChar(), 1, listOf())

        doTest(UIntRange.EMPTY, MaxUI, MinUI, 1, listOf())
        doTest(ULongRange.EMPTY, MaxUL, MinUL, 1L, listOf())
    }

    @Test fun emptyRange() {
        doTest(10..5, 10, 5, 1, listOf())
        doTest(10.toByte()..(-5).toByte(), 10, (-5), 1, listOf())
        doTest(10.toShort()..(-5).toShort(), 10, (-5), 1, listOf())
        doTest(10L..-5L, 10L, -5L, 1L, listOf())

        doTest('z'..'a', 'z', 'a', 1, listOf())

        doTest(10u..5u, 10u, 5u, 1, listOf())
        doTest(10u.toUByte()..5u.toUByte(), 10u, 5u, 1, listOf())
        doTest(10u.toUShort()..5u.toUShort(), 10u, 5u, 1, listOf())
        doTest(10uL..5uL, 10uL, 5uL, 1L, listOf())
    }

    @Test fun oneElementRange() {
        doTest(5..5, 5, 5, 1, listOf(5))
        doTest(5.toByte()..5.toByte(), 5, 5, 1, listOf(5))
        doTest(5.toShort()..5.toShort(), 5, 5, 1, listOf(5))
        doTest(5L..5L, 5L, 5L, 1L, listOf(5L))

        doTest('k'..'k', 'k', 'k', 1, listOf('k'))

        doTest(5u..5u, 5u, 5u, 1, listOf(5u))
        doTest(5u.toUByte()..5u.toUByte(), 5u, 5u, 1, listOf(5u))
        doTest(5u.toUShort()..5u.toUShort(), 5u, 5u, 1, listOf(5u))
        doTest(5uL..5uL, 5uL, 5uL, 1L, listOf(5uL))
    }

    @Test fun simpleRange() {
        doTest(3..9, 3, 9, 1, listOf(3, 4, 5, 6, 7, 8, 9))
        doTest(3.toByte()..9.toByte(), 3, 9, 1, listOf(3, 4, 5, 6, 7, 8, 9))
        doTest(3.toShort()..9.toShort(), 3, 9, 1, listOf(3, 4, 5, 6, 7, 8, 9))
        doTest(3L..9L, 3L, 9L, 1L, listOf<Long>(3, 4, 5, 6, 7, 8, 9))

        doTest('c'..'g', 'c', 'g', 1, listOf('c', 'd', 'e', 'f', 'g'))

        doTest(3u..5u, 3u, 5u, 1, listOf(3u, 4u, 5u))
        doTest(3.toUByte()..5.toUByte(), 3u, 5u, 1, listOf(3u, 4u, 5u))
        doTest(3.toUShort()..5.toUShort(), 3u, 5u, 1, listOf(3u, 4u, 5u))
        doTest(3uL..5uL, 3uL, 5uL, 1L, listOf<ULong>(3u, 4u, 5u))
    }


    @Test fun simpleRangeWithNonConstantEnds() {
        doTest((1 + 2)..(10 - 1), 3, 9, 1, listOf(3, 4, 5, 6, 7, 8, 9))
        doTest((1.toByte() + 2.toByte()).toByte()..(10.toByte() - 1.toByte()).toByte(), 3, 9, 1, listOf(3, 4, 5, 6, 7, 8, 9))
        doTest((1.toShort() + 2.toShort()).toShort()..(10.toShort() - 1.toShort()).toShort(), 3, 9, 1, listOf(3, 4, 5, 6, 7, 8, 9))
        doTest((1L + 2L)..(10L - 1L), 3L, 9L, 1L, listOf<Long>(3, 4, 5, 6, 7, 8, 9))

        doTest(("ace"[1])..("age"[1]), 'c', 'g', 1, listOf('c', 'd', 'e', 'f', 'g'))

        doTest((1u + 2u)..(6u - 1u), 3u, 5u, 1, listOf(3u, 4u, 5u))
        doTest((1u.toUByte() + 2u.toUByte()).toUByte()..(6u.toUByte() - 1u.toUByte()).toUByte(), 3u, 5u, 1, listOf(3u, 4u, 5u))
        doTest((1u.toUShort() + 2u.toUShort()).toUShort()..(6u.toUShort() - 1u.toUShort()).toUShort(), 3u, 5u, 1, listOf(3u, 4u, 5u))
        doTest((1uL + 2uL)..(6uL - 1uL), 3uL, 5uL, 1L, listOf<ULong>(3u, 4u, 5u))
    }

    @Test fun openRange() {
        doTest(1 until 5, 1, 4, 1, listOf(1, 2, 3, 4))
        doTest(1.toByte() until 5.toByte(), 1, 4, 1, listOf(1, 2, 3, 4))
        doTest(1.toShort() until 5.toShort(), 1, 4, 1, listOf(1, 2, 3, 4))
        doTest(1L until 5L, 1L, 4L, 1L, listOf<Long>(1, 2, 3, 4))
        doTest('a' until 'd', 'a', 'c', 1, listOf('a', 'b', 'c'))

        doTest(1u until 5u, 1u, 4u, 1, listOf(1u, 2u, 3u, 4u))
        doTest(1u.toUByte() until 5u.toUByte(), 1u, 4u, 1, listOf(1u, 2u, 3u, 4u))
        doTest(1u.toUShort() until 5u.toUShort(), 1u, 4u, 1, listOf(1u, 2u, 3u, 4u))
        doTest(1uL until 5uL, 1uL, 4uL, 1L, listOf<ULong>(1u, 2u, 3u, 4u))
    }


    @Test fun emptyDownto() {
        doTest(5 downTo 10, 5, 10, -1, listOf())
        doTest(5.toByte() downTo 10.toByte(), 5, 10, -1, listOf())
        doTest(5.toShort() downTo 10.toShort(), 5, 10, -1, listOf())
        doTest(5L downTo 10L, 5L, 10L, -1L, listOf())

        doTest('a' downTo 'z', 'a', 'z', -1, listOf())

        doTest(5u downTo 10u, 5u, 10u, -1, listOf())
        doTest(5u.toUByte() downTo 10u.toUByte(), 5u, 10u, -1, listOf())
        doTest(5u.toUShort() downTo 10u.toUShort(), 5u, 10u, -1, listOf())
        doTest(5uL downTo 10uL, 5uL, 10uL, -1L, listOf())

    }

    @Test fun oneElementDownTo() {
        doTest(5 downTo 5, 5, 5, -1, listOf(5))
        doTest(5.toByte() downTo 5.toByte(), 5, 5, -1, listOf(5))
        doTest(5.toShort() downTo 5.toShort(), 5, 5, -1, listOf(5))
        doTest(5L downTo 5L, 5L, 5L, -1L, listOf(5L))

        doTest('k' downTo 'k', 'k', 'k', -1, listOf('k'))

        doTest(5u downTo 5u, 5u, 5u, -1, listOf(5u))
        doTest(5u.toUByte() downTo 5u.toUByte(), 5u, 5u, -1, listOf(5u))
        doTest(5u.toUShort() downTo 5u.toUShort(), 5u, 5u, -1, listOf(5u))
        doTest(5uL downTo 5uL, 5uL, 5uL, -1L, listOf(5uL))
    }

    @Test fun simpleDownTo() {
        doTest(9 downTo 3, 9, 3, -1, listOf(9, 8, 7, 6, 5, 4, 3))
        doTest(9.toByte() downTo 3.toByte(), 9, 3, -1, listOf(9, 8, 7, 6, 5, 4, 3))
        doTest(9.toShort() downTo 3.toShort(), 9, 3, -1, listOf(9, 8, 7, 6, 5, 4, 3))
        doTest(9L downTo 3L, 9L, 3L, -1L, listOf<Long>(9, 8, 7, 6, 5, 4, 3))

        doTest('g' downTo 'c', 'g', 'c', -1, listOf('g', 'f', 'e', 'd', 'c'))

        doTest(5u downTo 3u, 5u, 3u, -1, listOf(5u, 4u, 3u))
        doTest(5u.toUByte() downTo 3u.toUByte(), 5u, 3u, -1, listOf(5u, 4u, 3u))
        doTest(5u.toUShort() downTo 3u.toUShort(), 5u, 3u, -1, listOf(5u, 4u, 3u))
        doTest(5uL downTo 3uL, 5uL, 3uL, -1L, listOf<ULong>(5u, 4u, 3u))
    }


    @Test fun simpleSteppedRange() {
        doTest(3..9 step 2, 3, 9, 2, listOf(3, 5, 7, 9))
        doTest(3.toByte()..9.toByte() step 2, 3, 9, 2, listOf(3, 5, 7, 9))
        doTest(3.toShort()..9.toShort() step 2, 3, 9, 2, listOf(3, 5, 7, 9))
        doTest(3L..9L step 2L, 3L, 9L, 2L, listOf<Long>(3, 5, 7, 9))

        doTest('c'..'g' step 2, 'c', 'g', 2, listOf('c', 'e', 'g'))

        doTest(3u..9u step 2, 3u, 9u, 2, listOf(3u, 5u, 7u, 9u))
        doTest(3u.toUByte()..9u.toUByte() step 2, 3u, 9u, 2, listOf(3u, 5u, 7u, 9u))
        doTest(3u.toUShort()..9u.toUShort() step 2, 3u, 9u, 2, listOf(3u, 5u, 7u, 9u))
        doTest(3uL..9uL step 2L, 3uL, 9uL, 2L, listOf<ULong>(3u, 5u, 7u, 9u))
    }

    @Test fun simpleSteppedDownTo() {
        doTest(9 downTo 3 step 2, 9, 3, -2, listOf(9, 7, 5, 3))
        doTest(9.toByte() downTo 3.toByte() step 2, 9, 3, -2, listOf(9, 7, 5, 3))
        doTest(9.toShort() downTo 3.toShort() step 2, 9, 3, -2, listOf(9, 7, 5, 3))
        doTest(9L downTo 3L step 2L, 9L, 3L, -2L, listOf<Long>(9, 7, 5, 3))

        doTest('g' downTo 'c' step 2, 'g', 'c', -2, listOf('g', 'e', 'c'))

        doTest(9u downTo 3u step 2, 9u, 3u, -2, listOf(9u, 7u, 5u, 3u))
        doTest(9u.toUByte() downTo 3u.toUByte() step 2, 9u, 3u, -2, listOf(9u, 7u, 5u, 3u))
        doTest(9u.toUShort() downTo 3u.toUShort() step 2, 9u, 3u, -2, listOf(9u, 7u, 5u, 3u))
        doTest(9uL downTo 3uL step 2L, 9uL, 3uL, -2L, listOf<ULong>(9u, 7u, 5u, 3u))
    }


    // 'inexact' means last element is not equal to sequence end
    @Test fun inexactSteppedRange() {
        doTest(3..8 step 2, 3, 7, 2, listOf(3, 5, 7))
        doTest(3.toByte()..8.toByte() step 2, 3, 7, 2, listOf(3, 5, 7))
        doTest(3.toShort()..8.toShort() step 2, 3, 7, 2, listOf(3, 5, 7))
        doTest(3L..8L step 2L, 3L, 7L, 2L, listOf<Long>(3, 5, 7))

        doTest('a'..'d' step 2, 'a', 'c', 2, listOf('a', 'c'))

        doTest(3u..8u step 2, 3u, 7u, 2, listOf(3u, 5u, 7u))
        doTest(3u.toUByte()..8u.toUByte() step 2, 3u, 7u, 2, listOf(3u, 5u, 7u))
        doTest(3u.toUShort()..8u.toUShort() step 2, 3u, 7u, 2, listOf(3u, 5u, 7u))
        doTest(3uL..8uL step 2L, 3uL, 7uL, 2L, listOf<ULong>(3u, 5u, 7u))
    }

    // 'inexact' means last element is not equal to sequence end
    @Test fun inexactSteppedDownTo() {
        doTest(8 downTo 3 step 2, 8, 4, -2, listOf(8, 6, 4))
        doTest(8.toByte() downTo 3.toByte() step 2, 8, 4, -2, listOf(8, 6, 4))
        doTest(8.toShort() downTo 3.toShort() step 2, 8, 4, -2, listOf(8, 6, 4))
        doTest(8L downTo 3L step 2L, 8L, 4L, -2L, listOf<Long>(8, 6, 4))

        doTest('d' downTo 'a' step 2, 'd', 'b', -2, listOf('d', 'b'))

        doTest(8u downTo 3u step 2, 8u, 4u, -2, listOf(8u, 6u, 4u))
        doTest(8u.toUByte() downTo 3u.toUByte() step 2, 8u, 4u, -2, listOf(8u, 6u, 4u))
        doTest(8u.toUShort() downTo 3u.toUShort() step 2, 8u, 4u, -2, listOf(8u, 6u, 4u))
        doTest(8uL downTo 3uL step 2L, 8uL, 4uL, -2L, listOf<ULong>(8u, 6u, 4u))
    }


    @Test fun reversedEmptyRange() {
        doTest((5..3).reversed(), 3, 5, -1, listOf())
        doTest((5.toByte()..3.toByte()).reversed(), 3, 5, -1, listOf())
        doTest((5.toShort()..3.toShort()).reversed(), 3, 5, -1, listOf())
        doTest((5L..3L).reversed(), 3L, 5L, -1L, listOf())

        doTest(('c'..'a').reversed(), 'a', 'c', -1, listOf())

        doTest((5u..3u).reversed(), 3u, 5u, -1, listOf())
        doTest((5u.toUByte()..3u.toUByte()).reversed(), 3u, 5u, -1, listOf())
        doTest((5u.toUShort()..3u.toUShort()).reversed(), 3u, 5u, -1, listOf())
        doTest((5uL..3uL).reversed(), 3uL, 5uL, -1L, listOf())
    }

    @Test fun reversedEmptyBackSequence() {
        doTest((3 downTo 5).reversed(), 5, 3, 1, listOf())
        doTest((3.toByte() downTo 5.toByte()).reversed(), 5, 3, 1, listOf())
        doTest((3.toShort() downTo 5.toShort()).reversed(), 5, 3, 1, listOf())
        doTest((3L downTo 5L).reversed(), 5L, 3L, 1L, listOf())

        doTest(('a' downTo 'c').reversed(), 'c', 'a', 1, listOf())

        doTest((3u downTo 5u).reversed(), 5u, 3u, 1, listOf())
        doTest((3u.toUByte() downTo 5u.toUByte()).reversed(), 5u, 3u, 1, listOf())
        doTest((3u.toUShort() downTo 5u.toUShort()).reversed(), 5u, 3u, 1, listOf())
        doTest((3uL downTo 5uL).reversed(), 5uL, 3uL, 1L, listOf())
    }

    @Test fun reversedRange() {
        doTest((3..5).reversed(), 5, 3, -1, listOf(5, 4, 3))
        doTest((3.toByte()..5.toByte()).reversed(),5, 3, -1, listOf(5, 4, 3))
        doTest((3.toShort()..5.toShort()).reversed(), 5, 3, -1, listOf(5, 4, 3))
        doTest((3L..5L).reversed(), 5L, 3L, -1L, listOf<Long>(5, 4, 3))

        doTest(('a'..'c').reversed(), 'c', 'a', -1, listOf('c', 'b', 'a'))

        doTest((3u..5u).reversed(), 5u, 3u, -1, listOf(5u, 4u, 3u))
        doTest((3u.toUByte()..5u.toUByte()).reversed(),5u, 3u, -1, listOf(5u, 4u, 3u))
        doTest((3u.toUShort()..5u.toUShort()).reversed(), 5u, 3u, -1, listOf(5u, 4u, 3u))
        doTest((3uL..5uL).reversed(), 5uL, 3uL, -1L, listOf<ULong>(5u, 4u, 3u))
    }

    @Test fun reversedBackSequence() {
        doTest((5 downTo 3).reversed(), 3, 5, 1, listOf(3, 4, 5))
        doTest((5.toByte() downTo 3.toByte()).reversed(), 3, 5, 1, listOf(3, 4, 5))
        doTest((5.toShort() downTo 3.toShort()).reversed(), 3, 5, 1, listOf(3, 4, 5))
        doTest((5L downTo 3L).reversed(), 3L, 5L, 1L, listOf<Long>(3, 4, 5))

        doTest(('c' downTo 'a').reversed(), 'a', 'c', 1, listOf('a', 'b', 'c'))

        doTest((5u downTo 3u).reversed(), 3u, 5u, 1, listOf(3u, 4u, 5u))
        doTest((5u.toUByte() downTo 3u.toUByte()).reversed(), 3u, 5u, 1, listOf(3u, 4u, 5u))
        doTest((5u.toUShort() downTo 3u.toUShort()).reversed(), 3u, 5u, 1, listOf(3u, 4u, 5u))
        doTest((5uL downTo 3uL).reversed(), 3uL, 5uL, 1L, listOf<ULong>(3u, 4u, 5u))
     }

    @Test fun reversedSimpleSteppedRange() {
        doTest((3..9 step 2).reversed(), 9, 3, -2, listOf(9, 7, 5, 3))
        doTest((3.toByte()..9.toByte() step 2).reversed(), 9, 3, -2, listOf(9, 7, 5, 3))
        doTest((3.toShort()..9.toShort() step 2).reversed(), 9, 3, -2, listOf(9, 7, 5, 3))
        doTest((3L..9L step 2L).reversed(), 9L, 3L, -2L, listOf<Long>(9, 7, 5, 3))

        doTest(('c'..'g' step 2).reversed(), 'g', 'c', -2, listOf('g', 'e', 'c'))

        doTest((3u..9u step 2).reversed(), 9u, 3u, -2, listOf(9u, 7u, 5u, 3u))
        doTest((3u.toUByte()..9u.toUByte() step 2).reversed(), 9u, 3u, -2, listOf(9u, 7u, 5u, 3u))
        doTest((3u.toUShort()..9u.toUShort() step 2).reversed(), 9u, 3u, -2, listOf(9u, 7u, 5u, 3u))
        doTest((3uL..9uL step 2L).reversed(), 9uL, 3uL, -2L, listOf<ULong>(9u, 7u, 5u, 3u))
    }

    // invariant progression.reversed().toList() == progression.toList().reversed() is preserved
    // 'inexact' means that start of reversed progression is not the end of original progression, but the last element
    @Test fun reversedInexactSteppedDownTo() {
        doTest((8 downTo 3 step 2).reversed(), 4, 8, 2, listOf(4, 6, 8))
        doTest((8.toByte() downTo 3.toByte() step 2).reversed(), 4, 8, 2, listOf(4, 6, 8))
        doTest((8.toShort() downTo 3.toShort() step 2).reversed(), 4, 8, 2, listOf(4, 6, 8))
        doTest((8L downTo 3L step 2L).reversed(), 4L, 8L, 2L, listOf<Long>(4, 6, 8))

        doTest(('d' downTo 'a' step 2).reversed(), 'b', 'd', 2, listOf('b', 'd'))

        doTest((8u downTo 3u step 2).reversed(), 4u, 8u, 2, listOf(4u, 6u, 8u))
        doTest((8u.toUByte() downTo 3u.toUByte() step 2).reversed(), 4u, 8u, 2, listOf(4u, 6u, 8u))
        doTest((8u.toUShort() downTo 3u.toUShort() step 2).reversed(), 4u, 8u, 2, listOf(4u, 6u, 8u))
        doTest((8uL downTo 3uL step 2L).reversed(), 4uL, 8uL, 2L, listOf<ULong>(4u, 6u, 8u))
    }


    @Test fun maxValueToMaxValue() {
        doTest(MaxI..MaxI, MaxI, MaxI, 1, listOf(MaxI))
        doTest(MaxB..MaxB, MaxB.toInt(), MaxB.toInt(), 1, listOf(MaxB.toInt()))
        doTest(MaxS..MaxS, MaxS.toInt(), MaxS.toInt(), 1, listOf(MaxS.toInt()))
        doTest(MaxL..MaxL, MaxL, MaxL, 1L, listOf(MaxL))

        doTest(MaxC..MaxC, MaxC, MaxC, 1, listOf(MaxC))

        doTest(MaxUI..MaxUI, MaxUI, MaxUI, 1, listOf(MaxUI))
        doTest(MaxUL..MaxUL, MaxUL, MaxUL, 1L, listOf(MaxUL))
    }

    @Test fun maxValueMinusTwoToMaxValue() {
        doTest((MaxI - 2)..MaxI, MaxI - 2, MaxI, 1, listOf(MaxI - 2, MaxI - 1, MaxI))
        doTest((MaxB - 2).toByte()..MaxB, (MaxB - 2).toInt(), MaxB.toInt(), 1, listOf((MaxB - 2).toInt(), (MaxB - 1).toInt(), MaxB.toInt()))
        doTest((MaxS - 2).toShort()..MaxS, (MaxS - 2).toInt(), MaxS.toInt(), 1, listOf((MaxS - 2).toInt(), (MaxS - 1).toInt(), MaxS.toInt()))
        doTest((MaxL - 2).toLong()..MaxL, (MaxL - 2).toLong(), MaxL, 1L, listOf((MaxL - 2).toLong(), (MaxL - 1).toLong(), MaxL))

        doTest((MaxC - 2)..MaxC, (MaxC - 2), MaxC, 1, listOf((MaxC - 2), (MaxC - 1), MaxC))

        doTest((MaxUI - 2u)..MaxUI, MaxUI - 2u, MaxUI, 1, listOf(MaxUI - 2u, MaxUI - 1u, MaxUI))
        doTest((MaxUL - 2u)..MaxUL, MaxUL - 2u, MaxUL, 1L, listOf(MaxUL - 2u, MaxUL - 1u, MaxUL))
    }

    @Test fun maxValueToMinValue() {
        doTest(MaxI..MinI, MaxI, MinI, 1, listOf())
        doTest(MaxB..MinB, MaxB.toInt(), MinB.toInt(), 1, listOf())
        doTest(MaxS..MinS, MaxS.toInt(), MinS.toInt(), 1, listOf())
        doTest(MaxL..MinL, MaxL, MinL, 1L, listOf())

        doTest(MaxC..MinC, MaxC, MinC, 1, listOf())

        doTest(MaxUI..MinUI, MaxUI, MinUI, 1, listOf())
        doTest(MaxUL..MinUL, MaxUL, MinUL, 1L, listOf())
    }

    @Test fun progressionMaxValueToMaxValue() {
        doTest(MaxI..MaxI step 1, MaxI, MaxI, 1, listOf(MaxI))
        doTest(MaxB..MaxB step 1, MaxB.toInt(), MaxB.toInt(), 1, listOf(MaxB.toInt()))
        doTest(MaxS..MaxS step 1, MaxS.toInt(), MaxS.toInt(), 1, listOf(MaxS.toInt()))
        doTest(MaxL..MaxL step 1, MaxL, MaxL, 1L, listOf(MaxL))

        doTest(MaxC..MaxC step 1, MaxC, MaxC, 1, listOf(MaxC))

        doTest(MaxUI..MaxUI step 1, MaxUI, MaxUI, 1, listOf(MaxUI))
        doTest(MaxUL..MaxUL step 1, MaxUL, MaxUL, 1L, listOf(MaxUL))
    }

    @Test fun progressionMaxValueMinusTwoToMaxValue() {
        doTest((MaxI - 2)..MaxI step 2, MaxI - 2, MaxI, 2, listOf(MaxI - 2, MaxI))
        doTest((MaxB - 2).toByte()..MaxB step 2, (MaxB - 2).toInt(), MaxB.toInt(), 2, listOf((MaxB - 2).toInt(), MaxB.toInt()))
        doTest((MaxS - 2).toShort()..MaxS step 2, (MaxS - 2).toInt(), MaxS.toInt(), 2, listOf((MaxS - 2).toInt(), MaxS.toInt()))
        doTest((MaxL - 2).toLong()..MaxL step 2, (MaxL - 2).toLong(), MaxL, 2L, listOf((MaxL - 2).toLong(), MaxL))

        doTest((MaxC - 2)..MaxC step 2, (MaxC - 2), MaxC, 2, listOf((MaxC - 2), MaxC))

        doTest((MaxUI - 2u)..MaxUI step 2, MaxUI - 2u, MaxUI, 2, listOf(MaxUI - 2u, MaxUI))
        doTest((MaxUB - 2u).toUByte()..MaxUB step 2, (MaxUB - 2u).toUInt(), MaxUB.toUInt(), 2, listOf((MaxUB - 2u).toUInt(), MaxUB.toUInt()))
        doTest((MaxUS - 2u).toUShort()..MaxUS step 2, (MaxUS - 2u).toUInt(), MaxUS.toUInt(), 2, listOf((MaxUS - 2u).toUInt(), MaxUS.toUInt()))
        doTest(MaxUL - 2u..MaxUL step 2, MaxUL - 2u, MaxUL, 2L, listOf(MaxUL - 2u, MaxUL))
    }

    @Test fun progressionMaxValueToMinValue() {
        doTest(MaxI..MinI step 1, MaxI, MinI, 1, listOf())
        doTest(MaxB..MinB step 1, MaxB.toInt(), MinB.toInt(), 1, listOf())
        doTest(MaxS..MinS step 1, MaxS.toInt(), MinS.toInt(), 1, listOf())
        doTest(MaxL..MinL step 1, MaxL, MinL, 1L, listOf())

        doTest(MaxC..MinC step 1, MaxC, MinC, 1, listOf())

        doTest(MaxUI..MinUI step 1, MaxUI, MinUI, 1, listOf())
        doTest(MaxUB..MinUB step 1, MaxUB.toUInt(), MinUB.toUInt(), 1, listOf())
        doTest(MaxUS..MinUS step 1, MaxUS.toUInt(), MinUS.toUInt(), 1, listOf())
        doTest(MaxUL..MinUL step 1, MaxUL, MinUL, 1L, listOf())
    }

    @Test fun progressionMinValueToMinValue() {
        doTest(MinI..MinI step 1, MinI, MinI, 1, listOf(MinI))
        doTest(MinB..MinB step 1, MinB.toInt(), MinB.toInt(), 1, listOf(MinB.toInt()))
        doTest(MinS..MinS step 1, MinS.toInt(), MinS.toInt(), 1, listOf(MinS.toInt()))
        doTest(MinL..MinL step 1, MinL, MinL, 1L, listOf(MinL))

        doTest(MinC..MinC step 1, MinC, MinC, 1, listOf(MinC))

        doTest(MinUI..MinUI step 1, MinUI, MinUI, 1, listOf(MinUI))
        doTest(MinUB..MinUB step 1, MinUB.toUInt(), MinUB.toUInt(), 1, listOf(MinUB.toUInt()))
        doTest(MinUS..MinUS step 1, MinUS.toUInt(), MinUS.toUInt(), 1, listOf(MinUS.toUInt()))
        doTest(MinUL..MinUL step 1, MinUL, MinUL, 1L, listOf(MinUL))
    }

    @Test fun inexactToMaxValue() {
        doTest((MaxI - 5)..MaxI step 3, MaxI - 5, MaxI - 2, 3, listOf(MaxI - 5, MaxI - 2))
        doTest((MaxB - 5).toByte()..MaxB step 3, (MaxB - 5).toInt(), (MaxB - 2).toInt(), 3, listOf((MaxB - 5).toInt(), (MaxB - 2).toInt()))
        doTest((MaxS - 5).toShort()..MaxS step 3, (MaxS - 5).toInt(), (MaxS - 2).toInt(), 3, listOf((MaxS - 5).toInt(), (MaxS - 2).toInt()))
        doTest((MaxL - 5).toLong()..MaxL step 3, (MaxL - 5).toLong(), (MaxL - 2).toLong(), 3L, listOf((MaxL - 5).toLong(), (MaxL - 2).toLong()))

        doTest((MaxC - 5)..MaxC step 3, (MaxC - 5), (MaxC - 2), 3, listOf((MaxC - 5), (MaxC - 2)))

        doTest((MaxUI - 5u)..MaxUI step 3, MaxUI - 5u, MaxUI - 2u, 3, listOf(MaxUI - 5u, MaxUI - 2u))
        doTest((MaxUB - 5u).toUByte()..MaxUB step 3, (MaxUB - 5u).toUInt(), (MaxUB - 2u).toUInt(), 3, listOf((MaxUB - 5u).toUInt(), (MaxUB - 2u).toUInt()))
        doTest((MaxUS - 5u).toUShort()..MaxUS step 3, (MaxUS - 5u).toUInt(), (MaxUS - 2u).toUInt(), 3, listOf((MaxUS - 5u).toUInt(), (MaxUS - 2u).toUInt()))
        doTest((MaxUL - 5u)..MaxUL step 3, (MaxUL - 5u), (MaxUL - 2u), 3L, listOf((MaxUL - 5u), (MaxUL - 2u)))
    }

    @Test fun overflowZeroToMinValue() {
        doTest(0.toByte()..MinB step 3, 0, MinB.toInt(), 3, listOf())
        doTest(0.toShort()..MinS step 3, 0, MinS.toInt(), 3, listOf())
        doTest(0..MinI step 3, 0, MinI, 3, listOf())
        doTest(0L..MinL step 3, 0, MinL, 3L, listOf())

        doTest(0.toByte() until MinB step 3, 0, MinB.toInt() - 1, 3, listOf())
        doTest(0.toShort() until MinS step 3, 0, MinS.toInt() - 1, 3, listOf())
        doTest(0 until MinI step 3, 1, 0, 3, listOf())
        doTest(0L until MinL step 3, 1L, 0L, 3L, listOf())

        // 1u as used as a start since min value is 0u for unsigned types
        doTest(1u.toUByte()..MinUB step 3, 1u, MinUB.toUInt(), 3, listOf())
        doTest(1u.toUShort()..MinUS step 3, 1u, MinUS.toUInt(), 3, listOf())
        doTest(1u..MinUI step 3, 1u, MinUI, 3, listOf())
        doTest(1uL..MinUL step 3, 1u, MinUL, 3L, listOf())

        doTest(1u.toUByte() until MinUB step 3, MaxUI, MinUI, 3, listOf())
        doTest(1u.toUShort() until MinUS step 3, MaxUI, MinUI, 3, listOf())
        doTest(1u until MinUI step 3, MaxUI, MinUI, 3, listOf())
        doTest(1uL until MinUL step 3, MaxUL, MinUL, 3L, listOf())
    }

    @Test fun progressionDownToMinValue() {
        doTest((MinI + 2) downTo MinI step 1, MinI + 2, MinI, -1, listOf(MinI + 2, MinI + 1, MinI))
        doTest((MinB + 2).toByte() downTo MinB step 1, (MinB + 2).toInt(), MinB.toInt(), -1, listOf((MinB + 2).toInt(), (MinB + 1).toInt(), MinB.toInt()))
        doTest((MinS + 2).toShort() downTo MinS step 1, (MinS + 2).toInt(), MinS.toInt(), -1, listOf((MinS + 2).toInt(), (MinS + 1).toInt(), MinS.toInt()))
        doTest((MinL + 2).toLong() downTo MinL step 1, (MinL + 2).toLong(), MinL, -1L, listOf((MinL + 2).toLong(), (MinL + 1).toLong(), MinL))

        doTest((MinC + 2) downTo MinC step 1, (MinC + 2), MinC, -1, listOf((MinC + 2), (MinC + 1), MinC))

        doTest((MinUI + 2u) downTo MinUI step 1, MinUI + 2u, MinUI, -1, listOf(MinUI + 2u, MinUI + 1u, MinUI))
        doTest((MinUB + 2u).toUByte() downTo MinUB step 1, (MinUB + 2u).toUInt(), MinUB.toUInt(), -1, listOf((MinUB + 2u).toUInt(), (MinUB + 1u).toUInt(), MinUB.toUInt()))
        doTest((MinUS + 2u).toUShort() downTo MinUS step 1, (MinUS + 2u).toUInt(), MinUS.toUInt(), -1, listOf((MinUS + 2u).toUInt(), (MinUS + 1u).toUInt(), MinUS.toUInt()))
        doTest((MinUL + 2u) downTo MinUL step 1, (MinUL + 2u), MinUL, -1L, listOf((MinUL + 2u), (MinUL + 1u), MinUL))
    }

    @Test fun inexactDownToMinValue() {
        doTest((MinI + 5) downTo MinI step 3, MinI + 5, MinI + 2, -3, listOf(MinI + 5, MinI + 2))
        doTest((MinB + 5).toByte() downTo MinB step 3, (MinB + 5).toInt(), (MinB + 2).toInt(), -3, listOf((MinB + 5).toInt(), (MinB + 2).toInt()))
        doTest((MinS + 5).toShort() downTo MinS step 3, (MinS + 5).toInt(), (MinS + 2).toInt(), -3, listOf((MinS + 5).toInt(), (MinS + 2).toInt()))
        doTest((MinL + 5).toLong() downTo MinL step 3, (MinL + 5).toLong(), (MinL + 2).toLong(), -3L, listOf((MinL + 5).toLong(), (MinL + 2).toLong()))

        doTest((MinC + 5) downTo MinC step 3, (MinC + 5), (MinC + 2), -3, listOf((MinC + 5), (MinC + 2)))

        doTest((MinUI + 5u) downTo MinUI step 3, MinUI + 5u, MinUI + 2u, -3, listOf(MinUI + 5u, MinUI + 2u))
        doTest((MinUB + 5u).toUByte() downTo MinUB step 3, (MinUB + 5u).toUInt(), (MinUB + 2u).toUInt(), -3, listOf((MinUB + 5u).toUInt(), (MinUB + 2u).toUInt()))
        doTest((MinUS + 5u).toUShort() downTo MinUS step 3, (MinUS + 5u).toUInt(), (MinUS + 2u).toUInt(), -3, listOf((MinUS + 5u).toUInt(), (MinUS + 2u).toUInt()))
        doTest(MinUL + 5u downTo MinUL step 3, (MinUL + 5u), (MinUL + 2u), -3L, listOf((MinUL + 5u), (MinUL + 2u)))
    }

    @Test fun overflowZeroDownToMaxValue() {
        doTest(0 downTo MaxI step 3, 0, MaxI, -3, listOf())
        doTest(0 downTo MaxL step 3, 0, MaxL, -3L, listOf())

        doTest(0u downTo MaxUI step 3, 0u, MaxUI, -3, listOf())
        doTest(0uL downTo MaxUL step 3, 0u, MaxUL, -3L, listOf())
    }
}
