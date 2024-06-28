/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.runner

import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeBlackBoxTest
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase
import org.jetbrains.kotlin.konan.test.blackbox.support.TestKind
import org.jetbrains.kotlin.konan.test.blackbox.support.TestName
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.NativeTestInstances
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Settings
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.SharedExecutionTestRunner
import org.jetbrains.kotlin.native.executors.Executor
import java.util.concurrent.ConcurrentHashMap

/**
 * This is a caching shared TestRun execution builder.
 *
 * Builds a [Runner] that is able to execute tests, caching results for all tests in the executable,
 * compiled with [TestCase.WithTestRunnerExtras]. The idea is to run [TestExecutable] that contains multiple [TestCase]s only once
 * and pass this shared result to all tests.
 *
 * @see SharedExecutionTestRunner
 */
internal object SharedExecutionBuilder {
    private val executableToSharedRun: ConcurrentHashMap<TestCompilationArtifact.Executable, TestRun> = ConcurrentHashMap()
    private val testCasesToExecuteSeparately: ConcurrentHashMap<TestExecutable, MutableList<TestCase>> = ConcurrentHashMap()

    fun buildRunner(settings: Settings, executor: Executor, testRun: TestRun): AbstractRunner<Unit> {
        if (testRun.testCase.kind !in listOf(TestKind.REGULAR, TestKind.STANDALONE)) {
            return RunnerWithExecutor(executor, testRun)
        }

        val separateTestCases = if (testRun.testCase.kind == TestKind.REGULAR)
            settings.computeSeparateTestCases(testRun)
        else emptyList()

        if (testRun.testCase in separateTestCases) {
            return RunnerWithExecutor(executor, testRun)
        }

        val sharedTestRun = executableToSharedRun.computeIfAbsent(testRun.executable.executable) {
            // Get ignored tests to exclude them from run by adding the test filtering option
            val ignoredTests = if (testRun.testCase.extras is TestCase.WithTestRunnerExtras) {
                testRun.testCase.extras.ignoredTests
            } else
                emptySet()

            // Get tests that are not compatible with others
            val testsThatMayFail = separateTestCases.map { it.nominalPackageName.toString() }

            val ignoredParameters = (ignoredTests + testsThatMayFail).map {
                TestRunParameter.WithIgnoredTestFilter(TestName(it))
            }
            val runParameters = testRun.runParameters.filterNot { it is TestRunParameter.WithFilter } + ignoredParameters

            // Increase timeout for the run, as there are multiple tests to be run.
            // At this point, there is only a number of tests available, but not each TestRun instance with exact timeout value.
            val timeout = testRun.checks.executionTimeoutCheck.timeout * testRun.executable.testNames.count()
            val checks = testRun.checks.copy(
                executionTimeoutCheck = TestRunCheck.ExecutionTimeout.ShouldNotExceed(timeout)
            )

            TestRun(
                displayName = "Shared TestRun for ${testRun.executable.executable.path} made from ${testRun.displayName}",
                executable = testRun.executable,
                runParameters = runParameters,
                testCase = testRun.testCase,
                checks = checks,
                expectedFailure = false
            )
        }

        return CachedRunResultRunner(executor, testRun, sharedTestRun)
    }

    private fun Settings.computeSeparateTestCases(testRun: TestRun): MutableList<TestCase> =
        testCasesToExecuteSeparately.computeIfAbsent(testRun.executable) {
            val testRunProvider = get<NativeTestInstances<AbstractNativeBlackBoxTest>>().enclosingTestInstance.testRunProvider
            val testCaseGroupId = testRun.testCase.id.testCaseGroupId

            // First, try to get all TestCases for the current executable from the same TestCaseGroup
            val testCases = testRunProvider.testCaseGroupProvider.getTestCaseGroup(testCaseGroupId, this)
                ?.getRegularOnly(
                    testRun.testCase.freeCompilerArgs,
                    testRun.testCase.sharedModules,
                    testRun.testCase.extras<TestCase.WithTestRunnerExtras>().runnerType
                )

            check(testCases != null && testRun.testCase in testCases) {
                "Should be not empty and contain at least $testRun"
            }

            // If the test run is expected to fail or timeout, it should not be executed with others.
            // Add it to the map of ignored test cases for the executable
            testCases.filter {
                it.expectedFailure || it.checks.executionTimeoutCheck is TestRunCheck.ExecutionTimeout.ShouldExceed
            }.toMutableList()
        }

    private val cachedRunResult = ConcurrentHashMap<TestRun, RunResult>()

    private class CachedRunResultRunner(executor: Executor, private val testRun: TestRun, private val sharedTestRun: TestRun) :
        RunnerWithExecutor(executor, sharedTestRun) {

        override fun buildRun() = AbstractRun {
            cachedRunResult.computeIfAbsent(sharedTestRun) { super.buildRun().run() }
        }

        override fun buildResultHandler(runResult: RunResult): ResultHandler = ResultHandler(
            runResult = runResult,
            checks = sharedTestRun.checks,
            testRun = testRun.copy(
                runParameters = sharedTestRun.runParameters
            ),
            loggedParameters = getLoggedParameters()
        )
    }
}
