/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact.KLIB
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.group.UsePartialLinkage
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeClassLoader
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getContents
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEqualsToFile
import org.junit.jupiter.api.Tag
import java.io.File

@Tag("klib")
@UsePartialLinkage(UsePartialLinkage.Mode.DISABLED)
abstract class AbstractNativeKlibDumpMetadataTest : AbstractNativeSimpleTest() {

    protected fun runTest(@TestDataFile testPath: String) {
        val testPathFull = getAbsoluteFile(testPath)
        muteTestIfNecessary(testPathFull)

        val testCase: TestCase = generateTestCaseWithSingleSource(testPathFull, listOf())
        val testCompilationResult: TestCompilationResult.Success<out KLIB> = compileToLibrary(testCase)

        val kotlinNativeClassLoader = testRunSettings.get<KotlinNativeClassLoader>()
        val klibContents = testCompilationResult.assertSuccess().resultingArtifact.getContents(kotlinNativeClassLoader.classLoader)
        val klibContentsFiltered = filterContentsOutput(klibContents, linestoExclude = listOf("package test {", "}", ""))
        assertEqualsToFile(File("${testPathFull.canonicalPath.substringBeforeLast(".")}.txt"), StringUtilRt.convertLineSeparators(klibContentsFiltered))
    }

    private fun generateTestCaseWithSingleSource(source: File, extraArgs: List<String>): TestCase {
        val moduleName: String = source.name
        val module = TestModule.Exclusive(moduleName, emptySet(), emptySet(), emptySet())
        module.files += TestFile.createCommitted(source, module)

        return TestCase(
            id = TestCaseId.Named(moduleName),
            kind = TestKind.STANDALONE,
            modules = setOf(module),
            freeCompilerArgs = TestCompilerArgs(extraArgs),
            nominalPackageName = PackageName.EMPTY,
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout),
            extras = TestCase.WithTestRunnerExtras(TestRunnerType.DEFAULT)
        ).apply {
            initialize(null, null)
        }
    }

    private fun filterContentsOutput(contents: String, linestoExclude: List<String>) =
        contents.lines()
            .filterNot { line ->
                linestoExclude.any { exclude -> exclude == line }
            }.joinToString(separator = "\n")
}
