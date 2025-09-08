/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.runner

import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase.NoTestRunnerExtras
import org.jetbrains.kotlin.konan.test.blackbox.support.TestKind
import org.jetbrains.kotlin.konan.test.blackbox.support.TestName
import org.jetbrains.kotlin.konan.test.blackbox.support.util.TCTestOutputFilter
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.jetbrains.kotlin.utils.addIfNotNull
import kotlin.test.assertIs

open class BaseTestRunProvider {
    protected fun createTestRun(testCase: TestCase, executable: TestExecutable, testRunName: String, testName: TestName?): TestRun {
        val runParameters = getTestRunParameters(testCase, testName)
        return TestRun(displayName = testRunName, executable, runParameters, testCase, testCase.checks, testCase.expectedFailure)
    }

    protected fun createSingleTestRun(testCase: TestCase, executable: TestExecutable): TestRun = createTestRun(
        testCase = testCase,
        executable = executable,
        testRunName = /* Unimportant. Used only in dynamic tests. */ "",
        testName = null
    )

    private fun getTestRunParameters(testCase: TestCase, testName: TestName?): List<TestRunParameter> = buildList {
        addIfNotNull(testCase.checks.outputDataFile?.file?.let(TestRunParameter::WithExpectedOutputData))

        when (testCase.kind) {
            TestKind.STANDALONE_LLDB -> {
                assertTrue(testName == null)
                // Note: TestRunParameter.WithLLDB adds program arguments and would therefore conflict
                // with other TestRunParameters that do the same (such as WithTCTestLogger).
                add(TestRunParameter.WithLLDB(testCase.extras<NoTestRunnerExtras>().arguments))
            }
            TestKind.STANDALONE_NO_TR -> {
                assertTrue(testName == null)
                val extras = testCase.extras<NoTestRunnerExtras>()
                addIfNotNull(extras.inputDataFile?.let(TestRunParameter::WithInputData))
                add(TestRunParameter.WithFreeCommandLineArguments(extras.arguments))
            }
            TestKind.STANDALONE -> {
                // WithTCLogger relies on TCTestOutputFilter to be present in the checkers.
                assertIs<TCTestOutputFilter>(testCase.checks.testFiltering.testOutputFilter)
                add(TestRunParameter.WithTCTestLogger)
                if (testName != null)
                    add(TestRunParameter.WithTestFilter(testName))
                else {
                    val ignoredTests = (testCase.extras as TestCase.WithTestRunnerExtras).ignoredTests
                    if (ignoredTests.isNotEmpty()) {
                        add(TestRunParameter.WithGTestPatterns(negativePatterns = ignoredTests))
                    }
                }
            }
            TestKind.REGULAR -> {
                // WithTCLogger relies on TCTestOutputFilter to be present in the checkers.
                assertIs<TCTestOutputFilter>(testCase.checks.testFiltering.testOutputFilter)
                add(TestRunParameter.WithTCTestLogger)
                addIfNotNull(
                    testName?.let(TestRunParameter::WithTestFilter) ?: TestRunParameter.WithPackageFilter(testCase.nominalPackageName)
                )
            }
        }
    }
}
