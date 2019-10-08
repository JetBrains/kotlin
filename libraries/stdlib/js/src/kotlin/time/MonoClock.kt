/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import org.w3c.performance.GlobalPerformance
import org.w3c.performance.Performance

@SinceKotlin("1.3")
@ExperimentalTime
public actual object MonoClock : Clock {

    private val actualClock: Clock = run {
        val isNode: Boolean = js("typeof process !== 'undefined' && process.versions && !!process.versions.node")

        if (isNode)
            HrTimeClock(js("process").unsafeCast<Process>())
        else
            js("self").unsafeCast<GlobalPerformance?>()?.performance?.let(::PerformanceClock)
                ?: DateNowClock

    }

    override fun markNow(): ClockMark = actualClock.markNow()
}

internal external interface Process {
    fun hrtime(time: Array<Double> = definedExternally): Array<Double>
}

@SinceKotlin("1.3")
@ExperimentalTime
internal class HrTimeClock(val process: Process) : Clock {

    override fun markNow(): ClockMark = object : ClockMark() {
        val startedAt = process.hrtime()
        override fun elapsedNow(): Duration =
            process.hrtime(startedAt).let { (seconds, nanos) -> seconds.seconds + nanos.nanoseconds }
    }

    override fun toString(): String = "Clock(process.hrtime())"
}

@SinceKotlin("1.3")
@ExperimentalTime
internal class PerformanceClock(val performance: Performance) : AbstractDoubleClock(unit = DurationUnit.MILLISECONDS) {
    override fun read(): Double = performance.now()
    override fun toString(): String = "Clock(self.performance.now())"
}

@SinceKotlin("1.3")
@ExperimentalTime
internal object DateNowClock : AbstractDoubleClock(unit = DurationUnit.MILLISECONDS) {
    override fun read(): Double = kotlin.js.Date.now()
    override fun toString(): String = "Clock(Date.now())"
}