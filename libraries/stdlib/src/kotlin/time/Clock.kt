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
public interface Clock {
    /**
     * Marks a time point on this clock.
     */
    fun mark(): ClockMark
}

/**
 * Represents a time point notched on a particular [Clock]. Remains bound to the clock it was taken from
 * and allows querying for the duration of time elapsed from that point (see the function [elapsed]).
 */
public interface ClockMark {
    /**
     * Returns the amount of time passed from this clock mark on the clock from which this mark was taken.
     */
    fun elapsed(): Duration

    /**
     * Returns a clock mark on the same clock that is ahead of this clock mark by the specified [duration].
     *
     * The returned clock mark is more _late_ when the [duration] is positive, and more _early_ when the [duration] is negative.
     */
    operator fun plus(duration: Duration): ClockMark = AdjustedClockMark(this, duration)

    /**
     * Returns a clock mark on the same clock that is behind this clock mark by the specified [duration].
     *
     * The returned clock mark is more _early_ when the [duration] is positive, and more _late_ when the [duration] is negative.
     */
    operator fun minus(duration: Duration): ClockMark = plus(-duration)
}


private class AdjustedClockMark(val mark: ClockMark, val adjustment: Duration) : ClockMark {
    override fun elapsed(): Duration = mark.elapsed() - adjustment

    override fun plus(duration: Duration): ClockMark = AdjustedClockMark(mark, adjustment + duration)
}