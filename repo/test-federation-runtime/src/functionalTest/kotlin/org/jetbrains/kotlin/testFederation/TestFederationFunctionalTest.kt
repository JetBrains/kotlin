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
import kotlin.time.Duration.Companion.seconds

/**
 * This test will launch a build in different [TestFederationMode] and affected [Domain]s.
 * The build launches the ':repo:test-federation-runtime:test' task and parses the output to check if the tests were executed correctly.
 * (e.g., the compiler contract test is expected to only be executed when the compiler subsystem is affected, or the full test mode is specified)
 */
class ContractAndSmokeTest {
    @Test
    fun `test - smoke - js contract`() {
        val result = runTestBuild(TestFederationMode.Smoke, Domain.Js)
        assertEquals(
            setOf(
                TestResult("PseudoTest", "smoke test"),
                TestResult("PseudoTest", "js contract test"),
            ),
            result.executedTests
        )
    }

    @Test
    fun `test - smoke - wasm contract`() {
        val result = runTestBuild(TestFederationMode.Smoke, Domain.Wasm)
        assertEquals(
            setOf(
                TestResult("PseudoTest", "smoke test"),
                TestResult("PseudoTest", "wasm contract test"),
            ),
            result.executedTests
        )
    }


    @Test
    fun `test - smoke - js and wasm contract`() {
        val result = runTestBuild(TestFederationMode.Smoke, Domain.Wasm, Domain.Js)
        assertEquals(
            setOf(
                TestResult("PseudoTest", "smoke test"),
                TestResult("PseudoTest", "js contract test"),
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
                TestResult("PseudoTest", "domain test"),
                TestResult("PseudoTest", "smoke test"),
                TestResult("PseudoTest", "js contract test"),
                TestResult("PseudoTest", "wasm contract test"),
                TestResult("PseudoTest", "gradle contract test"),
            ),
            result.executedTests
        )
    }

    /**
     * If the test task is marked as 'isSmokeTest', then we expect all tests to be executed, always
     */
    @Test
    fun `test - smokeTestConfig 'RunAllTests'`() {
        val result = runTestBuild(TestFederationMode.Smoke, smokeTestConfig = "RunAllTests")
        assertEquals(
            setOf(
                TestResult("PseudoTest", "domain test"),
                TestResult("PseudoTest", "smoke test"),
                TestResult("PseudoTest", "js contract test"),
                TestResult("PseudoTest", "wasm contract test"),
                TestResult("PseudoTest", "gradle contract test"),
            ),
            result.executedTests
        )
    }

    /**
     * If the test task is marked as 'isSmokeTest = false', then we expect it to be skipped in smoke test mode.
     */
    @Test
    fun `test - smokeTestConfig 'Disabled'`() {
        val result = runTestBuild(TestFederationMode.Smoke, smokeTestConfig = "Disabled")
        assertEquals(
            emptySet(),
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
 * Executes all tests in ':repo:test-federation-runtime:test' with the given [mode] and [affected] domains.
 * All executed tests are parsed and returned in [TestBuildResult.executedTests].
 */
private fun runTestBuild(mode: TestFederationMode, vararg affected: Domain, smokeTestConfig: String? = null): TestBuildResult {
    val builder = ProcessBuilder(
        "./gradlew", ":repo:test-federation-runtime:test", "--rerun", "--no-daemon",
        "-P$TEST_FEDERATION_ENABLED_KEY=true",
        "-Porg.gradle.daemon.idletimeout=${10.seconds.inWholeMilliseconds}",
    )

    builder.environment()[TEST_FEDERATION_MODE_ENV_KEY] = mode.name

    if (smokeTestConfig != null) {
        builder.environment()["_PSEUDO_TEST_"] = smokeTestConfig
    }

    if (affected.isNotEmpty()) {
        builder.environment()[TEST_FEDERATION_AFFECTED_DOMAINS_ENV_KEY] = affected.joinToString(";") { it.name }
    } else builder.environment()[TEST_FEDERATION_AFFECTED_DOMAINS_ENV_KEY] = "<none>"

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

        if (!process.waitFor(3, TimeUnit.MINUTES)) {
            throw TimeoutException("Timed out waiting for Gradle build process")
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
    if (exitCode != 0) error(buildString {
        appendLine("Build failed with exit code $exitCode\n")
        appendLine("Stdout:")
        output.forEach { appendLine(it) }
        appendLine("Stderr:")
        error.forEach { appendLine(it) }
    })

    return TestBuildResult(output, error, tests.toSet())
}
