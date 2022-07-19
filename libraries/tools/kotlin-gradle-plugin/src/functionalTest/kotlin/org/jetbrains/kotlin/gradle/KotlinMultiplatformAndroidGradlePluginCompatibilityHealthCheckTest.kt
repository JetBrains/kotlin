/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle

import com.android.build.gradle.LibraryPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.mpp.buildProjectWithKPM
import org.jetbrains.kotlin.gradle.mpp.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.mpp.kotlin
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformAndroidGradlePluginCompatibilityHealthCheck.AndroidGradlePluginStringProvider
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformAndroidGradlePluginCompatibilityHealthCheck.AndroidGradlePluginVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformAndroidGradlePluginCompatibilityHealthCheck.AndroidGradlePluginVersionParser
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformAndroidGradlePluginCompatibilityHealthCheck.MAX_SUPPORTED_AGP_MAJOR_VERSION
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformAndroidGradlePluginCompatibilityHealthCheck.MIN_SUPPORTED_AGP_MAJOR_VERSION
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformAndroidGradlePluginCompatibilityHealthCheck.Messages
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformAndroidGradlePluginCompatibilityHealthCheck.PROPERTY_KEY_EXECUTED_PROJECT_PATHS
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformAndroidGradlePluginCompatibilityHealthCheck.runMultiplatformAndroidGradlePluginCompatibilityHealthCheck
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformAndroidGradlePluginCompatibilityHealthCheck.runMultiplatformAndroidGradlePluginCompatibilityHealthCheckWhenAndroidIsApplied
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.utils.getOrPutRootProjectProperty
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class KotlinMultiplatformAndroidGradlePluginCompatibilityHealthCheckTest {

    val project by lazy {
        buildProjectWithMPP {
            plugins.apply(LibraryPlugin::class.java)
            kotlin { android() }
        }
    }

    private val testLogger = object : (String) -> Unit {
        val messages = mutableListOf<String>()
        override fun invoke(message: String) {
            messages.add(message)
        }
    }

    class FixedAndroidGradlePluginVersionProvider(private val version: String?) : AndroidGradlePluginStringProvider {
        override fun getAndroidGradlePluginString(): String? = version
    }

    @Test
    fun `test - version too low`() {
        val androidGradlePluginVersion = "${MIN_SUPPORTED_AGP_MAJOR_VERSION - 1}.0"

        project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheck(
            testLogger, FixedAndroidGradlePluginVersionProvider(androidGradlePluginVersion)
        )

        assertEquals(Messages.androidGradlePluginVersionTooLow(androidGradlePluginVersion), assertSingleWarningMessage())
    }

    @Test
    fun `test - version too high`() {
        val androidGradlePluginVersion = "${MAX_SUPPORTED_AGP_MAJOR_VERSION + 1}.0"

        project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheck(
            testLogger, FixedAndroidGradlePluginVersionProvider(androidGradlePluginVersion)
        )

        assertEquals(
            Messages.androidGradlePluginVersionTooHigh(androidGradlePluginVersion), assertSingleWarningMessage()
        )
    }

    @Test
    fun `test - missing Android Gradle Plugin version string`() {
        project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheck(
            testLogger, FixedAndroidGradlePluginVersionProvider(null)
        )

        assertEquals(
            Messages.FAILED_GETTING_ANDROID_GRADLE_PLUGIN_VERSION_STRING, assertSingleWarningMessage()
        )
    }

    @Test
    fun `test - failed parsing Android Gradle Plugin version`() {
        val androidGradlePluginVersion = "${MIN_SUPPORTED_AGP_MAJOR_VERSION}.0"

        val failingParser = object : AndroidGradlePluginVersionParser {
            override fun parseVersionString(version: String): AndroidGradlePluginVersion? {
                return null
            }
        }

        project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheck(
            testLogger, FixedAndroidGradlePluginVersionProvider(androidGradlePluginVersion), failingParser
        )

        assertEquals(Messages.failedParsingAndroidGradlePluginVersion(androidGradlePluginVersion), assertSingleWarningMessage())
    }

    @Test
    fun `test - nowarn property`() {
        project.propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_MPP_ANDROID_GRADLE_PLUGIN_COMPATIBILITY_NO_WARN, "true")

        /* Test with missing AGP version */
        project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheck(testLogger, FixedAndroidGradlePluginVersionProvider(null))
        assertNoWarningMessage()

        /* Test with too low AGP version */
        project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheck(
            testLogger, FixedAndroidGradlePluginVersionProvider("${MIN_SUPPORTED_AGP_MAJOR_VERSION - 1}.0")
        )
        assertNoWarningMessage()

        /* Test with too high AGP version */
        project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheck(
            testLogger, FixedAndroidGradlePluginVersionProvider("${MAX_SUPPORTED_AGP_MAJOR_VERSION + 1}.0")
        )
        assertNoWarningMessage()

        /* Re-enable the check and test with missing AGP version */
        project.propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_MPP_ANDROID_GRADLE_PLUGIN_COMPATIBILITY_NO_WARN, "false")
        project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheck(testLogger, FixedAndroidGradlePluginVersionProvider(null))
        assertEquals(Messages.FAILED_GETTING_ANDROID_GRADLE_PLUGIN_VERSION_STRING, assertSingleWarningMessage())
    }

    @Test
    fun `test - compatible versions`() {
        fun assertNoWarnings(androidGradlePluginVersion: String) {
            project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheck(
                testLogger, FixedAndroidGradlePluginVersionProvider(androidGradlePluginVersion)
            )
            assertNoWarningMessage()
        }

        assertNoWarnings("$MIN_SUPPORTED_AGP_MAJOR_VERSION.0")
        assertNoWarnings("$MIN_SUPPORTED_AGP_MAJOR_VERSION.0.0")
        assertNoWarnings("$MIN_SUPPORTED_AGP_MAJOR_VERSION.0.0-alpha01")
        assertNoWarnings("$MIN_SUPPORTED_AGP_MAJOR_VERSION.0.0-m-281")

        assertNoWarnings("$MIN_SUPPORTED_AGP_MAJOR_VERSION.1")
        assertNoWarnings("$MIN_SUPPORTED_AGP_MAJOR_VERSION.1.0")
        assertNoWarnings("$MIN_SUPPORTED_AGP_MAJOR_VERSION.1.0-alpha01")
        assertNoWarnings("$MIN_SUPPORTED_AGP_MAJOR_VERSION.1.0-m-281")

        assertNoWarnings("$MIN_SUPPORTED_AGP_MAJOR_VERSION.0.1")
        assertNoWarnings("$MIN_SUPPORTED_AGP_MAJOR_VERSION.0.1-alpha01")

        assertNoWarnings("$MAX_SUPPORTED_AGP_MAJOR_VERSION.0")
        assertNoWarnings("$MAX_SUPPORTED_AGP_MAJOR_VERSION.0.0")
        assertNoWarnings("$MAX_SUPPORTED_AGP_MAJOR_VERSION.0.0-alpha01")
        assertNoWarnings("$MAX_SUPPORTED_AGP_MAJOR_VERSION.0.0-m-281")

        assertNoWarnings("$MAX_SUPPORTED_AGP_MAJOR_VERSION.1")
        assertNoWarnings("$MAX_SUPPORTED_AGP_MAJOR_VERSION.1.0")
        assertNoWarnings("$MAX_SUPPORTED_AGP_MAJOR_VERSION.1.0-alpha01")
        assertNoWarnings("$MAX_SUPPORTED_AGP_MAJOR_VERSION.1.0-m-281")

        assertNoWarnings("$MAX_SUPPORTED_AGP_MAJOR_VERSION.0.1")
        assertNoWarnings("$MAX_SUPPORTED_AGP_MAJOR_VERSION.0.1-alpha01")
    }

    @Test
    fun `test WhenAndroidIsApplied - android is applied after the health check call`() {
        val project = ProjectBuilder.builder().build()

        project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheckWhenAndroidIsApplied(
            testLogger, FixedAndroidGradlePluginVersionProvider(null)
        )

        /* Not yet, executed, because Android is not applied yet */
        assertNoWarningMessage()

        project.plugins.apply(LibraryPlugin::class.java)
        assertEquals(Messages.FAILED_GETTING_ANDROID_GRADLE_PLUGIN_VERSION_STRING, assertSingleWarningMessage())
    }

    @Test
    fun `test - WhenAndroidIsApplied - android is applied before the health check call`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply(LibraryPlugin::class.java)

        project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheckWhenAndroidIsApplied(
            testLogger, FixedAndroidGradlePluginVersionProvider(null)
        )

        assertEquals(Messages.FAILED_GETTING_ANDROID_GRADLE_PLUGIN_VERSION_STRING, assertSingleWarningMessage())
    }

    @Test
    fun `test - WhenAndroidIsApplied - called multiple times - still emits only a single message`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply(LibraryPlugin::class.java)

        repeat(10) {
            project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheckWhenAndroidIsApplied(
                testLogger, FixedAndroidGradlePluginVersionProvider(null)
            )
        }

        assertEquals(Messages.FAILED_GETTING_ANDROID_GRADLE_PLUGIN_VERSION_STRING, assertSingleWarningMessage())
    }

    @Test
    fun `test - is automatically executed when android plugin is applied - mpp plugin`() {
        val project = buildProjectWithMPP { }
        val executedProjectPaths = project.getOrPutRootProjectProperty(PROPERTY_KEY_EXECUTED_PROJECT_PATHS) { mutableSetOf<String>() }
        if (executedProjectPaths.isNotEmpty()) fail("Expected 'executed' project paths to be empty")
        project.plugins.apply(LibraryPlugin::class.java)

        /* Expect project was checked */
        assertEquals(setOf(project.path), executedProjectPaths)
    }

    @Test
    fun `test - is automatically executed when android plugin is applied - kpm plugin`() {
        val project = buildProjectWithKPM { }
        val executedProjectPaths = project.getOrPutRootProjectProperty(PROPERTY_KEY_EXECUTED_PROJECT_PATHS) { mutableSetOf<String>() }
        if (executedProjectPaths.isNotEmpty()) fail("Expected 'executed' project paths to be empty")
        project.plugins.apply(LibraryPlugin::class.java)

        /* Expect project was checked */
        assertEquals(setOf(project.path), executedProjectPaths)
    }

    @Test
    fun `test - is not executed when android plugin is applied - kotlin-android plugin`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("kotlin-android")
        project.plugins.apply(LibraryPlugin::class.java)

        val executedProjectPaths = project.getOrPutRootProjectProperty(PROPERTY_KEY_EXECUTED_PROJECT_PATHS) { mutableSetOf<String>() }
        if (executedProjectPaths.isNotEmpty()) fail("Expected 'executed' project paths to be empty")
    }

    private fun assertSingleWarningMessage(): String {
        if (testLogger.messages.size != 1) fail("Expected exactly one warning message logged. Found: ${testLogger.messages}")
        return testLogger.messages.single()
    }

    private fun assertNoWarningMessage() {
        if (testLogger.messages.isNotEmpty()) {
            fail("Expected no warning messages to be emitted. Found: ${testLogger.messages}")
        }
    }
}
