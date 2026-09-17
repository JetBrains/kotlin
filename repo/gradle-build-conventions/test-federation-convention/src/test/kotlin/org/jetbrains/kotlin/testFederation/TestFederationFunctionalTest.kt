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
import kotlin.collections.filterNot
import kotlin.io.path.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlin.time.Duration.Companion.seconds

/**
 * Runs `:repo:test-runtime:test` with different modes and domain selections, then checks which tests ran.
 * Covers full test runs, selection by annotations and automatic sampling, and nightly filters.
 */
class TestFederationFunctionalTest {

    @Test
    fun `test - smoke - compiler contract`() {
        val result = runTestBuild(TestFederationMode.Smoke, Domain.CompilerInfrastructure)
        assertEquals(
            setOf(TestResult("PseudoTest", "smoke test")),
            result.executedTests
        )
    }

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
    fun `test - smoke - executes contracts of changed domains only`() {
        val result = runTestBuild(
            TestFederationMode.Smoke,
            changed = arrayOf(Domain.Js, Domain.Gradle),
            affected = listOf(Domain.Js, Domain.Gradle, Domain.Wasm)
        )
        assertEquals(
            setOf(
                TestResult("PseudoTest", "smoke test"),
                TestResult("PseudoTest", "js contract test"),
                TestResult("PseudoTest", "gradle contract test"),
            ),
            result.executedTests
        )
    }

    @Test
    fun `test - mode full`() {
        val result = runTestBuild(TestFederationMode.Full)
        assertEquals(allTests, result.executedTests)
    }

