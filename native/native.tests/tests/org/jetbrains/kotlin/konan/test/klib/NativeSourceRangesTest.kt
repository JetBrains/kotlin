/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.generateTestCaseWithSingleModule
import org.jetbrains.kotlin.konan.test.blackbox.getLibraryArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.LibraryCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult
import org.jetbrains.kotlin.konan.test.blackbox.support.group.FirPipeline
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Binaries
import org.jetbrains.kotlin.konan.test.blackbox.targets
import org.jetbrains.kotlin.test.KtAssert.fail
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

@FirPipeline()
class NativeSourceRangesTest : AbstractNativeSimpleTest() {
    private val BASE_DIR = File("native/native.tests/testData/klib/kt72356")
    private val buildDir get() = testRunSettings.get<Binaries>().testBinariesDir

    private fun compileLibrary(folder: File): TestCompilationResult<out TestCompilationArtifact.KLIB> {
        val args = buildList {
            add("-target")
            add(targets.testTarget.visibleName)
            add("-produce")
            add("library")
        }
        val testCase = generateTestCaseWithSingleModule(folder, TestCompilerArgs(args))
        val compilation = LibraryCompilation(
            settings = testRunSettings,
            freeCompilerArgs = testCase.freeCompilerArgs,
            sourceModules = testCase.modules,
            dependencies = emptyList(),
            expectedArtifact = getLibraryArtifact(testCase, buildDir, packed=false)
        )
        return compilation.result
    }

    @Test
    fun testKT72356PassWhenDifferentSourceRange() {
        val result = compileLibrary(BASE_DIR.resolve("differentSourceRange"))
        Assertions.assertTrue(result is TestCompilationResult.Success)
    }

    // Reproducer for KT-72356
    @Test
    fun testKT72356FailWhenSameSourceRange() {
        val result = compileLibrary(BASE_DIR.resolve("sameSourceRange"))
        // TODO: KT-72356: Fix the issue with `FirExpression.toConstantValue()`, correct testcase name, change asserts below
        if (result is TestCompilationResult.CompilationToolFailure)
            Assertions.assertTrue(result.loggedData.toolOutput.contains("error: compilation failed: java.lang.IllegalStateException: Cannot serialize annotation @R|Something|()"))
        else
            fail("Reproduced compilation failure for KT-72356 was expected")
    }
}