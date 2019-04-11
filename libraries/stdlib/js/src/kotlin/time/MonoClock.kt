/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import org.w3c.performance.GlobalPerformance
import org.w3c.performance.Performance

public actual object MonoClock : Clock {

    private val actualClock: Clock = run {
        val isNode: Boolean = js("typeof process !== 'undefined' && process.versions && !!process.versions.node")

        if (isNode)
            HrTimeClock(js("process").unsafeCast<Process>())
        else
            js("self").unsafeCast<GlobalPerformance?>()?.performance?.let(::PerformanceClock)
                ?: DateNowClock

    }

    override fun mark(initialElapsed: Duration): ClockMark = actualClock.mark(initialElapsed)
}

internal external interface Process {
    fun hrtime(time: Array<Double> = definedExternally): Array<Double>
}

internal class HrTimeClock(val process: Process) : Clock {

    override fun mark(initialElapsed: Duration): ClockMark = object : ClockMark {
        val startedAt = process.hrtime()
        override val clock: Clock get() = this@HrTimeClock // delegation problem?
        override val elapsedFrom: Duration
            get() = process.hrtime(startedAt).let { (seconds, nanos) -> seconds.seconds + nanos.nanoseconds + initialElapsed }
    }

    override fun toString(): String = "Clock(process.hrtime())"
}

internal class PerformanceClock(val performance: Performance) : DoubleReadingClock() {
    override val unit: DurationUnit get() = DurationUnit.MILLISECONDS
    override fun reading(): Double = performance.now()
    override fun toString(): String = "Clock(self.performance.now())"
}

internal object DateNowClock : DoubleReadingClock() {
    override val unit: DurationUnit get() = DurationUnit.MILLISECONDS
    override fun reading(): Double = kotlin.js.Date.now()
    override fun toString(): String = "Clock(Date.now())"
}