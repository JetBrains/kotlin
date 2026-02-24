/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test

import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase.NoTestRunnerExtras
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.ExecutableCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact.Executable
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.klib.compileToKlibsViaCli
import org.jetbrains.kotlin.konan.test.klib.newSourceModules
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class UsedLibrariesOnSecondStageTest : AbstractNativeSimpleTest() {
    @Test
    fun `Build program with stdlib (implicit) and platform libs (implicit)`() {
        compileProgram(
            klib = compileKlib(usePosix = false),
            implicitStdlib = true,
            implicitPlatformLibs = true
        )
    }

    private fun compileKlib(usePosix: Boolean): File {
        var klib: File? = null
        newSourceModules {
            addRegularModule("test") {
                sourceFileAddend(
                    buildString {
                        appendLine("fun main() = println(\"Hello, world!\")")
                        if (usePosix) {
                            appendLine("@kotlinx.cinterop.ExperimentalForeignApi fun callPosix() = platform.posix.fopen(\"test.txt\", \"r\")")
                        }
                    }
                )
            }
        }.compileToKlibsViaCli { _, successKlib ->
            klib = successKlib.resultingArtifact.klibFile
        }
        return checkNotNull(klib)
    }

    private fun compileProgram(klib: File, implicitStdlib: Boolean, implicitPlatformLibs: Boolean): File {
        val tempDir = Files.createTempDirectory("used-libs-test").toFile()
        val dumpDir = tempDir.resolve("dumps")

        val compilerArgs = TestCompilerArgs(
            listOfNotNull(
                "-Xinclude=${klib.absolutePath}",
                "-nostdlib".takeUnless { implicitStdlib },
                "-no-platform-libs".takeUnless { implicitPlatformLibs },
                "-Xwrite-dependencies-of-produced-binaries-to=${dumpDir.absolutePath}",
            )
        )

        val compilation = ExecutableCompilation(
            settings = testRunSettings,
            freeCompilerArgs = compilerArgs,
            sourceModules = emptyList(),
            extras = NoTestRunnerExtras(entryPoint = "test.main"),
            dependencies = emptyList(),
            expectedArtifact = Executable(tempDir.resolve("app.kexe")),
            tryPassSystemCacheDirectory = false,
        )

        compilation.result.assertSuccess()

        return dumpDir
    }
}
