/*
 * KT-76762 EXPERIMENT (this branch only, not intended for master):
 * observed behavior of a SATURATING `reading - zero` adjustment in AbstractLongTimeSource.timeMarkAt,
 * compared against the plain (wrapping) adjustment shipped on the main branch.
 *
 * Comparison table (zero pinned first via markNow() at the stated reading):
 *
 *   zero | reading        | plain (main branch)                       | saturating (this branch)
 *   -----+----------------+-------------------------------------------+--------------------------------
 *   -1   | Long.MAX_VALUE | wraps to MIN sentinel -> infinite PAST    | clamps to MAX -> infinite FUTURE
 *   -2   | Long.MAX_VALUE | wraps to MIN+1 -> far past (finite in ns) | clamps to MAX -> infinite FUTURE
 *    1   | Long.MIN_VALUE | wraps to MAX sentinel -> infinite FUTURE  | clamps to MIN -> infinite PAST
 *    0   | Long.MAX_VALUE | MAX sentinel -> infinite FUTURE           | identical (no overflow)
 *
 * Decisive difference: markNow() still wraps (adjustedRead() is plain), so under saturation
 * timeMarkAt(r) and a markNow() taken when read() == r disagree by an infinity — the clamp breaks
 * the core equivalence invariant exactly in the cases it was meant to improve.
 */
package test.time

import kotlin.test.*
import kotlin.time.*

class TimeMarkAtSaturationExperimentTest {
    private class LongTimeSource(unit: DurationUnit) : AbstractLongTimeSource(unit) {
        var reading: Long = 0L
        override fun read(): Long = reading
        fun mark(reading: Long): ComparableTimeMark = timeMarkAt(reading)
    }

    @Test
    fun saturatedClampBehavior() {
        // zero = -1, reading = Long.MAX_VALUE: clamped to the MAX sentinel => infinite FUTURE
        // (plain variant, measured on the main branch: wraps to the MIN sentinel => infinite PAST)
        run {
            val ts = LongTimeSource(DurationUnit.MILLISECONDS).apply { reading = -1; markNow() }
            assertEquals(-Duration.INFINITE, ts.mark(Long.MAX_VALUE).elapsedNow())
        }
        // zero = -2, reading = Long.MAX_VALUE: clamped to the MAX sentinel => infinite FUTURE
        // (plain variant: wraps to MIN + 1 => far PAST, huge finite in ns / INFINITE in ms)
        run {
            val ts = LongTimeSource(DurationUnit.NANOSECONDS).apply { reading = -2; markNow() }
            assertEquals(-Duration.INFINITE, ts.mark(Long.MAX_VALUE).elapsedNow())
        }
        // zero = 1, reading = Long.MIN_VALUE: clamped to the MIN sentinel => infinite PAST
        // (plain variant: wraps to the MAX sentinel => infinite FUTURE)
        run {
            val ts = LongTimeSource(DurationUnit.MILLISECONDS).apply { reading = 1; markNow() }
            assertEquals(Duration.INFINITE, ts.mark(Long.MIN_VALUE).elapsedNow())
        }
        // In-contract extremes are unaffected by the clamp: zero = 0, reading = Long.MAX_VALUE
        // yields the MAX sentinel under both variants.
        run {
            val ts = LongTimeSource(DurationUnit.MILLISECONDS).apply { markNow() }
            assertEquals(-Duration.INFINITE, ts.mark(Long.MAX_VALUE).elapsedNow())
        }
    }

    @Test
    fun saturationBreaksMarkNowEquivalence() {
        // markNow() still wraps, so for the same out-of-contract reading the two construction
        // paths disagree by an infinity. On the main branch (plain adjustment) the same pair
        // is EQUAL: both wrap to the MIN sentinel (see the plain-variant probe in the notebook).
        val ts = LongTimeSource(DurationUnit.MILLISECONDS).apply { reading = -1; markNow() }
        ts.reading = Long.MAX_VALUE
        val constructed = ts.mark(Long.MAX_VALUE)   // clamped: MAX sentinel (infinite future)
        val measured = ts.markNow()                 // wrapped: MIN sentinel (infinite past)
        assertEquals(Duration.INFINITE, constructed - measured)
        assertNotEquals(constructed, measured)
    }
}
