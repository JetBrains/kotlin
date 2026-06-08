/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalJsTestDsl::class)

package org.jetbrains.kotlin.gradle.js

import org.gradle.api.tasks.Copy
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBrowserTestDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTestsLocation
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.testbase.BuildOptions
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.JsGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.OsCondition
import org.jetbrains.kotlin.gradle.testbase.TestProject
import org.jetbrains.kotlin.gradle.testbase.addKgpToBuildScriptCompilationClasspath
import org.jetbrains.kotlin.gradle.testbase.assertOutputContains
import org.jetbrains.kotlin.gradle.testbase.assertOutputDoesNotContain
import org.jetbrains.kotlin.gradle.testbase.assertTasksExecuted
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.buildAndFail
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
import org.jetbrains.kotlin.gradle.testbase.disableIsolatedProjectsBecauseOfJsAndWasmKT75899
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.junit.jupiter.api.condition.OS
import kotlin.apply
import kotlin.test.Ignore
import kotlin.test.assertEquals

// remove after KTI-3326 Allow kotlin teamcity agents to run playwright browsers
@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC]
)
@OptIn(ExperimentalJsTestDsl::class)
@JsGradlePluginTests
class JsBrowserTestsWithPlaywrightIT : KGPBaseTest() {
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy().disableIsolatedProjectsBecauseOfJsAndWasmKT75899()

    @GradleTest
    fun `verify base KotlinJsTest configuration`(gradleVersion: GradleVersion) {
        project(
            "empty",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions
        ) {
            jsProject {
                firefox()
            }

            build(":jsBrowserTest") {
                assertOutputContains("dummy test")
            }
        }
    }

    @GradleTest
    fun `verify headless KotlinJsTest configuration`(gradleVersion: GradleVersion) {
        project(
            "empty",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions
        ) {
            jsProject {
                firefox()
                browserDefaults.apply {
                    headless.set(false)
                }
            }

            build(":jsBrowserTest") {
                assertOutputContains("dummy test")
            }
        }
    }

    @GradleTest
    fun `verify KotlinJsTest with browsers launchArgs configuration`(gradleVersion: GradleVersion) {
        project(
            "empty",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions
        ) {
            jsProject(
                testSource = """
                    class LangTest {
                      @kotlin.test.Test
                      fun lang() { 
                        println("LANG_FROM_ENV=" + js("navigator.language").toString()) 
                      }
                    }
                """.trimIndent()
            ) {
                chromium("customized") {
                    this.browserDefaults.apply {
                        launchArgs.set(listOf("--lang=fi-FI"))
                    }
                }
            }

            build(":jsBrowserTest") {
                assertOutputContains("LANG_FROM_ENV=fi-FI")
            }
        }
    }

