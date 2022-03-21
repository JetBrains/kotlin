/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.report.BuildReportType
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText

@DisplayName("Build statistics")
@JvmGradlePluginTests
class BuildStatisticsIT : KGPBaseTest() {

    @DisplayName("Http build report url problems are logged only ones")
    @GradleTest
    fun testHttpServiceWithInvalidUrl(gradleVersion: GradleVersion) {
        project("incrementalMultiproject", gradleVersion) {
            enableStatisticReports(BuildReportType.HTTP, "invalid/url")
            build("assemble") {
                assertOutputContainsExactTimes("Unable to open connection to")
            }
        }
    }

    @DisplayName("Http build service should be registered")
    @GradleTest
    fun testHttpReport(gradleVersion: GradleVersion) {
        project("incrementalMultiproject", gradleVersion) {
            enableStatisticReports(BuildReportType.HTTP, "invalid/url")
            build("assemble", buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertOutputContains("Statistics http service is registered")
            }
        }
    }

    @DisplayName("Build scan listener should be registered")
    @GradleTest
    fun testBuildScanReport(gradleVersion: GradleVersion) {
        val options = defaultBuildOptions.copy(buildReport = listOf(BuildReportType.BUILD_SCAN), logLevel = LogLevel.DEBUG)
        project("incrementalMultiproject", gradleVersion, buildOptions = options) {
            settingsGradle.appendText(
                """
                gradleEnterprise {
                    server = "https://invalid"
                    buildScan {
                        termsOfServiceUrl = "https://gradle.com/terms-of-service"
                        termsOfServiceAgree = "yes"
                    }
                }
                """.trimIndent()
            )

            build("assemble", "--scan") {
                assertOutputContains("Statistics build scan listener is registered")
                assertOutputContainsExactTimes("Report statistic to build scan takes", 2)
            }
        }
    }

    @DisplayName("Build without build reports")
    @GradleTest
    fun testBuildWithoutBuildReport(gradleVersion: GradleVersion) {
        project("incrementalMultiproject", gradleVersion) {
            build("assemble") {
                assertOutputDoesNotContain("Statistics build scan listener is registered")
                assertOutputDoesNotContain("Statistics http service is registered")
            }
        }
    }

}