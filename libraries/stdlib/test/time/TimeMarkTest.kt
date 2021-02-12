/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalTime::class)
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

            assertEquals(
                !hasPassed,
                this.elapsedNow() < Duration.ZERO,
                "Mark elapsed: ${this.elapsedNow()}, expected hasPassed: $hasPassed"
            )
        }

        val mark = timeSource.markNow()
        val markFuture1 = (mark + Duration.milliseconds(1)).apply { assertHasPassed(false) }
        val markFuture2 = (mark - Duration.milliseconds((-1))).apply { assertHasPassed(false) }

        val markPast1 = (mark - Duration.milliseconds(1)).apply { assertHasPassed(true) }
        val markPast2 = (markFuture1 + Duration.milliseconds((-2))).apply { assertHasPassed(true) }

        timeSource += Duration.nanoseconds(500_000)

        val elapsed = mark.elapsedNow()
        val elapsedFromFuture = elapsed - Duration.milliseconds(1)
        val elapsedFromPast = elapsed + Duration.milliseconds(1)

        assertEquals(Duration.milliseconds(0.5), elapsed)
        assertEquals(elapsedFromFuture, markFuture1.elapsedNow())
        assertEquals(elapsedFromFuture, markFuture2.elapsedNow())

        assertEquals(elapsedFromPast, markPast1.elapsedNow())
        assertEquals(elapsedFromPast, markPast2.elapsedNow())

        markFuture1.assertHasPassed(false)
        markPast1.assertHasPassed(true)

        timeSource += Duration.milliseconds(1)

        markFuture1.assertHasPassed(true)
        markPast1.assertHasPassed(true)

    }
}
