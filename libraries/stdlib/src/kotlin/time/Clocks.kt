/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

/**
 * The most precise clock available in the platform.
 *
 * The clock returns its readings from a source of monotonic time when it is available in a target platform,
 * and resorts to a non-monotonic time source otherwise.
 */
@SinceKotlin("1.3")
@ExperimentalTime
public expect object MonoClock : Clock

/**
 * An abstract class used to implement clocks that return their readings as [Long] values in the specified [unit].
 *
 * @property unit The unit in which this clock readings are expressed.
 */
@SinceKotlin("1.3")
@ExperimentalTime
public abstract class AbstractLongClock(protected val unit: DurationUnit) : Clock {
    /**
     * This protected method should be overridden to return the current reading of the clock expressed as a [Long] number
     * in the unit specified by the [unit] property.
     */
    protected abstract fun read(): Long

    private class LongClockMark(private val startedAt: Long, private val clock: AbstractLongClock, private val offset: Duration) : ClockMark() {
        override fun elapsedNow(): Duration = (clock.read() - startedAt).toDuration(clock.unit) - offset
        override fun plus(duration: Duration): ClockMark = LongClockMark(startedAt, clock, offset + duration)
    }

    override fun markNow(): ClockMark = LongClockMark(read(), this, Duration.ZERO)
}

/**
 * An abstract class used to implement clocks that return their readings as [Double] values in the specified [unit].
 *
 * @property unit The unit in which this clock readings are expressed.
 */
@SinceKotlin("1.3")
@ExperimentalTime
public abstract class AbstractDoubleClock(protected val unit: DurationUnit) : Clock {
    /**
     * This protected method should be overridden to return the current reading of the clock expressed as a [Double] number
     * in the unit specified by the [unit] property.
     */
    protected abstract fun read(): Double

    private class DoubleClockMark(private val startedAt: Double, private val clock: AbstractDoubleClock, private val offset: Duration) : ClockMark() {
        override fun elapsedNow(): Duration = (clock.read() - startedAt).toDuration(clock.unit) - offset
        override fun plus(duration: Duration): ClockMark = DoubleClockMark(startedAt, clock, offset + duration)
    }

    override fun markNow(): ClockMark = DoubleClockMark(read(), this, Duration.ZERO)
}

/**
 * A clock that has programmatically updatable readings. It is useful as a predictable source of time in tests.
 *
 * The current clock reading value can be advanced by the specified duration amount with the operator [plusAssign]:
 *
 * ```
 * val clock = TestClock()
 * clock += 10.seconds
 * ```
 *
 * Implementation note: the current clock reading value is stored as a [Long] number of nanoseconds,
 * thus it's capable to represent a time range of approximately ±292 years.
 * Should the reading value overflow as the result of [plusAssign] operation, an [IllegalStateException] is thrown.
 */
@SinceKotlin("1.3")
@ExperimentalTime
public class TestClock : AbstractLongClock(unit = DurationUnit.NANOSECONDS) {
    private var reading: Long = 0L

    override fun read(): Long = reading

    /**
     * Advances the current reading value of this clock by the specified [duration].
     *
     * [duration] value is rounded down towards zero when converting it to a [Long] number of nanoseconds.
     * For example, if the duration being added is `0.6.nanoseconds`, the clock reading won't advance because
     * the duration value will be rounded to zero nanoseconds.
     *
     * @throws IllegalStateException when the reading value overflows as the result of this operation.
     */
    public operator fun plusAssign(duration: Duration) {
        val delta = duration.toDouble(unit)
        val longDelta = delta.toLong()
        reading = if (longDelta != Long.MIN_VALUE && longDelta != Long.MAX_VALUE) {
            // when delta fits in long, add it as long
            val newReading = reading + longDelta
            if (reading xor longDelta >= 0 && reading xor newReading < 0) overflow(duration)
            newReading
        } else {
            // when delta is greater than long, add it as double
            val newReading = reading + delta
            if (newReading > Long.MAX_VALUE || newReading < Long.MIN_VALUE) overflow(duration)
            newReading.toLong()
        }
    }

    private fun overflow(duration: Duration) {
        throw IllegalStateException("TestClock will overflow if its reading ${reading}ns is advanced by $duration.")
    }
}

/*
public interface WallClock {
    fun currentTimeMilliseconds(): Long

    companion object : WallClock, AbstractLongClock(unit = DurationUnit.MILLISECONDS) {
        override fun currentTimeMilliseconds(): Long = System.currentTimeMillis()
        override fun read(): Long = System.currentTimeMillis()
        override fun toString(): String = "WallClock(System.currentTimeMillis())"
    }
}
*/