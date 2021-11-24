/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.util

import org.jetbrains.kotlin.konan.blackboxtest.support.PackageFQN
import org.jetbrains.kotlin.konan.blackboxtest.support.TestFunction
import org.jetbrains.kotlin.konan.blackboxtest.support.util.GTestListingParseState.*
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail

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
    stdOut.lineSequence().forEachIndexed { index, line ->
        when {
            index == 0 && line.startsWith(STDLIB_TESTS_IGNORED_LINE_PREFIX) -> Unit
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

/**
 * Extracts [TestFunction]s from GTest listing.
 *
 * Example:
 *   sample.test.SampleTestKt.
 *     one
 *     two
 *
 * yields TestFunction(sample.test, one) and TestFunction(sample.test, two).
 */
internal fun parseGTestListing(rawGTestListing: String): Collection<TestFunction> = buildList {
    var state: GTestListingParseState = Begin

    rawGTestListing.lineSequence().forEachIndexed { index, line ->
        fun parseError(message: String): Nothing = fail {
            buildString {
                appendLine("$message at line #$index: \"$line\"")
                appendLine()
                appendLine("Full listing:")
                appendLine(rawGTestListing)
            }
        }

        state = when {
            index == 0 && line.startsWith(STDLIB_TESTS_IGNORED_LINE_PREFIX) -> state
            line.isBlank() -> when (state) {
                is NewTest, is End -> End
                else -> parseError("Unexpected empty line")
            }
            line[0].isWhitespace() -> when (val s = state) {
                is HasPackageName -> {
                    this += TestFunction(s.packageName, line.trim())
                    NewTest(s.packageName)
                }
                else -> parseError("Test name encountered before test suite name")
            }
            else -> when (state) {
                is Begin, is NewTest -> {
                    val packageParts = line.trimEnd().removeSuffix(".").split('.')
                    if (packageParts.isEmpty()) parseError("Malformed test suite name")

                    // Drop the last part because it is related to class name (or file-class name).
                    // TODO: How to handle nested classes?
                    val packageName = packageParts.dropLast(1).joinToString(".")

                    NewTestSuite(packageName)
                }
                else -> parseError("Unexpected test suite name")
            }
        }
    }
}

private val RUN_LINE_REGEX = Regex("""^\[\s+RUN\s+]\s+.*""")
private val STATUS_LINE_REGEX = Regex("""^\[\s+([A-Z]+)\s+]\s+(\S+)\s+.*""")

// The very first line of stdlib test output may contain seed of Random. Such line should be ignored.
private const val STDLIB_TESTS_IGNORED_LINE_PREFIX = "Seed: "

private sealed interface GTestListingParseState {
    object Begin : GTestListingParseState
    object End : GTestListingParseState

    interface HasPackageName : GTestListingParseState {
        val packageName: PackageFQN
    }

    class NewTestSuite(override val packageName: PackageFQN) : HasPackageName
    class NewTest(override val packageName: PackageFQN) : HasPackageName
}
