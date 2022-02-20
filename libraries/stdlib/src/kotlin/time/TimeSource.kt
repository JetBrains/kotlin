/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
@SinceKotlin("1.3")
@ExperimentalTime
public interface TimeSource {
    /**
     * Marks a point in time on this time source.
     *
     * The returned [TimeMark] instance encapsulates the captured time point and allows querying
     * the duration of time interval [elapsed][TimeMark.elapsedNow] from that point.
     */
    public fun markNow(): TimeMark

    /**
     * The most precise time source available in the platform.
     *
     * This time source returns its readings from a source of monotonic time when it is available in a target platform,
     * and resorts to a non-monotonic time source otherwise.
     */
    public object Monotonic : TimeSource {
        override fun markNow(): DefaultTimeMark = MonotonicTimeSource.markNow()
        override fun toString(): String = MonotonicTimeSource.toString()
    }


    public companion object {

    }
}

/**
 * Represents a time point notched on a particular [TimeSource]. Remains bound to the time source it was taken from
 * and allows querying for the duration of time elapsed from that point (see the function [elapsedNow]).
 */
@SinceKotlin("1.3")
@ExperimentalTime
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
    public open operator fun plus(duration: Duration): TimeMark = AdjustedTimeMark(this, duration)

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
 * A specialized [TimeMark] returned by [TimeSource.Monotonic].
 *
 * This time mark is implemented as an inline value class wrapping a platform-dependent
 * time reading value of the default monotonic time source, thus allowing to avoid additional boxing
 * of that value.
 *
 * The operations [plus] and [minus] are also specialized to return [DefaultTimeMark] type.
 */
@ExperimentalTime
@SinceKotlin("1.7")
@JvmInline
public value class DefaultTimeMark internal constructor(internal val reading: DefaultTimeMarkReading) : TimeMark {
    override fun elapsedNow(): Duration = MonotonicTimeSource.elapsedFrom(this)
    override fun plus(duration: Duration): DefaultTimeMark = MonotonicTimeSource.adjustReading(this, duration)
    override fun minus(duration: Duration): DefaultTimeMark = MonotonicTimeSource.adjustReading(this, -duration)
    override fun hasPassedNow(): Boolean = !elapsedNow().isNegative()
    override fun hasNotPassedNow(): Boolean = elapsedNow().isNegative()
}

internal expect class DefaultTimeMarkReading


@ExperimentalTime
@SinceKotlin("1.3")
@kotlin.internal.InlineOnly
@Deprecated(
    "Subtracting one TimeMark from another is not a well defined operation because these time marks could have been obtained from the different time sources.",
    level = DeprecationLevel.ERROR
)
@Suppress("UNUSED_PARAMETER")
public inline operator fun TimeMark.minus(other: TimeMark): Duration = throw Error("Operation is disallowed.")

@ExperimentalTime
@SinceKotlin("1.3")
@kotlin.internal.InlineOnly
@Deprecated(
    "Comparing one TimeMark to another is not a well defined operation because these time marks could have been obtained from the different time sources.",
    level = DeprecationLevel.ERROR
)
@Suppress("UNUSED_PARAMETER")
public inline operator fun TimeMark.compareTo(other: TimeMark): Int = throw Error("Operation is disallowed.")


@ExperimentalTime
private class AdjustedTimeMark(val mark: TimeMark, val adjustment: Duration) : TimeMark {
    override fun elapsedNow(): Duration = mark.elapsedNow() - adjustment

    override fun plus(duration: Duration): TimeMark = AdjustedTimeMark(mark, adjustment + duration)
}
