/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.target

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class XcodeVersionTest {
    @Test
    fun `xcode version parsed correctly`() {
        assertEquals(XcodeVersion(14, 1), XcodeVersion.parse("14.1\nBuild version 14B47b\n"))
        assertEquals(XcodeVersion(14, 3), XcodeVersion.parse("14.3\nBuild version 14E222b\n"))
        assertEquals(XcodeVersion(1, 0), XcodeVersion.parse("1.0 RC\nBuild version 7B85\n"))
        assertEquals(XcodeVersion(1, 15), XcodeVersion.parse("1.15-RC\nBuild version 7B85\n"))
        assertEquals(XcodeVersion(14, 1), XcodeVersion.parse("14.1.1.3.2.1.123"))
        assertEquals(null, XcodeVersion.parse("14\nBuild version 14B47b\n"))
        assertEquals(null, XcodeVersion.parse("13"))
    }

    @Test
    fun `test xcode version comparison`() {
        assert(XcodeVersion(3, 2) == XcodeVersion(3, 2))
        assert(XcodeVersion(3, 2) > XcodeVersion(2, 14))
        assert(XcodeVersion(3, 2) > XcodeVersion(3, 1))
        assert(XcodeVersion(3, 2) < XcodeVersion(3, 3))
        assert(XcodeVersion(4, 0) > XcodeVersion(3, 3))
    }
}