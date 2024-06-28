/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import kotlin.jvm.JvmInline

/**
 * A source of time for measuring time intervals.
 *
 * The only operation provided by the time source is [markNow]. It returns a [TimeMark], which can be used to query the elapsed time later.
 *
 * @see [measureTime]
 * @see [measureTimedValue]
 */
@SinceKotlin("1.9")
@WasExperimental(ExperimentalTime::class)
public interface TimeSource {
    /**
     * Marks a point in time on this time source.
     *
     * The returned [TimeMark] instance encapsulates the captured time point and allows querying
     * the duration of time interval [elapsed][TimeMark.elapsedNow] from that point.
     */
    public fun markNow(): TimeMark

    /**
     * A [TimeSource] that returns [time marks][ComparableTimeMark] that can be compared for difference with each other.
     */
    @SinceKotlin("1.9")
    @WasExperimental(ExperimentalTime::class)
    public interface WithComparableMarks : TimeSource {
        override fun markNow(): ComparableTimeMark
    }

    /**
     * The most precise time source available in the platform.
     *
     * This time source returns its readings from a source of monotonic time when it is available in a target platform,
     * and resorts to a non-monotonic time source otherwise.
     *
     * The function [markNow] of this time source returns the specialized [ValueTimeMark] that is an inline value class
     * wrapping a platform-dependent time reading value.
     */
    public object Monotonic : TimeSource.WithComparableMarks {
        override fun markNow(): ValueTimeMark = MonotonicTimeSource.markNow()
        override fun toString(): String = MonotonicTimeSource.toString()

        /**
         * A specialized [kotlin.time.TimeMark] returned by [TimeSource.Monotonic] time source.
         *
         * This time mark is implemented as an inline value class wrapping a platform-dependent
         * time reading value of the default monotonic time source, thus allowing to avoid additional boxing
         * of that value.
         *
         * The operations [plus] and [minus] are also specialized to return [ValueTimeMark] type.
         *
         * This time mark implements [ComparableTimeMark] and therefore is comparable with other time marks
         * obtained from the same [TimeSource.Monotonic] time source.
         */
        @SinceKotlin("1.9")
        @WasExperimental(ExperimentalTime::class)
        @JvmInline
        public value class ValueTimeMark internal constructor(internal val reading: ValueTimeMarkReading) : ComparableTimeMark {
            override fun elapsedNow(): Duration = MonotonicTimeSource.elapsedFrom(this)
            override fun plus(duration: Duration): ValueTimeMark = MonotonicTimeSource.adjustReading(this, duration)
            override fun minus(duration: Duration): ValueTimeMark = MonotonicTimeSource.adjustReading(this, -duration)
            override fun hasPassedNow(): Boolean = !elapsedNow().isNegative()
            override fun hasNotPassedNow(): Boolean = elapsedNow().isNegative()

            override fun minus(other: ComparableTimeMark): Duration {
                if (other !is ValueTimeMark)
                    throw IllegalArgumentException("Subtracting or comparing time marks from different time sources is not possible: $this and $other")
                return this.minus(other)
            }

            /**
             * Returns the duration elapsed between the [other] time mark obtained from the same [TimeSource.Monotonic] time source and `this` time mark.
             *
             * The returned duration can be infinite if the time marks are far away from each other and
             * the result doesn't fit into [Duration] type,
             * or if one time mark is infinitely distant, or if both `this` and [other] time marks
             * lie infinitely distant on the opposite sides of the time scale.
             *
             * Two infinitely distant time marks on the same side of the time scale are considered equal and
             * the duration between them is [Duration.ZERO].
             */
            public operator fun minus(other: ValueTimeMark): Duration = MonotonicTimeSource.differenceBetween(this, other)

            /**
             * Compares this time mark with the [other] time mark for order.
             *
             * - Returns zero if this time mark represents *the same moment* of time as the [other] time mark.
             * - Returns a negative number if this time mark is *earlier* than the [other] time mark.
             * - Returns a positive number if this time mark is *later* than the [other] time mark.
             */
            public operator fun compareTo(other: ValueTimeMark): Int =
                (this - other).compareTo(Duration.ZERO)
        }
    }

    public companion object {

    }
}

/** A platform-specific reading type that is wrapped by [TimeSource.Monotonic.ValueTimeMark] inline class. */
internal expect class ValueTimeMarkReading


/**
 * Represents a time point notched on a particular [TimeSource]. Remains bound to the time source it was taken from
 * and allows querying for the duration of time elapsed from that point (see the function [elapsedNow]).
 */
