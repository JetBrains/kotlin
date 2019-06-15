/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:UseExperimental(ExperimentalTime::class)
package test.time

import kotlin.test.*
import kotlin.time.*

class ClockMarkTest {

    @Test
    fun adjustment() {
        val clock = TestClock(unit = DurationUnit.NANOSECONDS)

        val mark = clock.mark()
        val markFuture1 = mark + 1.milliseconds
        val markFuture2 = mark - (-1).milliseconds

        val markPast1 = mark - 1.milliseconds
        val markPast2 = markFuture1 + (-2).milliseconds

        clock.reading = 500_000L

        val elapsed = mark.elapsed()
        val elapsedFromFuture = elapsed - 1.milliseconds
        val elapsedFromPast = elapsed + 1.milliseconds

        assertEquals(0.5.milliseconds, elapsed)
        assertEquals(elapsedFromFuture, markFuture1.elapsed())
        assertEquals(elapsedFromFuture, markFuture2.elapsed())

        assertEquals(elapsedFromPast, markPast1.elapsed())
        assertEquals(elapsedFromPast, markPast2.elapsed())
    }
}