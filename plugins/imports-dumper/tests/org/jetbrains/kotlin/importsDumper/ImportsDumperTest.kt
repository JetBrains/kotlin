/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.importsDumper

import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.TestDataPath
import junit.framework.TestCase
import org.jetbrains.kotlin.cli.AbstractCliTest.executeCompilerGrabOutput
import org.jetbrains.kotlin.cli.AbstractCliTest.getNormalizedCompilerOutput
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import org.jetbrains.kotlin.test.TestMetadata
import java.io.File

@TestMetadata("plugins/imports-dumper/testData")
@TestDataPath("\$PROJECT_ROOT")
class ImportsDumperTest : TestCaseWithTmpdir() {

    fun testSimple() {
        doTest("plugins/imports-dumper/testData/simpleCase")
    }

    private fun doTest(testDataDirPath: String) {
        System.setProperty("java.awt.headless", "true")
        val testDataDir = File(testDataDirPath)
        val expectedDumpFile = testDataDir.resolve(testDataDir.name + ".dump")
        val expectedOutputFile = testDataDir.resolve(testDataDir.name + ".out")
        val actualDumpFile = tmpdir.resolve(testDataDir.name + ".dump")

        // Check CLI-output of compiler
        val actualOutput = invokeImportsDumperAndGrabOutput(testDataDir, tmpdir, actualDumpFile)
        KotlinTestUtils.assertEqualsToFile(expectedOutputFile, actualOutput)

        // Check imports dump
        // Note that imports dumper outputs absolute paths to files, which is inconvenient for tests,
        // so we have to relativize them
        val actualRelativizedDump = FileUtil.loadFile(actualDumpFile, Charsets.UTF_8.name(), /* convertLineSeparators = */ true)
            .relativizeAbsolutePaths(testDataDir)

        KotlinTestUtils.assertEqualsToFile(expectedDumpFile, actualRelativizedDump)
    }

    private fun invokeImportsDumperAndGrabOutput(testDataDir: File, tmpDir: File, actualDumpFile: File): String {
        val importsDumperJarInDist = File("dist/kotlinc/lib/kotlin-imports-dumper-compiler-plugin.jar")
        if (!importsDumperJarInDist.exists()) {
            TestCase.fail(".jar for imports dumper isn't found, searched: ${importsDumperJarInDist.absolutePath}")
        }

        val compiler = K2JVMCompiler()
        val (output, exitCode) = executeCompilerGrabOutput(
            compiler,
            listOf(
                testDataDir.absolutePath,
                "-d",
                tmpDir.path,
                "-language-version",
                "1.9",
                "-Xplugin=${importsDumperJarInDist.path}",
                "-P",
                "plugin:${ImportsDumperCommandLineProcessor.PLUGIN_ID}:" +
                        "${ImportsDumperCliOptions.DESTINATION.optionName}=${actualDumpFile.path}"
            )
        )

        return getNormalizedCompilerOutput(output, exitCode, testDataDir.path, tmpDir.absolutePath)
    }
}

private fun String.relativizeAbsolutePaths(relativeTo: File): String {
    // JSON escapes slashes
    val pattern = relativeTo.absoluteFile.toString().replace(File.separatorChar.toString(), "/")
    return this.replace(pattern, "\$TESTDATA_DIR$")
}
