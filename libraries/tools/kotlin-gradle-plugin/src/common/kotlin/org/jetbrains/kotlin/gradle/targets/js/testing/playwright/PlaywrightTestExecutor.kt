/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.playwright

import com.google.gson.Gson
import org.gradle.api.internal.tasks.testing.TestExecuter
import org.gradle.api.internal.tasks.testing.TestExecutionSpec
import org.gradle.api.internal.tasks.testing.TestResultProcessor
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessageOutputStreamHandler
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

private val log = LoggerFactory.getLogger("org.jetbrains.kotlin.gradle.tasks.testing.PlaywrightTestExecutor")

internal data class PwTestResult(val suites: List<PwSuite> = emptyList())

internal data class PwSuite(
    val name: String = "",
    val duration: Long = 0,
    val tests: List<PwTest> = emptyList(),
    val suites: List<PwSuite> = emptyList()
)

internal data class PwTest(
    val name: String = "",
    val status: String = "",
    val duration: Long = 0,
    val errorMessage: String? = null,
    val errorStack: String? = null,
    val expected: String? = null,
    val actual: String? = null
)

private fun tcEscape(s: String): String = s
    .replace("|", "||")
    .replace("'", "|'")
    .replace("\n", "|n")
    .replace("\r", "|r")
    .replace("[", "|[")
    .replace("]", "|]")

private fun tcMessage(name: String, attrs: Map<String, String>): String {
    val body = attrs.entries.joinToString(" ") { (k, v) -> "$k='${tcEscape(v)}'" }
    return "##teamcity[$name $body]"
}

private fun formatTcMessages(result: PwTestResult): List<String> {
    val out = mutableListOf<String>()
    for (suite in result.suites) formatSuite(suite, out)
    return out
}

private fun formatSuite(suite: PwSuite, out: MutableList<String>) {
    out += tcMessage("testSuiteStarted", mapOf("name" to suite.name))
    for (test in suite.tests) formatTest(test, out)
    for (child in suite.suites) formatSuite(child, out)
    out += tcMessage("testSuiteFinished", mapOf("name" to suite.name, "duration" to suite.duration.toString()))
}

private fun formatTest(test: PwTest, out: MutableList<String>) {
    if (test.status == "pending") {
        out += tcMessage("testIgnored", mapOf("name" to test.name, "message" to test.name))
        return
    }
    out += tcMessage("testStarted", mapOf("name" to test.name, "captureStandardOutput" to "true"))
    if (test.status == "failed") {
        val attrs = linkedMapOf("name" to test.name)
        test.errorMessage?.let { attrs["message"] = it }
        test.errorStack?.let { attrs["details"] = it }
        if (test.expected != null && test.actual != null) {
            attrs["type"] = "comparisonFailure"
            attrs["expected"] = test.expected
            attrs["actual"] = test.actual
        }
        out += tcMessage("testFailed", attrs)
    }
    out += tcMessage("testFinished", mapOf("name" to test.name, "duration" to test.duration.toString()))
}

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
                    val jsonLine = proc.inputStream.bufferedReader().readLine()
                    if (jsonLine != null) {
                        try {
                            val result = Gson().fromJson(jsonLine, PwTestResult::class.java)
                            for (msg in formatTcMessages(result)) {
                                handler.write(msg.toByteArray())
                                handler.writeEndLine()
                            }
                        } catch (e: Exception) {
                            log.error("Failed to parse Playwright test results JSON: $jsonLine", e)
                        }
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
