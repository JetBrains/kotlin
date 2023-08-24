/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle.unitTests

import com.android.build.gradle.LibraryPlugin
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.plugin.AndroidGradlePluginVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformAndroidGradlePluginCompatibilityHealthCheck.AndroidGradlePluginVersionProvider
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformAndroidGradlePluginCompatibilityHealthCheck.AndroidGradlePluginVersionRange
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformAndroidGradlePluginCompatibilityHealthCheck.PROPERTY_KEY_EXECUTED_PROJECT_PATHS
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformAndroidGradlePluginCompatibilityHealthCheck.compatibleAndroidGradlePluginVersionRange
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformAndroidGradlePluginCompatibilityHealthCheck.runMultiplatformAndroidGradlePluginCompatibilityHealthCheck
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformAndroidGradlePluginCompatibilityHealthCheck.runMultiplatformAndroidGradlePluginCompatibilityHealthCheckWhenAndroidIsApplied
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.kotlinToolingDiagnosticsCollector
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
            setMultiplatformAndroidSourceSetLayoutVersion(2) // To not provoke warnings
            plugins.apply(LibraryPlugin::class.java)
            kotlin { androidTarget() }
        }
    }

    internal class FixedAndroidGradlePluginVersionProvider(private val version: String?) : AndroidGradlePluginVersionProvider {
        override fun getAndroidGradlePluginVersion(): AndroidGradlePluginVersion? {
            if (version == null) return null
            return AndroidGradlePluginVersion(version)
        }
    }

    // Hardcode it so that testdata doesn't change when the version range is bumped
    private val AGP_COMPATIBILITY_RANGE_FOR_TESTS = AndroidGradlePluginVersionRange(
        minSupportedVersion = AndroidGradlePluginVersionRange.Version(4, 2),
        maxSupportedVersion = AndroidGradlePluginVersionRange.Version(8, 0)
    )

    private fun runAgpCompatiblityCheck(agpVersion: AndroidGradlePluginVersion) {
        project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheck(
            FixedAndroidGradlePluginVersionProvider(agpVersion.toString()),
            AGP_COMPATIBILITY_RANGE_FOR_TESTS
        )
    }

    @Test
    fun `test - version too low - major`() {
        val androidGradlePluginVersion = AndroidGradlePluginVersion(
            major = AGP_COMPATIBILITY_RANGE_FOR_TESTS.minSupportedVersion.major - 1,
            minor = AGP_COMPATIBILITY_RANGE_FOR_TESTS.minSupportedVersion.minor
        )

        runAgpCompatiblityCheck(androidGradlePluginVersion)

        project.checkDiagnostics("agpCompatibility/versionTooLowMajor")
    }

    @Test
    fun `test - version too low - minor`() {
        val androidGradlePluginVersion = AndroidGradlePluginVersion(
            major = AGP_COMPATIBILITY_RANGE_FOR_TESTS.minSupportedVersion.major,
            minor = AGP_COMPATIBILITY_RANGE_FOR_TESTS.minSupportedVersion.minor - 1
        )

        runAgpCompatiblityCheck(androidGradlePluginVersion)

        project.checkDiagnostics("agpCompatibility/versionTooLowMinor")
    }

    @Test
    fun `test - version too high - major`() {
        val androidGradlePluginVersion = AndroidGradlePluginVersion(
            major = AGP_COMPATIBILITY_RANGE_FOR_TESTS.maxSupportedVersion.major + 1,
            minor = AGP_COMPATIBILITY_RANGE_FOR_TESTS.maxSupportedVersion.minor
        )

        runAgpCompatiblityCheck(androidGradlePluginVersion)

        project.checkDiagnostics("agpCompatibility/versionTooHighMajor")
    }

    @Test
    fun `test - version too high - minor`() {
        val androidGradlePluginVersion = AndroidGradlePluginVersion(
            major = AGP_COMPATIBILITY_RANGE_FOR_TESTS.maxSupportedVersion.major,
            minor = AGP_COMPATIBILITY_RANGE_FOR_TESTS.maxSupportedVersion.minor + 1
        )

        runAgpCompatiblityCheck(androidGradlePluginVersion)

        project.checkDiagnostics("agpCompatibility/versionTooHighMinor")
    }


    @Test
    fun `test - missing Android Gradle Plugin version string`() {
        project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheck(
            FixedAndroidGradlePluginVersionProvider(null)
        )

        project.checkDiagnostics("agpCompatibility/missingAgpVersion")
    }

    @Test
    fun `test - nowarn property`() {
        project.propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_MPP_ANDROID_GRADLE_PLUGIN_COMPATIBILITY_NO_WARN, "true")

        /* Test with missing AGP version */
        project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheck(FixedAndroidGradlePluginVersionProvider(null))
        project.assertNoDiagnostics()

        /* Test with too low AGP version */
        project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheck(
            FixedAndroidGradlePluginVersionProvider("${AGP_COMPATIBILITY_RANGE_FOR_TESTS.minSupportedVersion}")
        )
        project.assertNoDiagnostics()

        /* Test with too high AGP version */
        project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheck(
            FixedAndroidGradlePluginVersionProvider("${AGP_COMPATIBILITY_RANGE_FOR_TESTS.maxSupportedVersion.major + 1}.0")
        )
        project.assertNoDiagnostics()

        /* Re-enable the check and test with missing AGP version */
        project.propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_MPP_ANDROID_GRADLE_PLUGIN_COMPATIBILITY_NO_WARN, "false")
        project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheck(FixedAndroidGradlePluginVersionProvider(null))
        project.checkDiagnostics("agpCompatibility/noWarnProperty")
    }

    @Test
    fun `test - compatible versions`() {
        fun assertNoWarnings(androidGradlePluginVersion: String) {
            project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheck(
                FixedAndroidGradlePluginVersionProvider(androidGradlePluginVersion)
            )
            project.assertNoWarningMessage()
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
        project.gradle.registerMinimalVariantImplementationFactoriesForTests()

        project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheckWhenAndroidIsApplied(
            FixedAndroidGradlePluginVersionProvider(null)
        )

        /* Not yet, executed, because Android is not applied yet */
        project.assertNoWarningMessage()

        addBuildEventsListenerRegistryMock(project)
        project.plugins.apply(LibraryPlugin::class.java)
        project.checkDiagnostics("agpCompatibility/androidIsAppliedAfterCheckerCall")
    }

    @Test
    fun `test - WhenAndroidIsApplied - android is applied before the health check call`() {
        val project = ProjectBuilder.builder().build()
        project.gradle.registerMinimalVariantImplementationFactoriesForTests()
        addBuildEventsListenerRegistryMock(project)
        project.plugins.apply(LibraryPlugin::class.java)

        project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheckWhenAndroidIsApplied(
            FixedAndroidGradlePluginVersionProvider(null)
        )

        project.checkDiagnostics("agpCompatibility/androidIsAppliedBeforeCheckerCall")
    }

    @Test
    fun `test - WhenAndroidIsApplied - called multiple times - still emits only a single message`() {
        val project = ProjectBuilder.builder().build()
        project.gradle.registerMinimalVariantImplementationFactoriesForTests()
        addBuildEventsListenerRegistryMock(project)
        project.plugins.apply(LibraryPlugin::class.java)

        repeat(10) {
            project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheckWhenAndroidIsApplied(
                FixedAndroidGradlePluginVersionProvider(null)
            )
        }

        project.checkDiagnostics("agpCompatibility/checkerCalledMultipleTimes")
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

    private fun Project.assertNoWarningMessage() {
        val diagnostics = kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(this)
        if (diagnostics.isNotEmpty()) {
            fail("Expected no warning messages to be emitted. Found: $diagnostics")
        }
    }
}
