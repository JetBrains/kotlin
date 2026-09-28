/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import com.intellij.openapi.util.JDOMUtil
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.testFederation.MustRunAlways
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@MppGradlePluginTests
@MustRunAlways
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

    @DisplayName("TestReporter.publishEntry metadata is included in JVM XML and HTML reports")
    @GradleTest
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_9_4)
    fun testTestReporterMetadataIsIncludedInXmlReport(gradleVersion: GradleVersion) {
        project("base-kotlin-multiplatform-library", gradleVersion) {
            buildScriptInjection {
                val jvmTarget = kotlinMultiplatform.jvm()
                jvmTarget.testRuns.configureEach {
                    it.executionTask.configure {
                        it.useJUnitPlatform()
                        it.ignoreFailures = true
                    }
                }
                kotlinMultiplatform.sourceSets.getByName("jvmTest").dependencies {
                    implementation("org.jetbrains.kotlin:kotlin-test-junit5")
                }
            }

            kotlinSourcesDir("jvmTest").source("TestReporterTest.kt") {
                """
                import org.junit.jupiter.api.Test
                import org.junit.jupiter.api.TestReporter

                class TestReporterTest {
                    @Test
                    fun publishEntry(reporter: TestReporter) {
                        reporter.publishEntry("metadata-key", "metadata-value")
                    }

                    @Test
                    fun publishEntryAndFail(reporter: TestReporter) {
                        reporter.publishEntry("failed-metadata-key", "failed-metadata-value")
                        error("Expected failure")
                    }
                }
                """.trimIndent()
            }

            build(":jvmTest")

            val testResultXml = projectPath.resolve("build/test-results/jvmTest")
                .allFilesWithExtension("xml")
                .single()
            val testSuite = JDOMUtil.load(testResultXml.toFile())
            val testCases = testSuite.getChildren("testcase").ifEmpty {
                testSuite.getChildren("testsuite").flatMap { it.getChildren("testcase") }
            }
            assertEquals(2, testCases.size)

            val expectedMetadata = mapOf(
                "publishEntry(TestReporter)" to ("metadata-key" to "metadata-value"),
                "publishEntryAndFail(TestReporter)" to ("failed-metadata-key" to "failed-metadata-value"),
            )
            testCases.forEach { testCase ->
                val testName = testCase.getAttributeValue("name")
                val properties = assertNotNull(testCase.getChild("properties"), "Expected test properties")
                val property = assertNotNull(properties.getChild("property"), "Expected published test metadata")
                val expectedMetadataValues = expectedMetadata.entries
                    .single { testName.startsWith(it.key) }
                    .value

                assertEquals(expectedMetadataValues.first, property.getAttributeValue("name"))
                assertEquals(expectedMetadataValues.second, property.getAttributeValue("value"))
                assertTrue(testName.contains("[jvm]"))
            }

            val failedTestCase = testCases.single { it.getAttributeValue("name").startsWith("publishEntryAndFail") }
            assertNotNull(failedTestCase.getChild("failure"), "Expected failure information")

            val htmlReport = testClassHtmlReport(
                ":jvmTest",
                "TestReporterTest",
                gradleVersion,
                targetName = "jvm",
            )
            assertFilesCombinedContains(
                htmlReport.parent.allFilesWithExtension("html"),
                "metadata-key",
                "metadata-value",
                "failed-metadata-key",
                "failed-metadata-value",
            )
        }
    }
}
