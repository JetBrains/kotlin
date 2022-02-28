import kotlin.test.Test
import kotlin.time.*

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@OptIn(kotlin.time.ExperimentalTime::class)
class PlatformIntDatetimeExtensionsTest {
    @Test
    fun testDays() {
        assertPrints(pli(1).days, "1d")
    }

    @Test
    fun testHours() {
        assertPrints(pli(1).hours, "1h")
    }

    @Test
    fun testMicroseconds() {
        assertPrints(pli(1).microseconds, "1us")
    }

    @Test
    fun testMilliseconds() {
        assertPrints(pli(1).milliseconds, "1ms")
    }

    @Test
    fun testMinutes() {
        assertPrints(pli(1).minutes, "1m")
    }

    @Test
    fun testSeconds() {
        assertPrints(pli(1).seconds, "1s")
    }

    @Test
    fun testToDuration() {
        assertPrints(pli(1).toDuration(DurationUnit.SECONDS), "1s")
    }
}