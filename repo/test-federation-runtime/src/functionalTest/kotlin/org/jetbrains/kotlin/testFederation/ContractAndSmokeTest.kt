/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.testFederation

import org.jetbrains.kotlin.testFederation.TestBuildResult.TestResult
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

/**
 * This test will launch a build in different [TestFederationMode] and affected [Subsystem]s.
 * The build launches the ':repo:test-federation-runtime:test' task and parses the output to check if the tests were executed correctly.
 * (e.g., the compiler contract test is expected to only be executed when the compiler subsystem is affected, or the full test mode is specified)
 */
class ContractAndSmokeTest {
    @Test
    fun `test - smoke - compiler contract`() {
        val result = runTestBuild(TestFederationMode.Smoke, Subsystem.Compiler)
        assertEquals(
            setOf(
                TestResult("PseudoTest", "smoke test"),
                TestResult("PseudoTest", "compiler contract test"),
            ),
            result.executedTests
        )
    }

    @Test
    fun `test - smoke - wasm contract`() {
        val result = runTestBuild(TestFederationMode.Smoke, Subsystem.Wasm)
        assertEquals(
            setOf(
                TestResult("PseudoTest", "smoke test"),
                TestResult("PseudoTest", "wasm contract test"),
            ),
            result.executedTests
        )
    }


    @Test
    fun `test - smoke - compiler and wasm contract`() {
        val result = runTestBuild(TestFederationMode.Smoke, Subsystem.Wasm, Subsystem.Compiler)
        assertEquals(
            setOf(
                TestResult("PseudoTest", "smoke test"),
                TestResult("PseudoTest", "compiler contract test"),
                TestResult("PseudoTest", "wasm contract test"),
            ),
            result.executedTests
        )
    }

    @Test
    fun `test - mode full`() {
        val result = runTestBuild(TestFederationMode.Full)
        assertEquals(
            setOf(
                TestResult("PseudoTest", "system test"),
                TestResult("PseudoTest", "smoke test"),
                TestResult("PseudoTest", "compiler contract test"),
                TestResult("PseudoTest", "wasm contract test"),
            ),
            result.executedTests
        )
    }
}

private data class TestBuildResult(
    val output: List<String>, val error: List<String>,
    val executedTests: Set<TestResult>,
) {
    data class TestResult(val className: String, val methodName: String, val status: String = "PASSED") {
        override fun toString(): String {
            return "$className > $methodName() $status"
        }
    }
}

/**
 * Executes all tests in ':repo:test-federation-runtime:test' with the given [mode] and [affected] subsystems.
 * All executed tests are parsed and returned in [TestBuildResult.executedTests].
 */
private fun runTestBuild(mode: TestFederationMode, vararg affected: Subsystem): TestBuildResult {
    val builder = ProcessBuilder(
        "./gradlew", ":repo:test-federation-runtime:test", "--rerun",
        "-P$TEST_FEDERATION_ENABLED_KEY=true"
    )

    builder.environment()[TEST_FEDERATION_MODE_ENV_KEY] = mode.name
    if (affected.isNotEmpty()) {
        builder.environment()[TEST_FEDERATION_AFFECTED_SUBSYSTEMS_ENV_KEY] = affected.joinToString(";") { it.name }
    }

    val process = builder.start()

    Runtime.getRuntime().addShutdownHook(Thread {
        if (process.isAlive) process.destroyForcibly()
    })

    val output = mutableListOf<String>()
    val error = mutableListOf<String>()

    try {
        val stdoutReader = thread(isDaemon = true) {
            process.inputStream.bufferedReader().lineSequence().forEach { line ->
                output.add(line)
            }
        }

        val stderrReader = thread(isDaemon = true) {
            process.errorStream.bufferedReader().lineSequence().forEach { line ->
                System.err.println(line)
                error.add(line)
            }
        }

        if (!process.waitFor(1, TimeUnit.MINUTES)) {
            throw TimeoutException("Process didn't finish in 1 minute")
        }

        stdoutReader.join(1.minutes.inWholeMilliseconds)
        stderrReader.join(1.minutes.inWholeMilliseconds)

    } finally {
        if (process.isAlive) {
            process.destroy()
        }
    }

    val testResultRegex = Regex("(?<testClass>.*) > (?<testName>.*)\\(\\) (?<status>.*)")
    val tests = output.mapNotNull { line ->
        val match = testResultRegex.matchEntire(line) ?: return@mapNotNull null
        TestResult(match.groupValues[1], match.groupValues[2], match.groupValues[3])
    }

    val exitCode = process.exitValue()
    if (exitCode != 0) error("Build failed with exit code $exitCode")

    return TestBuildResult(output, error, tests.toSet())
}