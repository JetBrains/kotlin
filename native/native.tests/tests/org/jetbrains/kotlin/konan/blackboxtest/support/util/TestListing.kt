/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.util

import org.jetbrains.kotlin.konan.blackboxtest.support.TestName
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

/**
 * Extracts [TestName]s from the test listing produced immediately during the compilation (turned on with
 * "-Xdump-tests-to=" compiler flag).
 *
 * Example:
 *   sample/test/SampleTestKt:one
 *   sample/test/SampleTestKt:two
 *
 * yields TestName(packageName = "sample.test", packagePartClassName = "SampleTestKt", functionName = "one")
 *    and TestName(packageName = "sample.test", packagePartClassName = "SampleTestKt", functionName = "two")
 */
internal object DumpedTestListing {
    fun parse(listing: String): Collection<TestName> {
        val lines = listing.lines()
        var emptyLineEncountered = false

        return lines.mapIndexedNotNull { index: Int, line: String ->
            fun parseError(message: String): Nothing = parseError(message, index, line, listing)

            when {
                line.isBlank() -> {
                    emptyLineEncountered = true
                    null
                }
                emptyLineEncountered -> parseError("Unexpected empty line")
                else -> {
                    val (packageAndClass, functionName) = line.trim()
                        .split(':')
                        .takeIf { items -> items.size == 2 && items.none(String::isBlank) }
                        ?: parseError("Malformed test name")

                    with(packageAndClass.split('/')) {
                        val classNames = last().split('.')
                        val packageSegments = dropLast(1)
                        TestName(packageSegments, classNames, functionName)
                    }
                }
            }
        }
    }
}

/**
 * Extracts [TestName]s from GTest listing.
 *
 * Example:
 *   sample.test.SampleTestKt.
 *     one
 *     two
 *
 * yields TestName(packageName = "sample.test", packagePartClassName = "SampleTestKt", functionName = "one")
 *    and TestName(packageName = "sample.test", packagePartClassName = "SampleTestKt", functionName = "two")
 */
internal object GTestListing {
    fun parse(listing: String): Collection<TestName> = buildList {
        var state: ParseState = ParseState.Begin

        val lines = listing.lines()
        lines.forEachIndexed { index, line ->
            fun parseError(message: String): Nothing = parseError(message, index, line, listing)

            state = when {
                line.startsWith(STDLIB_TESTS_IGNORED_LINE_PREFIX) && state is ParseState.Begin -> ParseState.Begin
                line.isBlank() -> when (state) {
                    is ParseState.NewTest, is ParseState.End -> ParseState.End
                    else -> parseError("Unexpected empty line")
                }
                line[0].isWhitespace() -> when (state) {
                    is ParseState.NewTestSuite,
                    is ParseState.NewTest -> {
                        val testSuite = state.testSuite
                        this += TestName(testSuite.testSuiteNameWithDotSuffix + line.trim())
                        ParseState.NewTest(testSuite)
                    }
                    else -> parseError("Test name encountered before test suite name")
                }
                else -> when (state) {
                    is ParseState.Begin, is ParseState.NewTest -> {
                        ParseState.NewTestSuite(line.trimEnd())
                    }
                    else -> parseError("Unexpected test suite name")
                }
            }
        }

        if (state is ParseState.NewTestSuite)
            parseError("Test name expected before test suite name", lines.lastIndex, lines.last(), listing)
    }

    private sealed interface ParseState {
        object Begin : ParseState
        object End : ParseState

        class NewTestSuite(val testSuiteNameWithDotSuffix: String) : ParseState
        class NewTest(val testSuite: NewTestSuite) : ParseState
    }

    private inline val ParseState.testSuite: ParseState.NewTestSuite
        get() = safeAs<ParseState.NewTestSuite>() ?: cast<ParseState.NewTest>().testSuite

    // The very first line of stdlib test output may contain seed of Random. Such line should be ignored.
    private const val STDLIB_TESTS_IGNORED_LINE_PREFIX = "Seed: "
}

private fun parseError(message: String, index: Int, line: String, listing: String): Nothing = fail {
    buildString {
        appendLine("$message at line #$index: \"$line\"")
        appendLine()
        appendLine("Full listing:")
        appendLine(listing)
    }
}
