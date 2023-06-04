/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.api.logging.configuration.WarningMode
import org.gradle.testkit.runner.BuildResult

/**
 * Asserts Gradle output contains [expectedSubString] string.
 */
fun BuildResult.assertOutputContains(
    expectedSubString: String,
    message: String = "Build output does not contain \"$expectedSubString\"",
) {
    assert(output.contains(expectedSubString)) {
        printBuildOutput()
        message
    }
}

/**
 * Asserts Gradle output contains any of [expectedSubString] strings.
 */
fun BuildResult.assertOutputContainsAny(
    vararg expectedSubStrings: String,
) {
    assert(expectedSubStrings.any { output.contains(it) }) {
        printBuildOutput()
        "Build output does not contain any of \"$expectedSubStrings\""
    }
}

/**
 * Asserts Gradle output contains [expectedSubString] string exact times.
 */
fun BuildResult.assertOutputContainsExactTimes(
    expectedSubString: String,
    expectedRepetitionTimes: Int = 1,
) {
    var currentOffset = 0
    var count = 0
    var nextIndex = output.indexOf(expectedSubString, currentOffset)

    while (nextIndex != -1 && count < expectedRepetitionTimes + 1) {
        count++
        currentOffset = nextIndex + expectedSubString.length
        nextIndex = output.indexOf(expectedSubString, currentOffset)
    }

    assert(count == expectedRepetitionTimes) {
        printBuildOutput()
        "Build output contains \"$expectedSubString\" $count times"
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
    expected: Regex,
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
    regexToCheck: Regex,
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
    expectedCount: Int = 1,
) {
    assertOutputContainsExactlyTimes(expected.toRegex(RegexOption.LITERAL), expectedCount)
}

fun BuildResult.assertOutputContainsExactlyTimes(
    expected: Regex,
    expectedCount: Int = 1,
) {
    val occurrenceCount = expected.findAll(output).count()
    assert(occurrenceCount == expectedCount) {
        printBuildOutput()

        "Build output contains different number of '$expected' string occurrences - $occurrenceCount then $expectedCount"
    }
}

/**
 * Assert build contains no warnings.
 */
fun BuildResult.assertNoBuildWarnings(
    expectedWarnings: Set<String> = emptySet(),
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

/**
 * Asserts compilation is running via Kotlin daemon with given jvm arguments.
 */
fun BuildResult.assertKotlinDaemonJvmOptions(
    expectedJvmArgs: List<String>,
) {
    val jvmArgsCommonMessage = "Kotlin compile daemon JVM options: "
    assertOutputContains(jvmArgsCommonMessage)
    val argsRegex = "\\[.+?]".toRegex()
    val argsStrings = output.lineSequence()
        .filter { it.contains(jvmArgsCommonMessage) }
        .map {
            argsRegex.findAll(it).last().value.removePrefix("[").removeSuffix("]").split(", ")
        }
    val containsArgs = argsStrings.any {
        it.containsAll(expectedJvmArgs)
    }

    assert(containsArgs) {
        printBuildOutput()

        "${argsStrings.toList()} does not contain expected args: $expectedJvmArgs"
    }
}

fun BuildResult.assertBuildReportPathIsPrinted() {
    assertOutputContains("Kotlin build report is written to file://")
}

val NO_GRADLE_WARNINGS_DETECTOR_PLUGIN_ERROR_MESSAGE =
    """
    The build uses warning mode other than `${WarningMode.Fail}` and uses a non-default project settings file.
    Please apply the `org.jetbrains.kotlin.test.gradle-warnings-detector` plugin to the settings.

    """.trimIndent()

fun getWarningModeChangeAdvice(warningMode: WarningMode) =
    "Warning mode is set to `$warningMode`, but the build produced no deprecation warnings. Please set it to `${WarningMode.Fail}`"

/**
 * Asserts that the build produced some deprecation warnings.
 *
 * Expected to be executed only for the case when [BuildOptions.warningMode] is not set to [WarningMode.Fail]
 */
fun BuildResult.assertDeprecationWarningsArePresent(warningMode: WarningMode) {
    assertOutputContains("[GradleWarningsDetectorPlugin] The plugin is being applied", NO_GRADLE_WARNINGS_DETECTOR_PLUGIN_ERROR_MESSAGE)
    assertOutputContains(
        "[GradleWarningsDetectorPlugin] Some deprecation warnings were found during this build.",
        getWarningModeChangeAdvice(warningMode)
    )
}

/**
 * This function searches for a given parameter in a multi-line output string and returns its value.
 *
 * The output string is assumed to be in the form of key-value pairs separated by an equal sign (‘=’) on each line.
 *
 * If the specified parameter name is found at the end of a key, the corresponding value is returned.
 * If the parameter is not found, the function returns null.
 */
fun findParameterInOutput(name: String, output: String): String? =
    output.lineSequence().mapNotNull { line ->
        val (key, value) = line.split('=', limit = 2).takeIf { it.size == 2 } ?: return@mapNotNull null
        if (key.endsWith(name)) value else null
    }.firstOrNull()

fun BuildResult.assertCompilerArgument(
    taskPath: String,
    expectedArgument: String,
) {
    val taskOutput = getOutputForTask(taskPath)
    val compilerArguments = taskOutput.lines().first {
        it.contains("Kotlin compiler args:")
    }.substringAfter("Kotlin compiler args:")

    assert(compilerArguments.contains(expectedArgument)) {
        printBuildOutput()

        "$taskPath task compiler arguments don't contain $expectedArgument. Actual content: $compilerArguments"
    }
}

fun BuildResult.assertCompilerArguments(
    taskPath: String,
    vararg expectedArguments: String,
) {
    val taskOutput = getOutputForTask(taskPath)
    val compilerArguments = taskOutput.lines().first {
        it.contains("Kotlin compiler args:")
    }.substringAfter("Kotlin compiler args:")

    val nonExistingArguments = expectedArguments
        .filter {
            !compilerArguments.contains(it)
        }

    assert(nonExistingArguments.isEmpty()) {
        printBuildOutput()

        "$taskPath task compiler arguments don't contain ${nonExistingArguments.joinToString()}. Actual content: $compilerArguments"
    }
}

fun BuildResult.assertNoCompilerArgument(
    taskPath: String,
    notExpectedArgument: String,
) {
    val taskOutput = getOutputForTask(taskPath)
    val compilerArguments = taskOutput.lines().first {
        it.contains("Kotlin compiler args:")
    }.substringAfter("Kotlin compiler args:")

    assert(!compilerArguments.contains(notExpectedArgument)) {
        printBuildOutput()

        "$taskPath task compiler arguments contains $notExpectedArgument. Actual content: $compilerArguments"
    }
}

/**
 * Asserts that the given list of command line arguments does not contain any of the expected arguments.
 *
 * @param expectedArgs the list of expected arguments
 * @throws AssertionError if any of the expected arguments are found in the actual arguments list
 */
fun CommandLineArguments.assertCommandLineArgumentsDoNotContain(
    vararg expectedArgs: String,
) {
    expectedArgs.forEach {
        assert(!args.contains(it)) {
            this.buildResult.printBuildOutput()
            "There is unexpected ${it} in actual command line arguments are: ${args}"
        }
    }
}

/**
 * Asserts that the given list of command line arguments contains all the expected arguments.
 *
 * @param expectedArgs the list of expected arguments
 * @throws AssertionError if any of the expected arguments are missing from the actual arguments list
 */
fun CommandLineArguments.assertCommandLineArgumentsContain(
    vararg expectedArgs: String,
) {
    expectedArgs.forEach {
        assert(args.contains(it)) {
            this.buildResult.printBuildOutput()
            "There is no ${it} in actual command line arguments are: ${args}"
        }
    }
}