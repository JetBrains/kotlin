/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import kotlin.wasm.internal.ExternalInterfaceType

@JsFun("() => typeof globalThis !== 'undefined' && typeof globalThis.performance !== 'undefined' ? globalThis.performance : null")
private external fun tryGetPerformance(): ExternalInterfaceType?

@JsFun("(performance) => performance.now()")
private external fun getPerformanceNow(performance: ExternalInterfaceType): Double

@JsFun("() => Date.now()")
private external fun dateNow(): Double

@SinceKotlin("1.3")
@ExperimentalTime
internal actual object MonotonicTimeSource : TimeSource {
    private val performance: ExternalInterfaceType? = tryGetPerformance()

    private fun read(): Double =
        if (performance != null) getPerformanceNow(performance) else dateNow()

    actual override fun markNow(): DefaultTimeMark = DefaultTimeMark(read())
    actual fun elapsedFrom(timeMark: DefaultTimeMark): Duration = (read() - timeMark.reading as Double).milliseconds
    actual fun adjustReading(timeMark: DefaultTimeMark, duration: Duration): DefaultTimeMark =
        DefaultTimeMark(sumCheckNaN(timeMark.reading as Double + duration.toDouble(DurationUnit.MILLISECONDS)))

    override fun toString(): String =
        if (performance != null) "TimeSource(globalThis.performance.now())" else "TimeSource(Date.now())"
}

@Suppress("ACTUAL_WITHOUT_EXPECT") // visibility
internal actual typealias DefaultTimeMarkReading = Double

private fun sumCheckNaN(value: Double): Double = value.also { if (it.isNaN()) throw IllegalArgumentException("Summing infinities of different signs") }