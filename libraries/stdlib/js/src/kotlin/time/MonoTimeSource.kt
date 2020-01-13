/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import org.w3c.performance.GlobalPerformance
import org.w3c.performance.Performance

@SinceKotlin("1.3")
@ExperimentalTime
internal actual object MonotonicTimeSource : TimeSource {

    private val actualSource: TimeSource = run {
        val isNode: Boolean = js("typeof process !== 'undefined' && process.versions && !!process.versions.node")

        if (isNode)
            HrTimeSource(js("process").unsafeCast<Process>())
        else
            js("self").unsafeCast<GlobalPerformance?>()?.performance?.let(::PerformanceTimeSource)
                ?: DateNowTimeSource

    }

    override fun markNow(): TimeMark = actualSource.markNow()
}

internal external interface Process {
    fun hrtime(time: Array<Double> = definedExternally): Array<Double>
}

@SinceKotlin("1.3")
@ExperimentalTime
internal class HrTimeSource(val process: Process) : TimeSource {

    override fun markNow(): TimeMark = object : TimeMark() {
        val startedAt = process.hrtime()
        override fun elapsedNow(): Duration =
            process.hrtime(startedAt).let { (seconds, nanos) -> seconds.seconds + nanos.nanoseconds }
    }

    override fun toString(): String = "TimeSource(process.hrtime())"
}

@SinceKotlin("1.3")
@ExperimentalTime
internal class PerformanceTimeSource(val performance: Performance) : AbstractDoubleTimeSource(unit = DurationUnit.MILLISECONDS) {
    override fun read(): Double = performance.now()
    override fun toString(): String = "TimeSource(self.performance.now())"
}

@SinceKotlin("1.3")
@ExperimentalTime
internal object DateNowTimeSource : AbstractDoubleTimeSource(unit = DurationUnit.MILLISECONDS) {
    override fun read(): Double = kotlin.js.Date.now()
    override fun toString(): String = "TimeSource(Date.now())"
}