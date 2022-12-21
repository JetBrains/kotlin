/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.tests

import kotlinx.validation.api.*
import java.io.File
import java.util.LinkedList
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.fail

private val OVERWRITE_EXPECTED_OUTPUT = System.getProperty("overwrite.output")?.toBoolean() ?: false // use -Doverwrite.output=true

fun List<ClassBinarySignature>.dumpAndCompareWith(to: File) {
    if (!to.exists()) {
        to.parentFile?.mkdirs()
        to.bufferedWriter().use { dump(to = it) }
        fail("Expected data file did not exist. Generating: $to")
    } else {
        val actual = dump(to = StringBuilder())
        assertEqualsToFile(to, actual)
    }
}

private fun assertEqualsToFile(expectedFile: File, actual: CharSequence) {
    val actualText = actual.trimTrailingWhitespacesAndAddNewlineAtEOF()
    val expectedText = expectedFile.readText().trimTrailingWhitespacesAndAddNewlineAtEOF()

    if (expectedText != actualText) {
        if (OVERWRITE_EXPECTED_OUTPUT) {
            expectedFile.writeText(actualText)
        }

        assertEqualsWithFirstLineDiff(
            expectedText,
            actualText,
            if (OVERWRITE_EXPECTED_OUTPUT) {
                "Actual data differs from file content: ${expectedFile.name}, rewriting\n"
            } else {
                "Actual data differs from file content: ${expectedFile.name}\nTo overwrite the expected API rerun with -Doverwrite.output=true parameter\n"
            }
        )
    }
}

fun assertEqualsWithFirstLineDiff(expectedText: String, actualText: String, message: String, diffLinesSurround: Int = 1) {
    val actualLinesIterator = actualText.lineSequence().iterator()
    val expectedLinesIterator = expectedText.lineSequence().iterator()

    val actualBufferLines = LinkedList<String?>()
    val expectedBufferLines = LinkedList<String?>()

    var diffFound = false
    var diffLine = -1
    var line = 0

    while ((actualLinesIterator.hasNext() || expectedLinesIterator.hasNext()) && (!diffFound || line - diffLine < diffLinesSurround)) {
        line++

        val actualLine: String? = if (actualLinesIterator.hasNext()) actualLinesIterator.next() else null
        actualBufferLines.add(actualLine)

        val expectedLine: String? = if (expectedLinesIterator.hasNext()) expectedLinesIterator.next() else null
        expectedBufferLines.add(expectedLine)

        if (actualBufferLines.size > diffLinesSurround * 2 + 1) {
            actualBufferLines.removeFirst()
            expectedBufferLines.removeFirst()
        }

        if (!diffFound && actualLine != expectedLine) {
            diffFound = true
            diffLine = line
        }
    }

    if (diffFound) {
        val lineInfo = "↓↓↓ Line $diffLine, $diffLinesSurround lines around the first difference ↓↓↓"
        val actualTextAroundDiff = actualBufferLines.filterNotNull().joinToString("\n", prefix = "$lineInfo\n")
        val expectedTextAroundDiff = expectedBufferLines.filterNotNull().joinToString("\n", prefix = "$lineInfo\n")

        assertNotEquals(expectedTextAroundDiff, actualTextAroundDiff, "Sanity check - chunks should be different")

        assertEquals(
            expectedTextAroundDiff,
            actualTextAroundDiff,
            message
        )
    }
}

private fun CharSequence.trimTrailingWhitespacesAndAddNewlineAtEOF(): String =
    this.lineSequence().map { it.trimEnd() }.joinToString(separator = "\n").let {
        if (it.endsWith("\n")) it else it + "\n"
    }


private val UPPER_CASE_CHARS = Regex("[A-Z]+")
fun String.replaceCamelCaseWithDashedLowerCase() = replace(UPPER_CASE_CHARS) { "-" + it.value.lowercase() }
