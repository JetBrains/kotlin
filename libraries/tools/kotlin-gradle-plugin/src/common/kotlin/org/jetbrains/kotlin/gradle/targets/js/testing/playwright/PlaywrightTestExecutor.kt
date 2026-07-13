/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 *
 * PwExecutionSpec.createImpl in this file is based on code from the Playwright project (https://github.com/microsoft/playwright-java)
 * Original method: com.microsoft.playwright.impl.PlaywrightImpl.createImpl
 * License: Apache License 2.0
 *
 * Modifications:
 * - use reflection to access private constructor of com.microsoft.playwright.impl.Connection
 * - override
 *
 * PwExecutionSpec.createProcessBuilde in this file is based on code from the Playwright project (https://github.com/microsoft/playwright-java)
 * Original method: com.microsoft.playwright.impl.driver.Driver.createProcessBuilder
 * License: Apache License 2.0
 *
 * Modifications:
 * - change path to cli.js
 *
 * Copyright [Original Playwright authors]
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.playwright

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.impl.Connection
import com.microsoft.playwright.impl.driver.Driver
import org.gradle.api.internal.tasks.testing.TestExecuter
import org.gradle.api.internal.tasks.testing.TestExecutionSpec
import org.gradle.api.internal.tasks.testing.TestResultProcessor
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessageOutputStreamHandler
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClient
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTestsLocation
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.absolutePathString
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
    val browsersDirectory: Path,
    val testsLocation: KotlinJsTestsLocation,
    val buildTestsExecutionerUrl: (baseUrl: URI) -> URI,
    val timeout: Duration,
    val finishMarker: String,
    val headless: Boolean,
    val launchArgs: List<String>,
    val launchEnvironmentVariables: Map<String, String>,
    val customBrowserExecutable: Path?,
)

/**
 * Execution spec containing all configured browser runners that have to be launched
 * within a single test task invocation.
 */
internal class PwExecutionSpec(
    val createClient: (TestResultProcessor, Logger) -> TCServiceMessagesClient,
    val runners: List<PwRunnerSpec>,
    val nodeExecutable: String,
    val playwrightCli: String,
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
            //  Use thin layer of Java Classes to interact with Playwright via std in/out pipes.
            val playwright = spec.createImpl(
                Playwright.CreateOptions().setEnv(
                    mapOf(
                        "PLAYWRIGHT_NODEJS_PATH" to spec.nodeExecutable,
                        "PLAYWRIGHT_BROWSERS_PATH" to spec.runners.first().browsersDirectory.absolutePathString()
                    )
                )
            )

            playwright.use {
                with(client) {
                    root {
                        for (runner in spec.runners) {
                            suite(id = runner.name) {
                                try {
                                    executeRunner(playwright, runner, handler)
                                } catch (t: Throwable) {
                                    val tsEnd = System.currentTimeMillis()
                                    closeSuiteWithFailingTestCause(suiteNode = this, tsEnd, failingTestCause = t)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun PwExecutionSpec.createImpl(options: Playwright.CreateOptions?): Playwright {
        val env = if (options != null && options.env != null) {
            options.env
        } else mutableMapOf<String?, String?>()

        try {
            val pb = createProcessBuilder(env)
            pb.command().add("run-driver")
            pb.redirectError(ProcessBuilder.Redirect.INHERIT)
            val p = pb.start()
            val pipeTransportClass = try {
                Class.forName("com.microsoft.playwright.impl.PipeTransport")
            } catch (e: Throwable) {
                throw RuntimeException("Failed to load PipeTransport class", e)
            }

            // KT-87396: Merge fix to playwright upstream to get rid of reflection calls
            val pipeTransport = try {
                val constructor = pipeTransportClass.getDeclaredConstructor(
                    InputStream::class.java,
                    OutputStream::class.java
                )
                constructor.isAccessible = true
                constructor.newInstance(p.inputStream, p.outputStream)
            } catch (e: Throwable) {
                throw RuntimeException("Failed to create PipeTransport via reflection", e)
            }

            // Create Connection via reflection
            val connectionClass = try {
                Class.forName("com.microsoft.playwright.impl.Connection")
            } catch (e: Throwable) {
                throw RuntimeException("Failed to load Connection class", e)
            }

            val connection = try {
                val transportInterface = Class.forName("com.microsoft.playwright.impl.Transport")
                val constructor = connectionClass.getDeclaredConstructor(
                    transportInterface,
                    MutableMap::class.java
                )
                constructor.isAccessible = true
                constructor.newInstance(pipeTransport, env)
            } catch (e: Throwable) {
                throw RuntimeException("Failed to create Connection via reflection", e)
            }

            val result = (connection as Connection).initializePlaywright()

            // Set driverProcess via reflection
            try {
                val field = result.javaClass.getDeclaredField("driverProcess")
                field.isAccessible = true
                field.set(result, p)
            } catch (e: Throwable) {
                throw RuntimeException("Failed to create Connection via reflection", e)
            }
            return result
        } catch (e: IOException) {
            throw RuntimeException("Failed to launch driver", e)
        }
    }


    private fun PwExecutionSpec.createProcessBuilder(env: MutableMap<String, String>): ProcessBuilder {
        val pb = ProcessBuilder(nodeExecutable)
        pb.command().add(playwrightCli) // This is the patched part. Original code loads playwright-cli from the JAR.
        pb.environment().putAll(env)
        pb.environment()["PW_LANG_NAME"] = "java"
        pb.environment()["PW_LANG_NAME_VERSION"] = getMajorJavaVersion()
        val version = Driver::class.java.getPackage().implementationVersion
        if (version != null) {
            pb.environment()["PW_CLI_DISPLAY_VERSION"] = version
        }
        return pb
    }

    private fun getMajorJavaVersion(): String {
        val version = System.getProperty("java.version")
        if (version.startsWith("1.")) {
            return version.substring(2, 3)
        }
        val dot = version.indexOf(".")
        if (dot != -1) {
            return version.substring(0, dot)
        }
        return version
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
                if (runner.launchEnvironmentVariables.isNotEmpty()) setEnv(runner.launchEnvironmentVariables)
                if (runner.customBrowserExecutable != null) setExecutablePath(runner.customBrowserExecutable)
            }

        log.info("Launching playwright runner '${runner.name}' (${runner.browserKind})")
        val browser: Browser = browserType.launch(launchOptions)
        val testLocationUrl = runner.testsLocation.url.get()
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
                val url = runner.buildTestsExecutionerUrl(testLocationUrl)
                log.info("Execute JS tests with ${runner.name} runner at URL: $url")
                page.navigate(url.toString())
                page.waitForCondition({ finished })
            }
        }
    }

    override fun stopNow() {
        // TODO: implement stop now now support
        log.warn("Playwright executor doesn't support immediate stop")
    }
}
