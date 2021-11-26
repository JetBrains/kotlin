/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("KDocUnresolvedReference")

package org.jetbrains.kotlin.konan.blackboxtest.support

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.konan.blackboxtest.support.TestCase.NoTestRunnerExtras
import org.jetbrains.kotlin.konan.blackboxtest.support.TestCase.WithTestRunnerExtras
import org.jetbrains.kotlin.konan.blackboxtest.support.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.blackboxtest.support.group.TestCaseGroupProvider
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.AbstractRunner
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.LocalTestRunner
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.GlobalSettings
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.Settings
import org.jetbrains.kotlin.konan.blackboxtest.support.util.ThreadSafeCache
import org.jetbrains.kotlin.konan.blackboxtest.support.util.TreeNode
import org.jetbrains.kotlin.konan.blackboxtest.support.util.buildTree
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.extension.ExtensionContext
import java.io.File

internal class TestRunProvider(
    private val settings: Settings,
    private val testCaseGroupProvider: TestCaseGroupProvider
) : ExtensionContext.Store.CloseableResource {
    private val compilationFactory = TestCompilationFactory(settings)
    private val cachedCompilations = ThreadSafeCache<TestCompilationCacheKey, TestCompilation>()

    fun setProcessors(testDataFile: File, sourceTransformers: List<(String) -> String>) {
        testCaseGroupProvider.setPreprocessors(testDataFile, sourceTransformers)
    }

    /**
     * Produces a single [TestRun] per testData file.
     *
     * If testData file contains multiple functions annotated with [kotlin.test.Test], then all these functions will be executed
     * in one shot. If either function will fail, the whole JUnit test will be considered as failed.
     *
     * Example:
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
     */
    fun getSingleTestRun(testDataFile: File): TestRun = withTestExecutable(testDataFile) { testCase, executable ->
        val runParameters = getRunParameters(testCase, testFunction = null)
        TestRun(displayName = testDataFile.nameWithoutExtension, executable, runParameters, testCase.origin)
    }

    /**
     * Produces at least one [TestRun] per testData file.
     *
     * If testData file contains multiple functions annotated with [kotlin.test.Test], then a separate [TestRun] will be produced
     * for each such function.
     *
     * This allows to have a better granularity in tests. So that every individual test method inside testData file will be considered
     * as an individual JUnit test, and will be presented as a separate row in JUnit test report.
     *
     * Example:
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
     */
    fun getTestRuns(testDataFile: File): TreeNode<TestRun> = withTestExecutable(testDataFile) { testCase, executable ->
        fun createTestRun(testRunName: String, testFunction: TestFunction?): TestRun {
            val runParameters = getRunParameters(testCase, testFunction)
            return TestRun(testRunName, executable, runParameters, testCase.origin)
        }

        when (testCase.kind) {
            TestKind.STANDALONE_NO_TR -> {
                val testRunName = testCase.extras<NoTestRunnerExtras>().entryPoint.substringAfterLast('.')
                val testRun = createTestRun(testRunName, testFunction = null)
                TreeNode.oneLevel(testRun)
            }
            TestKind.REGULAR, TestKind.STANDALONE -> {
                val testFunctions = testCase.extras<WithTestRunnerExtras>().testFunctions
                testFunctions.buildTree(TestFunction::packageName) { testFunction -> createTestRun(testFunction.functionName, testFunction) }
            }
        }
    }

    private fun <T> withTestExecutable(testDataFile: File, action: (TestCase, TestExecutable) -> T): T {
        settings.assertNotDisposed()

        val testDataDir = testDataFile.parentFile
        val testDataFileName = testDataFile.name

        val testCaseGroup = testCaseGroupProvider.getTestCaseGroup(testDataDir) ?: fail { "No test case for $testDataFile" }
        assumeTrue(testCaseGroup.isEnabled(testDataFileName), "Test case is disabled")

        val testCase = testCaseGroup.getByName(testDataFileName) ?: fail { "No test case for $testDataFile" }

        val testCompilation = when (testCase.kind) {
            TestKind.STANDALONE, TestKind.STANDALONE_NO_TR -> {
                // Create a separate compilation for each standalone test case.
                val cacheKey = TestCompilationCacheKey.Standalone(testDataFile)
                cachedCompilations.computeIfAbsent(cacheKey) {
                    compilationFactory.testCasesToExecutable(listOf(testCase))
                }
            }
            TestKind.REGULAR -> {
                // Group regular test cases by compiler arguments.
                val cacheKey = TestCompilationCacheKey.Grouped(testDataDir, testCase.freeCompilerArgs)
                cachedCompilations.computeIfAbsent(cacheKey) {
                    val testCases = testCaseGroup.getRegularOnlyByCompilerArgs(testCase.freeCompilerArgs)
                    assertTrue(testCases.isNotEmpty())
                    compilationFactory.testCasesToExecutable(testCases)
                }
            }
        }

        val (executableFile, loggedCompilerCall) = testCompilation.result.assertSuccess() // <-- Compilation happens here.
        val executable = TestExecutable(executableFile, loggedCompilerCall)

        return action(testCase, executable)
    }

    private fun getRunParameters(testCase: TestCase, testFunction: TestFunction?): List<TestRunParameter> = with(testCase) {
        when (kind) {
            TestKind.STANDALONE_NO_TR -> {
                assertTrue(testFunction == null)

                listOfNotNull(
                    extras<NoTestRunnerExtras>().inputDataFile?.let(TestRunParameter::WithInputData),
                    expectedOutputDataFile?.let(TestRunParameter::WithExpectedOutputData)
                )
            }
            TestKind.STANDALONE -> listOfNotNull(
                TestRunParameter.WithGTestLogger,
                testFunction?.let(TestRunParameter::WithFunctionFilter),
                expectedOutputDataFile?.let(TestRunParameter::WithExpectedOutputData)
            )
            TestKind.REGULAR -> listOfNotNull(
                TestRunParameter.WithGTestLogger,
                testFunction?.let(TestRunParameter::WithFunctionFilter) ?: TestRunParameter.WithPackageFilter(nominalPackageName),
                expectedOutputDataFile?.let(TestRunParameter::WithExpectedOutputData)
            )
        }
    }

    // Currently, only local test runner is supported.
    fun createRunner(testRun: TestRun): AbstractRunner<*> = with(settings.get<GlobalSettings>()) {
        when (val target = target) {
            hostTarget -> LocalTestRunner(testRun, executionTimeout)
            else -> fail {
                """
                    Running at non-host target is not supported yet.
                    Compilation target: $target
                    Host target: $hostTarget
                """.trimIndent()
            }
        }
    }

    override fun close() {
        Disposer.dispose(settings)
    }

    private sealed class TestCompilationCacheKey {
        data class Standalone(val testDataFile: File) : TestCompilationCacheKey()
        data class Grouped(val testDataDir: File, val freeCompilerArgs: TestCompilerArgs) : TestCompilationCacheKey()
    }
}
