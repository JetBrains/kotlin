/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.util

import org.jetbrains.kotlin.konan.blackboxtest.support.TestName
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.kotlin.konan.blackboxtest.support.util.GTestListingParseState as State

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
internal fun parseGTestListing(listing: String): Collection<TestName> = buildList {
    var state: State = State.Begin

    listing.lineSequence().forEachIndexed { index, line ->
        fun parseError(message: String): Nothing = fail {
            buildString {
                appendLine("$message at line #$index: \"$line\"")
                appendLine()
                appendLine("Full listing:")
                appendLine(listing)
            }
        }

        state = when {
            index == 0 && line.startsWith(STDLIB_TESTS_IGNORED_LINE_PREFIX) -> state
            line.isBlank() -> when (state) {
                is State.NewTest, is State.End -> State.End
                else -> parseError("Unexpected empty line")
            }
            line[0].isWhitespace() -> when (state) {
                is State.NewTestSuite,
                is State.NewTest -> {
                    val testSuite = state.testSuite
                    this += TestName(testSuite.testSuiteNameWithDotSuffix + line.trim())
                    State.NewTest(testSuite)
                }
                else -> parseError("Test name encountered before test suite name")
            }
            else -> when (state) {
                is State.Begin, is State.NewTest -> {
                    State.NewTestSuite(line.trimEnd())
                }
                else -> parseError("Unexpected test suite name")
            }
        }
    }
}

private sealed interface GTestListingParseState {
    object Begin : State
    object End : State

    class NewTestSuite(val testSuiteNameWithDotSuffix: String) : State
    class NewTest(val testSuite: NewTestSuite) : State
}

private inline val State.testSuite: State.NewTestSuite
    get() = safeAs<State.NewTestSuite>() ?: cast<State.NewTest>().testSuite

// The very first line of stdlib test output may contain seed of Random. Such line should be ignored.
private const val STDLIB_TESTS_IGNORED_LINE_PREFIX = "Seed: "
