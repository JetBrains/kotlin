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
}

@ExperimentalTime
private class AdjustedClockMark(val mark: ClockMark, val adjustment: Duration) : ClockMark() {
    override fun elapsedNow(): Duration = mark.elapsedNow() - adjustment

    override fun plus(duration: Duration): ClockMark = AdjustedClockMark(mark, adjustment + duration)
}