/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.runner

import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase
import org.jetbrains.kotlin.konan.test.blackbox.support.TestName
import org.jetbrains.kotlin.native.executors.Executor
import java.util.concurrent.ConcurrentHashMap

internal object SharedExecution {
    private val executionResults: ConcurrentHashMap<TestExecutable, AbstractRunner<Unit>> = ConcurrentHashMap()

    fun buildRunner(executor: Executor, testRun: TestRun): AbstractRunner<Unit> = executionResults.computeIfAbsent(testRun.executable) {
        val ignoredTests = if (testRun.testCase.extras is TestCase.WithTestRunnerExtras) {
            testRun.testCase.extras.ignoredTests
        } else
            emptySet()

        val ignoredParameters = ignoredTests.map { TestRunParameter.WithIgnoredTestFilter(TestName(it)) }
        val runParameters = testRun.runParameters.filterNot { it is TestRunParameter.WithFilter } + ignoredParameters

        check(testRun.runParameters.filterNot { it is TestRunParameter.WithFilter }.none { it is TestRunParameter.WithFilter })

        val sharedTestRun = TestRun(
            displayName = "Shared TestRun for ${testRun.displayName}",
            executable = testRun.executable,
            runParameters = runParameters,
            testCase = testRun.testCase,
            checks = testRun.checks,
            expectedFailure = testRun.expectedFailure
        )
        CachedRunResultRunner(executor, sharedTestRun)
    }

    private class CachedRunResultRunner(executor: Executor, testRun: TestRun) : RunnerWithExecutor(executor, testRun) {
        private val cachedRunResult by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            super.buildRun().run()
        }

        override fun buildRun() = AbstractRun { cachedRunResult }
    }
}
