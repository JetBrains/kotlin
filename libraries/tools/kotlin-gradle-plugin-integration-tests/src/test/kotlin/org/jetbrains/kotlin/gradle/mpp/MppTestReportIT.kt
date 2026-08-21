/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.testFederation.SmokeTest
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@MppGradlePluginTests
@SmokeTest
@DisplayName("Tests for KMP test reporting")
class MppTestReportIT : KGPBaseTest() {

    @DisplayName("Aggregated test report contains results from multiple targets (:allTests)")
    @GradleTest
    fun testAllTestsReportAggregation(gradleVersion: GradleVersion) {
        project(
            "base-kotlin-multiplatform-library",
            gradleVersion,
            buildOptions = defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899(),
        ) {
            buildScriptInjection {
                kotlinMultiplatform.jvm()
                kotlinMultiplatform.js {
                    nodejs()
                }
                kotlinMultiplatform.sourceSets.getByName("commonTest").dependencies {
                    implementation(kotlin("test"))
                }
            }

            kotlinSourcesDir("commonTest").source("org/example/project/SampleTest.kt") {
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

            build(":allTests") {
                assertTasksExecuted(":jvmTest", ":jsNodeTest", ":allTests")
            }

            assertFileInProjectExists("build/reports/tests/allTests/index.html")
            assertExecutedTestCases(
                ":jvmTest",
                "org.example.project.SampleTest#testOne",
                "org.example.project.SampleTest#testTwo",
            )
            assertExecutedTestCases(
                ":jsNodeTest",
                "org.example.project.SampleTest#testOne",
                "org.example.project.SampleTest#testTwo",
            )
        }
    }

    @DisplayName("Test failure details, stack trace anti-drift, and HTML report (:jvmTest)")
    @GradleTest
    fun testTestFailureReportingAndStackTrace(gradleVersion: GradleVersion) {
        project(
            "base-kotlin-multiplatform-library",
            gradleVersion,
            buildOptions = defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899(),
        ) {
            buildScriptInjection {
                kotlinMultiplatform.jvm()
                kotlinMultiplatform.sourceSets.getByName("commonTest").dependencies {
                    implementation(kotlin("test"))
                }
            }

            val testClass = "FailingTest"
            val testPackage = "org.example.project"
            val testSource = """
                package $testPackage

                import kotlin.test.Test

                class $testClass {
                    @Test
                    fun failing() {
                        throw IllegalStateException("boom")
                    }
                }
            """.trimIndent()

            val throwingLine = testSource.lines().indexOfFirst { "throw" in it } + 1

            kotlinSourcesDir("commonTest").source("$testPackage/$testClass.kt") {
                testSource
            }

            buildAndFail(":jvmTest") {
                assertTasksFailed(":jvmTest")
            }

            val testCases = readTestCases(":jvmTest")
            val failingTestCase = testCases.single { it.className == "$testPackage.$testClass" && it.name.startsWith("failing") }
            val failure = failingTestCase.failure
            assertNotNull(failure, "Expected failure information for test case")
            assertEquals("java.lang.IllegalStateException", failure.type)
            assertEquals("java.lang.IllegalStateException: boom", failure.message)
            assertNotNull(failure.stackTrace, "Expected stack trace in test failure")
            assertTrue(
                failure.stackTrace.contains("$testPackage.$testClass.failing($testClass.kt:$throwingLine)"),
                "Expected stack trace to contain '$testPackage.$testClass.failing($testClass.kt:$throwingLine)', but was:\n${failure.stackTrace}"
            )

            val htmlReport = testClassHtmlReport(":jvmTest", "$testPackage.$testClass", gradleVersion, targetName = "jvm")
            assertFileExists(htmlReport)
        }
    }
}
