/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.capitalize
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInGradle
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText

@MppGradlePluginTests
@DisplayName("Tests for aggregating kotlin test reports")
@MustRunOnChangesInGradle
class AggregatingKotlinTestReportIT : KGPBaseTest() {
    private fun TestProject.breakCommonTest() {
        kotlinSourcesDir("commonTest").resolve("TestCommonCode.kt").modify {
            it.replace("expectedFun()", "assertEquals(0, 1)")
        }
    }

    private fun TestProject.breakJvmTest() {
        kotlinSourcesDir("jvmWithoutJavaTest").resolve("TestWithoutJava.kt").modify {
            it.replace("assertFalse(isJavaThere())", "assertEquals(0, 1)")
        }
    }

    private val nativeTestTask: String
        get() = ":${MPPNativeTargets.current}Test"

    private val testTaskPaths: List<String>
        get() = listOf(":jsNodeTest", ":jvmWithoutJavaTest", nativeTestTask)

    @TestMetadata("new-mpp-lib-with-tests")
    @DisplayName("KT-54506: failing test tasks are rerun and never up-to-date")
    @GradleTest
    fun testFailedTestsAreNotUpToDate(gradleVersion: GradleVersion) {
        project(
            "new-mpp-lib-with-tests",
            gradleVersion,
            buildOptions = defaultBuildOptions
                .copy(buildCacheEnabled = true) // KT-90039: a failed execution must never be restored as a success
                .disableIsolatedProjectsBecauseOfJsAndWasmKT75899(),
        ) {
            // break all the test tasks
            breakCommonTest()

            val capitalizedNativeTarget = MPPNativeTargets.current.capitalize()
            val continueAfterFailure = buildOptions.copy(continueAfterFailure = true)

            buildAndFail(":allTests", buildOptions = continueAfterFailure) {
                // with --continue, every test task runs and fails on its own; the
                // aggregates are wired to the test tasks through their binary results
                // output only, so they still run and generate the reports
                assertTasksFailed(testTaskPaths)
                assertTasksExecuted(":jsTest", ":allTests")
                assertFileExists(projectPath.resolve("build/reports/tests/allTests/index.html"))
            }

            buildAndFail(":allTests", buildOptions = continueAfterFailure) {
                // KT-54506: a failed test task is never up-to-date (and KT-90039:
                // never restored from the build cache) and is re-executed
                assertTasksUpToDate(
                    ":compileKotlinJvmWithoutJava",
                    ":compileTestKotlinJvmWithoutJava",
                    ":compileKotlinJs",
                    ":compileTestKotlinJs",
                    ":compileKotlin$capitalizedNativeTarget",
                    ":compileTestKotlin$capitalizedNativeTarget",
                )
                assertTasksFailed(testTaskPaths)
                // the re-executed test tasks rewrite their binary results,
                // so the aggregates are not up-to-date and re-execute
                assertTasksExecuted(":jsTest", ":allTests")
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
            }
        }
    }

    @TestMetadata("new-mpp-lib-with-tests")
    @DisplayName("A failing test task fails the build and the aggregate is skipped")
    @GradleTest
    fun testFailingTestTaskFailsCheck(gradleVersion: GradleVersion) {
        project(
            "new-mpp-lib-with-tests",
            gradleVersion,
            buildOptions = defaultBuildOptions
                .disableIsolatedProjectsBecauseOfJsAndWasmKT75899(),
        ) {
            // break all the test tasks
            breakCommonTest()

            buildAndFail("check") {
                assertAnyTaskFailed(*testTaskPaths.toTypedArray())
                assertTasksAreNotInTaskGraph(":allTests")
            }
        }
    }

    @TestMetadata("new-mpp-lib-with-tests")
    @DisplayName("Aggregated report is generated when a test task fails with --continue")
    @GradleTest
    fun testAggregatedReportWithFailingTest(gradleVersion: GradleVersion) {
        project(
            "new-mpp-lib-with-tests",
            gradleVersion,
            buildOptions = defaultBuildOptions
                .disableIsolatedProjectsBecauseOfJsAndWasmKT75899(),
        ) {
            breakJvmTest()

            buildAndFail("check", buildOptions = buildOptions.copy(continueAfterFailure = true)) {
                // the broken test task fails the build on its own,
                // while the tests of the other targets pass
                assertTasksFailed(":jvmWithoutJavaTest")
                assertTasksExecuted(":jsNodeTest", nativeTestTask)
                // the aggregates are wired to the test tasks through their binary
                // results output only, so they still run despite the verification
                // failure and generate the reports; `check` is an outcome dependency
                // of the failed test task and is skipped
                assertTasksExecuted(":jsTest", ":allTests")
                assertTasksAreNotInTaskGraph(":check")
                assertFileExists(projectPath.resolve("build/reports/tests/allTests/index.html"))
                assertFileExists(projectPath.resolve("build/reports/tests/jsTest/index.html"))
            }
        }
    }

    @TestMetadata("new-mpp-lib-with-tests")
    @DisplayName("Aggregated report is generated when test failures are ignored")
    @GradleTest
    fun testAggregatedReportWithIgnoredFailures(gradleVersion: GradleVersion) {
        project(
            "new-mpp-lib-with-tests",
            gradleVersion,
            buildOptions = defaultBuildOptions
                .disableIsolatedProjectsBecauseOfJsAndWasmKT75899(),
        ) {
            breakJvmTest()
            buildGradle.appendText(
                """
                |
                |tasks.named('jvmWithoutJavaTest') { ignoreFailures = true }
                """.trimMargin()
            )

            build(":allTests") {
                assertTasksExecuted(
                    ":jsNodeTest",
                    ":jsTest",
                    ":jvmWithoutJavaTest",
                    nativeTestTask,
                    ":allTests",
                )
                assertFileExists(projectPath.resolve("build/reports/tests/allTests/index.html"))
            }
        }
    }

    @TestMetadata("new-mpp-lib-with-tests")
    @DisplayName("Test tasks are up-to-date when called after `allTests`")
    @GradleTest
    fun testSuccessfulTestTasksAreUpToDate(gradleVersion: GradleVersion) {
        project(
            "new-mpp-lib-with-tests",
            gradleVersion,
            buildOptions = defaultBuildOptions
                .disableIsolatedProjectsBecauseOfJsAndWasmKT75899(),
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
                assertFileExists(projectPath.resolve("build/reports/tests/allTests/index.html"))
            }

            build(":allTests") {
                assertTasksUpToDate(
                    ":jsNodeTest",
                    ":jsTest",
                    ":jvmWithoutJavaTest",
                    ":${nativeTarget}Test",
                    ":allTests",
                )
            }

            build(":jvmWithoutJavaTest") {
                assertTasksUpToDate(
                    ":jvmWithoutJavaTest",
                )
            }

            build(":jsTest") {
                assertTasksUpToDate(
                    ":jsNodeTest",
                )
            }

            build(":${nativeTarget}Test") {
                assertTasksUpToDate(
                    ":${nativeTarget}Test",
                )
            }
        }
    }
}
