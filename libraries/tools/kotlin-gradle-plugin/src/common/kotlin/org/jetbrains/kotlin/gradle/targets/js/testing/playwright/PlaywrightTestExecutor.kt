/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.playwright

import org.gradle.api.internal.tasks.testing.TestExecuter
import org.gradle.api.internal.tasks.testing.TestExecutionSpec
import org.gradle.api.internal.tasks.testing.TestResultProcessor
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessageOutputStreamHandler
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

private val log = LoggerFactory.getLogger("org.jetbrains.kotlin.gradle.tasks.testing.PlaywrightTestExecutor")

internal class PwExecutionSpec(
    val createClient: (TestResultProcessor, Logger) -> TCServiceMessagesClient,
    val url: String,
    val nodeExecutable: String,
    val nodeModulesDir: File,
    val npmProjectDir: File,
) : TestExecutionSpec

internal class PlaywrightTestExecutor : TestExecuter<PwExecutionSpec> {

    @Volatile
    private var process: Process? = null

    override fun execute(spec: PwExecutionSpec, testResultProcessor: TestResultProcessor) {
        val client = spec.createClient(testResultProcessor, log)

        val handler = TCServiceMessageOutputStreamHandler(
            client,
            { },
            log,
            false,
        )

//        client.root {
//            val browser = playwright.chromium().launch()
//            browser.use {
//                val page = browser.newPage()
//                var finished = false
//                page.onConsoleMessage {
//                    if (it.text().startsWith("THE END")) {
//                        finished = true
//                    } else {
//                        handler.write(it.text().toByteArray())
//                        handler.writeEndLine()
//                    }
//                }
//                page.navigate(spec.url)
//                page.waitForCondition { finished }
//            }
//        }
//
//        PlaywrightImpl


        val scriptFile = File.createTempFile("playwright-test-runner", ".js")
        try {
            scriptFile.deleteOnExit()
            javaClass.classLoader
                .getResourceAsStream("kotlinJsTestsForBrowser/playwright-test-runner.js")!!
                .use { it.copyTo(scriptFile.outputStream()) }

            client.root {
                val proc = ProcessBuilder(spec.nodeExecutable, scriptFile.absolutePath, spec.url)
                    .directory(spec.npmProjectDir)
                    .apply { environment()["NODE_PATH"] = spec.nodeModulesDir.absolutePath }
                    .start()
                process = proc

                val stdoutThread = Thread {
                    proc.inputStream.bufferedReader().forEachLine { line ->
                        handler.write(line.toByteArray())
                        handler.writeEndLine()
                    }
                }
                stdoutThread.start()

                val stderrThread = Thread {
                    proc.errorStream.bufferedReader().forEachLine { line ->
                        log.error(line)
                    }
                }
                stderrThread.start()

                proc.waitFor()
                stdoutThread.join()
                stderrThread.join()
            }
        } finally {
            scriptFile.delete()
        }
    }

    override fun stopNow() {
        process?.destroyForcibly()
    }
}
