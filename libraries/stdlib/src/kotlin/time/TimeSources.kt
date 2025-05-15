/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import kotlin.math.sign

@SinceKotlin("1.3")
internal expect object MonotonicTimeSource : TimeSource.WithComparableMarks {
    override fun markNow(): TimeSource.Monotonic.ValueTimeMark
    fun elapsedFrom(timeMark: TimeSource.Monotonic.ValueTimeMark): Duration
    fun differenceBetween(one: TimeSource.Monotonic.ValueTimeMark, another: TimeSource.Monotonic.ValueTimeMark): Duration
    fun adjustReading(timeMark: TimeSource.Monotonic.ValueTimeMark, duration: Duration): TimeSource.Monotonic.ValueTimeMark
}

/**
 * An abstract class used to implement time sources that return their readings as [Long] values in the specified [unit].
 *
 * Time marks returned by this time source can be compared for difference with other time marks
 * obtained from the same time source.
 *
 * @property unit The unit in which this time source's readings are expressed.
 */
@SinceKotlin("1.9")
@WasExperimental(ExperimentalTime::class)
public abstract class AbstractLongTimeSource(protected val unit: DurationUnit) : TimeSource.WithComparableMarks {
    /**
     * This protected method should be overridden to return the current reading of the time source expressed as a [Long] number
     * in the unit specified by the [unit] property.
     *
     * Note that the value returned by this method when [markNow] is called the first time is used as "zero" reading
     * and the difference from this "zero" reading is calculated for subsequent values.
     * Therefore, it's not recommended to return values farther than `±Long.MAX_VALUE` from the first returned reading
     * as this will cause this time source flip over future/past boundary for the returned time marks.
     */
    protected abstract fun read(): Long

    private val zero by lazy { read() }
    private fun adjustedRead(): Long = read() - zero

    private class LongTimeMark(private val startedAt: Long, private val timeSource: AbstractLongTimeSource, private val offset: Duration) : ComparableTimeMark {
        override fun elapsedNow(): Duration =
            saturatingOriginsDiff(timeSource.adjustedRead(), startedAt, timeSource.unit) - offset

        override fun plus(duration: Duration): ComparableTimeMark {
            val unit = timeSource.unit
            if (duration.isInfinite()) {
                val newValue = saturatingAdd(startedAt, unit, duration)
                return LongTimeMark(newValue, timeSource, Duration.ZERO)
            }
            val durationInUnit = duration.truncateTo(unit)
            val rest = (duration - durationInUnit) + offset
            var sum = saturatingAdd(startedAt, unit, durationInUnit)
            val restInUnit = rest.truncateTo(unit)
            sum = saturatingAdd(sum, unit, restInUnit)
            var restUnderUnit = rest - restInUnit
            val restUnderUnitNs = restUnderUnit.inWholeNanoseconds
            if (sum != 0L && restUnderUnitNs != 0L && (sum xor restUnderUnitNs) < 0L) {
                // normalize offset to be the same sign as new startedAt
                val correction = restUnderUnitNs.sign.toDuration(unit)
                sum = saturatingAdd(sum, unit, correction)
                restUnderUnit -= correction
            }
            val newValue = sum
            val newOffset = if (newValue.isSaturated()) Duration.ZERO else restUnderUnit
            return LongTimeMark(newValue, timeSource, newOffset)
        }

        override fun minus(other: ComparableTimeMark): Duration {
            if (other !is LongTimeMark || this.timeSource != other.timeSource)
                throw IllegalArgumentException("Subtracting or comparing time marks from different time sources is not possible: $this and $other")

            val startedAtDiff = saturatingOriginsDiff(this.startedAt, other.startedAt, timeSource.unit)
            return startedAtDiff + (offset - other.offset)
        }

        override fun equals(other: Any?): Boolean =
            other is LongTimeMark && this.timeSource == other.timeSource && (this - other) == Duration.ZERO

        override fun hashCode(): Int = offset.hashCode() * 37 + startedAt.hashCode()

        override fun toString(): String = "LongTimeMark($startedAt${timeSource.unit.shortName()} + $offset, $timeSource)"
    }

    override fun markNow(): ComparableTimeMark = LongTimeMark(adjustedRead(), this, Duration.ZERO)
}

/**
 * An abstract class used to implement time sources that return their readings as [Double] values in the specified [unit].
 *
 * @property unit The unit in which this time source's readings are expressed.
 */
@SinceKotlin("1.3")
@ExperimentalTime
@Deprecated(
    "Using AbstractDoubleTimeSource is no longer recommended, use AbstractLongTimeSource instead.",
    level = DeprecationLevel.ERROR
)
public abstract class AbstractDoubleTimeSource(protected val unit: DurationUnit) : TimeSource.WithComparableMarks {
    /**
     * This protected method should be overridden to return the current reading of the time source expressed as a [Double] number
     * in the unit specified by the [unit] property.
     */
    protected abstract fun read(): Double


    private class DoubleTimeMark(
        private val startedAt: Double,
        @Suppress("DEPRECATION_ERROR")
        private val timeSource: AbstractDoubleTimeSource,
        private val offset: Duration
    ) : ComparableTimeMark {
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
 * Time marks returned by this time source can be compared for difference with other time marks
 * obtained from the same time source.
 *
 * Implementation note: the current reading value is stored as a [Long] number of nanoseconds,
 * thus it's capable to represent a time range of approximately ±292 years.
 * Should the reading value overflow as the result of [plusAssign] operation, an [IllegalStateException] is thrown.
 *
 * @sample samples.time.MeasureTime.explicitMeasureTimeSample
 * @sample samples.time.MeasureTime.explicitMeasureTimedValueSample
 */
@SinceKotlin("1.9")
@WasExperimental(ExperimentalTime::class)
public class TestTimeSource : AbstractLongTimeSource(unit = DurationUnit.NANOSECONDS) {
    private var reading: Long = 0L

    init {
        val _ = markNow() // fix zero reading in the super time source
    }

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
        if (!longDelta.isSaturated()) {
            // when delta fits in long, add it as long
            val newReading = reading + longDelta
            if (reading xor longDelta >= 0 && reading xor newReading < 0) overflow(duration)
            reading = newReading
        } else {
            val half = duration / 2
            if (!half.toLong(unit).isSaturated()) {
                val readingBefore = reading
                try {
                    plusAssign(half)
                    plusAssign(duration - half)
                } catch (e: IllegalStateException) {
                    reading = readingBefore
                    throw e
                }
            } else {
                overflow(duration)
            }
        }
    }

    private fun overflow(duration: Duration) {
        throw IllegalStateException("TestTimeSource will overflow if its reading ${reading}${unit.shortName()} is advanced by $duration.")
    }
}
