/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import kotlin.math.truncate
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource.Monotonic.ValueTimeMark

@Suppress("ACTUAL_WITHOUT_EXPECT") // visibility
internal actual typealias ValueTimeMarkReading = Any

@ExperimentalTime
internal interface DefaultTimeSource : TimeSource.WithComparableMarks {
    override fun markNow(): ValueTimeMark
    fun elapsedFrom(timeMark: ValueTimeMark): Duration
    fun differenceBetween(one: ValueTimeMark, another: ValueTimeMark): Duration
    fun adjustReading(timeMark: ValueTimeMark, duration: Duration): ValueTimeMark
}

@SinceKotlin("1.3")
@ExperimentalTime
internal actual object MonotonicTimeSource : DefaultTimeSource, TimeSource.WithComparableMarks {  // TODO: interface should not be required here

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
    actual override fun differenceBetween(one: ValueTimeMark, another: ValueTimeMark): Duration = actualSource.differenceBetween(one, another)

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

    @Suppress("UNCHECKED_CAST")
    override fun differenceBetween(one: ValueTimeMark, another: ValueTimeMark): Duration {
        val (s1, n1) = one.reading as Array<Double>
        val (s2, n2) = another.reading as Array<Double>
        return (if (s1 == s2 && n1 == n2) Duration.ZERO else (s1 - s2).toDuration(DurationUnit.SECONDS)) + (n1 - n2).toDuration(DurationUnit.NANOSECONDS)
    }

    override fun adjustReading(timeMark: ValueTimeMark, duration: Duration): ValueTimeMark =
        @Suppress("UNCHECKED_CAST")
        (timeMark.reading as Array<Double>).let { (seconds, nanos) ->
            duration.toComponents { _, addNanos ->
                val resultSeconds = sumCheckNaN(seconds + truncate(duration.toDouble(DurationUnit.SECONDS)))
                arrayOf<Double>(resultSeconds, if (resultSeconds.isFinite()) nanos + addNanos else 0.0)
            }
        }.let(TimeSource.Monotonic::ValueTimeMark)


    override fun toString(): String = "TimeSource(process.hrtime())"
}

@SinceKotlin("1.3")
@ExperimentalTime
internal class PerformanceTimeSource(private val performance: Performance) :
    DefaultTimeSource { // AbstractDoubleTimeSource(unit = DurationUnit.MILLISECONDS) {
    private fun read(): Double = performance.now()

    override fun markNow(): ValueTimeMark = ValueTimeMark(read())
    override fun elapsedFrom(timeMark: ValueTimeMark): Duration = (read() - timeMark.reading as Double).milliseconds

    override fun differenceBetween(one: ValueTimeMark, another: ValueTimeMark): Duration {
        val ms1 = one.reading as Double
        val ms2 = another.reading as Double
        return if (ms1 == ms2) Duration.ZERO else (ms1 - ms2).milliseconds
    }

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

    override fun differenceBetween(one: ValueTimeMark, another: ValueTimeMark): Duration {
        val ms1 = one.reading as Double
        val ms2 = another.reading as Double
        return if (ms1 == ms2) Duration.ZERO else (ms1 - ms2).milliseconds
    }

    override fun adjustReading(timeMark: ValueTimeMark, duration: Duration): ValueTimeMark =
        ValueTimeMark(sumCheckNaN(timeMark.reading as Double + duration.toDouble(DurationUnit.MILLISECONDS)))

    override fun toString(): String = "TimeSource(Date.now())"
}

internal external interface GlobalPerformance {
    val performance: Performance
}

internal external interface Performance {
    fun now(): Double
}

private fun sumCheckNaN(value: Double): Double = value.also { if (it.isNaN()) throw IllegalArgumentException("Summing infinities of different signs") }