package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.incremental.parseTestBuildLog
import org.jetbrains.kotlin.utils.Printer
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class BuildLogParserParametrizedIT : BaseGradleIT() {

    @Parameterized.Parameter
    @JvmField
    var testDirName: String = ""

    @Test
    fun testParser() {
        val testDir = File(TEST_ROOT, testDirName)
        val logFile = File(testDir, LOG_FILE_NAME)
        assert(logFile.isFile) { "Log file: $logFile does not exist" }

        val parsedStages = parseTestBuildLog(logFile)
        val sb = StringBuilder()
        val p = Printer(sb)

        for ((i, stage) in parsedStages.withIndex()) {
            if (i > 0) {
                p.println()
            }

            p.println("Step #${i + 1}")

            p.println("Compiled java files:")
            p.printlnSorted(stage.compiledJavaFiles)

            p.println("Compiled kotlin files:")
            p.printlnSorted(stage.compiledKotlinFiles)

            p.println("Compile errors:")
            p.printlnSorted(stage.compileErrors)
        }

        val actual = sb.toString()
        val expectedFile = File(testDir, EXPECTED_PARSED_LOG_FILE_NAME)

        if (!expectedFile.isFile) {
            expectedFile.createNewFile()
            expectedFile.writeText(actual)

            throw AssertionError("Expected file log did not exist, created: $expectedFile")
        }

        val expectedContent = expectedFile.readText()
        Assert.assertEquals("Parsed content was unexpected: ", actual, expectedContent)
    }

    companion object {
        private val TEST_ROOT = File(resourcesRootFile, "buildLogsParserData")
        private val LOG_FILE_NAME = "build.log"
        private val EXPECTED_PARSED_LOG_FILE_NAME = "expected.txt"

        @Suppress("unused")
        @Parameterized.Parameters(name = "{index}: {0}")
        @JvmStatic
        fun data(): List<Array<String>> {
            val directories = TEST_ROOT.listFiles().filter { it.isDirectory }
            return directories.map { arrayOf(it.name) }.toList()
        }
    }
}

private fun <T : Comparable<T>> Printer.printlnSorted(elements: Iterable<T>) {
    pushIndent()
    elements.sorted().forEach { this.println(it) }
    popIndent()
}