@SinceKotlin("1.9")
@WasExperimental(ExperimentalTime::class)
public interface TimeMark {
    /**
     * Returns the amount of time passed from this mark measured with the time source from which this mark was taken.
     *
     * Note that the value returned by this function can change on subsequent invocations.
     *
     * @throws IllegalArgumentException an implementation may throw if calculating the elapsed time involves
     * adding a positive infinite duration to an infinitely distant past time mark or
     * a negative infinite duration to an infinitely distant future time mark.
     */
    public abstract fun elapsedNow(): Duration

    /**
     * Returns a time mark on the same time source that is ahead of this time mark by the specified [duration].
     *
     * The returned time mark is more _late_ when the [duration] is positive, and more _early_ when the [duration] is negative.
     *
     * If the time mark is adjusted too far in the past or in the future, it may saturate to an infinitely distant time mark.
     * In that case, [elapsedNow] will return an infinite duration elapsed from such infinitely distant mark.
     *
     * @throws IllegalArgumentException an implementation may throw if a positive infinite duration is added to an infinitely distant past time mark or
     * a negative infinite duration is added to an infinitely distant future time mark.
     */
    public operator fun plus(duration: Duration): TimeMark = AdjustedTimeMark(this, duration)

    /**
     * Returns a time mark on the same time source that is behind this time mark by the specified [duration].
     *
     * The returned time mark is more _early_ when the [duration] is positive, and more _late_ when the [duration] is negative.
     *
     * If the time mark is adjusted too far in the past or in the future, it may saturate to an infinitely distant time mark.
     * In that case, [elapsedNow] will return an infinite duration elapsed from such infinitely distant mark.
     *
     * @throws IllegalArgumentException an implementation may throw if a positive infinite duration is subtracted from an infinitely distant future time mark or
     * a negative infinite duration is subtracted from an infinitely distant past time mark.
     */
    public open operator fun minus(duration: Duration): TimeMark = plus(-duration)


    /**
     * Returns true if this time mark has passed according to the time source from which this mark was taken.
     *
     * Note that the value returned by this function can change on subsequent invocations.
     * If the time source is monotonic, it can change only from `false` to `true`, namely, when the time mark becomes behind the current point of the time source.
     */
    public fun hasPassedNow(): Boolean = !elapsedNow().isNegative()

    /**
     * Returns false if this time mark has not passed according to the time source from which this mark was taken.
     *
     * Note that the value returned by this function can change on subsequent invocations.
     * If the time source is monotonic, it can change only from `true` to `false`, namely, when the time mark becomes behind the current point of the time source.
     */
    public fun hasNotPassedNow(): Boolean = elapsedNow().isNegative()
}

/**
 * A [TimeMark] that can be compared for difference with other time marks obtained from the same [TimeSource.WithComparableMarks] time source.
 */
@SinceKotlin("1.9")
@WasExperimental(ExperimentalTime::class)
public interface ComparableTimeMark : TimeMark, Comparable<ComparableTimeMark> {
    public abstract override operator fun plus(duration: Duration): ComparableTimeMark
    public open override operator fun minus(duration: Duration): ComparableTimeMark = plus(-duration)

    /**
     * Returns the duration elapsed between the [other] time mark and `this` time mark.
     *
     * The returned duration can be infinite if the time marks are far away from each other and
     * the result doesn't fit into [Duration] type,
     * or if one time mark is infinitely distant, or if both `this` and [other] time marks
     * lie infinitely distant on the opposite sides of the time scale.
     *
     * Two infinitely distant time marks on the same side of the time scale are considered equal and
     * the duration between them is [Duration.ZERO].
     *
     * Note that the other time mark must be obtained from the same time source as this one.
     *
     * @throws IllegalArgumentException if time marks were obtained from different time sources.
     */
    public operator fun minus(other: ComparableTimeMark): Duration

    /**
     * Compares this time mark with the [other] time mark for order.
     *
     * - Returns zero if this time mark represents *the same moment* of time as the [other] time mark.
     * - Returns a negative number if this time mark is *earlier* than the [other] time mark.
     * - Returns a positive number if this time mark is *later* than the [other] time mark.
     *
     * Note that the other time mark must be obtained from the same time source as this one.
     *
     * @throws IllegalArgumentException if time marks were obtained from different time sources.
     */
    public override operator fun compareTo(other: ComparableTimeMark): Int =
        (this - other).compareTo(Duration.ZERO)

    /**
     * Returns `true` if two time marks from the same time source represent the same moment of time, and `false` otherwise,
     * including the situation when the time marks were obtained from different time sources.
     */
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}


private class AdjustedTimeMark(val mark: TimeMark, val adjustment: Duration) : TimeMark {
    override fun elapsedNow(): Duration = mark.elapsedNow() - adjustment

    override fun plus(duration: Duration): TimeMark = AdjustedTimeMark(mark, adjustment + duration)
}