    @Test
    fun `test - mode full - nightly disabled`() {
        val result = runTestBuild(TestFederationMode.Full, nightly = false)
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

    @Test
    fun `test - mode full - nightly enabled`() {
        val result = runTestBuild(TestFederationMode.Full, nightly = true)
        assertEquals(allTests, result.executedTests)
    }


    /**
     * Configuring RunAllTests selects all tests even when a different mode is explicitly requested.
     */
    @Test
    fun `test - smokeTestConfig RunAllTests`() {
        val result = runTestBuild(TestFederationMode.Smoke, smokeTestConfig = "RunAllTests")
        assertEquals(allTests, result.executedTests)
    }

    /**
     * Configuring Disabled skips the task when it is not selected for a full test run.
     */
    @Test
    fun `test - smokeTestConfig Disabled`() {
        val result = runTestBuild(TestFederationMode.Smoke, smokeTestConfig = "Disabled")
        assertEquals(
            emptySet(),
            result.executedTests
        )
    }

    /**
     * Overriding the task's domains changes whether it is selected for a full test run.
     */
    @Test
    fun `test - Test testFederationDomains`() {
        /* Js contains changes, task belongs to no domain -> select @MustRunAlways and @MustRunOnChangesInJs tests. */
        run {
            val result = runTestBuild(changed = arrayOf(Domain.Js), testTaskDomainsOverride = listOf())
            assertEquals(
                setOf(
                    TestResult("PseudoTest", "smoke test"),
                    TestResult("PseudoTest", "js contract test")
                ),
                result.executedTests
            )
        }

        /* Js contains changes, task belongs to Js and Wasm -> select all tests. */
        run {
            val result = runTestBuild(changed = arrayOf(Domain.Js), testTaskDomainsOverride = listOf(Domain.Js, Domain.Wasm))
            assertEquals(allTests, result.executedTests)
        }
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
                    TestResult("PseudoTest", "nightly test"),
                ),
                result.executedTests
            )
        }
    }

    @Test
    fun `test - explicit subsets override selects requested subsets`() {
        run {
            val result = runTestBuild(subsets = "SmokeTests")
            assertEquals(setOf(TestResult("PseudoTest", "smoke test")), result.executedTests)
        }

        run {
            val result = runTestBuild(changed = arrayOf(Domain.Wasm), subsets = "SmokeTests,ContractTestsForWasm")
            assertEquals(
                setOf(
                    TestResult("PseudoTest", "smoke test"),
                    TestResult("PseudoTest", "wasm contract test"),
                ),
                result.executedTests
            )
        }

        run {
            val result = runTestBuild(mode = TestFederationMode.Full, subsets = "AllTests")
            assertEquals(allTests, result.executedTests)
        }
    }

    @Test
    fun `test - explicit subsets override works without enabling test federation`() {
        val result = runTestBuild(subsets = "SmokeTests", testFederationEnabled = false)
        assertEquals(setOf(TestResult("PseudoTest", "smoke test")), result.executedTests)
    }

    @Test
    fun `test - explicit subsets override via environment variable`() {
        val result = runTestBuild(subsetsEnv = "SmokeTests", testFederationEnabled = false)
        assertEquals(setOf(TestResult("PseudoTest", "smoke test")), result.executedTests)
    }

    @Test
    fun `test - explicit subsets override does not override alwaysRunAllTests`() {
        val result = runTestBuild(subsets = "SmokeTests", smokeTestConfig = "RunAllTests")
        assertEquals(allTests, result.executedTests)
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
            changed = Domain.entries.toTypedArray(),
            additionalCliArgs = buildCacheArgs,
            rerun = false
        ).apply {
            assertEquals(TaskOutcome.SUCCESS, buildResult.requireTask(":repo:test-runtime:test").outcome)
            cache.listDirectoryEntries().filterNot { it.name == "gc.properties" }.ifEmpty {
                fail("No build cache entries produced after first build")
            }
        }

        cleanTest()
        runTestBuild(
            mode = TestFederationMode.Full,
            changed = Domain.entries.toTypedArray(),
            additionalCliArgs = buildCacheArgs,
            rerun = false,
            testFederationEnabled = false
        ).apply {
            assertEquals(TaskOutcome.FROM_CACHE, buildResult.requireTask(":repo:test-runtime:test").outcome)
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
            changed = Domain.entries.toTypedArray(),
            additionalCliArgs = buildCacheArgs,
            rerun = false,
            testFederationEnabled = false
        ).apply {
            assertEquals(TaskOutcome.SUCCESS, buildResult.requireTask(":repo:test-runtime:test").outcome)
            cache.listDirectoryEntries().filterNot { it.name == "gc.properties" }.ifEmpty {
                fail("No build cache entries produced after first build")
            }
        }

        cleanTest()
        runTestBuild(
            mode = TestFederationMode.Full,
            changed = Domain.entries.toTypedArray(),
            additionalCliArgs = buildCacheArgs,
            rerun = false,
            testFederationEnabled = true
        ).apply {
            assertEquals(TaskOutcome.FROM_CACHE, buildResult.requireTask(":repo:test-runtime:test").outcome)
        }
    }

    @Test
    fun `test - build with test federation disabled - build with test federation enabled (full) and smoke+runAllTests - reuses build caches`(
        @TempDir cache: Path,
    ) {
        val buildCacheArgs = buildCacheArgs(cache)

        cleanTest()
        runTestBuild(
            mode = TestFederationMode.Full,
            smokeTestConfig = "RunAllTests",
            changed = Domain.entries.toTypedArray(),
            additionalCliArgs = buildCacheArgs,
            rerun = false,
            testFederationEnabled = false
        ).apply {
            assertEquals(TaskOutcome.SUCCESS, buildResult.requireTask(":repo:test-runtime:test").outcome)
            cache.listDirectoryEntries().filterNot { it.name == "gc.properties" }.ifEmpty {
                fail("No build cache entries produced after first build")
            }
        }

        cleanTest()
        runTestBuild(
            mode = TestFederationMode.Smoke,
            smokeTestConfig = "RunAllTests",
            changed = Domain.entries.toTypedArray(),
            additionalCliArgs = buildCacheArgs,
            rerun = false,
            testFederationEnabled = true
        ).apply {
            assertEquals(TaskOutcome.FROM_CACHE, buildResult.requireTask(":repo:test-runtime:test").outcome)
        }
    }

    @Test
    fun `test - build with test federation disabled - build in smoke mode - cant reuse caches`(@TempDir cache: Path) {
        val buildCacheArgs = buildCacheArgs(cache)

        cleanTest()
        runTestBuild(
            mode = TestFederationMode.Full,
            changed = Domain.entries.toTypedArray(),
            additionalCliArgs = buildCacheArgs,
            rerun = false,
            testFederationEnabled = false
        ).apply {
            assertEquals(TaskOutcome.SUCCESS, buildResult.requireTask(":repo:test-runtime:test").outcome)
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
            assertEquals(TaskOutcome.SUCCESS, buildResult.requireTask(":repo:test-runtime:test").outcome)
            assertEquals(setOf(TestResult("PseudoTest", "smoke test")), executedTests)
        }
    }

    @Test
    fun `test - build cache can be reused in smoke mode - if affected domains match`(@TempDir cache: Path) {
        val buildCacheArgs = buildCacheArgs(cache)
        cleanTest()
        runTestBuild(
            mode = TestFederationMode.Smoke,
            changed = arrayOf(Domain.Js),
            additionalCliArgs = buildCacheArgs,
            rerun = false,
        ).apply {
            assertEquals(TaskOutcome.SUCCESS, buildResult.requireTask(":repo:test-runtime:test").outcome)
            assertEquals(setOf(TestResult("PseudoTest", "smoke test"), TestResult("PseudoTest", "js contract test")), executedTests)
        }

        cleanTest()
        runTestBuild(
            mode = TestFederationMode.Smoke,
            changed = arrayOf(Domain.Js),
            additionalCliArgs = buildCacheArgs,
            rerun = false,
        ).apply {
            assertEquals(TaskOutcome.FROM_CACHE, buildResult.requireTask(":repo:test-runtime:test").outcome)
        }

        cleanTest()
        runTestBuild(
            mode = TestFederationMode.Smoke,
            changed = arrayOf(Domain.Wasm),
            additionalCliArgs = buildCacheArgs,
            rerun = false,
        ).apply {
            assertEquals(TaskOutcome.SUCCESS, buildResult.requireTask(":repo:test-runtime:test").outcome)
            assertEquals(setOf(TestResult("PseudoTest", "smoke test"), TestResult("PseudoTest", "wasm contract test")), executedTests)
        }
    }

    @Test
    fun `infer affected domains reports changed domains to TeamCity`() {
        val result = createGradleRunner().withArguments(
            "inferAffectedDomains",
            "-P$TEST_FEDERATION_ENABLED_KEY=true",
            "-P$TEST_FEDERATION_AFFECTED_DOMAINS_KEY=Js;Wasm",
            "-P$TEST_FEDERATION_CHANGED_DOMAINS_KEY=Js",
            "-P$TEST_FEDERATION_CHANGED_FILES_KEY=",
            "-Dorg.gradle.daemon.idletimeout=${10.seconds.inWholeMilliseconds}",
        ).build()

        assertContains(
            result.output,
            "##teamcity[setParameter name='$TEST_FEDERATION_AFFECTED_DOMAINS_KEY' value='Wasm;Js']"
        )

        assertContains(
            result.output,
            "##teamcity[setParameter name='$TEST_FEDERATION_CHANGED_DOMAINS_KEY' value='Js']"
        )
    }

    @Test
    fun `test - parameterized tests`() {
        val testClass = "org.jetbrains.kotlin.testFederation.PseudoParameterizedTest"
        val full = runTestEvents(TestFederationMode.Full, testFilter = testClass)
        val smoke = runTestEvents(TestFederationMode.Smoke, testFilter = testClass)
        val contract = runTestEvents(TestFederationMode.Smoke, Domain.Js, testFilter = testClass)
        val noSelectedTests = runTestEvents(TestFederationMode.Smoke, testFilter = "$testClass.*domain*")

        assertEquals(
            listOf(
                "Executed: contract 1", "Executed: contract 2",
                "Executed: domain 1", "Executed: domain 2",
                "Executed: smoke 1", "Executed: smoke 2",
            ),
            full
        )
        assertEquals(listOf("Executed: smoke 1", "Executed: smoke 2"), smoke)
        assertEquals(listOf("Executed: contract 1", "Executed: contract 2", "Executed: smoke 1", "Executed: smoke 2"), contract)
        assertEquals(emptyList(), noSelectedTests)
    }

    @Test
    fun `test - repeated tests`() {
        val testClass = "org.jetbrains.kotlin.testFederation.PseudoRepeatedTest"
        val full = runTestEvents(TestFederationMode.Full, testFilter = testClass)
        val smoke = runTestEvents(TestFederationMode.Smoke, testFilter = testClass)
        val contract = runTestEvents(TestFederationMode.Smoke, Domain.Js, testFilter = testClass)
        val noSelectedTests = runTestEvents(TestFederationMode.Smoke, testFilter = "$testClass.*domain*")

        assertEquals(
            listOf(
                "Executed: contract", "Executed: contract",
                "Executed: domain", "Executed: domain",
                "Executed: smoke", "Executed: smoke",
            ),
            full
        )
        assertEquals(listOf("Executed: smoke", "Executed: smoke"), smoke)
        assertEquals(listOf("Executed: contract", "Executed: contract", "Executed: smoke", "Executed: smoke"), contract)
        assertEquals(emptyList(), noSelectedTests)
    }

    @Test
    fun `test - custom test templates`() {
        val testClass = "org.jetbrains.kotlin.testFederation.PseudoTemplateTest"
        val full = runTestEvents(TestFederationMode.Full, testFilter = testClass)
        val smoke = runTestEvents(TestFederationMode.Smoke, testFilter = testClass)
        val contract = runTestEvents(TestFederationMode.Smoke, Domain.Js, testFilter = testClass)
        val noSelectedTests = runTestEvents(TestFederationMode.Smoke, testFilter = "$testClass.*domain*")

        assertEquals(
            listOf(
                "Executed: contract", "Executed: contract",
                "Executed: domain", "Executed: domain",
                "Executed: smoke", "Executed: smoke",
            ),
            full
        )
        assertEquals(listOf("Executed: smoke", "Executed: smoke"), smoke)
        assertEquals(listOf("Executed: contract", "Executed: contract", "Executed: smoke", "Executed: smoke"), contract)
        assertEquals(emptyList(), noSelectedTests)
    }

    @Test
    fun `test - dynamic tests`() {
        val testClass = "org.jetbrains.kotlin.testFederation.PseudoDynamicTest"
        val full = runTestEvents(TestFederationMode.Full, testFilter = testClass)
        val smoke = runTestEvents(TestFederationMode.Smoke, testFilter = testClass)
        val contract = runTestEvents(TestFederationMode.Smoke, Domain.Js, testFilter = testClass)
        val noSelectedTests = runTestEvents(TestFederationMode.Smoke, testFilter = "$testClass.*domain*")

        assertEquals(
            listOf(
                "Created: contract", "Created: domain", "Created: smoke",
                "Executed: contract 1", "Executed: contract 2",
                "Executed: domain 1", "Executed: domain 2",
                "Executed: smoke 1", "Executed: smoke 2",
            ),
            full
        )
        assertEquals(listOf("Created: smoke", "Executed: smoke 1", "Executed: smoke 2"), smoke)
        assertEquals(
            listOf(
                "Created: contract", "Created: smoke",
                "Executed: contract 1", "Executed: contract 2",
                "Executed: smoke 1", "Executed: smoke 2",
            ),
            contract
        )
        assertEquals(emptyList(), noSelectedTests)
    }

    @Test
    fun `test - dynamic containers`() {
        val testClass = "org.jetbrains.kotlin.testFederation.PseudoDynamicContainerTest"
        val full = runTestEvents(TestFederationMode.Full, testFilter = testClass)
        val smoke = runTestEvents(TestFederationMode.Smoke, testFilter = testClass)
        val contract = runTestEvents(TestFederationMode.Smoke, Domain.Js, testFilter = testClass)
        val noSelectedTests = runTestEvents(TestFederationMode.Smoke, testFilter = "$testClass.*domain*")

        assertEquals(
            listOf(
                "Created: contract", "Created: domain", "Created: smoke",
                "Executed: contract 1", "Executed: contract 2",
                "Executed: domain 1", "Executed: domain 2",
                "Executed: smoke 1", "Executed: smoke 2",
            ),
            full
        )
        assertEquals(listOf("Created: smoke", "Executed: smoke 1", "Executed: smoke 2"), smoke)
        assertEquals(
            listOf(
                "Created: contract", "Created: smoke",
                "Executed: contract 1", "Executed: contract 2",
                "Executed: smoke 1", "Executed: smoke 2",
            ),
            contract
        )
        assertEquals(emptyList(), noSelectedTests)
    }

    @Test
    fun `test - nested tests inherit class tags`() {
        val testClass = "org.jetbrains.kotlin.testFederation.PseudoNestedTest"
        val full = runTestEvents(TestFederationMode.Full, testFilter = testClass)
        val smoke = runTestEvents(TestFederationMode.Smoke, testFilter = testClass)
        val contract = runTestEvents(TestFederationMode.Smoke, Domain.Js, testFilter = testClass)
        val noSelectedTests = runTestEvents(TestFederationMode.Smoke, testFilter = "$testClass.*domain*")

        assertEquals(listOf("Executed: contract", "Executed: domain", "Executed: smoke"), full)
        assertEquals(listOf("Executed: smoke"), smoke)
        assertEquals(listOf("Executed: contract", "Executed: smoke"), contract)
        assertEquals(emptyList(), noSelectedTests)
    }

    @Test
    fun `test - BeforeAll and AfterAll`() {
        val testClass = "org.jetbrains.kotlin.testFederation.PseudoLifecycleTest"
        val full = runTestEvents(TestFederationMode.Full, testFilter = testClass)
        val smoke = runTestEvents(TestFederationMode.Smoke, testFilter = testClass)
        val contract = runTestEvents(TestFederationMode.Smoke, Domain.Js, testFilter = testClass)
        val noSelectedTests = runTestEvents(TestFederationMode.Smoke, testFilter = "$testClass.*domain*")

        assertEquals(
            listOf("Executed: contract", "Executed: domain", "Executed: smoke", "Lifecycle: afterAll", "Lifecycle: beforeAll"),
            full
        )
        assertEquals(listOf("Executed: smoke", "Lifecycle: afterAll", "Lifecycle: beforeAll"), smoke)
        assertEquals(
            listOf("Executed: contract", "Executed: smoke", "Lifecycle: afterAll", "Lifecycle: beforeAll"),
            contract
        )
        assertEquals(emptyList(), noSelectedTests)
    }

    @Test
    fun `test - inherited tests and lifecycle callbacks`() {
        val testClass = "org.jetbrains.kotlin.testFederation.PseudoInheritedTest"
        val full = runTestEvents(TestFederationMode.Full, testFilter = testClass)
        val smoke = runTestEvents(TestFederationMode.Smoke, testFilter = testClass)
        val contract = runTestEvents(TestFederationMode.Smoke, Domain.Js, testFilter = testClass)
        val noSelectedTests = runTestEvents(TestFederationMode.Smoke, testFilter = "$testClass.*domain*")

        assertEquals(
            listOf("Executed: contract", "Executed: domain", "Executed: smoke", "Lifecycle: afterAll", "Lifecycle: beforeAll"),
            full
        )
        assertEquals(listOf("Executed: smoke", "Lifecycle: afterAll", "Lifecycle: beforeAll"), smoke)
        assertEquals(
            listOf("Executed: contract", "Executed: smoke", "Lifecycle: afterAll", "Lifecycle: beforeAll"),
            contract
        )
        assertEquals(emptyList(), noSelectedTests)
    }

    @Test
    fun `test - smoke - no selected tests does not execute BeforeAll or AfterAll`() {
        val testsFilter = "org.jetbrains.kotlin.testFederation.PseudoTest.domain test"
        val lifecycleMarkers = setOf("PseudoTest.beforeAll executed", "PseudoTest.afterAll executed")

        val fullResult = runTestBuild(TestFederationMode.Full, testsFilter = testsFilter)
        assertEquals(setOf(TestResult("PseudoTest", "domain test")), fullResult.executedTests)
        lifecycleMarkers.forEach { assertContains(fullResult.buildResult.output, it) }

        val smokeResult = runTestBuild(TestFederationMode.Smoke, testsFilter = testsFilter)
        assertEquals(emptySet(), smokeResult.executedTests)
        assertEquals(
            emptySet(),
            lifecycleMarkers.filter { it in smokeResult.buildResult.output }.toSet(),
            "Neither 'BeforeAll' nor 'AfterAll' should execute when no tests are selected"
        )
    }
}

