/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.runner

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase.NoTestRunnerExtras
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase.WithTestRunnerExtras
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact.Executable
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationFactory
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.group.TestCaseGroupProvider
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Settings
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.XCTestRunner
import org.jetbrains.kotlin.konan.test.blackbox.support.util.ThreadSafeCache
import org.jetbrains.kotlin.konan.test.blackbox.support.util.TreeNode
import org.jetbrains.kotlin.konan.test.blackbox.support.util.buildTree
import org.jetbrains.kotlin.konan.test.blackbox.support.util.startsWith
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * [TestRun] provider that is used in Kotlin/Native black box tests together with the corresponding [TestCaseGroupProvider].
 */
class TestRunProvider(
    internal val testCaseGroupProvider: TestCaseGroupProvider
) : BaseTestRunProvider(), ExtensionContext.Store.CloseableResource {
    private val compilationFactory = TestCompilationFactory()
    private val cachedCompilations = ThreadSafeCache<TestCompilationCacheKey, TestCompilation<Executable>>()
    private val cachedXCTestCompilations = ThreadSafeCache<TestCompilationCacheKey, TestCompilation<TestCompilationArtifact.XCTestBundle>>()

    /**
     * Produces a single [TestRun] per [TestCase]. So-called "one test case/one test run" mode.
     *
     * If [TestCase] contains multiple functions annotated with [kotlin.test.Test], then all these functions will be executed
     * in one shot. If either function fails, the whole JUnit test will be considered as failed.
     *
     * Example:
     * ```
     *   //+++ testData file (foo.kt): +++//
     *   @kotlin.test.Test
     *   fun one() { /* ... */ }
     *
     *   @kotlin.test.Test
     *   fun two() { /* ... */ }
     *
     *   //+++ generated JUnit test suite: +++//
     *   public class MyTestSuiteGenerated {
     *       @org.junit.jupiter.api.Test
     *       @org.jetbrains.kotlin.test.TestMetadata("foo.kt")
     *       public void testFoo() {
     *           // Compiles foo.kt with test-runner, probably together with other testData files (bar.kt, qux.kt, ...).
     *           // Then executes FooKt.one() and FooKt.two() test functions one after another in one shot.
     *           // If either of test functions fails, the whole "testFoo()" JUnit test is marked as failed.
     *       }
     *   }
     * ```
     */
    fun getSingleTestRun(
        testCaseId: TestCaseId,
        settings: Settings
    ): TestRun = withTestExecutable(testCaseId, settings) { testCase, executable ->
        createSingleTestRun(testCase, executable)
    }

    /**
     * Produces at least one [TestRun] per [TestCase]. So-called "one test function/one test run" mode.
     *
     * If [TestCase] contains multiple functions annotated with [kotlin.test.Test], then a separate [TestRun] will be produced
     * for each such function.
     *
     * This allows having a better granularity in tests. So that every test method inside [TestCase] will be considered
     * as an individual JUnit test, and will be presented as a separate row in JUnit test report.
     *
     * Example:
     * ```
     *   //+++ testData file (foo.kt): +++//
     *   @kotlin.test.Test
     *   fun one() { /* ... */ }
     *
     *   @kotlin.test.Test
     *   fun two() { /* ... */ }
     *
     *   //+++ generated JUnit test suite: +++//
     *   public class MyTestSuiteGenerated {
     *       @org.junit.jupiter.api.TestFactory
     *       @org.jetbrains.kotlin.test.TestMetadata("foo.kt")
     *       public Collection<org.junit.jupiter.api.DynamicNode> testFoo() {
     *           // Compiles foo.kt with test-runner, probably together with other testData files (bar.kt, qux.kt, ...).
     *           // Then produces two instances of DynamicTest for FooKt.one() and FooKt.two() functions.
     *           // Each DynamicTest is executed as a separate JUnit test.
     *           // So if FooKt.one() fails and FooKt.two() succeeds, then "testFoo.one" JUnit test will be presented as failed
     *           // in the test report, and "testFoo.two" will be presented as passed.
     *       }
     *   }
     * ```
     */
    fun getTestRuns(
        testCaseId: TestCaseId,
        settings: Settings
    ): Collection<TreeNode<TestRun>> = withTestExecutable(testCaseId, settings) { testCase, executable ->
        fun createTestRun(testRunName: String, testName: TestName?) = createTestRun(testCase, executable, testRunName, testName)

        when (testCase.kind) {
            TestKind.STANDALONE_NO_TR, TestKind.STANDALONE_LLDB -> {
                val testRunName = (testCase.extras<NoTestRunnerExtras>().entryPoint ?: "main").substringAfterLast('.')
                val testRun = createTestRun(testRunName, testName = null)
                TreeNode.oneLevel(testRun)
            }
            TestKind.REGULAR, TestKind.STANDALONE -> {
                val testNames = executable.testNames.filterIrrelevant(testCase)
                testNames.buildTree(TestName::packageName) { testName ->
                    createTestRun(testName.functionName, testName)
                }
            }
        }
    }

    private fun <T> withTestExecutable(
        testCaseId: TestCaseId,
        settings: Settings,
        action: (TestCase, TestExecutable) -> T
    ): T {
        val testCaseGroup = testCaseGroupProvider.getTestCaseGroup(testCaseId.testCaseGroupId, settings)
            ?: fail { "No test case for $testCaseId" }

        assumeTrue(testCaseGroup.isEnabled(testCaseId), "Test case is disabled")

        val testCase = testCaseGroup.getByName(testCaseId) ?: fail { "No test case for $testCaseId" }

        val testCaseGroupId = if (testCaseGroup is TestCaseGroup.MetaGroup)
            testCaseGroup.testCaseGroupId
        else
            testCaseId.testCaseGroupId

        val executable = if (settings.get<XCTestRunner>().isEnabled &&
            testCase.kind in listOf(TestKind.STANDALONE, TestKind.REGULAR)
        ) {
            val testCompilation = when (testCase.kind) {
                TestKind.STANDALONE -> {
                    // Create a separate compilation for each standalone test case.
                    cachedXCTestCompilations.computeIfAbsent(
                        TestCompilationCacheKey.Standalone(testCaseId)
                    ) {
                        compilationFactory.testCasesToTestBundle(listOf(testCase), settings)
                    }
                }
                TestKind.REGULAR -> {
                    // Group regular test cases by compiler arguments.
                    val testRunnerType = testCase.extras<WithTestRunnerExtras>().runnerType
                    cachedXCTestCompilations.computeIfAbsent(
                        TestCompilationCacheKey.Grouped(
                            testCaseGroupId = testCaseGroupId,
                            freeCompilerArgs = testCase.freeCompilerArgs,
                            sharedModules = testCase.sharedModules,
                            runnerType = testRunnerType
                        )
                    ) {
                        val testCases = testCaseGroup.getRegularOnly(testCase.freeCompilerArgs, testCase.sharedModules, testRunnerType)
                        assertTrue(testCases.isNotEmpty())
                        compilationFactory.testCasesToTestBundle(testCases, settings)
                    }
                }
                else -> error("Test kind ${testCase.kind} is not supported yet in XCTest runner")
            }

            val compilationResult = testCompilation.result.assertSuccess() // <-- Compilation happens here.

            // Create an adapter to match TestExecutable artifact.
            // Need to refactor TestRun and TestExecutable to be less artifact specific
            val adapter = TestCompilationResult.Success(
                resultingArtifact = Executable(
                    compilationResult.resultingArtifact.bundleDir,
                    compilationResult.resultingArtifact.fileCheckStage
                ),
                loggedData = compilationResult.loggedData
            )
            TestExecutable.fromCompilationResult(testCase, adapter)
        } else {
            val testCompilation = when (testCase.kind) {
                TestKind.STANDALONE, TestKind.STANDALONE_NO_TR, TestKind.STANDALONE_LLDB -> {
                    // Create a separate compilation for each standalone test case.
                    cachedCompilations.computeIfAbsent(
                        TestCompilationCacheKey.Standalone(testCaseId)
                    ) {
                        compilationFactory.testCasesToExecutable(listOf(testCase), settings)
                    }
                }
                TestKind.REGULAR -> {
                    // Group regular test cases by compiler arguments.
                    val testRunnerType = testCase.extras<WithTestRunnerExtras>().runnerType
                    cachedCompilations.computeIfAbsent(
                        TestCompilationCacheKey.Grouped(
                            testCaseGroupId = testCaseGroupId,
                            freeCompilerArgs = testCase.freeCompilerArgs,
                            sharedModules = testCase.sharedModules,
                            runnerType = testRunnerType
                        )
                    ) {
                        val testCases = testCaseGroup.getRegularOnly(testCase.freeCompilerArgs, testCase.sharedModules, testRunnerType)
                        assertTrue(testCases.isNotEmpty())
                        compilationFactory.testCasesToExecutable(testCases, settings)
                    }
                }
            }

            val compilationResult = testCompilation.result.assertSuccess() // <-- Compilation happens here.
            TestExecutable.fromCompilationResult(testCase, compilationResult)
        }

        return action(testCase, executable)
    }

    private fun Collection<TestName>.filterIrrelevant(testCase: TestCase) =
        if (testCase.kind == TestKind.REGULAR)
            filter { testName -> testName.packageName.startsWith(testCase.nominalPackageName) }
        else if (testCase.extras is WithTestRunnerExtras)
            filterNot { testName -> testCase.extras<WithTestRunnerExtras>().ignoredTests.any {
                    fromGTestPattern(it).matches(testName.toString())
                }}
        else
            this

    override fun close() {
        if (testCaseGroupProvider is Disposable) {
            Disposer.dispose(testCaseGroupProvider)
        }
    }

    private sealed class TestCompilationCacheKey {
        data class Standalone(val testCaseId: TestCaseId) : TestCompilationCacheKey()
        data class Grouped(
            val testCaseGroupId: TestCaseGroupId,
            val freeCompilerArgs: TestCompilerArgs,
            val sharedModules: Set<TestModule.Shared>,
            val runnerType: TestRunnerType
        ) : TestCompilationCacheKey()
    }
}
