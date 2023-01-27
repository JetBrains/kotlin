/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest

import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.konan.blackboxtest.support.*
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.*
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationArtifact.*
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.*
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.*
import org.jetbrains.kotlin.konan.blackboxtest.support.util.*
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEquals
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Tag
import java.io.File

@Tag("klib-contents")
abstract class AbstractNativeKlibContentsTest : AbstractNativeSimpleTest() {

    protected fun runTest(@TestDataFile testPath: String) {
        val testPathFull = getAbsoluteFile(testPath)
        muteTestIfNecessary(testPathFull)

        val testCase: TestCase = generateTestCaseWithSingleSource(testPathFull, listOf())
        val testCompilationResult: TestCompilationResult.Success<out KLIB> = compileToLibrary(testCase)

        val kotlinNativeClassLoader = testRunSettings.get<KotlinNativeClassLoader>()
        val klibContents = testCompilationResult.assertSuccess().resultingArtifact.getContents(kotlinNativeClassLoader.classLoader)
        val klibContentsFiltered = filterContentsOutput(klibContents, linestoExclude = listOf("package test {", "}", ""))
        val expectedContents = File("${testPathFull.canonicalPath.substringBeforeLast(".")}.txt").readText()
        assertEquals(StringUtilRt.convertLineSeparators(expectedContents), StringUtilRt.convertLineSeparators(klibContentsFiltered)) {
            "Test failed. Compilation result was: $testCompilationResult"
        }
    }

    private fun generateTestCaseWithSingleSource(source: File, extraArgs: List<String>): TestCase {
        val moduleName: String = source.name
        val module = TestModule.Exclusive(moduleName, emptySet(), emptySet())
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
        contents.split("\n").filterNot { line ->
            linestoExclude.any { exclude -> exclude == line }
        }.joinToString(separator = "\n")
}
