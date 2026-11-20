/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.configuration.WarningMode
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInGradle
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertEquals
import kotlin.test.assertNull

@DisplayName("Gradle warnings detector plugin")
@MustRunOnChangesInGradle
@OtherGradlePluginTests
class GradleWarningsDetectorIT : KGPBaseTest() {

    @DisplayName("Build without deprecation warnings passes")
    @GradleTest
    fun testNoDeprecations(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            build("help") {
                assertOutputContains(PLUGIN_APPLIED)
                assertNull(reportedDeprecations())
            }
        }
    }

    @DisplayName("Configuration time deprecation warning fails the build")
    @GradleTest
    fun testConfigurationTimeDeprecation(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            emitDeprecationAtConfigurationTime()
            buildAndFail("help") {
                assertConfigurationCacheStored()
                assertDetectorFailedBuild(CONFIGURATION_TIME_DEPRECATION)
            }

            buildAndFail("help") {
                assertConfigurationCacheReused()
                assertDetectorFailedBuild(CONFIGURATION_TIME_DEPRECATION)
            }
        }
    }

    @DisplayName("Deprecation warning fails the build regardless of the warning mode")
    @GradleTest
    fun testDeprecationWithWarningModeNone(gradleVersion: GradleVersion) {
        project(
            "empty",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(warningMode = WarningMode.None),
        ) {
            emitDeprecationAtConfigurationTime()
            buildAndFail("help") {
                assertDetectorFailedBuild(CONFIGURATION_TIME_DEPRECATION)
            }
        }
    }

    @DisplayName("Deprecation warning doesn't fail the build with the summary warning mode")
    @GradleTest
    fun testDeprecationWithWarningModeSummary(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            emitDeprecationAtConfigurationTime()
            build("help", buildOptions = buildOptions.copy(warningMode = WarningMode.Summary)) {
                assertReportedDeprecations(CONFIGURATION_TIME_DEPRECATION)
            }

            // The configuration cache entry stored with the summary warning mode must not be reused
            buildAndFail("help") {
                assertDetectorFailedBuild(CONFIGURATION_TIME_DEPRECATION)
            }
        }
    }

    @DisplayName("Execution time deprecation warning fails the build with configuration cache")
    @GradleTest
    fun testExecutionTimeDeprecationWithConfigurationCache(gradleVersion: GradleVersion) {
        project(
            "empty",
            gradleVersion,
        ) {
            registerTaskEmittingDeprecation()

            buildAndFail(EMIT_DEPRECATION_TASK) {
                assertConfigurationCacheStored()
                assertOutputContains(PLUGIN_APPLIED)
                assertDetectorFailedBuild(EXECUTION_TIME_DEPRECATION)
            }

            // Gradle doesn't apply settings plugins on a configuration cache hit, the detector is restored from the entry
            buildAndFail(EMIT_DEPRECATION_TASK) {
                assertConfigurationCacheReused()
                assertOutputDoesNotContain(PLUGIN_APPLIED)
                assertDetectorFailedBuild(EXECUTION_TIME_DEPRECATION)
            }
        }
    }

    private fun TestProject.emitDeprecationAtConfigurationTime() {
        buildGradle.append(nagUser(CONFIGURATION_TIME_DEPRECATION))
    }

    private fun TestProject.registerTaskEmittingDeprecation() {
        buildGradle.append(
            """
            |tasks.register("$EMIT_DEPRECATION_TASK") {
            |    doLast {
            |        ${nagUser(EXECUTION_TIME_DEPRECATION)}
            |    }
            |}
            """.trimMargin()
        )
    }

    private fun nagUser(action: String) =
        "org.gradle.internal.deprecation.DeprecationLogger.deprecateAction(\"$action\")" +
                ".willBecomeAnErrorInGradle10().undocumented().nagUser()"

    private fun BuildResult.reportedDeprecations(): Set<String>? {
        val lines = output.lines()
        val reportStart = lines.indexOfFirst { it.endsWith(REPORT_HEADER) }
        if (reportStart == -1) return null
        return lines
            .drop(reportStart + 1)
            .takeWhile { it.startsWith(DEPRECATION_PREFIX) }
            .toSet()
    }

    private fun BuildResult.assertReportedDeprecations(vararg expectedActions: String) {
        assertEquals(
            expectedActions.map { "$it has been deprecated." }.toSet(),
            // The removal details are worded differently between Gradle versions: "Gradle 10.0." vs "Gradle 10."
            reportedDeprecations()?.map { it.substringBefore(" This will fail with an error in Gradle") }?.toSet(),
        )
    }

    private fun BuildResult.assertDetectorFailedBuild(vararg expectedActions: String) {
        assertReportedDeprecations(*expectedActions)
        assertEquals(
            "Unexpected warnings were detected in the build!",
            output.lines().dropWhile { it != "* What went wrong:" }.drop(1).firstOrNull(),
        )
    }

    private companion object {
        const val EMIT_DEPRECATION_TASK = "emitDeprecation"
        const val DEPRECATION_PREFIX = "KT-88825"
        const val CONFIGURATION_TIME_DEPRECATION = "$DEPRECATION_PREFIX configuration time action"
        const val EXECUTION_TIME_DEPRECATION = "$DEPRECATION_PREFIX execution time action"
        const val PLUGIN_APPLIED = "[GradleWarningsDetectorPlugin] The plugin is being applied"
        const val REPORT_HEADER = "[GradleWarningsDetectorPlugin] Deprecation warnings were found during this build:"
    }
}
