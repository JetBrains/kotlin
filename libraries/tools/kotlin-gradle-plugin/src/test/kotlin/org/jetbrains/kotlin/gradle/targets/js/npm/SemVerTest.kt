/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.junit.Assert.*
import org.junit.Test

class SemVerTest {
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
}