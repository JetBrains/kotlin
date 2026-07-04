/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.time

import samples.*
import kotlin.time.*

class TimeSources {
    @Sample
    fun timeMarkAt() {
        // A time source based on an external millisecond counter, such as a system uptime counter.
        var uptimeMillis = 100_000L
        class UptimeSource : AbstractLongTimeSource(DurationUnit.MILLISECONDS) {
            override fun read(): Long = uptimeMillis
            // The author of a time source can re-expose the protected timeMarkAt function
            // to create time marks from raw readings recorded outside of this time source.
            fun timeMarkOfReading(reading: Long): ComparableTimeMark = timeMarkAt(reading)
        }
        val timeSource = UptimeSource()

        // A raw reading of the same counter, recorded earlier by another subsystem.
        val storedReading = 98_500L
        val eventMark = timeSource.timeMarkOfReading(storedReading)
        val startMark = timeSource.markNow()

        uptimeMillis += 1_000

        assertPrints(eventMark.elapsedNow(), "2.5s")
        assertPrints(startMark - eventMark, "1.5s")
    }
}
