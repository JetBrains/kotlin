/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.AndroidGradlePluginVersion
import org.jetbrains.kotlin.gradle.plugin.AndroidGradlePluginVersionOrNull
import org.jetbrains.kotlin.gradle.plugin.isAtLeast
import org.junit.Test
import kotlin.test.*

class AndroidGradlePluginVersionTest {

    @Test
    fun `test - AndroidGradlePluginVersion current - matches AGP version`() {
        assertEquals(com.android.Version.ANDROID_GRADLE_PLUGIN_VERSION, assertNotNull(AndroidGradlePluginVersion.current).toString())
    }

    @Test
    fun `test - compare`() {
        assertTrue(AndroidGradlePluginVersion("3.6") <= AndroidGradlePluginVersion("3.6"))
        assertTrue(AndroidGradlePluginVersion("3.6") >= AndroidGradlePluginVersion("3.6"))
        assertTrue(AndroidGradlePluginVersion("4.2") > AndroidGradlePluginVersion("3.6"))
        assertTrue(AndroidGradlePluginVersion("4.2") < AndroidGradlePluginVersion("7.2.1"))
        assertTrue(AndroidGradlePluginVersion("7.0.0-alpha01") < AndroidGradlePluginVersion("7.0.0-alpha02"))
        assertTrue(AndroidGradlePluginVersion("7.0.0") > AndroidGradlePluginVersion("7.0.0-alpha02"))
    }

    @Test
    fun `test - invalid version string`() {
        assertNull(AndroidGradlePluginVersionOrNull("x"))
        assertFailsWith<IllegalArgumentException> { AndroidGradlePluginVersion("x") }
    }


    @Test
    fun `test - atLeast`() {
        assertFalse(null.isAtLeast("1.0"))
        assertTrue(AndroidGradlePluginVersion("1.0").isAtLeast("1.0"))
        assertTrue(AndroidGradlePluginVersion("1.0").isAtLeast(AndroidGradlePluginVersion("1.0")))
    }
}