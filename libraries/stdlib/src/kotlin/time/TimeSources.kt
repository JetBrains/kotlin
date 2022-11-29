/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

@SinceKotlin("1.3")
@ExperimentalTime
internal expect object MonotonicTimeSource : TimeSource.WithComparableMarks {
    override fun markNow(): TimeSource.Monotonic.ValueTimeMark
    fun elapsedFrom(timeMark: TimeSource.Monotonic.ValueTimeMark): Duration
    fun differenceBetween(one: TimeSource.Monotonic.ValueTimeMark, another: TimeSource.Monotonic.ValueTimeMark): Duration
    fun adjustReading(timeMark: TimeSource.Monotonic.ValueTimeMark, duration: Duration): TimeSource.Monotonic.ValueTimeMark
}

/**
 * An abstract class used to implement time sources that return their readings as [Long] values in the specified [unit].
 *
 * @property unit The unit in which this time source's readings are expressed.
 */
@SinceKotlin("1.3")
@ExperimentalTime
public abstract class AbstractLongTimeSource(protected val unit: DurationUnit) : TimeSource.WithComparableMarks {
    /**
     * This protected method should be overridden to return the current reading of the time source expressed as a [Long] number
     * in the unit specified by the [unit] property.
     */
    protected abstract fun read(): Long

    private class LongTimeMark(private val startedAt: Long, private val timeSource: AbstractLongTimeSource, private val offset: Duration) : ComparableTimeMark {
        override fun elapsedNow(): Duration = if (offset.isInfinite()) -offset else (timeSource.read() - startedAt).toDuration(timeSource.unit) - offset
        override fun plus(duration: Duration): ComparableTimeMark = LongTimeMark(startedAt, timeSource, offset + duration)
        override fun minus(other: ComparableTimeMark): Duration {
            if (other !is LongTimeMark || this.timeSource != other.timeSource)
                throw IllegalArgumentException("Subtracting or comparing time marks from different time sources is not possible: $this and $other")

//            val thisValue = this.effectiveDuration()
//            val otherValue = other.effectiveDuration()
//            if (thisValue == otherValue && thisValue.isInfinite()) return Duration.ZERO
//            return thisValue - otherValue
            if (this.offset == other.offset && this.offset.isInfinite()) return Duration.ZERO
            val offsetDiff = this.offset - other.offset
            val startedAtDiff = (this.startedAt - other.startedAt).toDuration(timeSource.unit)
//            println("$startedAtDiff, $offsetDiff")
            return if (startedAtDiff == -offsetDiff) Duration.ZERO else startedAtDiff + offsetDiff
        }

        override fun equals(other: Any?): Boolean =
            other is LongTimeMark && this.timeSource == other.timeSource && (this - other) == Duration.ZERO

        internal fun effectiveDuration(): Duration {
            if (offset.isInfinite()) return offset
            val unit = timeSource.unit
            if (unit >= DurationUnit.MILLISECONDS) {
                return startedAt.toDuration(unit) + offset
            }
            val scale = convertDurationUnit(1L, DurationUnit.MILLISECONDS, unit)
            val startedAtMillis = startedAt / scale
            val startedAtRem = startedAt % scale

            return offset.toComponents { offsetSeconds, offsetNanoseconds ->
                val offsetMillis = offsetNanoseconds / NANOS_IN_MILLIS
                val offsetRemNanos = offsetNanoseconds % NANOS_IN_MILLIS

                // add component-wise
                (startedAtRem.toDuration(unit) + offsetRemNanos.nanoseconds) +
                        (startedAtMillis + offsetMillis).milliseconds +
                        offsetSeconds.seconds
            }

        }

        override fun hashCode(): Int = effectiveDuration().hashCode()

        override fun toString(): String = "LongTimeMark($startedAt${timeSource.unit.shortName()} + $offset (=${effectiveDuration()}), $timeSource)"
    }

    override fun markNow(): ComparableTimeMark = LongTimeMark(read(), this, Duration.ZERO)
}

/**
 * An abstract class used to implement time sources that return their readings as [Double] values in the specified [unit].
 *
 * @property unit The unit in which this time source's readings are expressed.
 */
@SinceKotlin("1.3")
@ExperimentalTime
@Deprecated("Using AbstractDoubleTimeSource is no longer recommended, use AbstractLongTimeSource instead.")
public abstract class AbstractDoubleTimeSource(protected val unit: DurationUnit) : TimeSource.WithComparableMarks {
    /**
     * This protected method should be overridden to return the current reading of the time source expressed as a [Double] number
     * in the unit specified by the [unit] property.
     */
    protected abstract fun read(): Double

    @Suppress("DEPRECATION")
    private class DoubleTimeMark(private val startedAt: Double, private val timeSource: AbstractDoubleTimeSource, private val offset: Duration) : ComparableTimeMark {
        override fun elapsedNow(): Duration = (timeSource.read() - startedAt).toDuration(timeSource.unit) - offset
        override fun plus(duration: Duration): ComparableTimeMark = DoubleTimeMark(startedAt, timeSource, offset + duration)

        override fun minus(other: ComparableTimeMark): Duration {
            if (other !is DoubleTimeMark || this.timeSource != other.timeSource)
                throw IllegalArgumentException("Subtracting or comparing time marks from different time sources is not possible: $this and $other")

            if (this.offset == other.offset && this.offset.isInfinite()) return Duration.ZERO
            val offsetDiff = this.offset - other.offset
            val startedAtDiff = (this.startedAt - other.startedAt).toDuration(timeSource.unit)
            return if (startedAtDiff == -offsetDiff) Duration.ZERO else startedAtDiff + offsetDiff
        }

        override fun equals(other: Any?): Boolean {
            return other is DoubleTimeMark && this.timeSource == other.timeSource && (this - other) == Duration.ZERO
        }

        override fun hashCode(): Int {
            return (startedAt.toDuration(timeSource.unit) + offset).hashCode()
        }

        override fun toString(): String = "DoubleTimeMark($startedAt${timeSource.unit.shortName()} + $offset, $timeSource)"
    }

    override fun markNow(): ComparableTimeMark = DoubleTimeMark(read(), this, Duration.ZERO)
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
        val longDelta = duration.toLong(unit)
        reading = if (longDelta != Long.MIN_VALUE && longDelta != Long.MAX_VALUE) {
            // when delta fits in long, add it as long
            val newReading = reading + longDelta
            if (reading xor longDelta >= 0 && reading xor newReading < 0) overflow(duration)
            newReading
        } else {
            val delta = duration.toDouble(unit)
            // when delta is greater than long, add it as double
            val newReading = reading + delta
            if (newReading > Long.MAX_VALUE || newReading < Long.MIN_VALUE) overflow(duration)
            newReading.toLong()
        }
    }

    private fun overflow(duration: Duration) {
        throw IllegalStateException("TestTimeSource will overflow if its reading ${reading}${unit.shortName()} is advanced by $duration.")
    }
}
