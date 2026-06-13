/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.playwright

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Playwright
import org.gradle.api.internal.tasks.testing.TestExecuter
import org.gradle.api.internal.tasks.testing.TestExecutionSpec
import org.gradle.api.internal.tasks.testing.TestResultProcessor
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessageOutputStreamHandler
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClient
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTestsLocation
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.time.Duration

private val log = LoggerFactory.getLogger("org.jetbrains.kotlin.gradle.tasks.testing.PlaywrightTestExecutor")

/**
 * Kind of Playwright-supported browser engine.
 */
internal enum class PwBrowserKind {
    CHROMIUM,
    FIREFOX,
    WEBKIT,
}

/**
 * A single Playwright browser run.
 */
internal class PwRunnerSpec(
    val name: String,
    val browserKind: PwBrowserKind,
    val testsLocation: KotlinJsTestsLocation,
    val buildTestsExecutionerUrl: (baseUrl: String) -> String,
    val timeout: Duration,
    val finishMarker: String,
    val headless: Boolean,
    val launchArgs: List<String>,
)

/**
 * Execution spec containing all configured browser runners that have to be launched
 * within a single test task invocation.
 */
internal class PwExecutionSpec(
    val createClient: (TestResultProcessor, Logger) -> TCServiceMessagesClient,
    val runners: List<PwRunnerSpec>,
) : TestExecutionSpec

internal class PlaywrightTestExecutor() : TestExecuter<PwExecutionSpec> {

    override fun execute(spec: PwExecutionSpec, testResultProcessor: TestResultProcessor) {
        if (spec.runners.isEmpty()) return

        val client = spec.createClient(testResultProcessor, log)
        val handler = TCServiceMessageOutputStreamHandler(
            client,
            { },
            log,
            false,
        )

        handler.use {
            // TODO: KT-86449 Provide Node.js and plawright-core npm dependency separately.
            //  Use thin layer of Java Classes to interact with Playwright via std in/out pipes.
            val playwright = Playwright.create()
            playwright.use {
                with(client) {
                    root {
                        for (runner in spec.runners) {
                            suite(id = runner.name) {
                                executeRunner(playwright, runner, handler)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun executeRunner(
        playwright: Playwright,
        runner: PwRunnerSpec,
        handler: TCServiceMessageOutputStreamHandler,
    ) {
        val browserType: BrowserType = when (runner.browserKind) {
            PwBrowserKind.CHROMIUM -> playwright.chromium()
            PwBrowserKind.FIREFOX -> playwright.firefox()
            PwBrowserKind.WEBKIT -> playwright.webkit()
        }
        val launchOptions = BrowserType.LaunchOptions()
            .setHeadless(runner.headless)
            .apply {
                if (runner.launchArgs.isNotEmpty()) setArgs(runner.launchArgs)
            }

        log.info("Launching playwright runner '${runner.name}' (${runner.browserKind})")
        val browser: Browser = browserType.launch(launchOptions)
        runner.testsLocation.devServer.get().use { baseUrl ->
            browser.use {
                val page = browser.newPage()
                page.use {
                    page.setDefaultTimeout(runner.timeout.inWholeMilliseconds.toDouble())
                    var finished = false
                    page.onConsoleMessage {
                        if (it.text().startsWith(runner.finishMarker)) {
                            finished = true
                        } else {
                            handler.write(it.text().toByteArray())
                            handler.writeEndLine()
                        }
                    }
                    val url = runner.buildTestsExecutionerUrl(baseUrl)
                    log.info("Execute JS tests with ${runner.name} runner at URL: $url")
                    page.navigate(url)
                    page.waitForCondition({ finished })
                }
            }
        }
    }

    override fun stopNow() {
        // TODO: implement stop now now support
        log.warn("Playwright executor doesn't support immediate stop")
    }
}
