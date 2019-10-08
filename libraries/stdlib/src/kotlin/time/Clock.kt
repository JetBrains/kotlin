/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

/**
 * A source of time for measuring time intervals.
 *
 * The only operation provided by the clock is [markNow]. It returns a [ClockMark], which can be used to query the elapsed time later.
 *
 * @see [measureTime]
 * @see [measureTimedValue]
 */
@SinceKotlin("1.3")
@ExperimentalTime
public interface Clock {
    /**
     * Marks a time point on this clock.
     *
     * The returned [ClockMark] instance encapsulates captured time point and allows querying
     * the duration of time interval [elapsed][ClockMark.elapsedNow] from that point.
     */
    public fun markNow(): ClockMark
}

/**
 * Represents a time point notched on a particular [Clock]. Remains bound to the clock it was taken from
 * and allows querying for the duration of time elapsed from that point (see the function [elapsedNow]).
 */
@SinceKotlin("1.3")
@ExperimentalTime
public abstract class ClockMark {
    /**
     * Returns the amount of time passed from this clock mark on the clock from which this mark was taken.
     *
     * Note that the value returned by this function can change on subsequent invocations.
     */
    public abstract fun elapsedNow(): Duration

    /**
     * Returns a clock mark on the same clock that is ahead of this clock mark by the specified [duration].
     *
     * The returned clock mark is more _late_ when the [duration] is positive, and more _early_ when the [duration] is negative.
     */
    public open operator fun plus(duration: Duration): ClockMark = AdjustedClockMark(this, duration)

    /**
     * Returns a clock mark on the same clock that is behind this clock mark by the specified [duration].
     *
     * The returned clock mark is more _early_ when the [duration] is positive, and more _late_ when the [duration] is negative.
     */
    public open operator fun minus(duration: Duration): ClockMark = plus(-duration)


    /**
     * Returns true if this clock mark has passed according to the clock from which this mark was taken.
     *
     * Note that the value returned by this function can change on subsequent invocations.
     * If the clock is monotonic, it can change only from `false` to `true`, namely, when the clock mark becomes behind the current point of the clock.
     */
    public fun hasPassedNow(): Boolean = !elapsedNow().isNegative()

    /**
     * Returns false if this clock mark has not passed according to the clock from which this mark was taken.
     *
     * Note that the value returned by this function can change on subsequent invocations.
     * If the clock is monotonic, it can change only from `true` to `false`, namely, when the clock mark becomes behind the current point of the clock.
     */
    public fun hasNotPassedNow(): Boolean = elapsedNow().isNegative()
}


@ExperimentalTime
@SinceKotlin("1.3")
@kotlin.internal.InlineOnly
@Deprecated("Subtracting one ClockMark from another is not a well defined operation because these clock marks could have been obtained from the different clocks.", level = DeprecationLevel.ERROR)
public inline operator fun ClockMark.minus(other: ClockMark): Duration = throw Error("Operation is disallowed.")

@ExperimentalTime
@SinceKotlin("1.3")
@kotlin.internal.InlineOnly
@Deprecated("Comparing one ClockMark to another is not a well defined operation because these clock marks could have been obtained from the different clocks.", level = DeprecationLevel.ERROR)
public inline operator fun ClockMark.compareTo(other: ClockMark): Int = throw Error("Operation is disallowed.")


@ExperimentalTime
private class AdjustedClockMark(val mark: ClockMark, val adjustment: Duration) : ClockMark() {
    override fun elapsedNow(): Duration = mark.elapsedNow() - adjustment

    override fun plus(duration: Duration): ClockMark = AdjustedClockMark(mark, adjustment + duration)
}