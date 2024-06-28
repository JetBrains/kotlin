/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeClassLoader
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.PipelineType
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
import org.jetbrains.kotlin.konan.test.blackbox.support.util.dumpIr
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEqualsToFile
import org.junit.jupiter.api.Tag
import java.io.File

@Tag("klib")
abstract class AbstractNativeKlibDumpIrTest : AbstractNativeSimpleTest() {
    protected fun runTest(@TestDataFile testPath: String) {
        val testPathFull = getAbsoluteFile(testPath)
        muteTestIfNecessary(testPathFull)

        val testCase: TestCase = generateTestCaseWithSingleSource(
            testPathFull,
            listOf("-Xklib-relative-path-base=${testPathFull.parent}")
        )
        val testCompilationResult: TestCompilationResult.Success<out TestCompilationArtifact.KLIB> = compileToLibrary(testCase)

        val testPathNoExtension = testPathFull.canonicalPath.substringBeforeLast(".")

        val firSpecificExt =
            if (testRunSettings.get<PipelineType>() == PipelineType.K2 && !firIdentical(testPathFull))
                ".fir"
            else ""

        val expectedContentsNoSig = File("$testPathNoExtension.ir$firSpecificExt.txt")
        assertIrMatchesExpected(testCompilationResult, expectedContentsNoSig, printSignatures = false)

        val expectedContentsWithSig = File("$testPathNoExtension.sig.ir$firSpecificExt.txt")
        assertIrMatchesExpected(testCompilationResult, expectedContentsWithSig, printSignatures = true)
    }

    private fun assertIrMatchesExpected(
        compilationResult: TestCompilationResult<out TestCompilationArtifact.KLIB>,
        expectedContents: File,
        printSignatures: Boolean
    ) {
        val artifact = compilationResult.assertSuccess().resultingArtifact
        val kotlinNativeClassLoader = testRunSettings.get<KotlinNativeClassLoader>()
        val klibIr = artifact.dumpIr(kotlinNativeClassLoader.classLoader, printSignatures, null) // TODO: test for all signature versions, KT-62828
        assertEqualsToFile(expectedContents, klibIr)
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
}
