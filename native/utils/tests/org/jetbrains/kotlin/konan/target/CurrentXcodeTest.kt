/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.target

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Assumptions.assumeTrue

internal class CurrentXcodeTest {
    companion object {
        @BeforeAll
        @JvmStatic
        fun assumeMacOS() {
            assumeTrue(HostManager.hostIsMac)
        }
    }

    @Test
    fun `Should be able to access Xcode bundle version`() {
        val version = CurrentXcode().bundleVersion
        assertNotEquals("", version)
    }

    @Test
    fun `Should be able to access xcodebuild version`() {
        val version = CurrentXcode().xcodebuildVersion
        assertNotEquals("", version)
    }

    @Test
    fun `Xcode bundle version version should match xcodebuild version`() {
        val xcode = CurrentXcode()

        val xcodebuildVersion = xcode.xcodebuildVersion
        val bundleVersion = xcode.bundleVersion
        val version = xcode.version

        assertEquals(xcodebuildVersion, bundleVersion)
        assertEquals(xcodebuildVersion, version)
    }
}