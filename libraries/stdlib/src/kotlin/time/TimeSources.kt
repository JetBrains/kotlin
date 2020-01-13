/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

@SinceKotlin("1.3")
@ExperimentalTime
internal expect object MonotonicTimeSource : TimeSource

/**
 * An abstract class used to implement time sources that return their readings as [Long] values in the specified [unit].
 *
 * @property unit The unit in which this time source's readings are expressed.
 */
@SinceKotlin("1.3")
@ExperimentalTime
public abstract class AbstractLongTimeSource(protected val unit: DurationUnit) : TimeSource {
    /**
     * This protected method should be overridden to return the current reading of the time source expressed as a [Long] number
     * in the unit specified by the [unit] property.
     */
    protected abstract fun read(): Long

    private class LongTimeMark(private val startedAt: Long, private val timeSource: AbstractLongTimeSource, private val offset: Duration) : TimeMark() {
        override fun elapsedNow(): Duration = (timeSource.read() - startedAt).toDuration(timeSource.unit) - offset
        override fun plus(duration: Duration): TimeMark = LongTimeMark(startedAt, timeSource, offset + duration)
    }

    override fun markNow(): TimeMark = LongTimeMark(read(), this, Duration.ZERO)
}

/**
 * An abstract class used to implement time sources that return their readings as [Double] values in the specified [unit].
 *
 * @property unit The unit in which this time source's readings are expressed.
 */
@SinceKotlin("1.3")
@ExperimentalTime
public abstract class AbstractDoubleTimeSource(protected val unit: DurationUnit) : TimeSource {
    /**
     * This protected method should be overridden to return the current reading of the time source expressed as a [Double] number
     * in the unit specified by the [unit] property.
     */
    protected abstract fun read(): Double

    private class DoubleTimeMark(private val startedAt: Double, private val timeSource: AbstractDoubleTimeSource, private val offset: Duration) : TimeMark() {
        override fun elapsedNow(): Duration = (timeSource.read() - startedAt).toDuration(timeSource.unit) - offset
        override fun plus(duration: Duration): TimeMark = DoubleTimeMark(startedAt, timeSource, offset + duration)
    }

    override fun markNow(): TimeMark = DoubleTimeMark(read(), this, Duration.ZERO)
}

/**
 * A time source that has programmatically updatable readings. It is useful as a predictable source of time in tests.
 *
 * The current reading value can be advanced by the specified duration amount with the operator [plusAssign]:
 *
 * ```
 * val timeSource = TestTimeSource()
 * timeSource += 10.seconds
 * ```
 *
 * Implementation note: the current reading value is stored as a [Long] number of nanoseconds,
 * thus it's capable to represent a time range of approximately ±292 years.
 * Should the reading value overflow as the result of [plusAssign] operation, an [IllegalStateException] is thrown.
 */
@SinceKotlin("1.3")
@ExperimentalTime
public class TestTimeSource : AbstractLongTimeSource(unit = DurationUnit.NANOSECONDS) {
    private var reading: Long = 0L

    override fun read(): Long = reading

    /**
     * Advances the current reading value of this time source by the specified [duration].
     *
     * [duration] value is rounded down towards zero when converting it to a [Long] number of nanoseconds.
     * For example, if the duration being added is `0.6.nanoseconds`, the reading doesn't advance because
     * the duration value is rounded to zero nanoseconds.
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
        throw IllegalStateException("TestTimeSource will overflow if its reading ${reading}ns is advanced by $duration.")
    }
}

@SinceKotlin("1.3")
@ExperimentalTime
@Deprecated("Use TimeSource.Monotonic instead.", ReplaceWith("TimeSource.Monotonic", "kotlin.time.TimeSource"))
public typealias MonoClock = TimeSource.Monotonic

@SinceKotlin("1.3")
@ExperimentalTime
@Deprecated("Use AbstractLongTimeSource instead.", ReplaceWith("AbstractLongTimeSource", "kotlin.time.AbstractLongTimeSource"))
public typealias AbstractLongClock = AbstractLongTimeSource

@SinceKotlin("1.3")
@ExperimentalTime
@Deprecated("Use AbstractDoubleTimeSource instead.", ReplaceWith("AbstractDoubleTimeSource", "kotlin.time.AbstractDoubleTimeSource"))
public typealias AbstractDoubleClock = AbstractDoubleTimeSource

@SinceKotlin("1.3")
@ExperimentalTime
@Deprecated("Use TestTimeSource instead.", ReplaceWith("TestTimeSource", "kotlin.time.TestTimeSource"))
public typealias TestClock = TestTimeSource
