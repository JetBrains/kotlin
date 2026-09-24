/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")
@file:OptIn(ExperimentalPathApi::class)

package org.jetbrains.kotlin.testFederation

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.jetbrains.kotlin.testFederation.TestBuildResult.TestResult
import org.jetbrains.kotlin.testFederation.TestSubset.ContractTestsForGradle
import org.jetbrains.kotlin.testFederation.TestSubset.ContractTestsForJs
import org.jetbrains.kotlin.testFederation.TestSubset.ContractTestsForWasm
import org.jetbrains.kotlin.testFederation.TestSubset.PlainTests
import org.jetbrains.kotlin.testFederation.TestSubset.SmokeTests
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

/**
 * Runs `:repo:test-runtime:test` with different modes and domain selections, then checks which tests ran.
 * Covers full test runs, selection by annotations and automatic sampling, and nightly filters.
 */

class TestFederationFunctionalTest {

    @Test
    fun `test - SmokeTests`() {
        val result = runTestBuild(SmokeTests)
        assertEquals(
            setOf(TestResult("PseudoTest", "smoke test")),
            result.executedTests
        )
    }

    @Test
    fun `test - ContractTestsForJs`() {
        val result = runTestBuild(ContractTestsForJs)
        assertEquals(
            setOf(TestResult("PseudoTest", "js contract test")),
            result.executedTests
        )
    }

    @Test
    fun `test - ContractTestsForWasm`() {
        val result = runTestBuild(ContractTestsForWasm)
        assertEquals(
            setOf(TestResult("PseudoTest", "wasm contract test")),
            result.executedTests
        )
    }

    @Test
    fun `test - ContractTestsForJs + ContractTestsForWasm`() {
        val result = runTestBuild(ContractTestsForJs, ContractTestsForWasm)
        assertEquals(
            setOf(
                TestResult("PseudoTest", "js contract test"),
                TestResult("PseudoTest", "wasm contract test"),
            ),
            result.executedTests
        )
    }

