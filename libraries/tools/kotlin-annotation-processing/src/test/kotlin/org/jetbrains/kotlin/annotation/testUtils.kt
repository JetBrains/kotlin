package org.jetbrains.kotlin.annotation

import org.junit.Assert
import java.io.File

fun assertEqualsToFile(expectedFile: File, actual: String) {
    val lineSeparator = System.getProperty("line.separator")
    val actualText = actual.replace(lineSeparator, "\n").trim('\n', ' ', '\t')

    if (!expectedFile.exists()) {
        expectedFile.writeText(actualText.replace("\n", lineSeparator))
        Assert.fail("Expected data file did not exist. Generating: " + expectedFile)
    }

    val expectedText = expectedFile.readText().replace(lineSeparator, "\n")

    Assert.assertEquals(expectedText, actualText)
}