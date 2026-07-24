/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalJsTestDsl::class)

package org.jetbrains.kotlin.gradle.js

import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.DelicateKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBrowserTestDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTestsLocation
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testbase.BuildOptions.ConfigurationCacheValue
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.junit.jupiter.api.condition.OS
import java.net.URI
import javax.inject.Inject
import kotlin.io.path.writeText
import kotlin.test.assertContains

@OptIn(ExperimentalJsTestDsl::class)
@OsCondition(
    supportedOn = [OS.LINUX, OS.MAC, OS.WINDOWS],
    enabledOnCI = [OS.LINUX, OS.MAC]
)
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
                                chromium()
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

            // replace default tests location with post-processed one
            buildScriptInjection {
                val jsTarget = kotlinMultiplatform.targets.filterIsInstance<KotlinJsIrTarget>().single()
                val defaultTestsLocation = jsTarget.browser.test.defaultTestsLocationProvider

                val postProcess = project.tasks.register(
                    "postProcessTestBundle",
                    PostProcessTestsBundle::class.java,
                ) {
                    it.originalBundleLocation.set(defaultTestsLocation.flatMap { it.bundleLocation })
                    it.newBundleLocation.set(project.layout.buildDirectory.dir("post-processed-test-bundle"))
                }

                @OptIn(DelicateKotlinGradlePluginApi::class)
                jsTarget.browser.test.testsLocation.set(postProcess.map { it.kotlinJsTestsLocation })
            }


            build(":jsBrowserTest", buildOptions = buildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertTasksExecuted(":postProcessTestBundle")
                assertOutputContains("post-processed test output")
                // since post process bundle is served via file scheme, instead http.
                // we shouldn't expect HTTP server to be started
                assertOutputDoesNotContain("Starting HTTP server")
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
                                <testcase name="passing[js, browser, chromium]" classname="jsBrowserTest.chromium.FailingTest" time="..." />
                                <testcase name="failing[js, browser, chromium]" classname="jsBrowserTest.chromium.FailingTest" time="...">
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
                                <testcase name="dummy[js, browser, first]" classname="jsBrowserTest.first.DummyTest" time="..." />
                                <system-out>dummy test</system-out>
                                <system-err />
                              </testsuite>
                              <testsuite name="jsBrowserTest.second.DummyTest" tests="1" skipped="0" failures="0" errors="0" timestamp="..." hostname="..." time="...">
                                <properties />
                                <testcase name="dummy[js, browser, second]" classname="jsBrowserTest.second.DummyTest" time="..." />
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
    fun `verify each browser runner executes the failing tests`(gradleVersion: GradleVersion) {
        fun testSuitFailed(browserName: String): String =
            """<testsuite name="jsBrowserTest.$browserName.DummyTest" tests="1" skipped="0" failures="1" errors="0" timestamp="..." hostname="..." time="...">
                                <properties />
                                <testcase name="dummy[js, browser, $browserName]" classname="jsBrowserTest.$browserName.DummyTest" time="...">
                                  <failure message="..." type="AssertionError">...</failure>
                                </testcase>
                                <system-out />
                                <system-err />
                              </testsuite>"""

        val assertionErrorMessage = "a failure happen"

        fun validateMessageField(message: String): Unit {
            assertContains(message, "AssertionError: ${assertionErrorMessage}")
        }

        project(
            "empty",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions
        ) {
            jsProject(
                testSource = """
                    class DummyTest {
                      @kotlin.test.Test
                      fun dummy() {
                        kotlin.test.assertTrue(false, "$assertionErrorMessage")
                      }
                    }
                """.trimIndent(),
            ) {
                chromium("first")
                firefox("second")
                webkit("third")
            }

            buildAndFail(":jsBrowserTest") {
                val expectedTestReport = projectPath.resolve("expected-test-report.xml").apply {
                    writeText(
                        """
                            <?xml version="1.0" encoding="UTF-8" standalone="no"?>
                            <results>
                              <testsuite name="jsBrowserTest.first.DummyTest" tests="1" skipped="0" failures="1" errors="0" timestamp="..." hostname="..." time="...">
                                <properties />
                                <testcase name="dummy[js, browser, first]" classname="jsBrowserTest.first.DummyTest" time="...">
                                  <failure message="..." type="AssertionError">...</failure>
                                </testcase>
                                <system-out />
                                <system-err />
                              </testsuite>
                              <testsuite name="jsBrowserTest.second.DummyTest" tests="1" skipped="0" failures="1" errors="0" timestamp="..." hostname="..." time="...">
                                <properties />
                                <testcase name="dummy[js, browser, second]" classname="jsBrowserTest.second.DummyTest" time="...">
                                  <failure message="..." type="AssertionError">...</failure>
                                </testcase>
                                <system-out />
                                <system-err />
                              </testsuite>
                              <testsuite name="jsBrowserTest.third.DummyTest" tests="1" skipped="0" failures="1" errors="0" timestamp="..." hostname="..." time="...">
                                <properties />
                                <testcase name="dummy[js, browser, third]" classname="jsBrowserTest.third.DummyTest" time="...">
                                  <failure message="..." type="AssertionError">...</failure>
                                </testcase>
                                <system-out />
                                <system-err />
                              </testsuite>
                            </results>
                        """.trimIndent().modifyForGradle(gradleVersion)
                    )
                }
                assertTestResults(
                    expectedTestReport,
                    "jsBrowserTest",
                    attributeValidators = mapOf("message" to ::validateMessageField)
                )
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

private class MyLocalFileLocation(
    @get:InputDirectory
    override val bundleLocation: Provider<Directory>,
    @get:Input
    override val testHtmlFileName: Provider<String>,
) : KotlinJsTestsLocation {
    @OptIn(DelicateKotlinGradlePluginApi::class)
    @get:Internal
    override val url: Provider<URI> = bundleLocation.map { it.asFile.resolve(testHtmlFileName.get()).toURI() }
}

private abstract class PostProcessTestsBundle : DefaultTask() {
    @get:InputDirectory
    abstract val originalBundleLocation: DirectoryProperty

    @get:OutputDirectory
    abstract val newBundleLocation: DirectoryProperty

    @get:Internal
    val kotlinJsTestsLocation
        get() = MyLocalFileLocation(
            bundleLocation = newBundleLocation,
            testHtmlFileName = project.provider { "test.html" },
        )

    @get:Inject
    abstract val fs: FileSystemOperations

    @TaskAction
    fun postProcess() {
        fs.copy {
            it.from(originalBundleLocation.get())
            it.into(newBundleLocation)
            it.filter { line: String ->
                line.replace("dummy test", "post-processed test output")
            }
        }
    }
}
