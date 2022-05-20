/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import org.w3c.performance.GlobalPerformance
import org.w3c.performance.Performance
import kotlin.math.truncate
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource.Monotonic.ValueTimeMark

@Suppress("ACTUAL_WITHOUT_EXPECT") // visibility
internal actual typealias ValueTimeMarkReading = Any

@ExperimentalTime
internal interface DefaultTimeSource : TimeSource {
    override fun markNow(): ValueTimeMark
    fun elapsedFrom(timeMark: ValueTimeMark): Duration
    fun adjustReading(timeMark: ValueTimeMark, duration: Duration): ValueTimeMark
}

@SinceKotlin("1.3")
@ExperimentalTime
internal actual object MonotonicTimeSource : DefaultTimeSource, TimeSource {  // TODO: interface should not be required here

    private val actualSource: DefaultTimeSource = run {
        val isNode: Boolean = js("typeof process !== 'undefined' && process.versions && !!process.versions.node")

        if (isNode)
            HrTimeSource(js("process").unsafeCast<Process>())
        else
            js("typeof self !== 'undefined' ? self : globalThis")
                .unsafeCast<GlobalPerformance?>()
                ?.performance
                ?.let(::PerformanceTimeSource)
                ?: DateNowTimeSource
    }

    actual override fun markNow(): ValueTimeMark = actualSource.markNow()
    actual override fun elapsedFrom(timeMark: ValueTimeMark): Duration = actualSource.elapsedFrom(timeMark)
    actual override fun adjustReading(timeMark: ValueTimeMark, duration: Duration): ValueTimeMark =
        actualSource.adjustReading(timeMark, duration)
}

internal external interface Process {
    fun hrtime(time: Array<Double> = definedExternally): Array<Double>
}

@SinceKotlin("1.3")
@ExperimentalTime
internal class HrTimeSource(private val process: Process) : DefaultTimeSource {

    override fun markNow(): ValueTimeMark = ValueTimeMark(process.hrtime())
    override fun elapsedFrom(timeMark: ValueTimeMark): Duration =
        @Suppress("UNCHECKED_CAST")
        process.hrtime(timeMark.reading as Array<Double>)
            .let { (seconds, nanos) -> seconds.toDuration(DurationUnit.SECONDS) + nanos.toDuration(DurationUnit.NANOSECONDS) }

    override fun adjustReading(timeMark: ValueTimeMark, duration: Duration): ValueTimeMark =
        @Suppress("UNCHECKED_CAST")
        (timeMark.reading as Array<Double>).let { (seconds, nanos) ->
            duration.toComponents { _, addNanos ->
                arrayOf<Double>(sumCheckNaN(seconds + truncate(duration.toDouble(DurationUnit.SECONDS))), nanos + addNanos)
            }
        }.let(TimeSource.Monotonic::ValueTimeMark)


    override fun toString(): String = "TimeSource(process.hrtime())"
}

@SinceKotlin("1.3")
@ExperimentalTime
internal class PerformanceTimeSource(val performance: Performance) :
    DefaultTimeSource { // AbstractDoubleTimeSource(unit = DurationUnit.MILLISECONDS) {
    private fun read(): Double = performance.now()

    override fun markNow(): ValueTimeMark = ValueTimeMark(read())
    override fun elapsedFrom(timeMark: ValueTimeMark): Duration = (read() - timeMark.reading as Double).milliseconds
    override fun adjustReading(timeMark: ValueTimeMark, duration: Duration): ValueTimeMark =
        ValueTimeMark(sumCheckNaN(timeMark.reading as Double + duration.toDouble(DurationUnit.MILLISECONDS)))

    override fun toString(): String = "TimeSource(self.performance.now())"
}

@SinceKotlin("1.3")
@ExperimentalTime
internal object DateNowTimeSource : DefaultTimeSource {
    private fun read(): Double = kotlin.js.Date.now()

    override fun markNow(): ValueTimeMark = ValueTimeMark(read())
    override fun elapsedFrom(timeMark: ValueTimeMark): Duration = (read() - timeMark.reading as Double).milliseconds
    override fun adjustReading(timeMark: ValueTimeMark, duration: Duration): ValueTimeMark =
        ValueTimeMark(sumCheckNaN(timeMark.reading as Double + duration.toDouble(DurationUnit.MILLISECONDS)))

    override fun toString(): String = "TimeSource(Date.now())"
}

private fun sumCheckNaN(value: Double): Double = value.also { if (it.isNaN()) throw IllegalArgumentException("Summing infinities of different signs") }