private val allTests = setOf(
    TestResult("PseudoTest", "domain test"),
    TestResult("PseudoTest", "smoke test"),
    TestResult("PseudoTest", "js contract test"),
    TestResult("PseudoTest", "wasm contract test"),
    TestResult("PseudoTest", "gradle contract test"),
    TestResult("PseudoTest", "nightly test")
)

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
 * Runs `:repo:test-runtime:test` with the given [mode] and [changed] domains.
 * Selects `PseudoTest` unless [additionalCliArgs] supplies a `--tests` filter.
 * Returns the full build result and test results parsed from the build output in [TestBuildResult.executedTests].
 */
private fun runTestBuild(
    mode: TestFederationMode? = null,
    vararg changed: Domain,
    affected: List<Domain> = changed.toList(),
    smokeTestConfig: String? = null,
    testTaskDomainsOverride: List<Domain>? = null,
    testFederationEnabled: Boolean = true,
    nightly: Boolean? = null,
    rerun: Boolean = true,
    testsFilter: String? = "org.jetbrains.kotlin.testFederation.PseudoTest",
    subsets: String? = null,
    subsetsEnv: String? = null,
    additionalCliArgs: List<String> = emptyList(),
): TestBuildResult {
    val environment = defaultEnv().toMutableMap().apply {
        remove(TEST_FEDERATION_ENABLED_ENV_KEY)
        remove(TEST_FEDERATION_MODE_ENV_KEY)
        remove(TEST_FEDERATION_AFFECTED_DOMAINS_ENV_KEY)
        remove(TEST_FEDERATION_CHANGED_DOMAINS_ENV_KEY)
        remove(TEST_FEDERATION_SUBSETS_ENV_KEY)

        if (mode != null) {
            this[TEST_FEDERATION_MODE_ENV_KEY] = mode.name
        }

        if (subsetsEnv != null) {
            this[TEST_FEDERATION_SUBSETS_ENV_KEY] = subsetsEnv
        }

        if (smokeTestConfig != null) {
            this["_PSEUDO_TEST_"] = smokeTestConfig
        }

        if (testTaskDomainsOverride != null) {
            this["_DOMAINS_OVERRIDE_"] = testTaskDomainsOverride.toArgumentString()
        }

        this[TEST_FEDERATION_CHANGED_DOMAINS_ENV_KEY] = if (changed.isNotEmpty()) {
            changed.joinToString(";") { it.name }
        } else {
            "<none>"
        }

        this[TEST_FEDERATION_AFFECTED_DOMAINS_ENV_KEY] = if (affected.isNotEmpty()) {
            affected.joinToString(";") { it.name }
        } else {
            "<none>"
        }
    }

    val arguments = buildList {
        add(":repo:test-runtime:test")
        add("-P$TEST_FEDERATION_ENABLED_KEY=$testFederationEnabled")
        if (subsets != null) add("-P$TEST_FEDERATION_SUBSETS_KEY=$subsets")
        if (nightly != null) add("-Pnightly=$nightly")
        add("-Dorg.gradle.daemon.idletimeout=${5.seconds.inWholeMilliseconds}")
        if (rerun) add("--rerun")
        if (testsFilter != null) {
            addAll(listOf("--tests", testsFilter))
        }
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
    }.toSet()

    return TestBuildResult(buildResult, tests)
}

