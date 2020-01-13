/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:UseExperimental(ExperimentalTime::class)
package test.time

import kotlin.test.*
import kotlin.time.*

class TimeMarkTest {

    @Test
    fun adjustment() {
        val timeSource = TestTimeSource()

        fun TimeMark.assertHasPassed(hasPassed: Boolean) {
            assertEquals(!hasPassed, this.hasNotPassedNow(), "Expected mark in the future")
            assertEquals(hasPassed, this.hasPassedNow(), "Expected mark in the past")

            assertEquals(!hasPassed, this.elapsedNow() < Duration.ZERO, "Mark elapsed: ${this.elapsedNow()}, expected hasPassed: $hasPassed")
        }

        val mark = timeSource.markNow()
        val markFuture1 = (mark + 1.milliseconds).apply { assertHasPassed(false) }
        val markFuture2 = (mark - (-1).milliseconds).apply { assertHasPassed(false) }

        val markPast1 = (mark - 1.milliseconds).apply { assertHasPassed(true) }
        val markPast2 = (markFuture1 + (-2).milliseconds).apply { assertHasPassed(true) }

        timeSource += 500_000.nanoseconds

        val elapsed = mark.elapsedNow()
        val elapsedFromFuture = elapsed - 1.milliseconds
        val elapsedFromPast = elapsed + 1.milliseconds

        assertEquals(0.5.milliseconds, elapsed)
        assertEquals(elapsedFromFuture, markFuture1.elapsedNow())
        assertEquals(elapsedFromFuture, markFuture2.elapsedNow())

        assertEquals(elapsedFromPast, markPast1.elapsedNow())
        assertEquals(elapsedFromPast, markPast2.elapsedNow())

        markFuture1.assertHasPassed(false)
        markPast1.assertHasPassed(true)

        timeSource += 1.milliseconds

        markFuture1.assertHasPassed(true)
        markPast1.assertHasPassed(true)

    }
}