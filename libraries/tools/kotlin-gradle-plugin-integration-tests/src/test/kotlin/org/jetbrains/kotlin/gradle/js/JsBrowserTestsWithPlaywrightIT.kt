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
import kotlin.io.path.writeText
import kotlin.test.Ignore
import kotlin.test.assertEquals

@OptIn(ExperimentalJsTestDsl::class)
@JsBrowserGradlePluginTests
class JsBrowserTestsWithPlaywrightIT : KGPBaseTest() {
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy().disableIsolatedProjectsBecauseOfJsAndWasmKT75899()


    @GradleTest
    fun `verify launchArgs configuration with browser api access`(gradleVersion: GradleVersion) {
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
                        println("Custom-User-Agent: " + kotlinx.browser.window.navigator.userAgent) 
                      }
                    }
                """.trimIndent()
            ) {
                chromium("customized") {
                    it.launchArgs.set(listOf("--user-agent=Foo/42.0"))
                }
            }

            build(":jsBrowserTest") {
                assertOutputContains("Custom-User-Agent: Foo/42.0")
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
                    writeText(
                        """
                            class DummyTest {
                              @kotlin.test.Test
                              fun dummy() {
                                println("dummy test")
                              }
                            }
                        """.trimIndent()
                    )
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

            build(":jsBrowserTest", "--tests", "IncludedTest") {
                assertOutputContains("RAN included")
                assertOutputDoesNotContain("RAN excluded")
            }
        }
    }

    @GradleTest
    fun `verify Gradle testsMatching filter selects only matching tests`(gradleVersion: GradleVersion) {
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
                    if (project.providers.gradleProperty("includeTestsMatching").isPresent) {
                        it.filter.includeTestsMatching("*included")
                    } else {
                        it.filter.excludeTestsMatching("*excluded")
                    }
                }
            }

            build(":jsBrowserTest", "-PincludeTestsMatching=true") {
                assertOutputContains("RAN included")
                assertOutputDoesNotContain("RAN excluded")
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
                val expectedTestReport = projectPath.resolve("expected-test-report.xml").apply {
                    writeText(
                        """
                            <?xml version="1.0" encoding="UTF-8"?>
                            <results>
                              <testsuite name="jsBrowserTest.chromium.FailingTest" tests="2" skipped="0" failures="1" errors="0" timestamp="..." hostname="..." time="...">
                                <properties />
                                <testcase name="passing[js, browser]" classname="jsBrowserTest.chromium.FailingTest" time="..." />
                                <testcase name="failing[js, browser]" classname="jsBrowserTest.chromium.FailingTest" time="...">
                                  <failure message="..." type="AssertionError">...</failure>
                                </testcase>
                                <system-out>PASSED marker</system-out>
                                <system-err />
                              </testsuite>
                            </results>
                        """.trimIndent().modifyForGradle(gradleVersion)
                    )
                }
                assertTestResults(expectedTestReport, "jsBrowserTest")
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
    fun `verify bundle task is up-to-date on second run`(gradleVersion: GradleVersion) {
        project(
            "empty",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions
        ) {
            jsProject {
                chromium()
            }

            build(":jsBrowserTest") {
                assertTasksExecuted(":prepareWebpackBundleForKotlinJsTests")
            }

            build(":jsBrowserTest") {
                assertTasksUpToDate(":prepareWebpackBundleForKotlinJsTests")
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
                val expectedTestReport = projectPath.resolve("expected-test-report.xml").apply {
                    writeText(
                        """
                            <?xml version="1.0" encoding="UTF-8"?>
                            <results>
                              <testsuite name="jsBrowserTest.first.DummyTest" tests="1" skipped="0" failures="0" errors="0" timestamp="..." hostname="..." time="...">
                                <properties />
                                <testcase name="dummy[js, browser]" classname="jsBrowserTest.first.DummyTest" time="..." />
                                <system-out>dummy test</system-out>
                                <system-err />
                              </testsuite>
                              <testsuite name="jsBrowserTest.second.DummyTest" tests="1" skipped="0" failures="0" errors="0" timestamp="..." hostname="..." time="...">
                                <properties />
                                <testcase name="dummy[js, browser]" classname="jsBrowserTest.second.DummyTest" time="..." />
                                <system-out>dummy test</system-out>
                                <system-err />
                              </testsuite>
                            </results>
                        """.trimIndent().modifyForGradle(gradleVersion)
                    )
                }
                assertTestResults(expectedTestReport, "jsBrowserTest")
            }
        }
    }

    @GradleTest
    fun `verify co-existing for browser and mocha tests`(gradleVersion: GradleVersion) {
        project(
            "empty",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions
        ) {
            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                project.applyMultiplatform {
                    js() {
                        browser {
                            test {
                                it.chromium()
                            }
                        }
                        nodejs {
                            testTask {
                                it.useMocha()
                            }
                        }
                    }
                    sourceSets.commonTest.dependencies {
                        implementation(kotlin("test"))
                    }
                }

                project.projectDir.resolve("src/jsTest/kotlin/JsTests.kt").apply {
                    parentFile.mkdirs()
                    writeText(
                        """
                            class JsTests {
                              @kotlin.test.Test
                              fun dummy() {
                                println("Is node: " + js("typeof window === 'undefined'"))
                              }
                            }
                        """.trimIndent()
                    )
                }
            }

            build(":jsBrowserTest") {
                assertOutputContains("Is node: false")
            }
            build(":jsNodeTest") {
                assertOutputContains("Is node: true")
            }
        }
    }
}

// expected report for gradle >=9.3
fun String.modifyForGradle(gradleVersion: GradleVersion) =
    if (gradleVersion < GradleVersion.version(TestVersions.Gradle.G_9_3)) {
        this.replace("jsBrowserTest.", "")
    } else this

private val FILTER_TEST_SOURCE = """
    class IncludedTest {
      @kotlin.test.Test
      fun included() { println("RAN included") }
    }

    class ExcludedTest {
      @kotlin.test.Test
      fun excluded() { println("RAN excluded") }
    }
""".trimIndent()

private fun TestProject.jsProject(
    testSource: String = """
        class DummyTest {
          @kotlin.test.Test
          fun dummy() {
            println("dummy test")
          }
        }
    """.trimIndent(),
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
