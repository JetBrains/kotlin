/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle.unitTests

import com.android.build.gradle.LibraryPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.plugin.AndroidGradlePluginVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformAndroidGradlePluginCompatibilityHealthCheck.AndroidGradlePluginVersionProvider
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformAndroidGradlePluginCompatibilityHealthCheck.AndroidGradlePluginVersionRange
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformAndroidGradlePluginCompatibilityHealthCheck.Messages
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformAndroidGradlePluginCompatibilityHealthCheck.PROPERTY_KEY_EXECUTED_PROJECT_PATHS
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformAndroidGradlePluginCompatibilityHealthCheck.compatibleAndroidGradlePluginVersionRange
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformAndroidGradlePluginCompatibilityHealthCheck.runMultiplatformAndroidGradlePluginCompatibilityHealthCheck
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformAndroidGradlePluginCompatibilityHealthCheck.runMultiplatformAndroidGradlePluginCompatibilityHealthCheckWhenAndroidIsApplied
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.utils.getOrPutRootProjectProperty
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
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

    internal class FixedAndroidGradlePluginVersionProvider(private val version: String?) : AndroidGradlePluginVersionProvider {
        override fun getAndroidGradlePluginVersion(): AndroidGradlePluginVersion? {
            if (version == null) return null
            return AndroidGradlePluginVersion(version)
        }
    }

    @Test
    fun `test - version too low - major`() {
        val androidGradlePluginVersion = AndroidGradlePluginVersion(
            major = compatibleAndroidGradlePluginVersionRange.minSupportedVersion.major - 1,
            minor = compatibleAndroidGradlePluginVersionRange.minSupportedVersion.minor
        )

        project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheck(
            testLogger, FixedAndroidGradlePluginVersionProvider(androidGradlePluginVersion.toString())
        )

        assertEquals(Messages.androidGradlePluginVersionTooLow(androidGradlePluginVersion.toString()), assertSingleWarningMessage())
    }

    @Test
    fun `test - version too low - minor`() {
        val androidGradlePluginVersion = AndroidGradlePluginVersion(1, 1)

        val compatibleAndroidGradlePluginVersionRange = AndroidGradlePluginVersionRange(
            minSupportedVersionMajor = 1, minSupportedVersionMinor = 2,
            maxSupportedVersionMajor = 2
        )

        project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheck(
            testLogger, FixedAndroidGradlePluginVersionProvider(androidGradlePluginVersion.toString()),
            compatibleAndroidGradlePluginVersionRange = compatibleAndroidGradlePluginVersionRange
        )

        assertEquals(Messages.androidGradlePluginVersionTooLow(androidGradlePluginVersion.toString()), assertSingleWarningMessage())
    }

    @Test
    fun `test - version too high - major`() {
        val androidGradlePluginVersion = AndroidGradlePluginVersion(
            major = compatibleAndroidGradlePluginVersionRange.maxSupportedVersion.major + 1,
            minor = compatibleAndroidGradlePluginVersionRange.maxSupportedVersion.minor
        )

        project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheck(
            testLogger, FixedAndroidGradlePluginVersionProvider(androidGradlePluginVersion.toString())
        )

        assertEquals(
            Messages.androidGradlePluginVersionTooHigh(androidGradlePluginVersion.toString()), assertSingleWarningMessage()
        )
    }

    @Test
    fun `test - version too high - minor`() {
        val androidGradlePluginVersion = AndroidGradlePluginVersion(2, 1)

        val compatibleAndroidGradlePluginVersionRange = AndroidGradlePluginVersionRange(
            minSupportedVersion = AndroidGradlePluginVersionRange.Version(1, 0),
            maxSupportedVersion = AndroidGradlePluginVersionRange.Version(2, 0)
        )

        project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheck(
            testLogger, FixedAndroidGradlePluginVersionProvider(androidGradlePluginVersion.toString()),
            compatibleAndroidGradlePluginVersionRange = compatibleAndroidGradlePluginVersionRange
        )

        assertEquals(
            Messages.androidGradlePluginVersionTooHigh(androidGradlePluginVersion.toString()), assertSingleWarningMessage()
        )
    }


    @Test
    fun `test - missing Android Gradle Plugin version string`() {
        project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheck(
            testLogger, FixedAndroidGradlePluginVersionProvider(null)
        )

        assertEquals(
            Messages.failedGettingAndroidGradlePluginVersion(), assertSingleWarningMessage()
        )
    }

    @Test
    fun `test - nowarn property`() {
        project.propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_MPP_ANDROID_GRADLE_PLUGIN_COMPATIBILITY_NO_WARN, "true")

        /* Test with missing AGP version */
        project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheck(testLogger, FixedAndroidGradlePluginVersionProvider(null))
        assertNoWarningMessage()

        /* Test with too low AGP version */
        project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheck(
            testLogger, FixedAndroidGradlePluginVersionProvider("${compatibleAndroidGradlePluginVersionRange.minSupportedVersion}")
        )
        assertNoWarningMessage()

        /* Test with too high AGP version */
        project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheck(
            testLogger,
            FixedAndroidGradlePluginVersionProvider("${compatibleAndroidGradlePluginVersionRange.maxSupportedVersion.major + 1}.0")
        )
        assertNoWarningMessage()

        /* Re-enable the check and test with missing AGP version */
        project.propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_MPP_ANDROID_GRADLE_PLUGIN_COMPATIBILITY_NO_WARN, "false")
        project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheck(testLogger, FixedAndroidGradlePluginVersionProvider(null))
        assertEquals(Messages.failedGettingAndroidGradlePluginVersion(), assertSingleWarningMessage())
    }

    @Test
    fun `test - compatible versions`() {
        fun assertNoWarnings(androidGradlePluginVersion: String) {
            project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheck(
                testLogger, FixedAndroidGradlePluginVersionProvider(androidGradlePluginVersion)
            )
            assertNoWarningMessage()
        }

        val minSupportedVersion = compatibleAndroidGradlePluginVersionRange.minSupportedVersion.run {
            AndroidGradlePluginVersion(major, minor)
        }

        val midSupportedVersion = minSupportedVersion.copy(minor = minSupportedVersion.minor + 1)

        val maxSupportedVersion = AndroidGradlePluginVersion(
            compatibleAndroidGradlePluginVersionRange.maxSupportedVersion.major,
            compatibleAndroidGradlePluginVersionRange.maxSupportedVersion.minor
        )

        assertNoWarnings("$minSupportedVersion")
        assertNoWarnings("${minSupportedVersion.copy(classifier = "alpha01")}")
        assertNoWarnings("${minSupportedVersion.copy(classifier = "m281")}")

        assertNoWarnings("$midSupportedVersion")
        assertNoWarnings("${midSupportedVersion.copy(classifier = "alpha01")})")
        assertNoWarnings("${midSupportedVersion.copy(classifier = "m281")}")

        assertNoWarnings("${minSupportedVersion.copy(patch = 1)}")
        assertNoWarnings("${minSupportedVersion.copy(patch = 1, classifier = "alpha01")}")

        assertNoWarnings("$maxSupportedVersion")
        assertNoWarnings("${maxSupportedVersion.copy(classifier = "alpha01")}")
        assertNoWarnings("${maxSupportedVersion.copy(classifier = "m281")}")

        assertNoWarnings("${maxSupportedVersion.copy(patch = 1)}")
        assertNoWarnings("${maxSupportedVersion.copy(patch = 1, classifier = "alpha01")}")
    }

    @Test
    fun `test WhenAndroidIsApplied - android is applied after the health check call`() {
        val project = ProjectBuilder.builder().build()
        project.gradle.registerConfigurationTimePropertiesAccessorForTests()

        project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheckWhenAndroidIsApplied(
            testLogger, FixedAndroidGradlePluginVersionProvider(null)
        )

        /* Not yet, executed, because Android is not applied yet */
        assertNoWarningMessage()

        addBuildEventsListenerRegistryMock(project)
        project.plugins.apply(LibraryPlugin::class.java)
        assertEquals(Messages.failedGettingAndroidGradlePluginVersion(), assertSingleWarningMessage())
    }

    @Test
    fun `test - WhenAndroidIsApplied - android is applied before the health check call`() {
        val project = ProjectBuilder.builder().build()
        project.gradle.registerConfigurationTimePropertiesAccessorForTests()
        addBuildEventsListenerRegistryMock(project)
        project.plugins.apply(LibraryPlugin::class.java)

        project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheckWhenAndroidIsApplied(
            testLogger, FixedAndroidGradlePluginVersionProvider(null)
        )

        assertEquals(Messages.failedGettingAndroidGradlePluginVersion(), assertSingleWarningMessage())
    }

    @Test
    fun `test - WhenAndroidIsApplied - called multiple times - still emits only a single message`() {
        val project = ProjectBuilder.builder().build()
        project.gradle.registerConfigurationTimePropertiesAccessorForTests()
        addBuildEventsListenerRegistryMock(project)
        project.plugins.apply(LibraryPlugin::class.java)

        repeat(10) {
            project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheckWhenAndroidIsApplied(
                testLogger, FixedAndroidGradlePluginVersionProvider(null)
            )
        }

        assertEquals(Messages.failedGettingAndroidGradlePluginVersion(), assertSingleWarningMessage())
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
        addBuildEventsListenerRegistryMock(project)
        project.plugins.apply("kotlin-android")
        project.plugins.apply(LibraryPlugin::class.java)

        val executedProjectPaths = project.getOrPutRootProjectProperty(PROPERTY_KEY_EXECUTED_PROJECT_PATHS) { mutableSetOf<String>() }
        if (executedProjectPaths.isNotEmpty()) fail("Expected 'executed' project paths to be empty")
    }

    @Test
    fun `test - AndroidGradlePluginVersion - compareTo`() {
        assertEquals(0, AndroidGradlePluginVersion(1, 0).compareTo(AndroidGradlePluginVersion(1, 0)))
        assertTrue(AndroidGradlePluginVersion(1, 0) < AndroidGradlePluginVersion(2, 0))
        assertTrue(AndroidGradlePluginVersion(2, 0) > AndroidGradlePluginVersion(1, 0))
        assertTrue(AndroidGradlePluginVersion(2, 0) > AndroidGradlePluginVersion(1, 3))

        assertTrue(AndroidGradlePluginVersion(1, 0) < AndroidGradlePluginVersion(1, 1))
        assertTrue(AndroidGradlePluginVersion(1, 1) > AndroidGradlePluginVersion(1, 0))
    }

    @Test
    fun `test - AndroidGradlePluginRange`() {
        /* Run test with specified maxSupportedVersionMinor */
        AndroidGradlePluginVersionRange(
            minSupportedVersionMajor = 2, minSupportedVersionMinor = 1,
            maxSupportedVersionMajor = 4, maxSupportedVersionMinor = 2
        ).apply {
            assertFalse(isTooHigh(AndroidGradlePluginVersion(1, 0)))
            assertTrue(isTooLow(AndroidGradlePluginVersion(1, 0)))

            assertFalse(isTooLow(AndroidGradlePluginVersion(2, 1)))
            assertFalse(isTooHigh(AndroidGradlePluginVersion(2, 1)))

            assertFalse(isTooLow(AndroidGradlePluginVersion(3, 0)))
            assertFalse(isTooHigh(AndroidGradlePluginVersion(3, 0)))

            assertFalse(isTooLow(AndroidGradlePluginVersion(4, 3)))
            assertTrue(isTooHigh(AndroidGradlePluginVersion(4, 3)))

            assertFalse(isTooLow(AndroidGradlePluginVersion(5, 0)))
            assertTrue(isTooHigh(AndroidGradlePluginVersion(5, 0)))
        }

        /* Run test with *un*-specified maxSupportedVersionMinor */
        AndroidGradlePluginVersionRange(
            minSupportedVersionMajor = 2, minSupportedVersionMinor = 1,
            maxSupportedVersionMajor = 4
        ).apply {
            assertFalse(isTooHigh(AndroidGradlePluginVersion(1, 0)))
            assertTrue(isTooLow(AndroidGradlePluginVersion(1, 0)))

            assertFalse(isTooLow(AndroidGradlePluginVersion(2, 1)))
            assertFalse(isTooHigh(AndroidGradlePluginVersion(2, 1)))

            assertFalse(isTooLow(AndroidGradlePluginVersion(3, 0)))
            assertFalse(isTooHigh(AndroidGradlePluginVersion(3, 0)))

            assertFalse(isTooLow(AndroidGradlePluginVersion(4, 3)))
            assertTrue(isTooHigh(AndroidGradlePluginVersion(4, 3)))

            assertFalse(isTooLow(AndroidGradlePluginVersion(5, 0)))
        }
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
