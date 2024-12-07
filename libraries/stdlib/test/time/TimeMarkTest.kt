/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.time

import test.TestPlatform
import test.current
import kotlin.math.sign
import kotlin.test.*
import kotlin.time.*
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

class TimeMarkTest {
    private val units = DurationUnit.entries

    private fun TimeMark.assertHasPassed(hasPassed: Boolean) {
        assertEquals(!hasPassed, this.hasNotPassedNow(), "Expected mark in the future")
        assertEquals(hasPassed, this.hasPassedNow(), "Expected mark in the past")

        assertEquals(
            !hasPassed,
            this.elapsedNow() < Duration.ZERO,
            "Mark elapsed: ${this.elapsedNow()}, expected hasPassed: $hasPassed"
        )
    }

    fun testAdjustment(timeSource: TimeSource.WithComparableMarks) {
        val mark = timeSource.markNow()
        for (unit in units) {
            val markFuture1 = (mark + 1.toDuration(unit)).apply { assertHasPassed(false) }
            val markFuture2 = (mark - (-1).toDuration(unit)).apply { assertHasPassed(false) }
            assertDifferentMarks(markFuture1, mark, 1)
            assertDifferentMarks(markFuture2, mark, 1)

            val markPast1 = (mark - 1.toDuration(unit)).apply { assertHasPassed(true) }
            val markPast2 = (markFuture1 + (-2).toDuration(unit)).apply { assertHasPassed(true) }
            assertDifferentMarks(markPast1, mark, -1)
            assertDifferentMarks(markPast2, mark, -1)

            if (unit > DurationUnit.NANOSECONDS) {
                val d = 1.toDuration(unit)
                val h = d / 2
                val markH1 = mark + h
                val markH2 = mark + d - h
                assertEqualMarks(markH1, markH2)
            }
        }
    }

    @Test
    fun adjustment() {
        testAdjustment(TestTimeSource())
        for (unit in units) {
            testAdjustment(LongTimeSource(unit))
        }
    }

    @Test
    fun adjustmentTestTimeSource() {
        val timeSource = TestTimeSource()
        val mark = timeSource.markNow()
        val markFuture1 = mark + 1.milliseconds
        val markPast1 = mark - 1.milliseconds

        timeSource += 500_000.nanoseconds

        val markElapsed = timeSource.markNow()
        val elapsedDiff = markElapsed - mark

        val elapsed = mark.elapsedNow()
        val elapsedFromFuture = elapsed - 1.milliseconds
        val elapsedFromPast = elapsed + 1.milliseconds

        assertEquals(0.5.milliseconds, elapsed)
        assertEquals(elapsedFromFuture, markFuture1.elapsedNow())
        assertEquals(elapsedDiff, elapsed)

        val markToElapsed = mark + elapsedDiff
        assertEqualMarks(markElapsed, markToElapsed)

        assertEquals(elapsedFromPast, markPast1.elapsedNow())

        markFuture1.assertHasPassed(false)
        markPast1.assertHasPassed(true)

        timeSource += 1.milliseconds

        markFuture1.assertHasPassed(true)
        markPast1.assertHasPassed(true)
    }

    fun testAdjustmentBig(timeSource: TimeSource.WithComparableMarks) {
        val baseMark = timeSource.markNow()
        val longDuration = Long.MAX_VALUE.nanoseconds
        val long2Duration = longDuration + 1001.milliseconds

        val pastMark = baseMark - longDuration
        val futureMark = pastMark + long2Duration
        val sameMark = futureMark - (long2Duration - longDuration)

        val elapsedMark = timeSource.markNow()
        run {
            val iterations = 1..100
            for (i in iterations) {
                val elapsedDiff1 = (sameMark.elapsedNow() - baseMark.elapsedNow()).absoluteValue
                val elapsedDiff2 = (baseMark.elapsedNow() - sameMark.elapsedNow()).absoluteValue
                // some iterations of this assertion can fail due to an unpredictable delay between subsequent elapsedNow calls
                // but if the mark adjustment arithmetic was wrong, all of them will fail
                if (maxOf(elapsedDiff1, elapsedDiff2) < 1.milliseconds) break
                if (i == iterations.last) fail("$elapsedDiff1, $elapsedDiff2")
            }
        }
        // may not pass exactly for double-based value time marks in JS/WASM due to rounding
//        assertEquals(elapsedMark - baseMark, elapsedMark - sameMark, "$elapsedMark; $baseMark; $sameMark")
        val elapsedBaseDiff = elapsedMark - baseMark
        val elapsedSameDiff = elapsedMark - sameMark
        assertTrue((elapsedBaseDiff - elapsedSameDiff).absoluteValue < 1.milliseconds, "elapsedMark=$elapsedMark; baseMark=$baseMark; sameMark=$sameMark")
    }

