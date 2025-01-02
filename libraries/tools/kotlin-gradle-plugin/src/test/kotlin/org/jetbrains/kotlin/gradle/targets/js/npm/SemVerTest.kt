/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.jetbrains.kotlin.test.MuteableTestRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class SemVerTest {
    @get:Rule val muteableTestRule = MuteableTestRule()

    @Test
    fun testParse() {
        assertEquals("SemVer(major=0, minor=0, patch=0, preRelease=null, build=null)", SemVer.from("0.0.0").toDebugString())
        assertEquals("SemVer(major=0, minor=0, patch=0, preRelease=a, build=null)", SemVer.from("0.0.0-a").toDebugString())
        assertEquals("SemVer(major=0, minor=0, patch=0, preRelease=a, build=b)", SemVer.from("0.0.0-a+b").toDebugString())
        assertEquals("SemVer(major=0, minor=0, patch=0, preRelease=null, build=b)", SemVer.from("0.0.0+b").toDebugString())
        assertEquals("SemVer(major=0, minor=0, patch=0, preRelease=null, build=null)", SemVer.from("0.0.0-").toDebugString())
        assertEquals("SemVer(major=0, minor=0, patch=0, preRelease=null, build=null)", SemVer.from("0.0.0-+").toDebugString())
        assertEquals("SemVer(major=0, minor=0, patch=0, preRelease=null, build=null)", SemVer.from("0.0.0+").toDebugString())
        assertEquals("SemVer(major=0, minor=0, patch=0, preRelease=a-b, build=null)", SemVer.from("0.0.0-a-b").toDebugString())
        assertEquals("SemVer(major=0, minor=0, patch=0, preRelease=a-b, build=b)", SemVer.from("0.0.0-a-b+b").toDebugString())
        assertEquals("SemVer(major=0, minor=0, patch=0, preRelease=a, build=b-c)", SemVer.from("0.0.0-a+b-c").toDebugString())
        assertEquals(
            "SemVer(major=9999999999999999, minor=9999999999999999, patch=9999999999999999, preRelease=9999999999999999, build=9999999999999999)",
            SemVer.from("9999999999999999.9999999999999999.9999999999999999-9999999999999999+9999999999999999").toDebugString()
        )
    }

    @Test
    fun testSort() {
        assertEquals(
            "1.3.0, 1.3.0+1, 1.3.0-SNAPSHOT, 1.3.0-SNAPSHOT+1, 1.3.30, 1.3.31, 1.3.40, 1.4.0, 2.0.0",
            listOf(
                SemVer.from("1.3.40"),
                SemVer.from("1.3-SNAPSHOT", true),
                SemVer.from("1.3.31"),
                SemVer.from("1.3.30"),
                SemVer.from("1.4.0"),
                SemVer.from("2.0.0"),
                SemVer.from("1.3-SNAPSHOT+1", true),
                SemVer.from("1.3", true),
                SemVer.from("1.3+1", true)
            ).sorted().joinToString(", ")
        )
    }

    @Test
    fun testParseGradleRichVersions() {
        val maxInt = Int.MAX_VALUE.toBigInteger()
        listOf(
            // Version prefix
            SemVer(maxInt, maxInt, maxInt) to SemVer.fromGradleRichVersion("+"),
            SemVer(1.toBigInteger(), maxInt, maxInt) to SemVer.fromGradleRichVersion("1.+"),
            SemVer(1.toBigInteger(), 10.toBigInteger(), maxInt) to SemVer.fromGradleRichVersion("1.10.+"),
            SemVer(1.toBigInteger(), 10.toBigInteger(), 0.toBigInteger()) to SemVer.fromGradleRichVersion("1.10.0.+"),

            // Version latest state
            SemVer(maxInt, maxInt, maxInt) to SemVer.fromGradleRichVersion("latest.release"),
            SemVer(maxInt, maxInt, maxInt) to SemVer.fromGradleRichVersion("latest.integration"),

            // Ranges
            SemVer(1.toBigInteger(), 5.toBigInteger(), 0.toBigInteger()) to SemVer.fromGradleRichVersion("(1.2,1.5]"),
            SemVer(1.toBigInteger(), 5.toBigInteger(), 55.toBigInteger()) to SemVer.fromGradleRichVersion("[1.2,1.5.55]"),
            SemVer(1.toBigInteger(), 5.toBigInteger(), 54.toBigInteger()) to SemVer.fromGradleRichVersion("[1.2,1.5.55-SNAPSHOT["),
            SemVer(1.toBigInteger(), maxInt, maxInt) to SemVer.fromGradleRichVersion("[1.1,2.0)"),
            SemVer(1.toBigInteger(), maxInt, maxInt) to SemVer.fromGradleRichVersion("(1.1,2.0)"),
            SemVer(1.toBigInteger(), maxInt, maxInt) to SemVer.fromGradleRichVersion("(1.1,2.0-SNAPSHOT)"),
            SemVer(1.toBigInteger(), maxInt, maxInt) to SemVer.fromGradleRichVersion("]1.0, 2.0["),
            SemVer(1.toBigInteger(), maxInt, maxInt) to SemVer.fromGradleRichVersion("[1.0, 2.0["),
            SemVer(1.toBigInteger(), 0.toBigInteger(), 0.toBigInteger()) to SemVer.fromGradleRichVersion("(,1.0]"),
            SemVer(1.toBigInteger(), 0.toBigInteger(), 0.toBigInteger(), "SNAPSHOT") to SemVer.fromGradleRichVersion("(,1.0-SNAPSHOT]"),
            SemVer(0.toBigInteger(), maxInt, maxInt) to SemVer.fromGradleRichVersion("(,1.0)"),
            SemVer(maxInt, maxInt, maxInt) to SemVer.fromGradleRichVersion("[1.0,)"),
            SemVer(10.toBigInteger(), 0.toBigInteger(), 20.toBigInteger()) to SemVer.fromGradleRichVersion("[10.0.20]"),

            // Normal
            SemVer(10.toBigInteger(), 0.toBigInteger(), 20.toBigInteger()) to SemVer.fromGradleRichVersion("10.0.20"),
            SemVer(10.toBigInteger(), 0.toBigInteger(), 0.toBigInteger(), "SNAPSHOT") to SemVer.fromGradleRichVersion("10.0-SNAPSHOT"),
        ).forEach { (expected, actual) ->
            assertEquals(expected, actual)
        }
    }
}