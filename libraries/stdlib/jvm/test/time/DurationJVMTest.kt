/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.time

import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds

class DurationJVMTest {

    @Test
    fun nanosecondsRounding() {
        for (i in 0..4) {
            testDefault(
                Duration.ZERO,
                "0s",
                "0.00000000000000578703703703703646777572819057${i}d",
                "0.000000000000138888888888888884692948339225925591386${i}h"
            )
            testDefault(
                1.nanoseconds,
                "1ns",
                "0.00000000000000578703703703703646777572819057${i + 5}d",
                "0.000000000000138888888888888884692948339225925591386${i + 5}h"
            )
            testDefault(1.days - 1.nanoseconds, "23h 59m 59.999999999s", "0.9999999999999941713291207179281627759337${i}d")
            testDefault(1.days, "1d", "0.9999999999999941713291207179281627759337${i + 5}d")
            testDefault(1.hours - 1.nanoseconds, "59m 59.999999999s", "0.999999999999861055588468161658965982${i}h")
            testDefault(1.hours, "1h", "0.999999999999861055588468161658965982${i + 5}h")
            testDefault(1.minutes - 1.nanoseconds, "59.999999999s", "0.9999999999916666104660123437${i}m")
            testDefault(1.minutes, "1m", "0.9999999999916666104660123437${i + 5}m")
        }
    }
}
