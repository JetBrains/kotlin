/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.runner

import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase
import org.jetbrains.kotlin.konan.test.blackbox.support.TestKind
import org.jetbrains.kotlin.konan.test.blackbox.support.TestName
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
    private val executionResults: ConcurrentHashMap<TestExecutable, AbstractRunner<Unit>> = ConcurrentHashMap()
    private val testRunsToExecuteSeparately: ConcurrentHashMap<TestExecutable, MutableList<TestCase>> = ConcurrentHashMap()

    fun buildRunner(executor: Executor, testRun: TestRun): AbstractRunner<Unit> {
        if (testRun.expectedFailure || testRun.checks.executionTimeoutCheck is TestRunCheck.ExecutionTimeout.ShouldExceed) {
            // If the test run is expected to fail or timeout it should not be executed with others.
            // Add it to the map of ignored test cases for the executable
            testRunsToExecuteSeparately.computeIfAbsent(testRun.executable) { mutableListOf() } += testRun.testCase

            return RunnerWithExecutor(executor, testRun)
        }

        if (testRun.testCase.kind != TestKind.REGULAR) {
            return RunnerWithExecutor(executor, testRun)
        }

        return executionResults.computeIfAbsent(testRun.executable) {
            // Get ignored tests to exclude them from run by adding the test filtering option
            val ignoredTests = if (testRun.testCase.extras is TestCase.WithTestRunnerExtras) {
                testRun.testCase.extras.ignoredTests
            } else
                emptySet()

            // Get tests that are not compatible with others
            val testsThatMayFail = testRunsToExecuteSeparately[testRun.executable]
                ?.map { it.nominalPackageName.toString() }
                ?: emptyList()

            val ignoredParameters = (ignoredTests + testsThatMayFail).map {
                TestRunParameter.WithIgnoredTestFilter(TestName(it))
            }
            val runParameters = testRun.runParameters.filterNot { it is TestRunParameter.WithFilter } + ignoredParameters

            // Increase timeout for the run, as there are multiple tests to be run.
            // At this point there is only amount of tests available, but not each TestRun instance with exact timeout value.
            val timeout = testRun.checks.executionTimeoutCheck.timeout * testRun.executable.testNames.count()
            val checks = testRun.checks.copy(
                executionTimeoutCheck = TestRunCheck.ExecutionTimeout.ShouldNotExceed(timeout)
            )

            val sharedTestRun = TestRun(
                displayName = "Shared TestRun for ${testRun.executable.executable.path} made from ${testRun.displayName}",
                executable = testRun.executable,
                runParameters = runParameters,
                testCase = testRun.testCase,
                checks = checks,
                expectedFailure = false
            )
            CachedRunResultRunner(executor, sharedTestRun)
        }
    }

    private class CachedRunResultRunner(executor: Executor, testRun: TestRun) : RunnerWithExecutor(executor, testRun) {
        private val cachedRunResult by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            super.buildRun().run()
        }

        override fun buildRun() = AbstractRun { cachedRunResult }
    }
}
