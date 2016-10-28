package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.incremental.dumpBuildLog
import org.jetbrains.kotlin.gradle.incremental.parseTestBuildLog
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

        val actualNormalized = dumpBuildLog(parseTestBuildLog(logFile)).replace("\r\n", "\n").trim()
        val expectedFile = File(testDir, EXPECTED_PARSED_LOG_FILE_NAME)

        if (!expectedFile.isFile) {
            expectedFile.createNewFile()
            expectedFile.writeText(actualNormalized)

            throw AssertionError("Expected file log did not exist, created: $expectedFile")
        }

        val expectedNormalized = expectedFile.readText().replace("\r\n", "\n").trim()
        Assert.assertEquals("Parsed content was unexpected: ", expectedNormalized, actualNormalized)

        // parse expected, dump again and compare (to check that dumped log can be parsed again)
        val reparsedActualNormalized = dumpBuildLog(parseTestBuildLog(expectedFile)).trim()
        Assert.assertEquals("Reparsed content was unexpected: ", expectedNormalized, reparsedActualNormalized)
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