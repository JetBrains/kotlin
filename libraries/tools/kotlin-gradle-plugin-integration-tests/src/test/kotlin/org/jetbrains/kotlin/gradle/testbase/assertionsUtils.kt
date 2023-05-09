/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.testkit.runner.BuildResult
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.kotlin.test.util.convertLineSeparators
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndAddNewlineAtEOF
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.fail

internal fun BuildResult.printBuildOutput() {
    println(
        """
        |Failed assertion build output:
        |#######################
        |$output
        |#######################
        |
        """.trimMargin()
    )
}

internal fun String.normalizeLineEndings(): String = replace("\n", System.lineSeparator())

/*
Ideally, this would've been just KotlinTestUtils.assertEqualsToFile, throwing FileComparisonException.
Normally this is actually desired, because IDEA and TC provide special support for those exceptions with
nice diff windows. However, due to shaded artifacts used in runtime of integration tests, actual thrown
exception will have FQN like org.jetbrains.kotlin.com.intellij... . This not only breaks the tooling support,
but also rendered diff will be trimmed, making working with such failures extremely inconvenient
*/
internal fun assertEqualsToFile(expectedFile: File, actualText: String) {
    val textSanitized: String = actualText.trim().convertLineSeparators().trimTrailingWhitespacesAndAddNewlineAtEOF()

    if (!expectedFile.exists()) {
        if (KtUsefulTestCase.IS_UNDER_TEAMCITY) {
            fail("Expected data file $expectedFile did not exist")
        } else {
            expectedFile.writeText(textSanitized)
            fail("Expected data file did not exist. Generating: $expectedFile")
        }
    }
    val expected: String = expectedFile.readText().convertLineSeparators().trimTrailingWhitespacesAndAddNewlineAtEOF()

    assertEquals(expected, textSanitized, "Comparison failure")
}
