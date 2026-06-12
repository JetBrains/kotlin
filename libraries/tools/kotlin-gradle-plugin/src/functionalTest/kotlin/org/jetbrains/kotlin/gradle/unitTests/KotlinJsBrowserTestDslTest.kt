/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalJsTestDsl::class)

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.file.Directory
import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBrowserTestDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinChromiumTestRunner
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinFirefoxTestRunner
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinWebkitTestRunner
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import java.time.Duration
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

class KotlinJsBrowserTestDslTest {

    @Test
    fun `allBrowserRunners contains declared runners with correct types and defaults`() {
        val test = configureBrowserTest {
            chromium()
            chromium("custom-chromium") {
                it.timeout.set(Duration.ofSeconds(30))
                it.headless.set(false)
                it.launchArgs.set(listOf("--lang=fi-FI"))
            }
            firefox()
            webkit()
            webkit("extra-webkit")
        }

        val bundle = test.defaultBundleDirectory
        assertEquals(
            mapOf(
                "chromium" to RunnerDump(
                    KotlinChromiumTestRunner::class,
                    Duration.ofSeconds(2),
                    true,
                    emptyList(),
                    bundle
                ),
                "custom-chromium" to RunnerDump(
                    KotlinChromiumTestRunner::class,
                    Duration.ofSeconds(30),
                    false,
                    listOf("--lang=fi-FI"),
                    bundle,
                ),
                "firefox" to RunnerDump(
                    KotlinFirefoxTestRunner::class,
                    Duration.ofSeconds(2),
                    true,
                    emptyList(),
                    bundle
                ),
                "webkit" to RunnerDump(
                    KotlinWebkitTestRunner::class,
                    Duration.ofSeconds(2),
                    true,
                    emptyList(),
                    bundle
                ),
                "extra-webkit" to RunnerDump(
                    KotlinWebkitTestRunner::class,
                    Duration.ofSeconds(2),
                    true,
                    emptyList(),
                    bundle
                ),
            ),
            test.dumpRunners(),
        )
    }

    @Test
    fun `top-level configuration propagates to runners unless overridden`() {
        val test = configureBrowserTest {
            browserDefaults.apply {
                timeout.set(Duration.ofSeconds(10))
                headless.set(false)
                launchArgs.set(listOf("--global"))
            }

            chromium()
            webkit("override") {
                it.headless.set(true)
                it.timeout.set(Duration.ofSeconds(42))
                it.launchArgs.set(listOf("--no-sandbox"))
            }
        }

        val bundle = test.defaultBundleDirectory
        assertEquals(
            mapOf(
                "chromium" to RunnerDump(
                    KotlinChromiumTestRunner::class,
                    Duration.ofSeconds(10),
                    false, listOf("--global"),
                    bundle
                ),
                "override" to RunnerDump(
                    KotlinWebkitTestRunner::class,
                    Duration.ofSeconds(42),
                    true, listOf("--no-sandbox"),
                    bundle
                ),
            ),
            test.dumpRunners(),
        )
    }

    @Test
    fun `bundleDirectory defaults to bundle task output and propagates to runners`() {
        val test = configureBrowserTest {
            webkit()
        }

        val expectedDefault = test.defaultBundleTask.flatMap { it.outputBundleDir }.get()
        assertEquals(expectedDefault, test.defaultTestsLocation.get().bundleLocation.get())
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
                    KotlinChromiumTestRunner::class,
                    Duration.ofSeconds(2),
                    false,
                    listOf("--flag"),
                    test.defaultBundleDirectory,
                ),
            ),
            test.dumpRunners(),
        )
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
        )
    }

internal val KotlinJsBrowserTestDsl.defaultBundleDirectory: Directory
    get() = defaultBundleTask.flatMap { it.outputBundleDir }.get()
