/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Timeout
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

@MppGradlePluginTests
class TestCancellationIT : KGPBaseTest() {

    /**
     * KGP launches Native, JS, and Wasm tests as external processes.
     * When the tests-tasks time out KGP must cancel these processes.
     */
    @GradleTest
    @TestMetadata("kmp-test-timeout")
    @Timeout(value = 10, unit = TimeUnit.MINUTES)
    fun `when test task has timeout, expect test processes are terminated`(
        gradleVersion: GradleVersion,
    ) {

        project("kmp-test-timeout", gradleVersion) {

            val kmpTasksThatUseExecHandle =
                fetchKmpTestTasksThatUseExecHandle()

            buildScriptInjection {
                project.tasks.withType(AbstractTestTask::class.java).configureEach { task ->

                    // Configure a short timeout for all test tasks.
                    // The tests have a delay so they run for 10 seconds.
                    // When Gradle cancels the task, we expect the ExecHandle process is aborted.
                    task.timeout.set(Duration.ofSeconds(5))

                    task.doFirst {
                        // log the timeout so we can verify the timeout is set
                        println("TestCancellationIT timeout for ${task.path} is ${task.timeout.orNull}")
                    }

                    task.testLogging {
                        // enable all test output logs, to help with diagnosing test failures
                        it.events.addAll(TestLogEvent.entries)
                    }
                }
            }

            buildAndFail(
                // use 'check' to run all KMP test tasks
                "check",
                // we want KMP test tasks to fail, and also check that one task failing doesn't interfere with
                "--continue",
                "--configuration-cache", // for parallel tasks
            ) {
                assertEquals(
                    kmpTasksThatUseExecHandle
                        .map { "TestCancellationIT timeout for $it is PT5S" }
                        .sorted()
                        .joinToString("\n"),
                    output.lines()
                        .filter { it.startsWith("TestCancellationIT timeout") }
                        .filter { kmpTasksThatUseExecHandle.any { task -> "$task " in it } }
                        .sorted()
                        .joinToString("\n"),
                    message = "The KMP test tasks timeout must be configured for $kmpTasksThatUseExecHandle.",
                )

                assertTasksFailed(kmpTasksThatUseExecHandle)

                assertEquals(
                    kmpTasksThatUseExecHandle
                        .map { "[ExecHandle $it] finished with exit value 143 (state: Aborted)" }
                        .sorted()
                        .joinToString("\n"),
                    output.lines()
                        .filter { kmpTasksThatUseExecHandle.any { t -> it.startsWith("[ExecHandle $t] finished") } }
                        .sorted()
                        .joinToString("\n"),
                    message = "All KMP tasks that use ExecHandle must be aborted $kmpTasksThatUseExecHandle.",
                )
            }
        }
    }

    companion object {
        /**
         * Fetch enabled KMP tasks that use ExecHandle.
         *
         * We must query the buildscript because test tasks are dynamically enabled based on the host machine.
         */
        private fun TestProject.fetchKmpTestTasksThatUseExecHandle(): List<String> {

            // Fetch enabled KMP tests that use ExecHandle to launch the tests.
            // Currently, this is only Native and JS tests.
            // Native test tasks are dynamically enabled based on the host machine.
            val enabledKotlinNativeTestPaths: List<String> =
                buildScriptReturn {
                    project.tasks.withType(KotlinNativeTest::class.java)
                        .matching { it.enabled }
                        .map { it.path }
                }.buildAndReturn(executingProject = this)

            val enabledKotlinJsTestPaths: List<String> =
                buildScriptReturn {
                    project.tasks.withType(KotlinJsTest::class.java)
                        .matching { it.enabled }
                        .map { it.path }
                }.buildAndReturn(executingProject = this)

            return buildList {
                addAll(enabledKotlinNativeTestPaths)
                addAll(enabledKotlinJsTestPaths)

                require(isNotEmpty()) {
                    "Expected some KMP task paths, but got none"
                }

                require(konanTargetPrettyNames.any { it in this }) {
                    "Must have at least one enabled Kotlin Native test task, but found none in ${joinToString()}"
                }
            }.sorted()
        }

        private val konanTargetPrettyNames: List<String> =
            KonanTarget.predefinedTargets.values.map { it.prettyName }

        private val KonanTarget.prettyName: String
            get() = name.split("_").joinToString { it.replaceFirstChar(Char::uppercaseChar) }
    }
}
