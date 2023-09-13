/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.group.UsePartialLinkage
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
import org.junit.jupiter.api.Tag
import java.io.File
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

@Tag("klib")
@UsePartialLinkage(UsePartialLinkage.Mode.ENABLED_WITH_ERROR)
abstract class AbstractNativeHeaderKlibComparisonTest : AbstractNativeSimpleTest() {

    protected fun runTest(@TestDataFile testPath: String) {
        val testPathFull = getAbsoluteFile(testPath)

        val testCaseBase: TestCase = generateTestcaseFromDirectory(testPathFull, "base", listOf())
        compileToLibrary(testCaseBase).assertSuccess()
        val headerKlibBase = File(getHeaderPath("base"))
        assert(headerKlibBase.exists())

        val sameAbiDir = testPathFull.resolve("sameAbi")
        val differentAbiDir = testPathFull.resolve("differentAbi")

        assert(sameAbiDir.exists() || differentAbiDir.exists()) { "Nothing to compare" }

        if (sameAbiDir.exists()) {
            val testCaseSameAbi: TestCase = generateTestcaseFromDirectory(testPathFull, "sameAbi", listOf())
            compileToLibrary(testCaseSameAbi).assertSuccess()
            val headerKlibSameAbi = File(getHeaderPath("sameAbi"))
            assert(headerKlibSameAbi.exists())
            assertContentEquals(headerKlibBase.readBytes(), headerKlibSameAbi.readBytes())
        }

        if (differentAbiDir.exists()) {
            val testCaseDifferentAbi: TestCase = generateTestcaseFromDirectory(testPathFull, "differentAbi", listOf())
            compileToLibrary(testCaseDifferentAbi).assertSuccess()
            val headerKlibDifferentAbi = File(getHeaderPath("differentAbi"))
            assert(headerKlibDifferentAbi.exists())
            assertFailsWith<AssertionError>("base and differentAbi header klib are equal") {
                assertContentEquals(headerKlibBase.readBytes(), headerKlibDifferentAbi.readBytes())
            }
        }
    }

}

@Tag("klib")
@UsePartialLinkage(UsePartialLinkage.Mode.ENABLED_WITH_ERROR)
abstract class AbstractNativeHeaderKlibCompilationTest : AbstractNativeSimpleTest() {

    protected fun runTest(@TestDataFile testPath: String) {
        val testPathFull = getAbsoluteFile(testPath)
        assert(testPathFull.exists())
        val testCaseLib: TestCase = generateTestcaseFromDirectory(testPathFull, "lib", listOf())
        val klibLib = compileToLibrary(testCaseLib)
        val headerKlibLib = File(getHeaderPath("lib"))
        assert(headerKlibLib.exists())
        val testPathApp = testPathFull.resolve("main")
        val klibAppFromHeader = compileToLibrary(testPathApp, TestCompilationArtifact.KLIB(headerKlibLib))
        val klibAppFromFull = compileToLibrary(testPathApp, klibLib.resultingArtifact)
        assertContentEquals(
            klibAppFromHeader.klibFile.readBytes(),
            klibAppFromFull.klibFile.readBytes()
        )
    }
}

private fun AbstractNativeSimpleTest.getHeaderPath(rev: String) = buildDir.absolutePath + "/header.$rev.klib"
private fun AbstractNativeSimpleTest.generateTestcaseFromDirectory(source: File, rev: String, extraArgs: List<String>): TestCase {
    val moduleName: String = source.name
    val module = TestModule.Exclusive(moduleName, emptySet(), emptySet(), emptySet())
    source.resolve(rev).listFiles()?.forEach {
        muteTestIfNecessary(it)
        module.files += TestFile.createCommitted(it, module)
    }

    val headerKlibPath = "-Xheader-klib-path=" + getHeaderPath(rev)
    val relativeBasePath = "-Xklib-relative-path-base=$source/$rev"

    return TestCase(
        id = TestCaseId.Named(moduleName),
        kind = TestKind.STANDALONE,
        modules = setOf(module),
        freeCompilerArgs = TestCompilerArgs(extraArgs + relativeBasePath + headerKlibPath),
        nominalPackageName = PackageName.EMPTY,
        checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout),
        extras = TestCase.WithTestRunnerExtras(TestRunnerType.DEFAULT)
    ).apply {
        initialize(null, null)
    }
}