    @Test
    fun `test - SmokeTests + ContractTestsForJs + ContractTestsForWasm`() {
        val result = runTestBuild(SmokeTests, ContractTestsForJs, ContractTestsForWasm)
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
    fun `test - PlainTests`() {
        val result = runTestBuild(PlainTests)
        assertEquals(
            setOf(
                TestResult("PseudoTest", "domain test"),
                TestResult("PseudoTest", "nightly test"),
            ),
            result.executedTests
        )
    }

    @Test
    fun `test - PlainTests - nightly disabled`() {
        val result = runTestBuild(PlainTests, nightly = false)
        assertEquals(
            setOf(
                TestResult("PseudoTest", "domain test"),
            ),
            result.executedTests
        )
    }

    @Test
    fun `test - PlainTests - nightly enabled`() {
        val result = runTestBuild(PlainTests, nightly = true)
        assertEquals(
            setOf(
                TestResult("PseudoTest", "domain test"),
                TestResult("PseudoTest", "nightly test"),
            ),
            result.executedTests
        )
    }

    @Test
    fun `test - PlainTests + SmokeTests`() {
        val result = runTestBuild(PlainTests, SmokeTests)
        assertEquals(
            setOf(
                TestResult("PseudoTest", "domain test"),
                TestResult("PseudoTest", "nightly test"),
                TestResult("PseudoTest", "smoke test"),
            ),
            result.executedTests
        )
    }

    @Test
    fun `test - all subsets (star)`() {
        val result = runTestBuild(*TestSubset.entries.toTypedArray())
        assertEquals(allTests, result.executedTests)
    }

    @Test
    fun `test - all subsets (star) - nightly disabled`() {
        val result = runTestBuild(*TestSubset.entries.toTypedArray(), nightly = false)
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
    fun `test - all subsets (star) - nightly enabled`() {
        val result = runTestBuild(*TestSubset.entries.toTypedArray(), nightly = true)
        assertEquals(allTests, result.executedTests)
    }

    @Test
    fun `test - SmokeTests - smokeTests { includeAll() }`() {
        val result = runTestBuild(SmokeTests, smokeTestsIncludeAll = true)
        assertEquals(allTests, result.executedTests)
    }

    @Test
    fun `test - SmokeTests - smokeTests { skip() }`() {
        val result = runTestBuild(SmokeTests, skipSmokes = true)
        assertEquals(
            emptySet(),
            result.executedTests
        )
    }

    @Test
    fun `test - SmokeTests - contractTests { skip() }`() {
        val result = runTestBuild(SmokeTests, skipContracts = true)
        assertEquals(
            setOf(TestResult("PseudoTest", "smoke test")),
            result.executedTests
        )
    }

    @Test
    fun `test - ContractTestsForJs - contractTests { skip() }`() {
        val result = runTestBuild(ContractTestsForJs, skipContracts = true)
        assertEquals(
            emptySet(),
            result.executedTests
        )
    }

    @Test
    fun `test - ContractTestsForJs - smokeTests { skip() }`() {
        val result = runTestBuild(ContractTestsForJs, skipSmokes = true)
        assertEquals(
            setOf(TestResult("PseudoTest", "js contract test")),
            result.executedTests
        )
    }

    @Test
    fun `test - SmokeTests + ContractTestsForJs - smokeTests { skip() } + contractTests { skip() }`() {
        val result = runTestBuild(SmokeTests, skipSmokes = true)
        assertEquals(
            emptySet(),
            result.executedTests
        )
    }

    @Test
    fun `test - mode=Smoke, changedDomains={} - maps to SmokeTests`() {
        val result = runTestBuild(mode = TestFederationMode.Smoke, changedDomains = emptyList())
        assertEquals(
            setOf(TestResult("PseudoTest", "smoke test")),
            result.executedTests
        )
        assertTrue(result.buildResult.output.contains("Test subsets: [SmokeTests]"))
    }

    @Test
    fun `test - mode=Smoke, changedDomains={Js} - maps to SmokeTests + ContractTestsForJs`() {
        val result = runTestBuild(mode = TestFederationMode.Smoke, changedDomains = listOf(Domain.Js))
        assertEquals(
            setOf(
                TestResult("PseudoTest", "smoke test"),
                TestResult("PseudoTest", "js contract test"),
            ),
            result.executedTests
        )
        assertTrue(result.buildResult.output.contains("Test subsets: [SmokeTests, ContractTestsForJs]"))
    }

    @Test
    fun `test - mode=Smoke, changedDomains={Js,Wasm} - maps to SmokeTests + ContractTestsForJs + ContractTestsForWasm`() {
        val result = runTestBuild(mode = TestFederationMode.Smoke, changedDomains = listOf(Domain.Js, Domain.Wasm))
        assertEquals(
            setOf(
                TestResult("PseudoTest", "smoke test"),
                TestResult("PseudoTest", "js contract test"),
                TestResult("PseudoTest", "wasm contract test")
            ),
            result.executedTests
        )
        assertTrue(result.buildResult.output.contains("Test subsets: [SmokeTests, ContractTestsForWasm, ContractTestsForJs]"))
    }

    @Test
    fun `test - mode=Full - maps to all subsets (star)`() {
        val result = runTestBuild(mode = TestFederationMode.Full)
        assertEquals(allTests, result.executedTests)
        assertTrue(result.buildResult.output.contains("Test subsets: [*]"))
    }

    @Test
    fun `test - test federation disabled`() {
        /* Test with federation enabled */
        run {
            val result = runTestBuild(mode = TestFederationMode.Smoke, testFederationEnabled = true)
            assertEquals(setOf(TestResult("PseudoTest", "smoke test")), result.executedTests)
        }

        /* Test with federation disabled */
        run {
            val result = runTestBuild(mode = TestFederationMode.Smoke, testFederationEnabled = false)
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
    fun `test - explicit subsets take precedence over mode and changed domains`() {
        run {
            val result = runTestBuild(
                SmokeTests, ContractTestsForWasm,
                mode = TestFederationMode.Smoke,
                changedDomains = listOf(Domain.Js)
            )
            assertEquals(
                setOf(
                    TestResult("PseudoTest", "smoke test"),
                    TestResult("PseudoTest", "wasm contract test"),
                ),
                result.executedTests
            )
        }
    }

    @Test
    fun `test - explicit subsets works without enabling test federation`() {
        val result = runTestBuild(SmokeTests, testFederationEnabled = false)
        assertEquals(
            setOf(TestResult("PseudoTest", "smoke test")),
            result.executedTests
        )
    }

    /**
     * We will check if running a test with test federation (full mode) produces a cache entry, which can be used
     * by running the same test task with test federation disabled.
     */
    @Test
    fun `test - build with test federation enabled (full) - build with test federation disabled - reuses build caches`(@TempDir cache: Path) {
        val buildCacheArgs = buildCacheArgs(cache)

        cleanTest()
        runTestBuild(
            mode = TestFederationMode.Full,
            changedDomains = Domain.entries,
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
            changedDomains = Domain.entries,
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
            changedDomains = Domain.entries,
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
            changedDomains = Domain.entries,
            additionalCliArgs = buildCacheArgs,
            rerun = false,
            testFederationEnabled = true
        ).apply {
            assertEquals(TaskOutcome.FROM_CACHE, buildResult.requireTask(":repo:test-runtime:test").outcome)
        }
    }

    @Test
    fun `test - build with test federation disabled - build with test federation enabled (full) and smokeTests { includeAll() } - reuses build caches`(
        @TempDir cache: Path,
    ) {
        val buildCacheArgs = buildCacheArgs(cache)

        cleanTest()
        runTestBuild(
            mode = TestFederationMode.Full,
            smokeTestsIncludeAll = true,
            changedDomains = Domain.entries,
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
            smokeTestsIncludeAll = true,
            changedDomains = Domain.entries,
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
            changedDomains = Domain.entries,
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
            changedDomains = listOf(Domain.Js),
            additionalCliArgs = buildCacheArgs,
            rerun = false,
        ).apply {
            assertEquals(TaskOutcome.SUCCESS, buildResult.requireTask(":repo:test-runtime:test").outcome)
            assertEquals(setOf(TestResult("PseudoTest", "smoke test"), TestResult("PseudoTest", "js contract test")), executedTests)
        }

        cleanTest()
        runTestBuild(
            mode = TestFederationMode.Smoke,
            changedDomains = listOf(Domain.Js),
            additionalCliArgs = buildCacheArgs,
            rerun = false,
        ).apply {
            assertEquals(TaskOutcome.FROM_CACHE, buildResult.requireTask(":repo:test-runtime:test").outcome)
        }

        cleanTest()
        runTestBuild(
            mode = TestFederationMode.Smoke,
            changedDomains = listOf(Domain.Wasm),
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
        val full = runTestEvents(*TestSubset.entries.toTypedArray(), testFilter = testClass)
        val plain = runTestEvents(PlainTests, testFilter = testClass)
        val smoke = runTestEvents(SmokeTests, testFilter = testClass)
        val contract = runTestEvents(SmokeTests, ContractTestsForJs, testFilter = testClass)
        val noSelectedTests = runTestEvents(SmokeTests, testFilter = "$testClass.*domain*")

        assertEquals(
            listOf(
                "Executed: contract 1", "Executed: contract 2",
                "Executed: domain 1", "Executed: domain 2",
                "Executed: smoke 1", "Executed: smoke 2",
            ),
            full
        )
        assertEquals(full, plain, "PlainTests selects all variants of exhaustive-capable tests")
        assertEquals(listOf("Executed: smoke 1", "Executed: smoke 2"), smoke)
        assertEquals(listOf("Executed: contract 1", "Executed: contract 2", "Executed: smoke 1", "Executed: smoke 2"), contract)
        assertEquals(emptyList(), noSelectedTests)
    }

    @Test
    fun `test - repeated tests`() {
        val testClass = "org.jetbrains.kotlin.testFederation.PseudoRepeatedTest"
        val full = runTestEvents(*TestSubset.entries.toTypedArray(), testFilter = testClass)
        val plain = runTestEvents(PlainTests, testFilter = testClass)
        val smoke = runTestEvents(SmokeTests, testFilter = testClass)
        val contract = runTestEvents(SmokeTests, ContractTestsForJs, testFilter = testClass)
        val noSelectedTests = runTestEvents(SmokeTests, testFilter = "$testClass.*domain*")

        assertEquals(
            listOf(
                "Executed: contract", "Executed: contract",
                "Executed: domain", "Executed: domain",
                "Executed: smoke", "Executed: smoke",
            ),
            full
        )
        assertEquals(full, plain, "PlainTests selects all variants of exhaustive-capable tests")
        assertEquals(listOf("Executed: smoke", "Executed: smoke"), smoke)
        assertEquals(listOf("Executed: contract", "Executed: contract", "Executed: smoke", "Executed: smoke"), contract)
        assertEquals(emptyList(), noSelectedTests)
    }

    @Test
    fun `test - custom test templates`() {
        val testClass = "org.jetbrains.kotlin.testFederation.PseudoTemplateTest"
        val full = runTestEvents(*TestSubset.entries.toTypedArray(), testFilter = testClass)
        val plain = runTestEvents(PlainTests, testFilter = testClass)
        val smoke = runTestEvents(SmokeTests, testFilter = testClass)
        val contract = runTestEvents(SmokeTests, ContractTestsForJs, testFilter = testClass)
        val noSelectedTests = runTestEvents(SmokeTests, testFilter = "$testClass.*domain*")

        assertEquals(
            listOf(
                "Executed: contract", "Executed: contract",
                "Executed: domain", "Executed: domain",
                "Executed: smoke", "Executed: smoke",
            ),
            full
        )
        assertEquals(full, plain, "PlainTests selects all variants of exhaustive-capable tests")
        assertEquals(listOf("Executed: smoke", "Executed: smoke"), smoke)
        assertEquals(listOf("Executed: contract", "Executed: contract", "Executed: smoke", "Executed: smoke"), contract)
        assertEquals(emptyList(), noSelectedTests)
    }

    @Test
    fun `test - dynamic tests`() {
        val testClass = "org.jetbrains.kotlin.testFederation.PseudoDynamicTest"
        val full = runTestEvents(*TestSubset.entries.toTypedArray(), testFilter = testClass)
        val plain = runTestEvents(PlainTests, testFilter = testClass)
        val smoke = runTestEvents(SmokeTests, testFilter = testClass)
        val contract = runTestEvents(SmokeTests, ContractTestsForJs, testFilter = testClass)
        val noSelectedTests = runTestEvents(SmokeTests, testFilter = "$testClass.*domain*")

        assertEquals(
            listOf(
                "Created: contract", "Created: domain", "Created: smoke",
                "Executed: contract 1", "Executed: contract 2",
                "Executed: domain 1", "Executed: domain 2",
                "Executed: smoke 1", "Executed: smoke 2",
            ),
            full
        )
        assertEquals(full, plain, "PlainTests selects all variants of exhaustive-capable tests")
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
        val full = runTestEvents(*TestSubset.entries.toTypedArray(), testFilter = testClass)
        val plain = runTestEvents(PlainTests, testFilter = testClass)
        val smoke = runTestEvents(SmokeTests, testFilter = testClass)
        val contract = runTestEvents(SmokeTests, ContractTestsForJs, testFilter = testClass)
        val noSelectedTests = runTestEvents(SmokeTests, testFilter = "$testClass.*domain*")

        assertEquals(
            listOf(
                "Created: contract", "Created: domain", "Created: smoke",
                "Executed: contract 1", "Executed: contract 2",
                "Executed: domain 1", "Executed: domain 2",
                "Executed: smoke 1", "Executed: smoke 2",
            ),
            full
        )
        assertEquals(full, plain, "PlainTests selects all variants of exhaustive-capable tests")
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
        val full = runTestEvents(*TestSubset.entries.toTypedArray(), testFilter = testClass)
        val smoke = runTestEvents(SmokeTests, testFilter = testClass)
        val contract = runTestEvents(SmokeTests, ContractTestsForJs, testFilter = testClass)
        val noSelectedTests = runTestEvents(SmokeTests, testFilter = "$testClass.*domain*")

        assertEquals(listOf("Executed: contract", "Executed: domain", "Executed: smoke"), full)
        assertEquals(listOf("Executed: smoke"), smoke)
        assertEquals(listOf("Executed: contract", "Executed: smoke"), contract)
        assertEquals(emptyList(), noSelectedTests)
    }

    @Test
    fun `test - BeforeAll and AfterAll`() {
        val testClass = "org.jetbrains.kotlin.testFederation.PseudoLifecycleTest"
        val full = runTestEvents(*TestSubset.entries.toTypedArray(), testFilter = testClass)
        val smoke = runTestEvents(SmokeTests, testFilter = testClass)
        val contract = runTestEvents(SmokeTests, ContractTestsForJs, testFilter = testClass)
        val noSelectedTests = runTestEvents(SmokeTests, testFilter = "$testClass.*domain*")

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
        val full = runTestEvents(*TestSubset.entries.toTypedArray(), testFilter = testClass)
        val smoke = runTestEvents(SmokeTests, testFilter = testClass)
        val contract = runTestEvents(SmokeTests, ContractTestsForJs, testFilter = testClass)
        val noSelectedTests = runTestEvents(SmokeTests, testFilter = "$testClass.*domain*")

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
    fun `test - SmokeTests - no selected tests does not execute BeforeAll or AfterAll`() {
        val testsFilter = "org.jetbrains.kotlin.testFederation.PseudoTest.domain test"
        val lifecycleMarkers = setOf("PseudoTest.beforeAll executed", "PseudoTest.afterAll executed")

        val allTestsResult = runTestBuild(*TestSubset.entries.toTypedArray(), testsFilter = testsFilter)
        assertEquals(setOf(TestResult("PseudoTest", "domain test")), allTestsResult.executedTests)
        lifecycleMarkers.forEach { assertContains(allTestsResult.buildResult.output, it) }

        val smokeTestsResult = runTestBuild(SmokeTests, testsFilter = testsFilter)
        assertEquals(emptySet(), smokeTestsResult.executedTests)
        assertEquals(
            emptySet(),
            lifecycleMarkers.filter { it in smokeTestsResult.buildResult.output }.toSet(),
            "Neither 'BeforeAll' nor 'AfterAll' should execute when no tests are selected"
        )
    }

    @Test
    @CleanConfigurationCache
    fun `test - SmokeTests - ContractTests - PlainTests - reuses configuration cache`() {
        val smokeTests = runTestBuild(SmokeTests)
        assertTrue(smokeTests.buildResult.output.contains("Configuration cache entry stored."))

        run {
            val contractTests = runTestBuild(ContractTestsForJs, ContractTestsForWasm)
            assertTrue(contractTests.buildResult.output.contains("Configuration cache entry reused."))
            assertEquals(
                setOf(
                    TestResult("PseudoTest", "js contract test"),
                    TestResult("PseudoTest", "wasm contract test")
                ), contractTests.executedTests
            )
        }

        run {
            val contractTests = runTestBuild(PlainTests)
            assertTrue(contractTests.buildResult.output.contains("Configuration cache entry reused."))
            assertEquals(
                setOf(
                    TestResult("PseudoTest", "domain test"),
                    TestResult("PseudoTest", "nightly test"),
                ), contractTests.executedTests
            )
        }
    }

    /**
     * Invariant: executing `*` (all subsets together in one run) selects the same tests as
     * executing each subset in separate runs. Contracts are folded into a single run to keep
     * execution time short — only the contracts covered by PseudoTest annotations are used.
     */
    @Test
    fun `test - invariant - star equals union of separate subset runs`() {
        val starResult = runTestBuild(*TestSubset.entries.toTypedArray())

        val plainResult = runTestBuild(PlainTests)
        val smokeResult = runTestBuild(SmokeTests)
        val contractResult = runTestBuild(ContractTestsForJs, ContractTestsForWasm, ContractTestsForGradle)

        assertEquals(
            starResult.executedTests,
            plainResult.executedTests + smokeResult.executedTests + contractResult.executedTests,
            "Executing '*' must select the same tests as executing PlainTests + SmokeTests + contracts separately"
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
 * Runs `:repo:test-runtime:test` with the given options.
 * Selects `PseudoTest` unless [additionalCliArgs] supplies a `--tests` filter.
 * Returns the full build result and test results parsed from the build output in [TestBuildResult.executedTests].
 */
private fun runTestBuild(
    vararg subsets: TestSubset,
    mode: TestFederationMode? = null,
    changedDomains: Collection<Domain> = emptySet(),
    testFederationEnabled: Boolean = true,
    nightly: Boolean? = null,
    rerun: Boolean = true,
    testsFilter: String? = "org.jetbrains.kotlin.testFederation.PseudoTest",
    smokeTestsIncludeAll: Boolean = false,
    skipSmokes: Boolean = false,
    skipContracts: Boolean = false,
    additionalCliArgs: List<String> = emptyList(),
): TestBuildResult {
    val environment = defaultEnv().toMutableMap().apply {
        remove(TEST_FEDERATION_ENABLED_ENV_KEY)
        remove(TEST_FEDERATION_MODE_ENV_KEY)
        remove(TEST_FEDERATION_AFFECTED_DOMAINS_ENV_KEY)
        remove(TEST_FEDERATION_CHANGED_DOMAINS_ENV_KEY)
        remove(TEST_FEDERATION_SUBSETS_ENV_KEY)

        if (smokeTestsIncludeAll) {
            this["_SMOKE_TESTS_INCLUDE_ALL_"] = "true"
        }

        if (skipSmokes) {
            this["_SKIP_SMOKES_"] = "true"
        }

        if (skipContracts) {
            this["_SKIP_CONTRACTS_"] = "true"
        }

        if (mode != null) {
            this[TEST_FEDERATION_MODE_ENV_KEY] = mode.name
        }

        this[TEST_FEDERATION_CHANGED_DOMAINS_ENV_KEY] = changedDomains.takeIf { it.isNotEmpty() }
            ?.joinToString(";") { it.name }
            ?: "<none>"
    }

    val arguments = buildList {
        add(":repo:test-runtime:test")
        add("-P$TEST_FEDERATION_ENABLED_KEY=$testFederationEnabled")
        if (subsets.isNotEmpty()) add("-P$TEST_FEDERATION_SUBSETS_KEY=${subsets.toList().toArgumentString()}")
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
private fun runTestEvents(vararg subsets: TestSubset, testFilter: String): List<String> {
    val result = runTestBuild(*subsets, testsFilter = testFilter)

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
        .forwardOutput()
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


@ExtendWith(CleanConfigurationCacheExtension::class)
private annotation class CleanConfigurationCache

private class CleanConfigurationCacheExtension : BeforeEachCallback, AfterEachCallback {
    private val configurationCacheDir = Path(".gradle/configuration-cache")
    private val configurationCacheBackupDir = Path(".gradle/configuration-cache.backup")

    override fun beforeEach(context: ExtensionContext?) {
        if (configurationCacheBackupDir.exists()) configurationCacheBackupDir.deleteRecursively()
        configurationCacheDir.moveTo(configurationCacheBackupDir, overwrite = true)
    }

    override fun afterEach(context: ExtensionContext?) {
        if (configurationCacheDir.exists() && configurationCacheBackupDir.exists()) configurationCacheDir.deleteRecursively()
        configurationCacheBackupDir.moveTo(configurationCacheDir, overwrite = true)
    }
}
