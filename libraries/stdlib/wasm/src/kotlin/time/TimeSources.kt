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
internal actual object MonotonicTimeSource : TimeSource, AbstractDoubleTimeSource(unit = DurationUnit.MILLISECONDS) {
    private val performance: ExternalInterfaceType? = tryGetPerformance()

    override fun read(): Double =
        if (performance != null) getPerformanceNow(performance) else dateNow()

    override fun toString(): String =
        if (performance != null) "TimeSource(globalThis.performance.now())" else "TimeSource(Date.now())"
}

@SinceKotlin("1.3")
@ExperimentalTime
internal actual object MonotonicTimeSource : TimeSource {
    actual override fun markNow(): DefaultTimeMark = TODO("Wasm stdlib: MonotonicTimeSource::markNow")
    actual fun elapsedFrom(timeMark: DefaultTimeMark): Duration = TODO("Wasm stdlib: MonotonicTimeSource")
    actual fun adjustReading(timeMark: DefaultTimeMark, duration: Duration): DefaultTimeMark = TODO("Wasm stdlib: MonotonicTimeSource")
}

internal actual class DefaultTimeMarkReading // TODO: Long?