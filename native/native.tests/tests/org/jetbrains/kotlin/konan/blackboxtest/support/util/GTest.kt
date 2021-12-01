/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.util

internal typealias TestName = String
internal typealias TestStatus = String

internal class GTestReport(
    private val testStatuses: Map<TestStatus, Collection<TestName>>,
    val cleanStdOut: String
) {
    fun getPassedTests(): Collection<TestName> = testStatuses[STATUS_OK].orEmpty()
    fun getFailedTests(): Collection<TestName> = (testStatuses - STATUS_OK).flatMap { it.value }

    fun isEmpty(): Boolean = testStatuses.isEmpty()

    companion object {
        private const val STATUS_OK = "OK"
    }
}

/**
 * Parses GTest reports like this:
 *
 *   [==========] Running 2 tests from 1 test cases.
 *   [----------] Global test environment set-up.
 *   [----------] 2 tests from sample.test.SampleTestKt
 *   [ RUN      ] sample.test.SampleTestKt.one
 *   [       OK ] sample.test.SampleTestKt.one (0 ms)
 *   [ RUN      ] sample.test.SampleTestKt.two
 *   [       OK ] sample.test.SampleTestKt.two (0 ms)
 *   [----------] 2 tests from sample.test.SampleTestKt (0 ms total)
 *
 *   [----------] Global test environment tear-down
 *   [==========] 2 tests from 1 test cases ran. (0 ms total)
 *   [  PASSED  ] 2 tests.
 */
internal fun parseGTestReport(stdOut: String): GTestReport {
    val testStatuses = hashMapOf<TestStatus, MutableSet<TestName>>()
    val cleanStdOut = StringBuilder()

    var expectStatusLine = false
    stdOut.lineSequence().forEach { line ->
        when {
            expectStatusLine -> {
                val matcher = STATUS_LINE_REGEX.matchEntire(line)
                if (matcher != null) {
                    // Read the line with test status.
                    val testStatus = matcher.groupValues[1]
                    val testName = matcher.groupValues[2]
                    testStatuses.getOrPut(testStatus) { hashSetOf() } += testName
                    expectStatusLine = false
                } else {
                    // If current line is not a status line then it could be only the line with the process' output.
                    cleanStdOut.appendLine(line)
                }
            }
            line.matches(RUN_LINE_REGEX) -> {
                expectStatusLine = true // Next line contains either  test status.
            }
            else -> Unit
        }
    }

    return GTestReport(testStatuses, cleanStdOut.toString())
}

private val RUN_LINE_REGEX = Regex("""^\[\s+RUN\s+]\s+.*""")
private val STATUS_LINE_REGEX = Regex("""^\[\s+([A-Z]+)\s+]\s+(\S+)\s+.*""")
