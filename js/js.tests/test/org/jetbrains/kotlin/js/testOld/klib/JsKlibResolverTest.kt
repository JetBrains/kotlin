/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testOld.klib

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.js.K2JsIrCompiler
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import org.jetbrains.kotlin.test.services.StandardLibrariesPathProviderForKotlinProject
import org.jetbrains.kotlin.test.util.JUnit4Assertions
import org.jetbrains.kotlin.test.utils.assertCompilerOutputHasKlibResolverIncompatibleAbiMessages
import org.jetbrains.kotlin.test.utils.patchManifestToBumpAbiVersion
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

class JsKlibResolverTest : TestCaseWithTmpdir() {
    fun testWarningAboutRejectedLibraryIsNotSuppressed() {
        val testDataDir = File("compiler/testData/klib/resolve/mismatched-abi-version")

        val lib1V1 = createKlibDir("lib1", 1)

        compileKlib(
            sourceFile = testDataDir.resolve("lib1.kt"),
            dependency = null,
            outputFile = lib1V1
        ).assertSuccess() // Should compile successfully.

        compileKlib(
            sourceFile = testDataDir.resolve("lib2.kt"),
            dependency = lib1V1,
            outputFile = createKlibDir("lib2", 1)
        ).assertSuccess() // Should compile successfully.

        // Now patch lib1:
        val lib1V2 = createKlibDir("lib1", 2)
        lib1V1.copyRecursively(lib1V2)
        patchManifestToBumpAbiVersion(JUnit4Assertions, lib1V2)

        val result = compileKlib(
            sourceFile = testDataDir.resolve("lib2.kt"),
            dependency = lib1V2,
            outputFile = createKlibDir("lib2", 2)
        )

        result.assertFailure() // Should not compile successfully.

        assertCompilerOutputHasKlibResolverIncompatibleAbiMessages(JUnit4Assertions, result.output, missingLibrary = "/v2/lib1", tmpdir)
    }

    private fun createKlibDir(name: String, version: Int): File =
        tmpdir.resolve("v$version").resolve(name).apply(File::mkdirs)

    private fun compileKlib(sourceFile: File, dependency: File?, outputFile: File): CompilationResult {
        val libraries = listOfNotNull(
            StandardLibrariesPathProviderForKotlinProject.fullJsStdlib(),
            dependency
        ).joinToString(File.pathSeparator) { it.absolutePath }

        val args = arrayOf(
            "-Xir-produce-klib-dir",
            "-libraries", libraries,
            "-ir-output-dir", outputFile.absolutePath,
            "-ir-output-name", outputFile.nameWithoutExtension,
            sourceFile.absolutePath
        )

        val compilerXmlOutput = ByteArrayOutputStream()
        val exitCode = PrintStream(compilerXmlOutput).use { printStream ->
            K2JsIrCompiler().execFullPathsInMessages(printStream, args)
        }

        return CompilationResult(exitCode, compilerXmlOutput.toString())
    }

    private data class CompilationResult(val exitCode: ExitCode, val output: String) {
        fun assertSuccess() = JUnit4Assertions.assertTrue(exitCode == ExitCode.OK) {
            buildString {
                appendLine("Expected exit code: ${ExitCode.OK}, Actual: $exitCode")
                appendLine("Compiler output:")
                appendLine(output)
            }
        }

        fun assertFailure() = JUnit4Assertions.assertTrue(exitCode != ExitCode.OK) {
            buildString {
                appendLine("Expected exit code: any but ${ExitCode.OK}, Actual: $exitCode")
                appendLine("Compiler output:")
                appendLine(output)
            }
        }
    }
}
