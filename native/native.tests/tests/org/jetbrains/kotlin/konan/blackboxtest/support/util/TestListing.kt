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
        fun parseError(message: String, index: Int, line: String): Nothing = fail {
            buildString {
                appendLine("$message at line #$index: \"$line\"")
                appendLine()
                appendLine("Full listing:")
                appendLine(listing)
            }
        }

        var state: ParseState = ParseState.Begin

        val lines = listing.lines()
        lines.forEachIndexed { index, line ->
            fun parseError(message: String): Nothing = parseError(message, index, line)

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
            parseError("Test name expected before test suite name", lines.lastIndex, lines.last())
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