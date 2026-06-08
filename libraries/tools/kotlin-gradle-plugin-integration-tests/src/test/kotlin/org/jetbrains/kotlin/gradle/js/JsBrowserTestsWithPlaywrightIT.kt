/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalJsTestDsl::class)

package org.jetbrains.kotlin.gradle.js

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBrowserTestDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTestsLocation
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testbase.BuildOptions.ConfigurationCacheValue
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.junit.jupiter.api.condition.OS
import javax.inject.Inject
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

    @Ignore("KT-86911 Configured post-processing for playwright reports missing an input or output annotation")
    @GradleTest
    fun `verify KotlinJsTest with bundle post processing`(gradleVersion: GradleVersion) {
        project(
            "empty",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(configurationCache = ConfigurationCacheValue.DISABLED)
        ) {
            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                project.applyMultiplatform {
                    js {
                        browser {
                            test.apply {
                                val postProcess = project.tasks.register(
                                    "postProcessTestBundle",
                                    PostProcessTestsBundle::class.java,
                                ) {
                                    it.originalTestsLocation.set(defaultTestsLocation)
                                    it.outputBundleDir.set(project.layout.buildDirectory.dir("post-processed-test-bundle"))
                                }

                                chromium()
                                browserDefaults.testsLocation.set(
                                    postProcess.flatMap { it.postProcessedTestsLocation }
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

abstract class PostProcessTestsBundle : DefaultTask() {

    @get:Nested
    abstract val originalTestsLocation: Property<KotlinJsTestsLocation>

    @get:OutputDirectory
    abstract val outputBundleDir: DirectoryProperty

    @get:Internal
    abstract val postProcessedTestsLocation: Property<KotlinJsTestsLocation>

    @get:Inject
    abstract val fs: FileSystemOperations

    @TaskAction
    fun postProcess() {
        fs.copy {
            it.from(originalTestsLocation.get().bundleLocation)
            it.into(outputBundleDir)
            it.filter { line: String ->
                line.replace("dummy test", "post-processed test output")
            }
        }

        postProcessedTestsLocation.set(object : KotlinJsTestsLocation {
            override val devServer get() = originalTestsLocation.get().devServer
            override val bundleLocation = outputBundleDir
        })
    }
}
