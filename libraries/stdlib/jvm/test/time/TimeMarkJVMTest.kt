/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.time

import kotlin.test.*
import kotlin.time.*
import kotlin.time.Duration.Companion.nanoseconds

class TimeMarkJVMTest {

    @Test
    fun longDurationElapsed() {
        TimeMarkTest().testLongAdjustmentElapsedPrecision(TimeSource.Monotonic, { waitDuration -> Thread.sleep((waitDuration * 1.1).inWholeMilliseconds) })
    }

    @Test
    fun defaultTimeMarkAdjustmentInfinite() {
        val baseMark = TimeSource.Monotonic.markNow()
        val longDuration = Long.MAX_VALUE.nanoseconds

        val pastMark = baseMark - longDuration
        val infiniteFutureMark1 = pastMark + longDuration * 3

        assertEquals(-Duration.INFINITE, infiniteFutureMark1.elapsedNow())
    }

}