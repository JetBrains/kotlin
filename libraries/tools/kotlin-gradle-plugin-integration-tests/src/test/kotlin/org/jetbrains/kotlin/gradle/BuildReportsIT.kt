/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.internal.build.metrics.GradleBuildMetricsData
import org.jetbrains.kotlin.gradle.report.BuildReportType
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import java.io.ObjectInputStream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@DisplayName("Build reports")
@JvmGradlePluginTests
class BuildReportsIT : KGPBaseTest() {
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(
            buildReport = listOf(BuildReportType.FILE)
        )

    @DisplayName("Build report is created")
    @GradleTest
    fun testBuildReportSmokeTest(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            build("assemble") {
                assertBuildReportPathIsPrinted()
            }

            build("clean", "assemble") {
                assertBuildReportPathIsPrinted()
            }
        }
    }

    @DisplayName("Build report output property accepts only certain values")
    @GradleTest
    fun testBuildReportOutputProperty(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            buildAndFail("assemble", "-Pkotlin.build.report.output=file,invalid") {
                assertOutputContains("Unknown output type:")
            }
        }
    }

    @DisplayName("Build metrics produces valid report")
    @GradleTest
    fun testBuildMetricsSmokeTest(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            build("assemble") {
                assertBuildReportPathIsPrinted()
            }
            val reportFolder = projectPath.resolve("build/reports/kotlin-build").toFile()
            val reports = reportFolder.listFiles()
            assertNotNull(reports)
            assertEquals(1, reports.size)
            val report = reports[0].readText()

            //Should contains build metrics for all compile kotlin tasks
            assertTrue { report.contains("Time metrics:") }
            assertTrue { report.contains("Run compilation:") }
            assertTrue { report.contains("Incremental compilation in daemon:") }
            assertTrue { report.contains("Size metrics:") }
            assertTrue { report.contains("Total size of the cache directory:") }
            assertTrue { report.contains("Total compiler iteration:") }
            assertTrue { report.contains("ABI snapshot size:") }
            //for non-incremental builds
            assertTrue { report.contains("Build attributes:") }
            assertTrue { report.contains("REBUILD_REASON:") }
        }
    }

    @DisplayName("Compiler build metrics report is produced")
    @GradleTest
    fun testCompilerBuildMetricsSmokeTest(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            build("assemble") {
                assertBuildReportPathIsPrinted()
            }
            val reportFolder = projectPath.resolve("build/reports/kotlin-build").toFile()
            val reports = reportFolder.listFiles()
            assertNotNull(reports)
            assertEquals(1, reports.size)
            val report = reports[0].readText()
            assertTrue { report.contains("Compiler code analysis:") }
            assertTrue { report.contains("Compiler code generation:") }
            assertTrue { report.contains("Compiler initialization time:") }
        }
    }

    @DisplayName("smoke")
    @GradleTest
    fun testSingleBuildMetricsFileSmoke(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            val metricsFile = projectPath.resolve("metrics.bin").toFile()
            build(
                "compileKotlin",
                "-Pkotlin.build.report.output=SINGLE_FILE",
                "-Pkotlin.build.report.single_file=${metricsFile.absolutePath}"
            )

            assertTrue { metricsFile.exists() }
            // test whether we can deserialize data from the file
            ObjectInputStream(metricsFile.inputStream().buffered()).use { input ->
                input.readObject() as GradleBuildMetricsData
            }
        }
    }

    @DisplayName("custom value limit")
    @GradleTest
    fun testCustomValueLimitForBuildScan(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion, buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)) {
            build(
                "compileKotlin",
                "-Pkotlin.build.report.output=BUILD_SCAN",
                "-Pkotlin.build.report.build_scan.custom_values_limit=0",
                "--scan"
            ) {
                assertOutputContains("Can't add any more custom values into build scan")
            }
        }
    }

    @DisplayName("build scan listener lazy initialisation")
    @GradleTest
    fun testBuildScanListenerLazyInitialisation(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion, buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)) {
            build(
                "compileKotlin",
                "-Pkotlin.build.report.output=BUILD_SCAN",
                "-Pkotlin.build.report.build_scan.custom_values_limit=0",
            ) {
                assertOutputDoesNotContain("Can't add any more custom values into build scan")
            }
        }
    }
}
