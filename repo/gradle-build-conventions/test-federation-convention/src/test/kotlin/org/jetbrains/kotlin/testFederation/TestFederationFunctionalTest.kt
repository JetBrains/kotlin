/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.testFederation

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.jetbrains.kotlin.testFederation.TestBuildResult.TestResult
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlin.time.Duration.Companion.seconds

/**
 * This test will launch a build in different [TestFederationMode] and affected [Domain]s.
 * The build launches the ':repo:test-federation-runtime:test' task and parses the output to check if the tests were executed correctly.
 * (e.g., the compiler contract test is expected to only be executed when the compiler subsystem is affected, or the full test mode is specified)
 */
class TestFederationFunctionalTest {
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

    @Test
    fun `test - test federation disabled`() {
        /* Test with federation enabled */
        run {
            val result = runTestBuild(TestFederationMode.Smoke, testFederationEnabled = true)
            assertEquals(setOf(TestResult("PseudoTest", "smoke test")), result.executedTests)
        }

        /* Test with federation disabled */
        run {
            val result = runTestBuild(TestFederationMode.Smoke, testFederationEnabled = false)
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
    }

    /**
     * We will check if  running a test with test federation (full mode) produces a cache entry, which can be used
     * by running the same test task with test federation disabled.
     */
    @Test
    fun `test - build with test federation enabled (full) - build with test federation disabled - reuses build caches`(@TempDir cache: Path) {
        val buildCacheArgs = buildCacheArgs(cache)

        cleanTest()
        runTestBuild(
            mode = TestFederationMode.Full,
            affected = Domain.entries.toTypedArray(),
            additionalCliArgs = buildCacheArgs,
            rerun = false
        ).apply {
            assertEquals(TaskOutcome.SUCCESS, buildResult.requireTask(":repo:test-federation-runtime:test").outcome)
            cache.listDirectoryEntries().filterNot { it.name == "gc.properties" }.ifEmpty {
                fail("No build cache entries produced after first build")
            }
        }

        cleanTest()
        runTestBuild(
            mode = TestFederationMode.Full,
            affected = Domain.entries.toTypedArray(),
            additionalCliArgs = buildCacheArgs,
            rerun = false,
            testFederationEnabled = false
        ).apply {
            assertEquals(TaskOutcome.FROM_CACHE, buildResult.requireTask(":repo:test-federation-runtime:test").outcome)
        }
    }

    /**
     * We will check if running a test with test federation disabled produces a cache entry, which can be used
     * by running the same test with test federation enabled (full mode)
     */
    @Test
    fun `test - build with test federation disabled - build with test federation enabled (full) - reuses build caches`(@TempDir cache: Path) {
        val buildCacheArgs = buildCacheArgs(cache)

        cleanTest()
        runTestBuild(
            mode = TestFederationMode.Full,
            affected = Domain.entries.toTypedArray(),
            additionalCliArgs = buildCacheArgs,
            rerun = false,
            testFederationEnabled = false
        ).apply {
            assertEquals(TaskOutcome.SUCCESS, buildResult.requireTask(":repo:test-federation-runtime:test").outcome)
            cache.listDirectoryEntries().filterNot { it.name == "gc.properties" }.ifEmpty {
                fail("No build cache entries produced after first build")
            }
        }

        cleanTest()
        runTestBuild(
            mode = TestFederationMode.Full,
            affected = Domain.entries.toTypedArray(),
            additionalCliArgs = buildCacheArgs,
            rerun = false,
            testFederationEnabled = true
        ).apply {
            assertEquals(TaskOutcome.FROM_CACHE, buildResult.requireTask(":repo:test-federation-runtime:test").outcome)
        }
    }

    @Test
    fun `test - build with test federation enabled - build in smoke mode - cant reuse caches`(@TempDir cache: Path) {
        val buildCacheArgs = buildCacheArgs(cache)

        cleanTest()
        runTestBuild(
            mode = TestFederationMode.Full,
            affected = Domain.entries.toTypedArray(),
            additionalCliArgs = buildCacheArgs,
            rerun = false,
            testFederationEnabled = false
        ).apply {
            assertEquals(TaskOutcome.SUCCESS, buildResult.requireTask(":repo:test-federation-runtime:test").outcome)
            cache.listDirectoryEntries().filterNot { it.name == "gc.properties" }.ifEmpty {
                fail("No build cache entries produced after first build")
            }
        }

        cleanTest()
        runTestBuild(
            mode = TestFederationMode.Smoke,
            additionalCliArgs = buildCacheArgs,
            rerun = false,
            testFederationEnabled = true
        ).apply {
            assertEquals(TaskOutcome.SUCCESS, buildResult.requireTask(":repo:test-federation-runtime:test").outcome)
            assertEquals(setOf(TestResult("PseudoTest", "smoke test")), executedTests)
        }
    }

    @Test
    fun `test - build cache can be reused in smoke mode - if affected domains match`(@TempDir cache: Path) {
        val buildCacheArgs = buildCacheArgs(cache)
        cleanTest()
        runTestBuild(
            mode = TestFederationMode.Smoke,
            affected = arrayOf(Domain.Js),
            additionalCliArgs = buildCacheArgs,
            rerun = false,
        ).apply {
            assertEquals(TaskOutcome.SUCCESS, buildResult.requireTask(":repo:test-federation-runtime:test").outcome)
            assertEquals(setOf(TestResult("PseudoTest", "smoke test"), TestResult("PseudoTest", "js contract test")), executedTests)
        }

        cleanTest()
        runTestBuild(
            mode = TestFederationMode.Smoke,
            affected = arrayOf(Domain.Js),
            additionalCliArgs = buildCacheArgs,
            rerun = false,
        ).apply {
            assertEquals(TaskOutcome.FROM_CACHE, buildResult.requireTask(":repo:test-federation-runtime:test").outcome)
        }

        cleanTest()
        runTestBuild(
            mode = TestFederationMode.Smoke,
            affected = arrayOf(Domain.Wasm),
            additionalCliArgs = buildCacheArgs,
            rerun = false,
        ).apply {
            assertEquals(TaskOutcome.SUCCESS, buildResult.requireTask(":repo:test-federation-runtime:test").outcome)
            assertEquals(setOf(TestResult("PseudoTest", "smoke test"), TestResult("PseudoTest", "wasm contract test")), executedTests)
        }
    }
}

private data class TestBuildResult(
    val buildResult: BuildResult,
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
private fun runTestBuild(
    mode: TestFederationMode,
    vararg affected: Domain,
    smokeTestConfig: String? = null,
    testFederationEnabled: Boolean = true,
    rerun: Boolean = true,
    additionalCliArgs: List<String> = emptyList(),
): TestBuildResult {
    val environment = defaultEnv().toMutableMap().apply {
        remove(TEST_FEDERATION_ENABLED_ENV_KEY)
        remove(TEST_FEDERATION_MODE_ENV_KEY)
        remove(TEST_FEDERATION_AFFECTED_DOMAINS_ENV_KEY)

        this[TEST_FEDERATION_MODE_ENV_KEY] = mode.name

        if (smokeTestConfig != null) {
            this["_PSEUDO_TEST_"] = smokeTestConfig
        }

        this[TEST_FEDERATION_AFFECTED_DOMAINS_ENV_KEY] = if (affected.isNotEmpty()) {
            affected.joinToString(";") { it.name }
        } else {
            "<none>"
        }
    }

    val arguments = buildList {
        add(":repo:test-federation-runtime:test")
        add("-Dorg.gradle.daemon=false")
        add("-P$TEST_FEDERATION_ENABLED_KEY=$testFederationEnabled")
        add("-Porg.gradle.daemon.idletimeout=${10.seconds.inWholeMilliseconds}")
        if (rerun) add("--rerun")
        addAll(additionalCliArgs)
    }

    val buildResult = try {
        createGradleRunner(environment = environment).withArguments(arguments).build()
    } catch (failure: UnexpectedBuildFailure) {
        val output = failure.buildResult.output
        error(buildString {
            appendLine("Build failed with non-zero exit code")
            appendLine("Output:")
            output.lineSequence().forEach { appendLine(it) }
        })
    }

    val output = buildResult.output.lineSequence().toList()

    val testResultRegex = Regex("(?<testClass>.*) > (?<testName>.*)\\(\\) (?<status>.*)")
    val tests = output.mapNotNull { line ->
        val match = testResultRegex.matchEntire(line) ?: return@mapNotNull null
        TestResult(match.groupValues[1], match.groupValues[2], match.groupValues[3])
    }

    return TestBuildResult(buildResult, tests.toSet())
}

private fun cleanTest(): BuildResult {
    return try {
        createGradleRunner().withArguments(
            ":repo:test-federation-runtime:cleanTest",
            "-Porg.gradle.daemon.idletimeout=${10.seconds.inWholeMilliseconds}",
        ).build()
    } catch (failure: UnexpectedBuildFailure) {
        error(buildString {
            appendLine("Gradle cleaning failed with non-zero exit code")
            appendLine("Output:")
            failure.buildResult.output.lineSequence().forEach { appendLine(it) }
        })
    }
}

private fun createGradleRunner(
    environment: Map<String, String> = defaultEnv(),
): GradleRunner {
    val gradleUserHome = System.getenv("GRADLE_USER_HOME") ?: error("Missing 'GRADLE_USER_HOME' environment variable")
    return GradleRunner.create()
        .withProjectDir(Path("").toAbsolutePath().toFile())
        .withEnvironment(System.getenv() + environment)
        .withTestKitDir(File(gradleUserHome))
}

private fun defaultEnv(): Map<String, String> {
    return System.getenv().toMutableMap().apply {
        remove(TEST_FEDERATION_ENABLED_ENV_KEY)
        remove(TEST_FEDERATION_MODE_ENV_KEY)
        remove(TEST_FEDERATION_AFFECTED_DOMAINS_ENV_KEY)
    }
}

private fun buildCacheArgs(cache: Path) = listOf(
    "-Pkotlin.build.cache.local.directory=$cache",
    "-Pkotlin.build.cache.local.enabled=true"
)

private fun BuildResult.requireTask(path: String) =
    task(path) ?: fail("Task '$path' could not be found\nTasks: ${tasks.joinToString("\n")}")
