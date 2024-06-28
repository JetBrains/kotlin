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

internal interface DefaultTimeSource : TimeSource.WithComparableMarks {
    override fun markNow(): ValueTimeMark
    fun elapsedFrom(timeMark: ValueTimeMark): Duration
    fun differenceBetween(one: ValueTimeMark, another: ValueTimeMark): Duration
    fun adjustReading(timeMark: ValueTimeMark, duration: Duration): ValueTimeMark
}

@SinceKotlin("1.3")
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
internal class HrTimeSource(private val process: Process) : DefaultTimeSource {
    @Suppress("NOTHING_TO_INLINE")
    private class Reading(val components: Array<Double>) {
        inline operator fun component1(): Double = components.component1()
        inline operator fun component2(): Double = components.component2()
        override fun equals(other: Any?): Boolean = other is Reading && this.components contentEquals other.components
        override fun hashCode(): Int = components.contentHashCode()
        override fun toString(): String = components.contentToString()
    }

    override fun markNow(): ValueTimeMark =
        ValueTimeMark(Reading(process.hrtime()))

    override fun elapsedFrom(timeMark: ValueTimeMark): Duration =
        process.hrtime((timeMark.reading as Reading).components)
            .let { (seconds, nanos) -> seconds.toDuration(DurationUnit.SECONDS) + nanos.toDuration(DurationUnit.NANOSECONDS) }

    override fun differenceBetween(one: ValueTimeMark, another: ValueTimeMark): Duration {
        val (s1, n1) = one.reading as Reading
        val (s2, n2) = another.reading as Reading
        return (if (s1 == s2 && n1 == n2) Duration.ZERO else (s1 - s2).toDuration(DurationUnit.SECONDS)) + (n1 - n2).toDuration(DurationUnit.NANOSECONDS)
    }

    override fun adjustReading(timeMark: ValueTimeMark, duration: Duration): ValueTimeMark =
        (timeMark.reading as Reading).let { (seconds, nanos) ->
            duration.toComponents { _, addNanos ->
                val resultSeconds = sumCheckNaN(seconds + truncate(duration.toDouble(DurationUnit.SECONDS)))
                Reading(arrayOf(resultSeconds, if (resultSeconds.isFinite()) nanos + addNanos else 0.0))
            }
        }.let(TimeSource.Monotonic::ValueTimeMark)


    override fun toString(): String = "TimeSource(process.hrtime())"
}

@SinceKotlin("1.3")
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