    @Test
    fun adjustmentBig() {
        testAdjustmentBig(TestTimeSource())
        for (unit in units) {
            testAdjustmentBig(LongTimeSource(unit))
        }
    }

    fun testAdjustmentInfinite(timeSource: TimeSource.WithComparableMarks) {
        val baseMark = timeSource.markNow()
        val infiniteFutureMark = baseMark + Duration.INFINITE
        val infinitePastMark = baseMark - Duration.INFINITE

        assertDifferentMarks(infinitePastMark, baseMark, -1)
        assertDifferentMarks(infiniteFutureMark, baseMark, 1)
        assertDifferentMarks(infinitePastMark, infiniteFutureMark, -1)

        assertEquals(Duration.INFINITE, infiniteFutureMark - infinitePastMark)
        assertEquals(Duration.INFINITE, infiniteFutureMark - baseMark)
        assertEquals(-Duration.INFINITE, infinitePastMark - baseMark)
        assertEqualMarks(infiniteFutureMark, infiniteFutureMark)
        assertEqualMarks(infinitePastMark, infinitePastMark)

        assertEquals(-Duration.INFINITE, infiniteFutureMark.elapsedNow())
        assertTrue(infiniteFutureMark.hasNotPassedNow())

        assertEquals(Duration.INFINITE, infinitePastMark.elapsedNow())
        assertTrue(infinitePastMark.hasPassedNow())

        assertFailsWith<IllegalArgumentException> { infiniteFutureMark - Duration.INFINITE }
        assertFailsWith<IllegalArgumentException> { infinitePastMark + Duration.INFINITE }

        for (infiniteMark in listOf(infiniteFutureMark, infinitePastMark)) {
            for (offset in listOf(Duration.ZERO, 1.nanoseconds, 10.microseconds, 1.milliseconds, 15.seconds)) {
                assertEqualMarks(infiniteMark, infiniteMark + offset)
                assertEqualMarks(infiniteMark, infiniteMark - offset)
            }
        }
    }

    @Test
    fun adjustmentInfinite() {
        testAdjustmentInfinite(TestTimeSource())
        for (unit in units) {
            testAdjustmentInfinite(LongTimeSource(unit))
        }
    }

    fun testLongAdjustmentElapsedPrecision(timeSource: TimeSource.WithComparableMarks, wait: (Duration) -> Unit) {
        val baseMark = timeSource.markNow()
        val longDuration = Long.MAX_VALUE.nanoseconds
        val waitDuration = 20.milliseconds
        val pastMark = baseMark - longDuration
        wait(waitDuration)
        val elapsedMark = timeSource.markNow()
        val elapsed = pastMark.elapsedNow()
        val elapsedDiff = elapsedMark - pastMark
        assertTrue(elapsed > longDuration)
        assertTrue(elapsed >= longDuration + waitDuration, "$elapsed, $longDuration, $waitDuration")
        assertTrue(elapsedDiff >= longDuration + waitDuration)
        // 'elapsed' was measured later than time marks from 'elapsedDiff'
        assertTrue(elapsed >= elapsedDiff)
    }

    @Test
    fun longDisplacement() {
        val timeSource = TestTimeSource()
        testLongAdjustmentElapsedPrecision(timeSource, { waitDuration -> timeSource += waitDuration })
    }

    private fun assertEqualMarks(mark1: ComparableTimeMark, mark2: ComparableTimeMark) {
        assertEquals(Duration.ZERO, mark1 - mark2)
        assertEquals(Duration.ZERO, mark2 - mark1)
        assertEquals(0, mark1 compareTo mark2)
        assertEquals(0, mark2 compareTo mark1)
        assertEquals(mark1, mark2)
        assertEquals(mark1.hashCode(), mark2.hashCode(), "hashCodes of: $mark1, $mark2")
    }

    private fun assertDifferentMarks(mark1: ComparableTimeMark, mark2: ComparableTimeMark, expectedCompare: Int) {
        assertNotEquals(Duration.ZERO, mark1 - mark2)
        assertNotEquals(Duration.ZERO, mark2 - mark1)
        assertEquals(expectedCompare, (mark1 compareTo mark2).sign)
        assertEquals(-expectedCompare, (mark2 compareTo mark1).sign)
        assertNotEquals(mark1, mark2)
        // can't say anything about hash codes for non-equal marks
        // assertNotEquals(mark1.hashCode(), mark2.hashCode(), "hashCodes of: $mark1, $mark2")
    }

