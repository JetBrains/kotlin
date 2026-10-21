/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test

import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.compileToExecutableInOneStage
import org.jetbrains.kotlin.konan.test.blackbox.generateTestCaseWithSingleFile
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class OneStageCompilation : AbstractNativeSimpleTest() {
    @Test
    @DisplayName("Test one-stage compilation (KT-80298)")
    fun testOneStageCompilation() {
        val srcDir = ForTestCompileRuntime.transformTestDataPath("native/native.tests/testData/oneStageCompilation")
        val testCase = generateTestCaseWithSingleFile(
            sourceFile = srcDir.resolve("main.kt"),
        )
        compileToExecutableInOneStage(testCase).assertSuccess()
    }
}
