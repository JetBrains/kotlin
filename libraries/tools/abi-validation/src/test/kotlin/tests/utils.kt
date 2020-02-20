/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation.api.tests

import kotlinx.validation.api.*
import java.io.File
import kotlin.test.assertEquals
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

    if (OVERWRITE_EXPECTED_OUTPUT && expectedText != actualText) {
        expectedFile.writeText(actualText)
        assertEquals(expectedText, actualText, "Actual data differs from file content: ${expectedFile.name}, rewriting")
    }

    assertEquals(expectedText, actualText, "Actual data differs from file content: ${expectedFile.name}\nTo overwrite the expected API rerun with -Doverwrite.output=true parameter\n")
}

private fun CharSequence.trimTrailingWhitespacesAndAddNewlineAtEOF(): String =
        this.lineSequence().map { it.trimEnd() }.joinToString(separator = "\n").let {
            if (it.endsWith("\n")) it else it + "\n"
        }


private val UPPER_CASE_CHARS = Regex("[A-Z]+")
fun String.replaceCamelCaseWithDashedLowerCase() = replace(UPPER_CASE_CHARS) { "-" + it.value.toLowerCase() }
