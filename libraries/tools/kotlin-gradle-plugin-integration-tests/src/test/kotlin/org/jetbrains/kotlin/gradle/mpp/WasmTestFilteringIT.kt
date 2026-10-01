/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

@MppGradlePluginTests
@DisplayName("Tests for Kotlin/Wasm test filtering")
class WasmTestFilteringIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
        get() = super.defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()

    private val wasmTestTasks = listOf(":wasmJsNodeTest", ":wasmWasiNodeTest", ":wasmWasiWasmtimeTest")

    private fun TestProject.setupSampleSources() {
        buildScriptInjection {
            kotlinMultiplatform.wasmJs {
                nodejs()
            }
            kotlinMultiplatform.wasmWasi {
                nodejs()
                wasmtime()
            }
            kotlinMultiplatform.sourceSets.getByName("commonTest").dependencies {
                implementation(kotlin("test"))
            }
        }

        kotlinSourcesDir("commonTest").apply {
            source("org/example/project/SampleTest.kt") {
                """
                package org.example.project

                import kotlin.test.Test
                import kotlin.test.assertTrue

                class SampleTest {
                    @Test
                    fun testOne() {
                        assertTrue(true)
                    }

                    @Test
                    fun testTwo() {
                        assertTrue(true)
                    }
                }
                """.trimIndent()
            }

            source("org/example/project/OtherTest.kt") {
                """
                package org.example.project

                import kotlin.test.Test
                import kotlin.test.assertTrue

                class OtherTest {
                    @Test
                    fun testOther() {
                        assertTrue(true)
                    }
                }
                """.trimIndent()
            }

            source("org/example/other/ExternalTest.kt") {
                """
                package org.example.other

                import kotlin.test.Test
                import kotlin.test.assertTrue

                class ExternalTest {
                    @Test
                    fun testExternal() {
                        assertTrue(true)
                    }
                }
                """.trimIndent()
            }
        }
    }

    /**
     * Runs all [wasmTestTasks] in a single build, passing each of [testFilters] as `--tests` to every task
     * (a task option applies only to the task preceding it on the command line).
     */
    private fun TestProject.buildWasmTests(vararg testFilters: String) {
        val arguments = wasmTestTasks.flatMap { task ->
            listOf(task) + testFilters.flatMap { listOf("--tests", it) }
        }
        build(*arguments.toTypedArray()) {
            assertTasksExecuted(wasmTestTasks)
        }
    }

    private fun TestProject.assertExecutedWasmTestCases(vararg expectedIds: String) {
        wasmTestTasks.forEach { assertExecutedTestCases(it, *expectedIds) }
    }

    @DisplayName("CLI --tests filtering: class, method, wildcard, and package patterns")
    @GradleTest
    fun testCliTestsFiltering(gradleVersion: GradleVersion) {
        project("base-kotlin-multiplatform-library", gradleVersion) {
            setupSampleSources()

            // Filter by simple class name
            buildWasmTests("SampleTest")
            assertExecutedWasmTestCases(
                "org.example.project.SampleTest#testOne",
                "org.example.project.SampleTest#testTwo",
            )

            // Filter by fully-qualified and simple method names
            buildWasmTests("org.example.project.SampleTest.testOne")
            assertExecutedWasmTestCases(
                "org.example.project.SampleTest#testOne",
            )

            buildWasmTests("SampleTest.testTwo")
            assertExecutedWasmTestCases(
                "org.example.project.SampleTest#testTwo",
            )

            // Filter by wildcard class pattern
            buildWasmTests("*SampleTest")
            assertExecutedWasmTestCases(
                "org.example.project.SampleTest#testOne",
                "org.example.project.SampleTest#testTwo",
            )

            // Filter by package wildcard pattern
            buildWasmTests("org.example.project.*")
            assertExecutedWasmTestCases(
                "org.example.project.SampleTest#testOne",
                "org.example.project.SampleTest#testTwo",
                "org.example.project.OtherTest#testOther",
            )

            // Filter with multiple --tests arguments (union)
            buildWasmTests("*SampleTest", "*ExternalTest")
            assertExecutedWasmTestCases(
                "org.example.project.SampleTest#testOne",
                "org.example.project.SampleTest#testTwo",
                "org.example.other.ExternalTest#testExternal",
            )
        }
    }

    @DisplayName("CLI --tests filtering: no matching tests")
    @GradleTest
    fun testCliTestsNoMatches(gradleVersion: GradleVersion) {
        project("base-kotlin-multiplatform-library", gradleVersion) {
            setupSampleSources()

            // KotlinJsTest sets isFailOnNoMatchingTests = false, so the build succeeds without running any test
            buildWasmTests("NoSuchTest")
            assertExecutedWasmTestCases()
        }
    }

    @DisplayName("Task DSL filter configuration: exclude patterns")
    @GradleTest
    fun testDslExcludePatterns(gradleVersion: GradleVersion) {
        project("base-kotlin-multiplatform-library", gradleVersion) {
            setupSampleSources()

            buildScriptInjection {
                project.tasks.withType(KotlinJsTest::class.java).configureEach {
                    it.filter.excludeTestsMatching("*OtherTest")
                    it.filter.excludeTest("SampleTest", "testTwo")
                }
            }
            buildWasmTests()
            assertExecutedWasmTestCases(
                "org.example.project.SampleTest#testOne",
                "org.example.other.ExternalTest#testExternal",
            )
        }
    }

    @DisplayName("Task DSL filter configuration: include and exclude patterns")
    @GradleTest
    fun testDslIncludeAndExcludePatterns(gradleVersion: GradleVersion) {
        project("base-kotlin-multiplatform-library", gradleVersion) {
            setupSampleSources()

            buildScriptInjection {
                project.tasks.withType(KotlinJsTest::class.java).configureEach {
                    it.filter.includeTestsMatching("org.example.project.*")
                    it.filter.excludeTestsMatching("*OtherTest")
                }
            }
            buildWasmTests()
            assertExecutedWasmTestCases(
                "org.example.project.SampleTest#testOne",
                "org.example.project.SampleTest#testTwo",
            )
        }
    }

    @DisplayName("CLI --tests filtering combined with task DSL exclude patterns")
    @GradleTest
    fun testCliIncludeWithDslExclude(gradleVersion: GradleVersion) {
        project("base-kotlin-multiplatform-library", gradleVersion) {
            setupSampleSources()

            buildScriptInjection {
                project.tasks.withType(KotlinJsTest::class.java).configureEach {
                    it.filter.excludeTest("SampleTest", "testTwo")
                }
            }
            buildWasmTests("org.example.project.*")
            assertExecutedWasmTestCases(
                "org.example.project.SampleTest#testOne",
                "org.example.project.OtherTest#testOther",
            )
        }
    }
}