    @Test
    fun timeMarkDifferenceAndComparison() {
        val timeSource = TestTimeSource()
        val timeSource2 = TestTimeSource()
        val baseMark = timeSource.markNow()

        var markBefore = baseMark
        markBefore -= 100.microseconds
        markBefore -= 100.microseconds

        val markAfter = baseMark + 100.microseconds

        assertEquals(300.microseconds,markAfter - markBefore)
        assertTrue(markBefore < markAfter)
        assertFalse(markBefore > markAfter)
        assertEqualMarks(baseMark, baseMark)

        timeSource += 100.microseconds
        val markElapsed = timeSource.markNow()
        assertEqualMarks(markElapsed, markAfter)

        val differentSourceMark = TimeSource.Monotonic.markNow()
        assertFailsWith<IllegalArgumentException> { baseMark - differentSourceMark }
        assertFailsWith<IllegalArgumentException> { baseMark < differentSourceMark }

        val differentSourceMark2 = timeSource2.markNow()
        assertFailsWith<IllegalArgumentException> { baseMark - differentSourceMark2 }
        assertFailsWith<IllegalArgumentException> { baseMark < differentSourceMark2 }
    }

    private class LongTimeSource(unit: DurationUnit) : AbstractLongTimeSource(unit) {
        var reading: Long = 0L
        override fun read(): Long = reading
    }

    @OptIn(ExperimentalTime::class)
    @Suppress("DEPRECATION")
    private class DoubleTimeSource(unit: DurationUnit) : AbstractDoubleTimeSource(unit) {
        var reading: Double = 0.0
        override fun read(): Double = reading
    }


    @Test
    fun longTimeMarkInfinities() {
        for (unit in units) {
            val timeSource = LongTimeSource(unit).apply {
                markNow() // fix zero reading
                reading = Long.MIN_VALUE + 1
            }

            val mark1 = timeSource.markNow()
            timeSource.reading = 0
            val mark2 = timeSource.markNow() - Duration.INFINITE
            if (unit >= DurationUnit.MILLISECONDS) {
                assertEquals(Duration.INFINITE, mark1.elapsedNow())
            }
            assertEquals(Duration.INFINITE, mark2.elapsedNow())
            assertDifferentMarks(mark1, mark2, 1)

            val mark3 = mark1 + Duration.INFINITE
            assertEquals(-Duration.INFINITE, mark3.elapsedNow(), "infinite offset should override distant past reading")
            val mark4 = timeSource.markNow() + Duration.INFINITE
            assertEquals(-Duration.INFINITE, mark4.elapsedNow())
            assertEqualMarks(mark3, mark4) // different readings, same infinite offset
        }
    }

    @Test
    fun doubleTimeMarkInfiniteEqualHashCode() {
        val timeSource = DoubleTimeSource(unit = DurationUnit.MILLISECONDS).apply { reading = -Double.MAX_VALUE }

        val mark1 = timeSource.markNow()
        timeSource.reading = 0.0
        val mark2 = timeSource.markNow() - Duration.INFINITE
        assertEquals(Duration.INFINITE, mark1.elapsedNow())
        assertEquals(Duration.INFINITE, mark2.elapsedNow())
        assertEqualMarks(mark1, mark2)
    }

    @Test
    fun longTimeMarkRoundingEqualHashCode() {
        run {
            val step = Long.MAX_VALUE / 4
            val timeSource = LongTimeSource(DurationUnit.NANOSECONDS)
            val mark0 = timeSource.markNow() + step.nanoseconds + step.nanoseconds
            timeSource.reading += step
            val mark1 = timeSource.markNow() + step.nanoseconds
            timeSource.reading += step
            val mark2 = timeSource.markNow()
            assertEqualMarks(mark1, mark2)
            assertEqualMarks(mark0, mark2)
            assertEqualMarks(mark0, mark1)
        }

        for (unit in units) {
            val baseReading = Long.MAX_VALUE - 1000
            val timeSource = LongTimeSource(unit).apply { reading = baseReading }
            // large reading, small offset
            val baseMark = timeSource.markNow()
            for (delta in listOf((1..<500).random(), (500..<1000).random())) {
                val deltaDuration = delta.toDuration(unit)
                timeSource.reading = baseReading + delta
                val mark1e = timeSource.markNow()
                assertEquals(deltaDuration, mark1e - baseMark)
                val mark1d = baseMark + deltaDuration
                assertEqualMarks(mark1e, mark1d)

                val subUnit = units.getOrNull(units.indexOf(unit) - 1) ?: continue
                val deltaSubUnitDuration = delta.toDuration(subUnit)
                val mark1s = baseMark + deltaSubUnitDuration
                assertDifferentMarks(mark1s, baseMark, 1)
                assertEquals(deltaSubUnitDuration, mark1s - baseMark)
            }

            // compared saturated reading from time source and saturated time mark as a result of plus operation
            run {
                val delta = 1000
                val deltaDuration = delta.toDuration(unit)
                timeSource.reading = baseReading + 1000
                val mark2 = timeSource.markNow()
                assertEquals(deltaDuration, mark2 - baseMark)
                val offset = Long.MAX_VALUE.nanoseconds
                val mark2e = mark2 + offset
                val mark2d = baseMark + offset + deltaDuration
                assertEqualMarks(mark2e, mark2d)
            }
        }
    }



