/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

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
    public object Monotonic : TimeSource by MonotonicTimeSource {
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
public abstract class TimeMark {
    /**
     * Returns the amount of time passed from this mark measured with the time source from which this mark was taken.
     *
     * Note that the value returned by this function can change on subsequent invocations.
     */
    public abstract fun elapsedNow(): Duration

    /**
     * Returns a time mark on the same time source that is ahead of this time mark by the specified [duration].
     *
     * The returned time mark is more _late_ when the [duration] is positive, and more _early_ when the [duration] is negative.
     */
    public open operator fun plus(duration: Duration): TimeMark = AdjustedTimeMark(this, duration)

    /**
     * Returns a time mark on the same time source that is behind this time mark by the specified [duration].
     *
     * The returned time mark is more _early_ when the [duration] is positive, and more _late_ when the [duration] is negative.
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
private class AdjustedTimeMark(val mark: TimeMark, val adjustment: Duration) : TimeMark() {
    override fun elapsedNow(): Duration = mark.elapsedNow() - adjustment

    override fun plus(duration: Duration): TimeMark = AdjustedTimeMark(mark, adjustment + duration)
}


@SinceKotlin("1.3")
@ExperimentalTime
@Deprecated("Use TimeSource interface instead.", ReplaceWith("TimeSource", "kotlin.time.TimeSource"))
public typealias Clock = TimeSource

@SinceKotlin("1.3")
@ExperimentalTime
@Deprecated("Use TimeMark class instead.", ReplaceWith("TimeMark", "kotlin.time.TimeMark"))
public typealias ClockMark = TimeMark
