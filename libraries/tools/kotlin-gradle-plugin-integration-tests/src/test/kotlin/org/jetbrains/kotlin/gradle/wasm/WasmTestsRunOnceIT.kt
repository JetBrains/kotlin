/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.wasm

import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertEquals

/**
 * Wasm test runners start the test process twice: first a dry run with `--dryRun`, of which only the exit code is checked,
 * then the real run, whose output is reported.
 * If kotlin.test misreads its arguments, test bodies are also executed by the dry run, or a `--tests` filter is ignored.
 * Neither is visible in the test report, so every test body here prints a marker to stderr,
 * which reaches the build output from both processes.
 */
@MppGradlePluginTests
@DisplayName("Kotlin/Wasm tests are executed exactly once")
class WasmTestsRunOnceIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
        get() = super.defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()

    @DisplayName("wasmJs Node.js tests are executed exactly once")
    @GradleTest
    fun testWasmJsNode(gradleVersion: GradleVersion) {
        testTestsRunOnce(gradleVersion, ":wasmJsNodeTest")
    }

    @DisplayName("wasmJs D8 tests are executed exactly once")
    @GradleTest
    fun testWasmJsD8(gradleVersion: GradleVersion) {
        testTestsRunOnce(gradleVersion, ":wasmJsD8Test")
    }

    @DisplayName("wasmWasi Node.js tests are executed exactly once")
    @GradleTest
    fun testWasmWasiNode(gradleVersion: GradleVersion) {
        testTestsRunOnce(gradleVersion, ":wasmWasiNodeTest")
    }

    @DisplayName("wasmWasi Wasmtime tests are executed exactly once")
    @GradleTest
    fun testWasmWasiWasmtime(gradleVersion: GradleVersion) {
        testTestsRunOnce(gradleVersion, ":wasmWasiWasmtimeTest")
    }

    private fun testTestsRunOnce(gradleVersion: GradleVersion, testTask: String) {
        project("base-kotlin-multiplatform-library", gradleVersion) {
            setupProject()

            build(testTask) {
                assertTasksExecuted(testTask)
                assertEquals(listOf("RunOnceTest.testOne", "RunOnceTest.testTwo"), executedTestBodies())
            }
            // Not a set, so that duplicated test cases are visible
            assertEquals(
                listOf("org.example.RunOnceTest#testOne", "org.example.RunOnceTest#testTwo"),
                readTestCases(testTask).map { it.id }.sorted(),
            )

            build(testTask, "--tests", "org.example.RunOnceTest.testOne") {
                assertTasksExecuted(testTask)
                assertEquals(listOf("RunOnceTest.testOne"), executedTestBodies())
            }
            assertEquals(listOf("org.example.RunOnceTest#testOne"), readTestCases(testTask).map { it.id })
        }
    }

    private fun BuildResult.executedTestBodies(): List<String> =
        testBodyMarkerRegex.findAll(output).map { it.groupValues[1] }.sorted().toList()

    @OptIn(ExperimentalWasmDsl::class)
    private fun TestProject.setupProject() {
        buildScriptInjection {
            kotlinMultiplatform.wasmJs {
                nodejs()
                d8()
            }
            kotlinMultiplatform.wasmWasi {
                nodejs()
                wasmtime()
            }
            kotlinMultiplatform.sourceSets.getByName("commonTest").dependencies {
                implementation(kotlin("test"))
            }
            // The real run merges stderr into the test output, which is not logged by default
            project.tasks.withType(AbstractTestTask::class.java).configureEach { task ->
                task.testLogging {
                    it.get(DEFAULT_LOG_LEVEL).showStandardStreams = true
                }
            }
        }

        kotlinSourcesDir("commonTest").source("org/example/RunOnceTest.kt") {
            """
            package org.example

            import kotlin.test.Test

            expect fun reportExecution(testName: String)

            class RunOnceTest {
                @Test
                fun testOne() {
                    reportExecution("RunOnceTest.testOne")
                }

                @Test
                fun testTwo() {
                    reportExecution("RunOnceTest.testTwo")
                }
            }
            """.trimIndent()
        }

        kotlinSourcesDir("wasmJsTest").source("org/example/reportExecution.kt") {
            """
            package org.example

            private fun consoleError(message: String): Unit = js("console.error(message)")

            actual fun reportExecution(testName: String) {
                consoleError("$TEST_BODY_MARKER" + testName)
            }
            """.trimIndent()
        }

        kotlinSourcesDir("wasmWasiTest").source("org/example/reportExecution.kt") {
            """
            @file:OptIn(ExperimentalWasmInterop::class, UnsafeWasmMemoryApi::class)

            package org.example

            import kotlin.wasm.ExperimentalWasmInterop
            import kotlin.wasm.WasmImport
            import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
            import kotlin.wasm.unsafe.withScopedMemoryAllocator

            private const val STDERR = 2

            @WasmImport("wasi_snapshot_preview1", "fd_write")
            private external fun wasiFdWrite(descriptor: Int, scatterPtr: Int, scatterSize: Int, resultPtr: Int): Int

            actual fun reportExecution(testName: String) {
                val bytes = ("$TEST_BODY_MARKER" + testName + "\n").encodeToByteArray()
                withScopedMemoryAllocator { allocator ->
                    val data = allocator.allocate(bytes.size)
                    for (i in bytes.indices) {
                        (data + i).storeByte(bytes[i])
                    }
                    val scatter = allocator.allocate(8)
                    scatter.storeInt(data.address.toInt())
                    (scatter + 4).storeInt(bytes.size)
                    val written = allocator.allocate(4)
                    val result = wasiFdWrite(STDERR, scatter.address.toInt(), 1, written.address.toInt())
                    check(result == 0) { "fd_write failed with error code " + result }
                }
            }
            """.trimIndent()
        }
    }

    companion object {
        private const val TEST_BODY_MARKER = "TEST_BODY_EXECUTED: "
        private val testBodyMarkerRegex = Regex(TEST_BODY_MARKER + "([\\w.]+)")
    }
}
