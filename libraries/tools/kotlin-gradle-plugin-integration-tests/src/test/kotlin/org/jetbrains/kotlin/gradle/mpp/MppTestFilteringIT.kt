/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.api.tasks.testing.Test
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

@MppGradlePluginTests
@DisplayName("Tests for Multiplatform test filtering")
class MppTestFilteringIT : KGPBaseTest() {

    private fun TestProject.setupSampleSources(includeJs: Boolean = false) {
        buildScriptInjection {
            kotlinMultiplatform.jvm()
            if (includeJs) {
                kotlinMultiplatform.js {
                    nodejs()
                }
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

    @DisplayName("CLI --tests filtering: class, method, wildcard, and package patterns")
    @GradleTest
    fun testCliTestsFiltering(gradleVersion: GradleVersion) {
        project("base-kotlin-multiplatform-library", gradleVersion) {
            setupSampleSources()

            // Filter by simple class name
            build(":jvmTest", "--tests", "SampleTest") {
                assertTasksExecuted(":jvmTest")
            }
            assertExecutedTestCases(
                ":jvmTest",
                "org.example.project.SampleTest#testOne",
                "org.example.project.SampleTest#testTwo",
            )

            // Filter by fully-qualified and simple method names
            build(":jvmTest", "--tests", "org.example.project.SampleTest.testOne") {
                assertTasksExecuted(":jvmTest")
            }
            assertExecutedTestCases(
                ":jvmTest",
                "org.example.project.SampleTest#testOne",
            )

            build(":jvmTest", "--tests", "SampleTest.testOne") {
                assertTasksExecuted(":jvmTest")
            }
            assertExecutedTestCases(
                ":jvmTest",
                "org.example.project.SampleTest#testOne",
            )

            // Filter by wildcard class pattern
            build(":jvmTest", "--tests", "*SampleTest") {
                assertTasksExecuted(":jvmTest")
            }
            assertExecutedTestCases(
                ":jvmTest",
                "org.example.project.SampleTest#testOne",
                "org.example.project.SampleTest#testTwo",
            )

            // Filter by package wildcard pattern
            build(":jvmTest", "--tests", "org.example.project.*") {
                assertTasksExecuted(":jvmTest")
            }
            assertExecutedTestCases(
                ":jvmTest",
                "org.example.project.SampleTest#testOne",
                "org.example.project.SampleTest#testTwo",
                "org.example.project.OtherTest#testOther",
            )

            // Filter with multiple --tests arguments (union)
            build(":jvmTest", "--tests", "*SampleTest", "--tests", "*ExternalTest") {
                assertTasksExecuted(":jvmTest")
            }
            assertExecutedTestCases(
                ":jvmTest",
                "org.example.project.SampleTest#testOne",
                "org.example.project.SampleTest#testTwo",
                "org.example.other.ExternalTest#testExternal",
            )
        }
    }

    @DisplayName("Task DSL filter configuration: include, exclude, and CLI narrowing behavior")
    @GradleTest
    fun testDslTaskFilterConfiguration(gradleVersion: GradleVersion) {
        project("base-kotlin-multiplatform-library", gradleVersion) {
            setupSampleSources()

            // 1. Task DSL includeTestsMatching pattern
            buildScriptInjection {
                project.tasks.withType(Test::class.java).configureEach {
                    it.filter.includeTestsMatching("*SampleTest")
                }
            }
            build(":jvmTest") {
                assertTasksExecuted(":jvmTest")
            }
            assertExecutedTestCases(
                ":jvmTest",
                "org.example.project.SampleTest#testOne",
                "org.example.project.SampleTest#testTwo",
            )

            // 2. Task DSL include pattern + exclude pattern
            buildScriptInjection {
                project.tasks.withType(Test::class.java).configureEach {
                    it.filter.setIncludePatterns("org.example.project.*")
                    it.filter.excludeTestsMatching("*OtherTest")
                }
            }
            build(":jvmTest") {
                assertTasksExecuted(":jvmTest")
            }
            assertExecutedTestCases(
                ":jvmTest",
                "org.example.project.SampleTest#testOne",
                "org.example.project.SampleTest#testTwo",
            )

            // 3. CLI narrowing behavior: script include filter is further narrowed by CLI --tests
            buildScriptInjection {
                project.tasks.withType(Test::class.java).configureEach {
                    it.filter.setIncludePatterns("*SampleTest")
                    it.filter.setExcludePatterns()
                }
            }

            // CLI pattern includes org.example.project.*, but script limits to *SampleTest -> only SampleTest runs
            build(":jvmTest", "--tests", "org.example.project.*") {
                assertTasksExecuted(":jvmTest")
            }
            assertExecutedTestCases(
                ":jvmTest",
                "org.example.project.SampleTest#testOne",
                "org.example.project.SampleTest#testTwo",
            )

            // CLI pattern matches *OtherTest, but script limits to *SampleTest -> intersection is empty, build fails
            buildAndFail(":jvmTest", "--tests", "*OtherTest") {
                assertTasksFailed(":jvmTest")
            }
        }
    }

    @DisplayName("jvmTest fails on non-matching --tests filter, jsNodeTest does not")
    @GradleTest
    fun testTargetBehavioralAsymmetryOnNoMatchingTests(gradleVersion: GradleVersion) {
        project(
            "base-kotlin-multiplatform-library",
            gradleVersion,
            buildOptions = defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899(),
        ) {
            setupSampleSources(includeJs = true)

            // 1. :jvmTest fails by default on NoSuchTest because Gradle's standard Test task has isFailOnNoMatchingTests = true by default.
            buildAndFail(":jvmTest", "--tests", "NoSuchTest") {
                assertTasksFailed(":jvmTest")
            }
            assertExecutedTestCases(":jvmTest")

            // 2. :jsNodeTest succeeds on NoSuchTest because KotlinTest (base class for KotlinJsTest)
            // explicitly sets isFailOnNoMatchingTests = false.
            build(":jsNodeTest", "--tests", "NoSuchTest") {
                assertTasksExecuted(":jsNodeTest")
            }
            assertExecutedTestCases(":jsNodeTest")
        }
    }

    @DisplayName("Disable failOnNoMatchingTests on JVM test task allows non-matching filter to succeed")
    @GradleTest
    fun testDisableFailOnNoMatchingTests(gradleVersion: GradleVersion) {
        project("base-kotlin-multiplatform-library", gradleVersion) {
            setupSampleSources()

            buildScriptInjection {
                project.tasks.withType(Test::class.java).configureEach {
                    it.filter.isFailOnNoMatchingTests = false
                }
            }

            build(":jvmTest", "--tests", "NoSuchTest") {
                assertTasksExecuted(":jvmTest")
            }
            assertExecutedTestCases(":jvmTest")
        }
    }
}
