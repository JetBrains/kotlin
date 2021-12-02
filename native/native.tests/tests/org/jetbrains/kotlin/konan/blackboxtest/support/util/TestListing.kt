/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.util

import org.jetbrains.kotlin.konan.blackboxtest.support.PackageFQN
import org.jetbrains.kotlin.konan.blackboxtest.support.TestFunction
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import org.jetbrains.kotlin.konan.blackboxtest.support.util.GTestListingParseState as State

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
    var state: State = State.Begin

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
                is State.NewTest, is State.End -> State.End
                else -> parseError("Unexpected empty line")
            }
            line[0].isWhitespace() -> when (val s = state) {
                is State.HasPackageName -> {
                    this += TestFunction(s.packageName, line.trim())
                    State.NewTest(s.packageName)
                }
                else -> parseError("Test name encountered before test suite name")
            }
            else -> when (state) {
                is State.Begin, is State.NewTest -> {
                    val packageParts = line.trimEnd().removeSuffix(".").split('.')
                    if (packageParts.isEmpty()) parseError("Malformed test suite name")

                    // Drop the last part because it is related to class name (or file-class name).
                    // TODO: How to handle nested classes?
                    val packageName = packageParts.dropLast(1).joinToString(".")

                    State.NewTestSuite(packageName)
                }
                else -> parseError("Unexpected test suite name")
            }
        }
    }
}

private sealed interface GTestListingParseState {
    object Begin : State
    object End : State
    class NewTestSuite(override val packageName: PackageFQN) : State, HasPackageName
    class NewTest(override val packageName: PackageFQN) : State, HasPackageName

    interface HasPackageName {
        val packageName: PackageFQN
    }
}

// The very first line of stdlib test output may contain seed of Random. Such line should be ignored.
private const val STDLIB_TESTS_IGNORED_LINE_PREFIX = "Seed: "