    @GradleTest
    fun `verify KotlinJsTest with bundle post processing`(gradleVersion: GradleVersion) {
        project(
            "empty",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions
        ) {
            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                project.applyMultiplatform {
                    js {
                        browser {
                            val postProcess = project.tasks.register(
                                "postProcessTestBundle",
                                Copy::class.java,
                            ) { copy ->
                                copy.from(test.defaultBundleTask.flatMap { it.outputBundleDir })
                                copy.into(project.layout.buildDirectory.dir("post-processed-test-bundle"))
                                copy.filter { line: String ->
                                    line.replace("dummy test", "post-processed test output")
                                }
                            }

                            test.apply {
                                chromium()

                                browserDefaults.testsLocation.set(
                                    defaultTestsLocation.map { default ->
                                        object : KotlinJsTestsLocation {
                                            override val devServer = default.devServer
                                            override val bundleLocation = project.layout.dir(postProcess.map { it.destinationDir })
                                        }
                                    }
                                )
                            }
                        }
                    }
                    sourceSets.commonTest.dependencies {
                        implementation(kotlin("test"))
                    }
                }

                project.projectDir.resolve("src/jsTest/kotlin/DummyTest.kt").apply {
                    parentFile.mkdirs()
                    writeText(DUMMY_TEST_SOURCE)
                }
            }

            build(":jsBrowserTest") {
                assertTasksExecuted(":postProcessTestBundle")
                assertOutputContains("post-processed test output")
            }
        }
    }

    @GradleTest
    fun `verify test filtering with include pattern selects only matching tests`(gradleVersion: GradleVersion) {
        project(
            "empty",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions
        ) {
            jsProject(
                testSource = FILTER_TEST_SOURCE,
                testFileName = "FilterTest.kt",
            ) {
                chromium()
            }

            build(":jsBrowserTest", "--tests", "*included") {
                assertOutputContains("RAN included")
                assertOutputDoesNotContain("RAN excluded")
            }
        }
    }

    @GradleTest
    fun `verify Gradle test filter includeTestsMatching selects only matching tests`(gradleVersion: GradleVersion) {
        project(
            "empty",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions
        ) {
            jsProject(
                testSource = FILTER_TEST_SOURCE,
                testFileName = "FilterTest.kt",
            ) {
                chromium()
            }

            buildScriptInjection {
                project.tasks.withType(KotlinJsTest::class.java).configureEach {
                    it.filter.includeTestsMatching("*included")
                }
            }

            build(":jsBrowserTest") {
                assertOutputContains("RAN included")
                assertOutputDoesNotContain("RAN excluded")
            }
        }
    }

    @GradleTest
    fun `verify Gradle test filter excludeTestsMatching skips matching tests`(gradleVersion: GradleVersion) {
        project(
            "empty",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions
        ) {
            jsProject(
                testSource = FILTER_TEST_SOURCE,
                testFileName = "FilterTest.kt",
            ) {
                webkit()
            }

            buildScriptInjection {
                project.tasks.withType(KotlinJsTest::class.java).configureEach {
                    it.filter.excludeTestsMatching("*excluded")
                }
            }

            build(":jsBrowserTest") {
                assertOutputContains("RAN included")
                assertOutputDoesNotContain("RAN excluded")
            }
        }
    }

    @GradleTest
    fun `verify failing test fails the build and is reported`(gradleVersion: GradleVersion) {
        project(
            "empty",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions
        ) {
            jsProject(
                testSource = """
                    class FailingTest {
                      @kotlin.test.Test
                      fun passing() { println("PASSED marker") }

                      @kotlin.test.Test
                      fun failing() { kotlin.test.assertTrue(false, "boom failure") }
                    }
                """.trimIndent(),
                testFileName = "FailingTest.kt",
            ) {
                chromium()
            }

            buildAndFail(":jsBrowserTest") {
                assertOutputContains("PASSED marker")
                assertOutputContains("boom failure")
            }
        }
    }

    @Ignore("KT-86797 Failed JS tests don't report errors in the output")
    @GradleTest
    fun `verify failing test fails the build and is reported for webkit`(gradleVersion: GradleVersion) {
        project(
            "empty",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions
        ) {
            jsProject(
                testSource = """
                    class FailingTest {
                      @kotlin.test.Test
                      fun passing() { println("PASSED marker") }

                      @kotlin.test.Test
                      fun failing() { kotlin.test.assertTrue(false, "boom failure") }
                    }
                """.trimIndent(),
                testFileName = "FailingTest.kt",
            ) {
                webkit()
            }

            buildAndFail(":jsBrowserTest") {
                assertOutputContains("PASSED marker")
                assertOutputContains("boom failure")
            }
        }
    }

    @GradleTest
    fun `verify each browser runner executes the tests`(gradleVersion: GradleVersion) {
        project(
            "empty",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions
        ) {
            jsProject {
                chromium("first")
                firefox("second")
            }

            build(":jsBrowserTest") {
                val occurrences = "dummy test".toRegex().findAll(output).count()
                assertEquals(
                    2, occurrences,
                    "Expected 'dummy test' to be printed by each runner (2), but found $occurrences"
                )
            }
        }
    }
}


private val DUMMY_TEST_SOURCE = """
    class DummyTest {
      @kotlin.test.Test
      fun dummy() {
        println("dummy test")
      }
    }
""".trimIndent()

private val FILTER_TEST_SOURCE = """
    class FilterTest {
      @kotlin.test.Test
      fun included() { println("RAN included") }

      @kotlin.test.Test
      fun excluded() { println("RAN excluded") }
    }
""".trimIndent()

private fun TestProject.jsProject(
    testSource: String = DUMMY_TEST_SOURCE,
    testFileName: String = "DummyTest.kt",
    testConfigure: KotlinJsBrowserTestDsl.() -> Unit,
) {
    addKgpToBuildScriptCompilationClasspath()
    buildScriptInjection {
        project.applyMultiplatform {
            js().browser {
                test.apply {
                    testConfigure()
                }
            }
            sourceSets.commonTest.dependencies {
                implementation(kotlin("test"))
            }
        }

        project.projectDir.resolve("src/jsTest/kotlin/$testFileName").apply {
            parentFile.mkdirs()
            writeText(testSource)
        }
    }
}
