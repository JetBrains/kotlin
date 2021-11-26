/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.testkit.runner.BuildResult
import java.nio.file.Path

/**
 * Asserts Gradle output contains [expectedSubString] string.
 */
fun BuildResult.assertOutputContains(
    expectedSubString: String
) {
    assert(output.contains(expectedSubString)) {
        printBuildOutput()
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
        printBuildOutput()

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
        printBuildOutput()

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
        printBuildOutput()

        val matchedStrings = regexToCheck
            .findAll(output)
            .map { it.value }
            .joinToString(prefix = "  ", separator = "\n  ")
        "Build output contains following regex '$regexToCheck' matches:\n$matchedStrings"
    }
}

/**
 * Asserts build output contains exactly [expectedCount] of occurrences of [expected] string.
 */
fun BuildResult.assertOutputContainsExactlyTimes(
    expected: String,
    expectedCount: Int = 1
) {
    val occurrenceCount = expected.toRegex(RegexOption.LITERAL).findAll(output).count()
    assert(occurrenceCount == expectedCount) {
        printBuildOutput()

        "Build output contains different number of '$expected' string occurrences - $occurrenceCount then $expectedCount"
    }
}

/**
 * Assert build contains no warnings.
 */
fun BuildResult.assertNoBuildWarnings(
    expectedWarnings: Set<String> = emptySet()
) {
    val cleanedOutput = expectedWarnings.fold(output) { acc, s ->
        acc.replace(s, "")
    }
    val warnings = cleanedOutput
        .lineSequence()
        .filter { it.trim().startsWith("w:") }
        .toList()

    assert(warnings.isEmpty()) {
        printBuildOutput()

        "Build contains following warnings:\n ${warnings.joinToString(separator = "\n")}"
    }
}

fun BuildResult.assertIncrementalCompilation(
    modifiedFiles: Set<Path> = emptySet(),
    deletedFiles: Set<Path> = emptySet()
) {
    val incrementalCompilationOptions = output
        .lineSequence()
        .filter { it.trim().startsWith("Options for KOTLIN DAEMON: IncrementalCompilationOptions(") }
        .map {
            it
                .removePrefix("Options for KOTLIN DAEMON: IncrementalCompilationOptions(")
                .removeSuffix(")")
        }
        .toList()

    assert(incrementalCompilationOptions.isNotEmpty()) {
        printBuildOutput()
        "No incremental compilation options were found in the build"
    }

    val modifiedFilesPath = modifiedFiles.map { it.toAbsolutePath().toString() }
    val deletedFilesPath = deletedFiles.map { it.toAbsolutePath().toString() }
    val hasMatch = incrementalCompilationOptions
        .firstOrNull {
            val optionModifiedFiles = it
                .substringAfter("modifiedFiles=[")
                .substringBefore("]")
                .split(",")
                .filter(String::isNotEmpty)

            val modifiedFilesFound = if (modifiedFilesPath.isEmpty()) {
                optionModifiedFiles.isEmpty()
            } else {
                modifiedFilesPath.subtract(optionModifiedFiles).isEmpty()
            }

            val optionDeletedFiles = it
                .substringAfter("deletedFiles=[")
                .substringBefore("]")
                .split(",")
                .filter(String::isNotEmpty)

            val deletedFilesFound = if (deletedFilesPath.isEmpty()) {
                optionDeletedFiles.isEmpty()
            } else {
                deletedFilesPath.subtract(optionDeletedFiles).isEmpty()
            }

            modifiedFilesFound && deletedFilesFound
        } != null

    assert(hasMatch) {
        printBuildOutput()

        """
        |Expected incremental compilation options with:
        |- modified files: ${modifiedFilesPath.joinToString()}
        |- deleted files: ${deletedFilesPath.joinToString()}
        |        
        |but none of following compilation options match:
        |${incrementalCompilationOptions.joinToString(separator = "\n")}
        """.trimMargin()
    }
}
