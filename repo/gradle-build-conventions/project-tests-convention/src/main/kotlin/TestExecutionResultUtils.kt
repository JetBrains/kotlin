/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestResult

/**
 * Where a single test sits in the test hierarchy, as TeamCity names it.
 *
 * TeamCity derives the full name of a test by joining the names of the enclosing suites and the test
 * itself with `": "`. [TestInventoryListener] stores that joined name, while [TestExecutionsListener]
 * stores the same parts separately, so that the suite nesting can be replayed as
 * `testSuiteStarted` / `testSuiteFinished` service messages. Both go through this file, so the two
 * cannot drift apart.
 */
internal class TestPath(val suites: List<String>, val leaf: String) {
    fun joinToTeamCityName(): String = (suites + leaf).joinToString(": ")
}

/**
 * The method part of the name as TeamCity's own Gradle runner reports it, without the class prefix.
 *
 * A port of `TestNameDescriptor.DISPLAY_NAME.getTestName` from the runner's init script, see
 * https://jetbrains.team/p/tc/repositories/teamcity-gradle/files/a7fe40dfe94c4af5c4407003fb6fecaed4cc6795/gradle-runner-agent/src/main/scripts/init_since_8.gradle
 *
 * Note that the last step compares against the *raw* descriptor name, not against the name stripped at
 * the first bracket: for a JUnit method Gradle reports both `name` and `displayName` as `method()`, so
 * the comparison fails and the parentheses stay on. Dropping them is the server's job, not ours - see
 * [dropEmptyParameterList].
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
 * (`server-model/src/jetbrains/buildServer/tests/TestNameParser.java`), which refuses to read a
 * parameter list - leaving the name exactly as it was reported - unless the part closes with a brace,
 * the opening brace is neither absent nor the first character, and the character before it is not a
 * space. `jetbrains.test.Test.test()` therefore registers as `jetbrains.test.Test.test`, while
 * `Test.test - a name ending in a space ()` and `Test.CHECK mode (TeamCity) - x()` keep their
 * parentheses, the former because a space precedes the brace and the latter because the parameter list
 * it finds is `(TeamCity) - x()` rather than `()`.
 *
 * Only the empty round list is handled here, which is the only case observed to be dropped; anything
 * else is left untouched, as TeamCity leaves it.
 */
private fun String.dropEmptyParameterList(): String {
    if (!endsWith("()")) return this

    val openBrace = length - 2
    // The first brace of either kind has to be the one that opens this very list.
    val firstBrace = indexOfFirst { it == '(' || it == '[' }
    if (firstBrace != openBrace) return this
    // TeamCity ignores a list opened by the first character, or preceded by a space.
    if (openBrace == 0 || this[openBrace - 1] == ' ') return this

    return substring(0, openBrace)
}

/**
 * The name TeamCity's Gradle runner sends for this test, before the server normalizes it.
 *
 * This is what a replay has to emit: those messages go through the same server-side normalization as
 * the runner's, so emitting an already-normalized name is what made a replayed name differ from an
 * executed one.
 */
internal fun TestDescriptor.toTeamCityRunnerTestName(): String {
    val methodName = teamCityRunnerMethodName()
    return className?.let { "$it.$methodName" } ?: methodName
}

/**
 * The name TeamCity ends up registering for this test - the runner's name put through the server's
 * normalization. This is the name to compare against anything read back from TeamCity.
 */
internal fun TestDescriptor.toTeamCityRegisteredTestName(): String {
    val methodName = teamCityRunnerMethodName().dropEmptyParameterList()
    return className?.let { "$it.$methodName" } ?: methodName
}

/**
 * Splits this descriptor into the suites that enclose it and its own name.
 *
 * The synthetic suites Gradle inserts (the test run, the executor, and the partitions the test tasks
 * are split into) carry no meaning for TeamCity and are dropped, as is the suite that merely repeats
 * the class name already present in the leaf.
 */
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