    @Test
    fun defaultTimeMarkAdjustment() {
        val baseMark = TimeSource.Monotonic.markNow()

        var markBefore = baseMark
        markBefore -= 100.microseconds
        markBefore -= 100.microseconds

        val markAfter = baseMark + 100.microseconds

        MeasureTimeTest.longRunningCalc()

        val elapsedMark = TimeSource.Monotonic.markNow()
        val elapsedDiff = elapsedMark - baseMark
        assertTrue(elapsedDiff > Duration.ZERO)

        val elapsedAfter = markAfter.elapsedNow()
        val elapsedBase = baseMark.elapsedNow()
        val elapsedBefore = markBefore.elapsedNow()
        assertTrue(elapsedBefore >= elapsedBase + 200.microseconds)
        assertTrue(elapsedAfter <= elapsedBase - 100.microseconds)
        assertTrue(elapsedBase >= elapsedDiff)
    }

    @Test
    fun defaultTimeMarkAdjustmentBig() {
        if (TestPlatform.current == TestPlatform.WasmWasi) return

        testAdjustmentBig(TimeSource.Monotonic)

        // do the same with specialized methods
        val baseMark = TimeSource.Monotonic.markNow()
        val longDuration = Long.MAX_VALUE.nanoseconds
        val long2Duration = longDuration + 1001.milliseconds

        val pastMark = baseMark - longDuration
        val futureMark = pastMark + long2Duration
        val sameMark = futureMark - (long2Duration - longDuration)

        run {
            val iterations = 1..100
            for (i in iterations) {
                val elapsedDiff1 = (sameMark.elapsedNow() - baseMark.elapsedNow()).absoluteValue
                val elapsedDiff2 = (baseMark.elapsedNow() - sameMark.elapsedNow()).absoluteValue
                // some iterations of this assertion can fail due to an unpredictable delay between subsequent elapsedNow calls
                // but if the mark adjustment arithmetic was wrong, all of them will fail
                if (maxOf(elapsedDiff1, elapsedDiff2) < 1.milliseconds) break
                if (i == iterations.last) fail("$elapsedDiff1, $elapsedDiff2")
            }
        }
        val elapsedMark = TimeSource.Monotonic.markNow()
        val elapsedBaseDiff = elapsedMark - baseMark
        val elapsedSameDiff = elapsedMark - sameMark
        assertTrue((elapsedBaseDiff - elapsedSameDiff).absoluteValue < 1.milliseconds, "elapsedMark=$elapsedMark; baseMark=$baseMark; sameMark=$sameMark")
    }

    @Test
    fun defaultTimeMarkAdjustmentInfinite() {
        if (TestPlatform.current == TestPlatform.WasmWasi) return

        testAdjustmentInfinite(TimeSource.Monotonic)

        // do the same with specialized methods
        val baseMark = TimeSource.Monotonic.markNow()
        val infiniteFutureMark = baseMark + Duration.INFINITE
        val infinitePastMark = baseMark - Duration.INFINITE

        assertEquals(-Duration.INFINITE, infiniteFutureMark.elapsedNow())
        assertTrue(infiniteFutureMark.hasNotPassedNow())

        assertEquals(Duration.INFINITE, infinitePastMark.elapsedNow())
        assertTrue(infinitePastMark.hasPassedNow())

        assertFailsWith<IllegalArgumentException> { infiniteFutureMark - Duration.INFINITE }
        assertFailsWith<IllegalArgumentException> { infinitePastMark + Duration.INFINITE }
    }

    @Test
    fun defaultTimeMarkDifferenceAndComparison() {
        val baseMark = TimeSource.Monotonic.markNow()

        var markBefore = baseMark
        markBefore -= 100.microseconds
        markBefore -= 100.microseconds

        val markAfter = baseMark + 100.microseconds

        assertEquals(300.microseconds,markAfter - markBefore)
        assertTrue(markBefore < markAfter)
        assertFalse(markBefore > markAfter)
        assertEquals(0,baseMark compareTo baseMark)
        assertEquals(baseMark as Any, baseMark as Any)
        assertEquals(baseMark.hashCode(), baseMark.hashCode())

        val differentSourceMark = TestTimeSource().markNow()
        assertFailsWith<IllegalArgumentException> { baseMark - differentSourceMark }
        assertFailsWith<IllegalArgumentException> { baseMark < differentSourceMark }
    }
}
