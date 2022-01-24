/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import kotlin.time.Duration.Companion.nanoseconds

@SinceKotlin("1.3")
@ExperimentalTime
internal actual object MonotonicTimeSource : TimeSource {
    private fun read(): Long = System.nanoTime()
    override fun toString(): String = "TimeSource(System.nanoTime())"

    actual override fun markNow(): DefaultTimeMark = DefaultTimeMark(read())
    actual fun elapsedFrom(timeMark: DefaultTimeMark): Duration = (read() - timeMark.reading).nanoseconds

    // may have questionable contract
    actual fun adjustReading(timeMark: DefaultTimeMark, duration: Duration): DefaultTimeMark =
        DefaultTimeMark(timeMark.reading + duration.inWholeNanoseconds)
}


@Suppress("ACTUAL_WITHOUT_EXPECT") // visibility
internal actual typealias DefaultTimeMarkReading = Long


