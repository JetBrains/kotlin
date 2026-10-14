/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.internals.MINIMALLY_SUPPORTED_GRADLE_VERSION
import org.jetbrains.kotlin.gradle.testbase.OtherGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.TestVersions
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OtherGradlePluginTests
class GradleTestVersionsSanityTest {

    @Test
    fun minSupportedGradleVersionMatchesKgpMinimallySupportedVersion() {
        val testMinSupported = GradleVersion.version(TestVersions.Gradle.MIN_SUPPORTED)
        val kgpMinSupported = GradleVersion.version(MINIMALLY_SUPPORTED_GRADLE_VERSION)

        // Only the patch release is allowed to differ, so tests could run against the latest patch release
        assertEquals(
            kgpMinSupported.majorMinor,
            testMinSupported.majorMinor,
            "TestVersions.Gradle.MIN_SUPPORTED (${TestVersions.Gradle.MIN_SUPPORTED}) must be the same minor release " +
                    "as MINIMALLY_SUPPORTED_GRADLE_VERSION ($MINIMALLY_SUPPORTED_GRADLE_VERSION)"
        )
        assertTrue(
            testMinSupported >= kgpMinSupported,
            "TestVersions.Gradle.MIN_SUPPORTED (${TestVersions.Gradle.MIN_SUPPORTED}) must not be lower " +
                    "than MINIMALLY_SUPPORTED_GRADLE_VERSION ($MINIMALLY_SUPPORTED_GRADLE_VERSION)"
        )
    }

    private val GradleVersion.majorMinor: String
        get() = baseVersion.version.split(".").take(2).joinToString(".")
}
