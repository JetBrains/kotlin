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
 * reports for the same tests (see [toTestPath]).
 *
 * The format intentionally has no header row, as it is consumed positionally by external tooling.
 *
 * Because the suite names and the test name are joined, the nesting cannot be recovered from this file:
 * the separator may also occur inside a test name. [TestExecutionsListener] keeps the same data with the
 * nesting preserved, for consumers that need it.
 */
class TestInventoryListener(private val taskName: String, buildDir: Provider<File>) : TestListener {
    val inventoryFile = buildDir.map { it.resolve("test-inventory").resolve(taskName).resolve("test-inventory.tsv") }
    private val records = mutableListOf<String>()

    private companion object {
        val WHITESPACE = Regex("[\t\r\n]")
    }

    override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {
        val fullName = testDescriptor.toTestPath(taskName).joinToTeamCityName().replace(WHITESPACE, " ")
        records += "$fullName\t${result.statusName()}\t${result.durationMillis}"
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
