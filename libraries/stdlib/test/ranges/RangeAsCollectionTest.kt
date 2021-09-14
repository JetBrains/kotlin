/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.ranges

import test.collections.compare
import kotlin.math.sign
import kotlin.test.*

class RangeAsCollectionTest {
    private fun <T : Comparable<T>, S : Number> progressionElements(
        bounds: Collection<T>,
        steps: Collection<S>,
        buildRange: (T, T) -> Collection<T>,
        buildProgression: (T, T, S) -> Collection<T>,
        difference: (T, T) -> S,
    ) {
        fun rangeElementsAreInOrder(range: Collection<T>, start: T, finish: T, step: S) {
            val valuesRange = if (step.toLong() > 0) start..finish else finish..start
            val iterator = range.iterator()
            if (!iterator.hasNext()) {
                assertEquals(0, range.size)
                assertTrue(range.isEmpty())
            } else {
                val count = iterator.asSequence()
                    .onEach { assertContains(valuesRange, it) }
                    .zipWithNext { prev, next ->
                        assertEquals(step.toLong().sign, next.compareTo(prev).sign)
                        assertEquals(step, difference(next, prev))
                    }.count() + 1 // zipWithNext reduces count by 1
                assertEquals(count, range.size)
            }
        }

        val stepOne = steps.single { it.toLong() == 1L }

        for (start in bounds) {
            for (finish in bounds) {
                run {
                    val range = buildRange(start, finish)
                    rangeElementsAreInOrder(range, start, finish, stepOne)
                    rangeAsCollectionBehavior(range, bounds)
                }
                for (step in steps) {
                    if (step.toLong() == 0L) continue
                    val progression = buildProgression(start, finish, step)
                    try {
                        rangeElementsAreInOrder(progression, start, finish, step)
                        rangeAsCollectionBehavior(progression, bounds)
                    } catch (e: AssertionError) {
                        fail("Current progression is $progression.", e)
                    }
                }
            }
        }
    }

    private fun <T : Any> rangeAsCollectionBehavior(
        range: Collection<T>,
        bounds: Collection<T>
    ) {
        compare(range.toList(), range) {
            propertyEquals { isEmpty() }
            propertyEquals { size }

            for (element in bounds) {
                propertyEquals("$element") { element in this }
            }

            repeat(10) {
                for (length in 1..3) {
                    val randomElements = List(length) { bounds.random() }
                    propertyEquals { containsAll(randomElements) }
                }
            }
        }

        assertTrue(range.containsAll(emptyList()))
        assertFalse(range.containsAll(listOf(Any())))
    }

    private fun <T : Comparable<T>, C : Collection<T>, S : Number> progressionModerateSize(
        bounds: List<T>,
        steps: List<S>,
        buildProgression: (T, T, S) -> C,
        elementToLong: T.() -> Long,
        getLast: C.() -> T,
    ) {
        for (start in bounds) {
            for (finish in bounds) {
                for (step in steps) {
                    val progression = buildProgression(start, finish, step)
                    if (start > finish && step.toLong() > 0L || start < finish && step.toLong() < 0L)
                        assertEquals(0, progression.size)
                    else {
                        val expected = minOf((progression.getLast().elementToLong() - start.elementToLong()) / step.toLong() + 1, Int.MAX_VALUE.toLong())
                        assertEquals(expected, progression.size.toLong())
                    }
                }
            }
        }
    }

    private fun <T : Comparable<T>, S : Number> progressionClampedSize(
        mins: Iterable<T>,
        belowZeros: Iterable<T>,
        aboveZeros: Iterable<T>,
        maxes: Iterable<T>,
        steps: List<S>,
        buildProgression: (T, T, S) -> Collection<T>
    ) {
        fun checkForStep(start: T, finish: T, step: S) {
            val progression = when {
                step.toLong() > 0 -> buildProgression(start, finish, step)
                else -> buildProgression(finish, start, step)
            }
            assertEquals(Int.MAX_VALUE, progression.size)
        }
        for (start in mins)
            for (finish in aboveZeros + maxes) {
                for (step in steps) {
                    checkForStep(start, finish, step)
                }
            }
        for (start in belowZeros)
            for (finish in maxes) {
                for (step in steps) {
                    checkForStep(start, finish, step)
                }
            }
    }

    @Test
    fun intProgressionIsCollection() {
        val nearZero = -2..2
        val smallSteps = -2..2
        progressionElements(
            bounds = nearZero,
            steps = smallSteps,
            buildRange = Int::rangeTo,
            buildProgression = IntProgression.Companion::fromClosedRange,
            difference = Int::minus
        )

        val min = Int.MIN_VALUE
        val nearMin = min..(min + 2)
        val max = Int.MAX_VALUE
        val nearMax = (max - 2)..max
        val bigSteps = (-10..10).minus(0).map { max / it }.flatMap { (it - 2)..(it + 2) }

        progressionModerateSize(
            bounds = nearMin + nearZero + nearMax,
            steps = smallSteps + bigSteps - 0,
            buildProgression = IntProgression.Companion::fromClosedRange,
            elementToLong = Int::toLong,
            getLast = IntProgression::last
        )
        progressionClampedSize(
            mins = listOf(min),
            belowZeros = nearMin + nearZero.filter { it <= 0 },
            aboveZeros = nearZero.filter { it >= -1 } + nearMax,
            maxes = listOf(max),
            steps = listOf(1, -1),
            buildProgression = IntProgression.Companion::fromClosedRange
        )
    }

