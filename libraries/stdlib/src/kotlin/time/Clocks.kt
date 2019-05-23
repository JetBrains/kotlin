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

    override fun mark(): ClockMark = object : ClockMark {
        val startedAt = read()
        override fun elapsed(): Duration = (read() - startedAt).toDuration(this@AbstractLongClock.unit)
    }
}

/**
 * An abstract class used to implement clocks that return their readings as [Double] values in the specified [unit].
 */
public abstract class AbstractDoubleClock(protected val unit: DurationUnit) : Clock {
    protected abstract fun read(): Double

    override fun mark(): ClockMark = object : ClockMark {
        val startedAt = read()
        override fun elapsed(): Duration = (read() - startedAt).toDuration(this@AbstractDoubleClock.unit)
    }
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