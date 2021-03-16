/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.testkit.runner.BuildResult

/**
 * Asserts Gradle output contains [expectedSubString] string.
 */
fun BuildResult.assertOutputContains(
    expectedSubString: String
) {
    assert(output.contains(expectedSubString)) {
        "Build output does not contain \"$expectedSubString\""
    }
}

/**
 * Asserts Gradle output does not contain [notExpectedSubString] string.
 *
 * @param wrappingCharsCount amount of chars to include before and after [notExpectedSubString] occurrence
 */
fun BuildResult.assertOutputDoesNotContain(
    notExpectedSubString: String,
    wrappingCharsCount: Int = 100,
) {
    assert(!output.contains(notExpectedSubString)) {
        // In case if notExpectedSubString is multiline string
        val occurrences = mutableListOf<Pair<Int, Int>>()
        var startIndex = output.indexOf(notExpectedSubString)
        var endIndex = startIndex + notExpectedSubString.length
        do {
            occurrences.add(startIndex to endIndex)
            startIndex = output.indexOf(notExpectedSubString, endIndex)
            endIndex = startIndex + notExpectedSubString.length
        } while (startIndex != -1)

        val linesContainingSubString = occurrences.map { (startIndex, endIndex) ->
            output.subSequence(
                (startIndex - wrappingCharsCount).coerceAtLeast(0),
                (endIndex + wrappingCharsCount).coerceAtMost(output.length)
            )
        }

        """
        |
        |===> Build output contains non-expected sub-string:
        |'$notExpectedSubString'
        |===> in following places:
        |${linesContainingSubString.joinToString(separator = "\n|===> Next case:\n")}
        |===> End of occurrences
        |
        """.trimMargin()
    }
}

/**
 * Assert build output contains one or more strings matching [expected] regex.
 */
fun BuildResult.assertOutputContains(
    expected: Regex
) {
    assert(output.contains(expected)) {
        "Build output does not contain any line matching '$expected' regex."
    }
}

/**
 * Asserts build output does not contain any lines matching [regexToCheck] regex.
 */
fun BuildResult.assertOutputDoesNotContain(
    regexToCheck: Regex
) {
    assert(!output.contains(regexToCheck)) {
        val matchedStrings = regexToCheck
            .findAll(output)
            .map { it.value }
            .joinToString(prefix = "  ", separator = "\n  ")
        "Build output contains following regex '$regexToCheck' matches:\n$matchedStrings"
    }
}
