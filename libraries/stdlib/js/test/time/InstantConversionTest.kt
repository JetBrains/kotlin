/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.time

import kotlin.js.Date
import kotlin.test.*
import kotlin.time.*

class InstantConversionTest {
    @Test
    fun toJSDateTest() {
        val releaseInstant = Instant.parse("2016-02-15T00:00:00Z")
        val releaseDate = releaseInstant.toJSDate()
        assertEquals(2016, releaseDate.getUTCFullYear())
        assertEquals(1, releaseDate.getUTCMonth())
        assertEquals(15, releaseDate.getUTCDate())
    }

    @Test
    fun toInstantTest() {
        val kotlinReleaseEpochMilliseconds = 1455494400000
        val releaseDate = Date(milliseconds = kotlinReleaseEpochMilliseconds)
        val releaseInstant = Instant.parse("2016-02-15T00:00:00Z")
        assertEquals(releaseInstant, releaseDate.toKotlinInstant())
    }
}
