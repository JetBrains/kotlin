/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.capitalize
import org.junit.jupiter.api.DisplayName

@MppGradlePluginTests
@DisplayName("Tests for aggregating kotlin test reports")
class AggregatingKotlinTestReportIT : KGPBaseTest() {
    @DisplayName("KT-54506: `allTests` is not false positively up-to-date after failure")
    @GradleTest
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

            val nativeTarget = MPPNativeTargets.current
            val capitalizedNativeTarget = nativeTarget.capitalize()

            buildAndFail(":allTests") {
                // expect only an aggregate test task fail
                assertTasksExecuted(
                    ":compileKotlinJs",
                    ":compileTestKotlinJs",
                    ":jsTest",
                    ":compileKotlinJvmWithoutJava",
                    ":compileTestKotlinJvmWithoutJava",
                    ":jvmWithoutJavaTest",
                    ":compileKotlin$capitalizedNativeTarget",
                    ":compileTestKotlin$capitalizedNativeTarget",
                    ":${nativeTarget}Test"
                )
                assertTasksFailed(":allTests")
                assertNoTestsStateFileException()
            }

            buildAndFail(":allTests") {
                // still expect only an aggregate test task fail
                assertTasksUpToDate(
                    ":compileKotlinJvmWithoutJava",
                    ":compileTestKotlinJvmWithoutJava",
                    ":compileKotlinJs",
                    ":compileTestKotlinJs",
                    ":compileKotlin$capitalizedNativeTarget",
                    ":compileTestKotlin$capitalizedNativeTarget",
                )
                assertTasksExecuted(
                    ":jvmWithoutJavaTest",
                    ":jsTest",
                    ":${nativeTarget}Test",
                )
                assertTasksFailed(":allTests")
                assertNoTestsStateFileException()
            }

            buildAndFail(":allTests") {
                // still expect only an aggregate test task fail
                assertTasksUpToDate(
                    ":compileKotlinJvmWithoutJava",
                    ":compileTestKotlinJvmWithoutJava",
                    ":compileKotlinJs",
                    ":compileTestKotlinJs",
                    ":compileKotlin$capitalizedNativeTarget",
                    ":compileTestKotlin$capitalizedNativeTarget",
                )
                assertTasksExecuted(
                    ":jvmWithoutJavaTest",
                    ":jsTest",
                    ":${nativeTarget}Test",
                )
                assertTasksFailed(":allTests")
                assertNoTestsStateFileException()
            }

            buildAndFail(":jvmWithoutJavaTest") {
                // expect that a single test task invocation also still fails
                assertTasksUpToDate(
                    ":compileKotlinJvmWithoutJava",
                    ":compileTestKotlinJvmWithoutJava",
                )
                assertTasksFailed(
                    ":jvmWithoutJavaTest",
                )
                assertNoTestsStateFileException()
            }
        }
    }

    @DisplayName("Test tasks are up-to-date when called after `allTests`")
    @GradleTest
    fun testSuccessfulTestTasksAreUpToDate(gradleVersion: GradleVersion) {
        project(
            "new-mpp-lib-with-tests",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                freeArgs = listOf("-Pkotlin.tests.individualTaskReports=false"),
                // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
                isolatedProjects = BuildOptions.IsolatedProjectsMode.DISABLED,
            )
        ) {
            val nativeTarget = MPPNativeTargets.current

            build(":allTests") {
                assertTasksExecuted(
                    ":jsNodeTest",
                    ":jsTest",
                    ":jvmWithoutJavaTest",
                    ":${nativeTarget}Test",
                    ":allTests",
                )
                assertNoTestsStateFileException()
            }

            build(":allTests") {
                assertTasksUpToDate(
                    ":jsNodeTest",
                    ":jsTest",
                    ":jvmWithoutJavaTest",
                    ":${nativeTarget}Test",
                    ":allTests",
                )
                assertNoTestsStateFileException()
            }

            build(":jvmWithoutJavaTest") {
                assertTasksUpToDate(
                    ":jvmWithoutJavaTest",
                )
                assertNoTestsStateFileException()
            }

            build(":jsTest") {
                assertTasksUpToDate(
                    ":jsNodeTest",
                )
                assertNoTestsStateFileException()
            }

            build(":${nativeTarget}Test") {
                assertTasksUpToDate(
                    ":${nativeTarget}Test",
                )
                assertNoTestsStateFileException()
            }
        }
    }
}

private fun BuildResult.assertNoTestsStateFileException() {
    // KT-55134
    assertOutputDoesNotContain("Cannot read test tasks state from")
    assertOutputDoesNotContain("Cannot store test tasks state into")
}
