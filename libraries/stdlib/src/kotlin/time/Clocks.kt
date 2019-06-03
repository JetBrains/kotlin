/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import kotlin.js.JsName

/**
 * The most precise clock available in the platform, whose readings increase monotonically over time.
 */
public expect object MonoClock : Clock

/**
 * An abstract class used to implement clocks that return their readings as [Long] values in the specified [unit].
 */
public abstract class AbstractLongClock(protected val unit: DurationUnit) : Clock {
    protected abstract fun read(): Long

    private class LongClockMark(val startedAt: Long, val clock: AbstractLongClock, val offset: Duration) : ClockMark() {
        override fun elapsed(): Duration = (clock.read() - startedAt).toDuration(clock.unit) - offset
        override fun plus(duration: Duration): ClockMark = LongClockMark(startedAt, clock, offset + duration)
    }

    override fun mark(): ClockMark = LongClockMark(read(), this, Duration.ZERO)
}

/**
 * An abstract class used to implement clocks that return their readings as [Double] values in the specified [unit].
 */
public abstract class AbstractDoubleClock(protected val unit: DurationUnit) : Clock {
    protected abstract fun read(): Double

    private class DoubleClockMark(val startedAt: Double, val clock: AbstractDoubleClock, val offset: Duration) : ClockMark() {
        override fun elapsed(): Duration = (clock.read() - startedAt).toDuration(clock.unit) - offset
        override fun plus(duration: Duration): ClockMark = DoubleClockMark(startedAt, clock, offset + duration)
    }

    override fun mark(): ClockMark = DoubleClockMark(read(), this, Duration.ZERO)
}

/**
 * A clock, whose readings can be preset and changed manually. It is useful as a predictable source of time in tests.
 */
public class TestClock(
    @JsName("readingValue")
    var reading: Long = 0L,
    unit: DurationUnit = DurationUnit.NANOSECONDS
) : AbstractLongClock(unit) {
    override fun read(): Long = reading
}

/*
public interface WallClock {
    fun currentTimeMilliseconds(): Long

    companion object : WallClock, AbstractLongClock(unit = DurationUnit.MILLISECONDS) {
        override fun currentTimeMilliseconds(): Long = System.currentTimeMillis()
        override fun read(): Long = System.currentTimeMillis()
        override fun toString(): String = "WallClock(System.currentTimeMillis())"
    }
}
*/