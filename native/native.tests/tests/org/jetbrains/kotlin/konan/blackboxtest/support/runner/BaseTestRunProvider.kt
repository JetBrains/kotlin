/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.runner

import org.jetbrains.kotlin.konan.blackboxtest.support.TestCase
import org.jetbrains.kotlin.konan.blackboxtest.support.TestCase.NoTestRunnerExtras
import org.jetbrains.kotlin.konan.blackboxtest.support.TestKind
import org.jetbrains.kotlin.konan.blackboxtest.support.TestName
import org.jetbrains.kotlin.test.services.JUnit5Assertions

internal open class BaseTestRunProvider {
    protected fun createTestRun(testCase: TestCase, executable: TestExecutable, testRunName: String, testName: TestName?): TestRun {
        val runParameters = getTestRunParameters(testCase, testName)
        return TestRun(displayName = testRunName, executable, runParameters, testCase.id)
    }

    protected fun createSingleTestRun(testCase: TestCase, executable: TestExecutable): TestRun = createTestRun(
        testCase = testCase,
        executable = executable,
        testRunName = /* Unimportant. Used only in dynamic tests. */ "",
        testName = null
    )

    private fun getTestRunParameters(testCase: TestCase, testName: TestName?): List<TestRunParameter> = with(testCase) {
        when (kind) {
            TestKind.STANDALONE_NO_TR -> {
                JUnit5Assertions.assertTrue(testName == null)
                listOfNotNull(
                    extras<NoTestRunnerExtras>().inputDataFile?.let(TestRunParameter::WithInputData),
                    expectedOutputDataFile?.let(TestRunParameter::WithExpectedOutputData)
                )
            }
            TestKind.STANDALONE -> listOfNotNull(
                TestRunParameter.WithTCTestLogger,
                testName?.let(TestRunParameter::WithTestFilter),
                expectedOutputDataFile?.let(TestRunParameter::WithExpectedOutputData)
            )
            TestKind.REGULAR -> listOfNotNull(
                TestRunParameter.WithTCTestLogger,
                testName?.let(TestRunParameter::WithTestFilter) ?: TestRunParameter.WithPackageFilter(nominalPackageName),
                expectedOutputDataFile?.let(TestRunParameter::WithExpectedOutputData)
            )
        }
    }
}