    @Test
    fun longProgressionIsCollection() {
        val nearZero = -2L..2L
        val smallSteps = nearZero
        val intMin = Int.MIN_VALUE.toLong()
        val nearIntMin = (intMin - 10L)..(intMin + 10L)
        val nearLongMin = Long.MIN_VALUE..(Long.MIN_VALUE + 2L)
        val intMax = Int.MAX_VALUE.toLong()
        val nearIntMax = (intMax - 2L)..(intMax + 2L)
        val nearLongMax = (Long.MAX_VALUE - 2L)..Long.MAX_VALUE
        val bigSteps = (-10L..10L).minus(0L).map { intMax / it }.flatMap { (it - 2L)..(it + 2L) }

        progressionElements(
            bounds = nearZero,
            steps = smallSteps,
            buildRange = Long::rangeTo,
            buildProgression = LongProgression.Companion::fromClosedRange,
            difference = Long::minus
        )
        progressionModerateSize(
            bounds = nearIntMin + nearZero + nearIntMax,
            steps = smallSteps + bigSteps - 0L,
            buildProgression = LongProgression.Companion::fromClosedRange,
            elementToLong = { this },
            getLast = LongProgression::last
        )
        progressionClampedSize(
            mins = nearLongMin + intMin,
            belowZeros = nearIntMin + nearZero.filter { it <= 0 },
            aboveZeros = nearZero.filter { it >= -1 } + nearIntMax,
            maxes = nearLongMax + intMax,
            steps = listOf(1L, -1L),
            buildProgression = LongProgression.Companion::fromClosedRange
        )
    }

    @Test
    fun charProgressionIsCollection() {
        progressionElements(
            bounds = 'a'..'d',
            steps = -2..2,
            buildRange = Char::rangeTo,
            buildProgression = CharProgression.Companion::fromClosedRange,
            difference = Char::minus
        )
        progressionModerateSize(
            bounds = listOf(Char.MIN_VALUE, Char(1024), Char.MAX_VALUE),
            steps = listOf(-Int.MAX_VALUE, -100, -1, 1, 100, Char.MAX_VALUE.code, Int.MAX_VALUE),
            buildProgression = CharProgression.Companion::fromClosedRange,
            elementToLong = { code.toLong() },
            getLast = CharProgression::last
        )
    }


    @Test
    fun uintProgressionIsCollection() {
        progressionElements(
            bounds = 0U..5U,
            steps = -3..3,
            buildRange = UInt::rangeTo,
            buildProgression = UIntProgression.Companion::fromClosedRange,
            difference = { a, b -> (a - b).toInt() },
        )
        progressionModerateSize(
            bounds = listOf(0u, Int.MAX_VALUE.toUInt(), UInt.MAX_VALUE),
            steps = listOf(2, -2, Int.MAX_VALUE, -Int.MAX_VALUE),
            buildProgression = UIntProgression.Companion::fromClosedRange,
            elementToLong = UInt::toLong,
            getLast = UIntProgression::last,
        )
        progressionClampedSize(
            mins = listOf(0u),
            belowZeros = listOf(0u),
            aboveZeros = listOf(Int.MAX_VALUE.toUInt() + 1u),
            maxes = listOf(UInt.MAX_VALUE),
            steps = listOf(1, -1),
            buildProgression = UIntProgression.Companion::fromClosedRange,
        )
    }

    @Test
    fun ulongProgressionIsCollection() {
        progressionElements(
            bounds = ULong.MAX_VALUE - 10u..ULong.MAX_VALUE,
            steps = -3L..3L,
            buildRange = ULong::rangeTo,
            buildProgression = ULongProgression.Companion::fromClosedRange,
            difference = { a, b -> (a - b).toLong() }
        )
        progressionModerateSize(
            bounds = listOf(0uL, Int.MAX_VALUE.toULong(), UInt.MAX_VALUE.toULong()),
            steps = listOf(2L, -2L, Int.MAX_VALUE.toLong(), -Int.MAX_VALUE.toLong()),
            buildProgression = ULongProgression.Companion::fromClosedRange,
            elementToLong = ULong::toLong,
            getLast = ULongProgression::last,
        )
        progressionClampedSize(
            mins = listOf(0uL),
            belowZeros = listOf(0uL),
            aboveZeros = listOf(Long.MAX_VALUE.toULong() + 1u),
            maxes = listOf(ULong.MAX_VALUE),
            steps = listOf(1L, -1L, 10L, -10L),
            buildProgression = ULongProgression.Companion::fromClosedRange,
        )
    }

}