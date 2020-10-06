/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import com.intellij.testFramework.LightPlatformTestCase
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.idea.KotlinPluginVersion
import org.jetbrains.kotlin.idea.PlatformVersion
import org.junit.runner.RunWith

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class CompatibilityVerifierVersionComparisonTest : LightPlatformTestCase() {
    fun testKotlinVersionParsing() {
        val version = KotlinPluginVersion.parse("1.2.40-dev-193-Studio3.0-1") ?: throw AssertionError("Version should not be null")

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
        val version = KotlinPluginVersion.parse("1.4-M1-eap-27-IJ2020.1-1") ?: throw AssertionError("Version should not be null")

        assertEquals("1.4", version.kotlinVersion)
        assertEquals("M1", version.milestone)
        assertEquals("eap", version.status)
        assertEquals("27", version.buildNumber)
        assertEquals(PlatformVersion.Platform.IDEA, version.platformVersion.platform)
        assertEquals("2020.1", version.platformVersion.version)
        assertEquals("1", version.patchNumber)
    }

    fun testMilestoneVersionWithoutStatus() {
        val version = KotlinPluginVersion.parse("1.4-M1-42-IJ2020.1-1") ?: throw AssertionError("Version should not be null")

        assertEquals("1.4", version.kotlinVersion)
        assertEquals("M1", version.milestone)
        assertEquals("42", version.buildNumber)
        assertEquals(PlatformVersion.Platform.IDEA, version.platformVersion.platform)
        assertEquals("2020.1", version.platformVersion.version)
        assertEquals("1", version.patchNumber)
    }

    fun testInvalidVersion() {
        val version = KotlinPluginVersion.parse("1.4-release-M5-IJ2020.1-1")

        assertNull(version)
    }

    fun testPlatformVersionParsing() {
        PlatformVersion.getCurrent() ?: throw AssertionError("Version should not be null")
    }

    fun testCurrentPluginVersionParsing() {
        val pluginVersion = KotlinPluginUtil.getPluginVersion()
        if (pluginVersion == "@snapshot@") return

        assertNotNull("Can not parse current Kotlin Plugin version: $pluginVersion", KotlinPluginVersion.getCurrent())
    }
}