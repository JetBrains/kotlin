/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.cli.AbstractCliTest
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.konan.blackboxtest.support.*
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.LibraryCompilation
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationResult
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.jupiter.api.Test
import java.io.File

@TestDataPath("\$PROJECT_ROOT")
@EnforcedProperty(ClassLevelProperty.COMPILER_OUTPUT_INTERCEPTOR, "NONE")
class CompilerOutputTest : AbstractNativeSimpleTest() {
    @Test
    fun testReleaseCompilerAgainstPreReleaseLibrary() {
        // We intentionally use JS testdata, because the compilers should behave the same way in such a test.
        // To be refactored later, after CompileKotlinAgainstCustomBinariesTest.testReleaseCompilerAgainstPreReleaseLibraryJs is fixed.
        val rootDir = File("compiler/testData/compileKotlinAgainstCustomBinaries/releaseCompilerAgainstPreReleaseLibraryJs")

        doTestPreReleaseKotlinLibrary(rootDir, emptyList())
    }

    @Test
    fun testReleaseCompilerAgainstPreReleaseLibrarySkipPrereleaseCheck() {
        // We intentionally use JS testdata, because the compilers should behave the same way in such a test.
        // To be refactored later, after
        // CompileKotlinAgainstCustomBinariesTest.testReleaseCompilerAgainstPreReleaseLibraryJsSkipPrereleaseCheck is fixed.
        val rootDir =
            File("compiler/testData/compileKotlinAgainstCustomBinaries/releaseCompilerAgainstPreReleaseLibraryJsSkipPrereleaseCheck")

        doTestPreReleaseKotlinLibrary(rootDir, listOf("-Xskip-prerelease-check"))
    }

    private fun doTestPreReleaseKotlinLibrary(rootDir: File, additionalOptions: List<String>) {
        val someNonStableVersion = LanguageVersion.values().firstOrNull { it > LanguageVersion.LATEST_STABLE } ?: return

        val libraryOptions = listOf(
            "-language-version", someNonStableVersion.versionString,
            // Suppress the "language version X is experimental..." warning.
            "-Xsuppress-version-warnings"
        )
        val library = compileLibrary(
            source = rootDir.resolve("library"),
            freeCompilerArgs = libraryOptions,
            dependencies = emptyList()
        ).assertSuccess().resultingArtifact

        val compilationResult = compileLibrary(
            source = rootDir.resolve("source.kt"),
            freeCompilerArgs = additionalOptions + listOf("-language-version", LanguageVersion.LATEST_STABLE.versionString),
            dependencies = listOf(library)
        )

        KotlinTestUtils.assertEqualsToFile(rootDir.resolve("output.txt"), compilationResult.toOutput())
    }

    private fun compileLibrary(
        source: File,
        freeCompilerArgs: List<String>,
        dependencies: List<TestCompilationArtifact.KLIB>
    ): TestCompilationResult<out TestCompilationArtifact.KLIB> {
        val testCase = generateTestCaseWithSingleModule(source, TestCompilerArgs(freeCompilerArgs))
        val compilation = LibraryCompilation(
            settings = testRunSettings,
            freeCompilerArgs = testCase.freeCompilerArgs,
            sourceModules = testCase.modules,
            dependencies = dependencies.map { it.asLibraryDependency() },
            expectedArtifact = getLibraryArtifact(testCase, buildDir)
        )
        return compilation.result
    }

    private fun TestCompilationResult<*>.toOutput(): String {
        check(this is TestCompilationResult.ImmediateResult<*>) { this }
        val loggedData = this.loggedData
        check(loggedData is LoggedData.CompilationToolCall) { loggedData::class }
        return normalizeOutput(loggedData.toolOutput, loggedData.exitCode)
    }

    private fun normalizeOutput(output: String, exitCode: ExitCode): String {
        return AbstractCliTest.getNormalizedCompilerOutput(
            output,
            exitCode,
            "compiler/testData/compileKotlinAgainstCustomBinaries/"
        )
    }
}
