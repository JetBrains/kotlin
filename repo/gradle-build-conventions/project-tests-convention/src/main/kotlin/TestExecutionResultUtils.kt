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
internal fun TestDescriptor.teamCityRunnerMethodName(): String {
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
 * The name TeamCity ends up registering for a test.
 * This is the name to compare against anything read back from TeamCity.
 */
internal fun TestDescriptor.toTeamCityRegisteredTestName(): String =
    className.qualifying(teamCityRunnerMethodName().dropEmptyParameterList())

/**
 * Spells a test name the way TeamCity does, `<class>.<method>`.
 *
 * [TestExecutionsListener] records the two halves apart, so that the class is not repeated in every
 * test name now that it is a suite of its own, and a replay puts them back together with this.
 */
internal fun String?.qualifying(methodName: String): String = this?.let { "$it.$methodName" } ?: methodName

/**
 * Whether Gradle inserted this suite itself - the test run, the executor, and the partitions the test
 * tasks are split into - rather than it standing for something in the test sources.
 *
 * The runner's init script drops the same ones, see `SuiteDescriptorWrapper.isIgnored`.
 */
internal fun isGradleInsertedSuiteName(suiteName: String, taskName: String): Boolean =
    suiteName.startsWith("Gradle Test Executor") ||
            suiteName.startsWith("Gradle Test Run") ||
            suiteName.startsWith("Partition") ||
            suiteName == taskName

/**
 * Whether a suite of this name carries nothing of its own in a test's TeamCity name - either Gradle
 * inserted it, or it stands for the test's own class, which TeamCity spells as part of the test name
 * rather than as an enclosing suite.
 *
 * A task may name a class suite after itself as well: Kotlin/JS reports
 * `jsNodeTest.kotlin.powerassert.DefaultMessageTest` where the JVM tasks report the bare class name.
 */
internal fun isSyntheticSuiteName(suiteName: String, taskName: String, className: String?): Boolean =
    isGradleInsertedSuiteName(suiteName, taskName) || suiteName == "$taskName.$className"

/**
 * The name [TestExecutionsListener] records this suite under: its class, for a task that prefixes a
 * class suite with its own name, and the name Gradle reports otherwise.
 *
 * Only that exact spelling is rewritten, rather than every suite that has a class. A JUnit 5
 * `@Nested` or parameterized container also carries a `className` while standing for something
 * narrower than the class, and must keep the name of its own.
 */
internal fun TestDescriptor.recordedSuiteName(taskName: String): String {
    val className = className ?: return name
    return if (name == "$taskName.$className") className else name
}

/**
 * The suites enclosing this descriptor, outermost first, with only the ones Gradle inserted removed.
 *
 * Unlike [toTestPath] this keeps the suite that repeats the test's own class: it is the structure
 * Gradle reports, which is what [TestExecutionsListener] records so that per-class timings survive -
 * including on the tasks that name such a suite after the task, see [recordedSuiteName]. Collapsing
 * it into the test name is TeamCity's convention and belongs at the point the tests are replayed,
 * not in the recorded file.
 */
internal fun TestDescriptor.enclosingSuiteNames(taskName: String): List<String> =
    generateSequence(parent) { it.parent }
        .filterNot { isGradleInsertedSuiteName(it.name, taskName) }
        .map { it.recordedSuiteName(taskName) }
        .toList()
        .asReversed()

internal fun TestDescriptor.toTestPath(taskName: String): TestPath {
    val suites = generateSequence(parent) { it.parent }
        .map { it.name }
        .dropWhile { it == className }
        .filterNot { isSyntheticSuiteName(it, taskName, className) }
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
