/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestResult

internal class TestPath(val suites: List<String>, val testName: String) {
    fun joinToTeamCityName(): String = (suites + testName).joinToString(": ")
}

/**
 * The method part of the name as TeamCity's own Gradle runner reports it, without the class prefix.
 *
 * A port of `TestNameDescriptor.DISPLAY_NAME.getTestName` from the runner's init script, see
 * gradle-runner-agent/src/main/scripts/init_since_8.gradle
 */
private fun TestDescriptor.teamCityRunnerMethodName(): String {
    val methodName = name.takeWhile { it !in "([{<" }
    val candidate = if (displayName.startsWith(methodName)) displayName else "$methodName($displayName)"
    return if (candidate == "$name()") name else candidate
}

/**
 * Emulates the normalization TeamCity's server applies to the method part of a test name: an *empty*
 * parameter list is dropped, a non-empty one is kept.
 *
 * Ported from `TestNameParser.ParsePart.checkForParameters`
 * (`server-model/src/jetbrains/buildServer/tests/TestNameParser.java`)
 *
 * Only the empty round list is handled here, which is the only case observed to be dropped; anything
 * else is left untouched, as TeamCity leaves it.
 */
private fun String.dropEmptyParameterList(): String {
    if (!endsWith("()")) return this

    val openBrace = length - 2
    val firstBrace = indexOfFirst { it == '(' || it == '[' }
    if (firstBrace != openBrace) return this
    if (openBrace == 0 || this[openBrace - 1] == ' ') return this

    return substring(0, openBrace)
}

/**
 * The name TeamCity's Gradle runner sends for a test using service message, before the server normalizes it.
 */
internal fun TestDescriptor.toTeamCityRunnerTestName(): String {
    val methodName = teamCityRunnerMethodName()
    return className?.let { "$it.$methodName" } ?: methodName
}

/**
 * The name TeamCity ends up registering for a test.
 * This is the name to compare against anything read back from TeamCity.
 */
internal fun TestDescriptor.toTeamCityRegisteredTestName(): String {
    val methodName = teamCityRunnerMethodName().dropEmptyParameterList()
    return className?.let { "$it.$methodName" } ?: methodName
}

internal fun TestDescriptor.toTestPath(taskName: String): TestPath {
    val suites = generateSequence(parent) { it.parent }
        .map { it.name }
        .dropWhile { it == className }
        .filterNot {
            it.startsWith("Gradle Test Executor") ||
                    it.startsWith("Gradle Test Run") ||
                    it.startsWith("Partition") ||
                    it == taskName ||
                    it == "$taskName.$className"
        }
        .toList()
        .asReversed()

    val testName = toTeamCityRegisteredTestName()

    if (testName.endsWith("'")) {
        error("Test $testName ends with ' (apostrophe) symbol and may be processed incorrectly by TeamCity (TW-101796)")
    }

    return TestPath(suites, testName)
}


/** The status names TeamCity's own Gradle integration uses. */
internal fun TestResult.statusName(): String = when (resultType) {
    TestResult.ResultType.FAILURE -> "Failure"
    TestResult.ResultType.SUCCESS -> "OK"
    TestResult.ResultType.SKIPPED -> "Ignored"
}

internal val TestResult.durationMillis: Long get() = endTime - startTime
