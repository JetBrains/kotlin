/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import com.intellij.testFramework.LightPlatformTestCase
import org.jetbrains.kotlin.idea.KotlinPluginVersion
import org.jetbrains.kotlin.idea.PlatformVersion

class CompatibilityVerifierVersionComparisonTest : LightPlatformTestCase() {
    fun testKotlinVersionParsing() {
        val version = KotlinPluginVersion.parse("1.2.40-dev-193-Studio3.0-1")
                ?: throw AssertionError("Version should not be null")

        assertEquals("1.2.40", version.kotlinVersion)
        assertEquals("dev", version.status)
        assertEquals("193", version.buildNumber)
        assertEquals(PlatformVersion.Platform.ANDROID_STUDIO, version.platformVersion.platform)
        assertEquals("3.0", version.platformVersion.version)
        assertEquals("1", version.patchNumber)
    }

    fun testPlatformVersionParsing() {
        PlatformVersion.getCurrent() ?: throw AssertionError("Version should not be null")
    }
}