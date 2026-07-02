/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.js

import org.gradle.api.logging.LogLevel
import org.gradle.kotlin.dsl.kotlin
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.util.isTeamCityRun
import kotlin.io.path.moveTo
import org.junit.jupiter.api.Assumptions.assumeFalse
import kotlin.test.assertContains

@JsGradlePluginTests
class JsBrowserTestsIT : KGPBaseTest() {

    @GradleTest
    fun `verify custom custom KotlinJsTest environment variables are used to launch tests`(gradleVersion: GradleVersion) {
        project(
            "empty",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                logLevel = LogLevel.DEBUG,
            )
        ) {
            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                project.applyMultiplatform {
                    js().browser()
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

                project.tasks.withType(KotlinJsTest::class.java).configureEach { task ->
                    task.environment("CUSTOM_ENV", "custom-env-value")

                    // KT-77134 verify doFirst {} workaround,
                    // which is necessary because KotlinJsTest doesn't use Provider API.
                    val lazyValue = project.provider { "lazy-custom-env-value" }
                    task.doFirst { _ ->
                        task.environment("CUSTOM_ENV_LAZY", lazyValue.get())
                    }
                }
            }

            build(
                ":jsBrowserTest",
                // :jsBrowserTest might fail if no browsers are installed (e.g. on CI).
                // For this test we don't care if the task passes or fails, only if the custom environment variables are set correctly.
                // So, use `GradleRunner.run()` to ignore the build outcome.
                gradleRunnerAction = GradleRunner::run,
            ) {
                val execAsyncHandleLogs = output.lineSequence()
                    .mapNotNull {
                        it
                            .substringAfter(" [DEBUG] [org.jetbrains.kotlin.gradle.utils.processes.ExecAsyncHandle] ", "")
                            .ifBlank { null }
                    }

                val createdExecSpecLog = execAsyncHandleLogs
                    .singleOrNull { it.startsWith("[ExecAsyncHandle :jsBrowserTest] created ExecSpec.") }

                requireNotNull(createdExecSpecLog) {
                    "Could not find 'created ExecSpec' log in build output:\n${execAsyncHandleLogs.joinToString("\n").prependIndent()}"
                }

                val env = createdExecSpecLog.substringAfter("Environment: {").substringBefore("},")
                assertContains(env, "CUSTOM_ENV=custom-env-value")
                assertContains(env, "CUSTOM_ENV_LAZY=lazy-custom-env-value")
            }
        }
    }

    @GradleTest
    fun `karma should run when kotlin-node-modules are not installed on top-level node_modules`(gradleVersion: GradleVersion) {
        project(
            "empty",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()
        ) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    js().browser {
                        testTask {
                            it.useKarma {
                                useChromeHeadless()
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

            // prepare build/js/
            build("kotlinNpmInstall")
            val rootLevelWebHelpers = projectPath.resolve("build/js/node_modules/kotlin-web-helpers")
            val packageLevelWebHelpers = projectPath.resolve("build/js/packages/empty-test/node_modules/kotlin-web-helpers")
            rootLevelWebHelpers.moveTo(packageLevelWebHelpers)

            // -x kotlinNpmInstall to prevent overwriting
            if (isTeamCityRun) { // Should be fixed by KTI-3326, but only in master branch
                buildAndFail("jsBrowserTest", "-x", "kotlinNpmInstall") {
                    assertTasksFailed(":jsBrowserTest")
                    assertOutputContains("> Errors occurred during launch of browser for testing.")
                    assertOutputDoesNotContain(":jsBrowserTest exited with errors (exit code: 1)")
                }
            } else {
                build("jsBrowserTest", "-x", "kotlinNpmInstall") {
                    assertTasksAreNotInTaskGraph("kotlinNpmInstall")
                    assertTasksExecuted(":jsBrowserTest")
                }
            }
        }
    }

    @GradleTest
    fun `smoke js browser test`(
        gradleVersion: GradleVersion
    ) {
        project(
            "empty",
            gradleVersion = gradleVersion,
        ) {
            plugins {
                kotlin("multiplatform")
            }

            buildScriptInjection {
                project.applyMultiplatform {
                    js {
                        browser {
                            @OptIn(ExperimentalJsTestDsl::class)
                            with(test) {
                                chromium()
                            }
                        }
                    }

                    sourceSets.commonTest {
                        dependencies {
                            implementation(kotlin("test"))
                        }
                    }

                    sourceSets.commonTest.get().compileSource(
                        """
                        import kotlin.test.*
                        
                        class JsBrowserSmokeTest {
                            @Test
                            fun assertOk() {
                                assertTrue(42 == 42)
                            }
                            
                            @Test
                            fun assertFails() {
                                assertTrue(42 == 0)
                            }
                        }
                        """.trimIndent())
                }
            }

            buildAndFail("jsBrowserTest") {
                assumeFalse(
                    output.contains("error while loading shared libraries: libglib-2.0"),
                    "No libglib-2.0 on the test runner machine"
                )
                assertTasksExecuted(":prepareWebpackBundleForKotlinJsTests")
                assertTasksFailed(":jsBrowserTest")
                assertOutputContains("""Execute JS tests with chromium runner at URL: file.*kotlinJsTest/dist/test.html""".toRegex())
                assertOutputContains("chromium.JsBrowserSmokeTest.assertFails[js, browser] FAILED")
                assertOutputContains("2 tests completed, 1 failed")
                // TODO: KT-86778 Add verification of test report
            }
        }
    }
}
