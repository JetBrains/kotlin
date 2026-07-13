/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.provider.Provider
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult
import java.io.File

/**
 * Writes an inventory of executed tests to a headerless, tab-separated `test-inventory.tsv` file.
 *
 * Each line is one test with three columns:
 *  1. full name  - colon-separated suite path ending in `<class>.<test>` (tabs/newlines collapsed to spaces);
 *  2. status     - one of `OK`, `Failure`, `Ignored`;
 *  3. duration   - wall-clock execution time in milliseconds.
 *
 * The full name is derived to match TeamCity's test naming so the inventory aligns with the names TeamCity
 * reports for the same tests (see the linked TeamCity Gradle init script on [getTestName]).
 *
 * The format intentionally has no header row, as it is consumed positionally by external tooling.
 */
class TestInventoryListener(private val taskName: String, buildDir: Provider<File>) : TestListener {
    val inventoryFile = buildDir.map { it.resolve("test-inventory").resolve(taskName).resolve("test-inventory.tsv") }
    private val records = mutableListOf<String>()

    private companion object {
        val WHITESPACE = Regex("[\t\r\n]")
    }

    // See https://jetbrains.team/p/tc/repositories/teamcity-gradle/files/a7fe40dfe94c4af5c4407003fb6fecaed4cc6795/gradle-runner-agent/src/main/scripts/init_since_8.gradle
    private fun TestDescriptor.getTestName(): String {
        val methodName = name.takeWhile { it !in "([{<" }
        val testName = if (displayName.startsWith(methodName)) displayName else "$methodName($displayName)"
        return testName.takeUnless { it == "$methodName()" } ?: methodName
    }

    override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {
        val className = testDescriptor.className
        val suites = generateSequence(testDescriptor.parent) { it.parent }
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

        val status = when (result.resultType) {
            TestResult.ResultType.FAILURE -> "Failure"
            TestResult.ResultType.SUCCESS -> "OK"
            TestResult.ResultType.SKIPPED -> "Ignored"
        }

        val duration = result.endTime - result.startTime
        val testName = testDescriptor.getTestName()
        val leaf = className?.let { "$it.$testName" } ?: testName
        val fullName = (suites + leaf).joinToString(": ").replace(WHITESPACE, " ")
        records += "$fullName\t$status\t$duration"
    }

    override fun afterSuite(suite: TestDescriptor, result: TestResult) {
        if (suite.parent == null) {
            val outputFile = inventoryFile.get()
            outputFile.parentFile.mkdirs()
            outputFile.bufferedWriter(Charsets.UTF_8).use { writer ->
                records.forEach { writer.appendLine(it) }
            }
        }
    }
}
