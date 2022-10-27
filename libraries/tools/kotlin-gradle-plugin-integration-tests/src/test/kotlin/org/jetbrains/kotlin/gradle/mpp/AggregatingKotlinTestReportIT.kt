/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName

@MppGradlePluginTests
@DisplayName("Tests for aggregating kotlin test reports")
class AggregatingKotlinTestReportIT : KGPBaseTest() {
    @DisplayName("KT-54506: `allTests` is not false positively up-to-date after failure")
    @GradleTest
    @Disabled
    fun testFailedTestsAreNotUpToDate(gradleVersion: GradleVersion) {
        project(
            "new-mpp-lib-with-tests",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(freeArgs = listOf("-Pkotlin.tests.individualTaskReports=false"))
        ) {
            // break all the test tasks
            kotlinSourcesDir("commonTest").resolve("TestCommonCode.kt").modify {
                it.replace("expectedFun()", "assertEquals(0, 1)")
            }

            buildAndFail(":allTests") {
                // expect only an aggregate test task fail
                assertTasksExecuted(
                    ":compileKotlinJs",
                    ":compileTestKotlinJs",
                    ":jsTest",
                    ":compileKotlinJvmWithoutJava",
                    ":compileTestKotlinJvmWithoutJava",
                    ":jvmWithoutJavaTest",
                )
                assertTasksFailed(":allTests")
            }

            buildAndFail(":allTests") {
                // still expect only an aggregate test task fail
                assertTasksUpToDate(
                    ":compileKotlinJvmWithoutJava",
                    ":compileTestKotlinJvmWithoutJava",
                    ":compileKotlinJs",
                    ":compileTestKotlinJs",
                )
                assertTasksExecuted(
                    ":jvmWithoutJavaTest",
                    ":jsTest",
                )
                assertTasksFailed(":allTests")
            }

            buildAndFail(":jvmWithoutJavaTest") {
                // expect a single test task still fails after an aggregate test task fail
                assertTasksUpToDate(
                    ":compileKotlinJvmWithoutJava",
                    ":compileTestKotlinJvmWithoutJava",
                )
                assertTasksFailed(
                    ":jvmWithoutJavaTest",
                )
            }
        }
    }
}