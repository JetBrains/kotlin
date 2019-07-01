/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

/**
 * A source of time for measuring time intervals.
 *
 * The only operation provided by the clock is [mark]. It returns a [ClockMark], which can be used to query the elapsed time later.
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
     * the duration of time interval [elapsed][ClockMark.elapsed] from that point.
     */
    public fun mark(): ClockMark
}

/**
 * Represents a time point notched on a particular [Clock]. Remains bound to the clock it was taken from
 * and allows querying for the duration of time elapsed from that point (see the function [elapsed]).
 */
@SinceKotlin("1.3")
@ExperimentalTime
public abstract class ClockMark {
    /**
     * Returns the amount of time passed from this clock mark on the clock from which this mark was taken.
     */
    public abstract fun elapsed(): Duration

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
    override fun elapsed(): Duration = mark.elapsed() - adjustment

    override fun plus(duration: Duration): ClockMark = AdjustedClockMark(mark, adjustment + duration)
}