/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives
import org.jetbrains.kotlin.konan.test.blackbox.support.TestKind
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Compile .def and .kt files in most straightforward way to check the structure of packages created by `cinterop` tool into interop klib.
// Same test sources are also checked with new native test infra using its package renaming feature.

// This demonstrates the behavior of cinterop tool:
//   how the packages are arranged in klib in different combinations of compound filenames and package directives.
// Cinterop tool here is executed directly, not under new test system, which might modify sources before compilation.
// The most important test is dotFnameRoot.kt, where the order of package parts is counterintuitive:
//   filename is `root1.root2.def`, and the resulting package is `root2.root1`
// This test explains the necessity of flipping package parts in new test system, happening in `val KtFile.packageFqNameForKLib`.
// Without this test it would be unclear, why such odd flipping code is written there.
// In case code in packageFqNameForKLib would be simpler -> the package directive in tests might be rewritten to pass the test.
// However, these tests would not work without renaming, for ex, when making the test standalone.
// CInteropPackagesTest makes sure the package directives are written in the way to work without renaming.

// It could be another less obvious approach: to run same tests under new test system 1) with renaming and 2) without,
// while CInteropPackagesTest is simple and straightforward, and also allows to validate new test system's package rename approach.

class CInteropPackagesTest : AbstractNativeSimpleTest() {
    @Test
    fun testAllTestSourcesInCinteropPackages() {
        val testSources = File(TEST_DATA_DIR).listFiles()
        assertTrue(testSources.isNotEmpty())

        testSources.forEach { testSource ->
            val lines = testSource.readLines()
            val testCaseBuildDir = buildDir.resolve(testSource.name)
            testCaseBuildDir.mkdirs()
            var output_file: File? = null
            val generatedFilesByExtension = mutableMapOf<String, File>()
            lines.forEach { line ->
                val prefix = "// ${TestDirectives.FILE}: "
                if (line.startsWith(prefix)) {
                    output_file = testCaseBuildDir.resolve(line.removePrefix(prefix))
                    val extension = line.substringAfterLast(".")
                    assertNull(
                        generatedFilesByExtension[extension],
                        "Test source ${testSource.absolutePath} must contain only one directive `// ${TestDirectives.FILE}: <SOMEFILE.$extension>`"
                    )
                    generatedFilesByExtension[extension] = output_file!!
                } else {
                    output_file?.appendText("$line\n")
                }
            }
            val defFile = generatedFilesByExtension["def"]
            assertNotNull(
                defFile,
                "Test source ${testSource.absolutePath} must contain directive `// ${TestDirectives.FILE}: <SOMEFILE.def>`"
            )
            val ktFile = generatedFilesByExtension["kt"]
            assertNotNull(
                ktFile,
                "Test source ${testSource.absolutePath} must contain directive `// ${TestDirectives.FILE}: <SOMEFILE.kt>`"
            )
            ktFile.appendText("fun main() { box() }")

            val library = cinteropToLibrary(
                targets = targets,
                defFile = defFile,
                outputDir = buildDir,
                freeCompilerArgs = TestCompilerArgs.EMPTY
            ).assertSuccess().resultingArtifact

            compileToExecutable(
                generateTestCaseWithSingleFile(
                    sourceFile = ktFile,
                    testKind = TestKind.STANDALONE_NO_TR,
                    extras = TestCase.NoTestRunnerExtras("main")
                ),
                library.asLibraryDependency()
            ).assertSuccess()
        }
    }

    companion object {
        private const val TEST_DATA_DIR = "native/native.tests/testData/codegen/cinterop/packages"
    }
}
