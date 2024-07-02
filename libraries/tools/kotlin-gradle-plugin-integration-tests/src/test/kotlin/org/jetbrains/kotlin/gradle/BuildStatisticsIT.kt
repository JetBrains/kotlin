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

@DisplayName("Build statistics")
@JvmGradlePluginTests
class BuildStatisticsIT : KGPBaseTest() {

    @DisplayName("Http build report url problems are logged only ones")
    @GradleTest
    fun testHttpServiceWithInvalidUrl(gradleVersion: GradleVersion) {
        project("incrementalMultiproject", gradleVersion) {
            enableStatisticReports(BuildReportType.HTTP, "invalid/url")
            build("assemble") {
                assertOutputContainsExactlyTimes("Http report: Unable to open connection to")
            }
        }
    }

    @DisplayName("invalid http host")
    @GradleTest
    fun testHttpReportWithUnknownHost(gradleVersion: GradleVersion) {
        project("incrementalMultiproject", gradleVersion) {
            enableStatisticReports(BuildReportType.HTTP, "https://invalid")
            build("compileKotlin", "-Pkotlin.internal.build.report.http.use.executor=false", buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertOutputContainsExactlyTimes("Http report: Unexpected exception happened: ", 6) //twice for every module and at the end of the build
            }
        }
    }

}