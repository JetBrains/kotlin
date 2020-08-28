/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import com.intellij.testFramework.LightPlatformTestCase
import org.jetbrains.kotlin.idea.*
import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.runner.RunWith

@RunWith(JUnit38ClassRunner::class)
class CompatibilityVerifierVersionComparisonTest : LightPlatformTestCase() {
    fun testKotlinVersionParsing() {
        val version = OldKotlinPluginVersion.parse("1.2.40-dev-193-Studio3.0-1") ?: throw AssertionError("Version should not be null")

        assertEquals("1.2.40", version.kotlinVersion)
        assertNull(version.milestone)
        assertEquals("dev", version.status)
        assertEquals("193", version.buildNumber)
        assertEquals(PlatformVersion.Platform.ANDROID_STUDIO, version.platformVersion.platform)
        assertEquals("3.0", version.platformVersion.version)
        assertEquals("1", version.patchNumber)
    }

    fun testReleaseVersionDoesntHaveBuildNumber() {
        val version = KotlinPluginVersion.parse("1.2.40-release-Studio3.0-1") ?: throw AssertionError("Version should not be null")

        assertNull(version.buildNumber)
    }

    fun testMilestoneVersion() {
        val version = OldKotlinPluginVersion.parse("1.4-M1-eap-27-IJ2020.1-1") ?: throw AssertionError("Version should not be null")

        assertEquals("1.4", version.kotlinVersion)
        assertEquals("M1", version.milestone)
        assertEquals("eap", version.status)
        assertEquals("27", version.buildNumber)
        assertEquals(PlatformVersion.Platform.IDEA, version.platformVersion.platform)
        assertEquals("2020.1", version.platformVersion.version)
        assertEquals("1", version.patchNumber)
    }

    fun testPlatformVersionParsing() {
        PlatformVersion.getCurrent() ?: throw AssertionError("Version should not be null")
    }

    fun testCurrentPluginVersionParsing() {
        val pluginVersion = KotlinPluginUtil.getPluginVersion()
        if (pluginVersion == "@snapshot@") return

        val currentVersion = KotlinPluginVersion.getCurrent()
        assert(currentVersion is KidKotlinPluginVersion) { "Can not parse current Kotlin Plugin version: $pluginVersion" }
    }
}