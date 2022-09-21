package org.jetbrains.kotlin.native.interop.indexer

import java.io.File
import kotlin.contracts.ExperimentalContracts
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.nio.file.Files

@ExperimentalContracts
abstract class AbstractIndexerFromSourcesTest : KtUsefulTestCase() {
    companion object {
        init {
            System.setProperty("java.awt.headless", "true")
        }
    }

    private fun getTestDataDir(): File {
        val testCaseDir = lowercaseFirstLetter(
                this::class.java.simpleName.substringBefore("FromSources").substringBefore("Test"),
                true
        )
        val testDir = testDirectoryName.substringBefore("FModules")

        return File(KtTestUtil.getHomeDirectory())
                .resolve("kotlin-native/Interop/Indexer/src/testData")
                .resolve(testCaseDir)
                .resolve("defs")
                .resolve(testDir)
                .also(::assertIsDirectory)
    }

    private fun assertIsDirectory(file: File) {
        if (!file.isDirectory)
            kotlin.test.fail("Not a directory: $file")
    }

    protected fun doTestSuccessfulCInterop(fmodules: Boolean) {
        val testDataDir = getTestDataDir()
        val testCaseDir = File(testDataDir.parent).parent
        val programArgs = listOf("$testCaseDir${File.separator}cinterop_contents.sh",
                "-o", "klib.klib",
                "-def", "${testDataDir.canonicalPath}${File.separator}pod1.def",
                "-compiler-option", "-F$testCaseDir", "-compiler-option", "-I$testCaseDir/include"
        )
        val fmodulesArgs = if (fmodules) listOf("-compiler-option", "-fmodules") else listOf()
        val tempDirectory = Files.createTempDirectory("$testDirectoryName-")
        TestRunner(programArgs + fmodulesArgs, tempDirectory.toFile()).run()
    }
}
