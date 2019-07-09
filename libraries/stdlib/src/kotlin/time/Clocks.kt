/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import kotlin.js.JsName

/**
 * The most precise clock available in the platform.
 *
 * The clock returns its readings from a source of monotonic time when it is available in a target platform,
 * and resorts to a non-monotonic time source otherwise.
 */
@SinceKotlin("1.3")
@ExperimentalTime
public expect object MonoClock : Clock

/**
 * An abstract class used to implement clocks that return their readings as [Long] values in the specified [unit].
 *
 * @property unit The unit in which this clock readings are expressed.
 */
@SinceKotlin("1.3")
@ExperimentalTime
public abstract class AbstractLongClock(protected val unit: DurationUnit) : Clock {
    /**
     * This protected method should be overridden to return the current reading of the clock expressed as a [Long] number
     * in the unit specified by the [unit] property.
     */
    protected abstract fun read(): Long

    private class LongClockMark(val startedAt: Long, val clock: AbstractLongClock, val offset: Duration) : ClockMark() {
        override fun elapsed(): Duration = (clock.read() - startedAt).toDuration(clock.unit) - offset
        override fun plus(duration: Duration): ClockMark = LongClockMark(startedAt, clock, offset + duration)
    }

    override fun mark(): ClockMark = LongClockMark(read(), this, Duration.ZERO)
}

/**
 * An abstract class used to implement clocks that return their readings as [Double] values in the specified [unit].
 *
 * @property unit The unit in which this clock readings are expressed.
 */
@SinceKotlin("1.3")
@ExperimentalTime
public abstract class AbstractDoubleClock(protected val unit: DurationUnit) : Clock {
    /**
     * This protected method should be overridden to return the current reading of the clock expressed as a [Double] number
     * in the unit specified by the [unit] property.
     */
    protected abstract fun read(): Double

    private class DoubleClockMark(val startedAt: Double, val clock: AbstractDoubleClock, val offset: Duration) : ClockMark() {
        override fun elapsed(): Duration = (clock.read() - startedAt).toDuration(clock.unit) - offset
        override fun plus(duration: Duration): ClockMark = DoubleClockMark(startedAt, clock, offset + duration)
    }

    override fun mark(): ClockMark = DoubleClockMark(read(), this, Duration.ZERO)
}

/**
 * A clock that has programmatically updatable readings. It is useful as a predictable source of time in tests.
 *
 * @param reading The initial value of the clock reading.
 * @param unit The unit of time in which [reading] value is expressed.
 *
 * @property reading Gets or sets this clock's current reading value.
 */
@SinceKotlin("1.3")
@ExperimentalTime
public class TestClock(
    @JsName("readingValue")
    public var reading: Long = 0L,
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