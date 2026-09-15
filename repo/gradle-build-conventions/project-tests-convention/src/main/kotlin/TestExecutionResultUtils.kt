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

// See https://jetbrains.team/p/tc/repositories/teamcity-gradle/files/a7fe40dfe94c4af5c4407003fb6fecaed4cc6795/gradle-runner-agent/src/main/scripts/init_since_8.gradle
private fun TestDescriptor.getTestName(): String {
    val methodName = name.takeWhile { it !in "([{<" }
    val testName = if (displayName.startsWith(methodName)) displayName else "$methodName($displayName)"
    return testName.takeUnless { it == "$methodName()" } ?: methodName
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

    val testName = getTestName()
    val leaf = className?.let { "$it.$testName" } ?: testName

    if (leaf.endsWith("'")) {
        error("Test $leaf ends with ' (apostrophe) symbol and may be processed incorrectly by TeamCity (TW-101796)")
    }

    return TestPath(suites, leaf)
}

/** The status names TeamCity's own Gradle integration uses. */
internal fun TestResult.statusName(): String = when (resultType) {
    TestResult.ResultType.FAILURE -> "Failure"
    TestResult.ResultType.SUCCESS -> "OK"
    TestResult.ResultType.SKIPPED -> "Ignored"
}

internal val TestResult.durationMillis: Long get() = endTime - startTime