/**
 * Runs [runTestBuild] with [testFilter] as the Gradle `--tests` filter.
 * Returns trimmed fixture output lines starting with `Executed:`, `Created:`, or `Lifecycle:`.
 * The list is sorted for assertions, not in execution order; duplicate events are preserved.
 */
private fun runTestEvents(mode: TestFederationMode, vararg changed: Domain, testFilter: String): List<String> {
    val result = runTestBuild(
        mode, *changed,
        testsFilter = testFilter
    )
    return result.buildResult.output.lineSequence()
        .map { it.trim() }
        .filter { it.startsWith("Executed:") || it.startsWith("Created:") || it.startsWith("Lifecycle:") }
        .sorted()
        .toList()
}

private fun cleanTest(): BuildResult {
    return try {
        createGradleRunner().withArguments(
            ":repo:test-runtime:cleanTest",
            "-Dorg.gradle.daemon.idletimeout=${10.seconds.inWholeMilliseconds}",
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
        remove(TEST_FEDERATION_CHANGED_DOMAINS_ENV_KEY)
        remove(TEST_FEDERATION_SUBSETS_ENV_KEY)
    }
}

private fun buildCacheArgs(cache: Path) = listOf(
    "-Pkotlin.build.cache.local.directory=$cache",
    "-Pkotlin.build.cache.local.enabled=true"
)

private fun BuildResult.requireTask(path: String) =
    task(path) ?: fail("Task '$path' could not be found\nTasks: ${tasks.joinToString("\n")}")
