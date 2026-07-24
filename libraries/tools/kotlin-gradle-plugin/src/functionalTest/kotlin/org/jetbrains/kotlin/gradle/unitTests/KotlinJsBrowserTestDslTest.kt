/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalJsTestDsl::class)

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.file.Directory
import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBrowserTestDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinChromiumTestRunner
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinFirefoxTestRunner
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinWebkitTestRunner
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class KotlinJsBrowserTestDslTest {

    @Test
    fun `allBrowserRunners contains declared runners with correct types and defaults`() {
        val test = configureBrowserTest {
            chromium()
            chromium("custom-chromium") {
                it.timeout.set(31L.seconds)
                it.headless.set(false)
                it.launchArgs.set(listOf("--lang=fi-FI"))
            }
            firefox()
            webkit()
            webkit("extra-webkit")
        }

        val bundle = test.defaultBundleDirectory
        assertEquals(
            expected = mapOf(
                "chromium" to RunnerDump(
                    type = KotlinChromiumTestRunner::class,
                    timeout = 30L.seconds,
                    headless = true,
                    launchArgs = emptyList(),
                    launchEnvironmentVariables = mapOf(),
                    testsLocation = bundle
                ),
                "custom-chromium" to RunnerDump(
                    type = KotlinChromiumTestRunner::class,
                    timeout = 31L.seconds,
                    headless = false,
                    launchArgs = listOf("--lang=fi-FI"),
                    launchEnvironmentVariables = mapOf(),
                    testsLocation = bundle,
                ),
                "firefox" to RunnerDump(
                    type = KotlinFirefoxTestRunner::class,
                    timeout = 30L.seconds,
                    headless = true,
                    launchArgs = emptyList(),
                    launchEnvironmentVariables = mapOf(),
                    testsLocation = bundle
                ),
                "webkit" to RunnerDump(
                    type = KotlinWebkitTestRunner::class,
                    timeout = 30L.seconds,
                    headless = true,
                    launchArgs = emptyList(),
                    launchEnvironmentVariables = mapOf(),
                    testsLocation = bundle
                ),
                "extra-webkit" to RunnerDump(
                    type = KotlinWebkitTestRunner::class,
                    timeout = 30L.seconds,
                    headless = true,
                    launchArgs = emptyList(),
                    launchEnvironmentVariables = mapOf(),
                    testsLocation = bundle
                ),
            ),
            actual = test.dumpRunners(),
        )
    }

    @Test
    fun `top-level configuration propagates to runners unless overridden`() {
        val test = configureBrowserTest {
            timeout.set(10L.seconds)
            headless.set(false)
            launchEnvironmentVariables.put("A", "1")
            // launchArgs is not available on top level

            firefox()

            chromium {
                it.launchArgs.set(listOf("--global"))
                // this should override convention,
                // i.e. it will create new list instead of appending
                it.launchEnvironmentVariables.put("B", "2")
            }

            webkit("override") {
                it.headless.set(true)
                it.timeout.set(42L.seconds)
                it.launchArgs.set(listOf("--no-sandbox"))
                it.launchEnvironmentVariables.set(mapOf("C" to "3"))
            }
        }

        val bundle = test.defaultBundleDirectory
        assertEquals(
            expected = mapOf(
                "chromium" to RunnerDump(
                    type = KotlinChromiumTestRunner::class,
                    timeout = 10L.seconds,
                    headless = false,
                    launchArgs = listOf("--global"),
                    launchEnvironmentVariables = mapOf("B" to "2"),
                    testsLocation = bundle
                ),
                "firefox" to RunnerDump(
                    type = KotlinFirefoxTestRunner::class,
                    timeout = 10L.seconds,
                    headless = false,
                    launchArgs = listOf(),
                    launchEnvironmentVariables = mapOf("A" to "1"),
                    testsLocation = bundle
                ),
                "override" to RunnerDump(
                    type = KotlinWebkitTestRunner::class,
                    timeout = 42L.seconds,
                    headless = true,
                    launchArgs = listOf("--no-sandbox"),
                    launchEnvironmentVariables = mapOf("C" to "3"),
                    testsLocation = bundle
                ),
            ),
            actual = test.dumpRunners(),
        )
    }

    @Test
    fun `bundleDirectory defaults to bundle task output and propagates to runners`() {
        val test = configureBrowserTest {
            webkit()
        }

        val expectedDefault = test.defaultTestsLocationProvider.flatMap { it.bundleLocation }.get()
        assertEquals(expectedDefault, test.allBrowserRunners.get().getValue("webkit").testsLocation.get().bundleLocation.get())
    }

    @Test
    fun `declaring the same runner name twice configures the same runner`() {
        val test = configureBrowserTest {
            chromium("repeated") {
                it.headless.set(false)
            }
            chromium("repeated") {
                it.launchArgs.set(listOf("--flag"))
            }
        }

        assertEquals(
            mapOf(
                "repeated" to RunnerDump(
                    type = KotlinChromiumTestRunner::class,
                    timeout = 30L.seconds,
                    headless = false,
                    launchArgs = listOf("--flag"),
                    launchEnvironmentVariables = mapOf(),
                    testsLocation = test.defaultBundleDirectory,
                ),
            ),
            test.dumpRunners(),
        )
    }

    @Test
    fun `using new test DSL in wasmJs target throws an error`() {
        val project = buildProjectWithMPP {
            with(multiplatformExtension) {
                @OptIn(ExperimentalWasmDsl::class)
                wasmJs {
                    browser {
                        test {
                            it.chromium()
                        }
                    }
                }
            }
        }

        project.evaluate()

        project.assertContainsDiagnostic(KotlinToolingDiagnostics.NewJsTestDslNotSupportedForWasmError)
    }
}


private fun configureBrowserTest(configure: KotlinJsBrowserTestDsl.() -> Unit): KotlinJsBrowserTestDsl {
    lateinit var testDsl: KotlinJsBrowserTestDsl
    val project = buildProjectWithMPP {
        with(multiplatformExtension) {
            js {
                browser {
                    testDsl = test
                    test(configure)
                }
            }
        }
    }
    project.evaluate()
    return testDsl
}

internal data class RunnerDump(
    val type: KClass<*>,
    val timeout: Duration,
    val headless: Boolean,
    val launchArgs: List<String>,
    val launchEnvironmentVariables: Map<String, String>,
    val testsLocation: Directory,
)

internal fun KotlinJsBrowserTestDsl.dumpRunners(): Map<String, RunnerDump> =
    allBrowserRunners.get().mapValues { (_, runner) ->
        RunnerDump(
            type = runner::class,
            timeout = runner.timeout.get(),
            headless = runner.headless.get(),
            launchArgs = runner.launchArgs.get(),
            testsLocation = runner.testsLocation.get().bundleLocation.get(),
            launchEnvironmentVariables = runner.launchEnvironmentVariables.get(),
        )
    }

internal val KotlinJsBrowserTestDsl.defaultBundleDirectory: Directory
    get() = defaultTestsLocationProvider.flatMap { it.bundleLocation }.get()
