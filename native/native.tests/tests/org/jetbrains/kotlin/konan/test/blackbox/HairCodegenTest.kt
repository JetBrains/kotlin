/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("JUnitTestCaseWithNoTests")

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.konan.test.blackbox.support.ClassLevelProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.TestKind
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestExecutable
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Test

/**
 * Blackbox tests for the HaIR backend (`IrToHair`), enabled via `-Xbinary=enableHair=true`.
 *
 * Each test exercises one operation that `IrToHair` currently supports. HaIR is incomplete and
 * silently falls back to the regular backend for functions it cannot translate, so these tests only
 * w
 * assert that the produced program behaves correctly — they do not yet assert which path was taken.
 *
 * Test sources live in `native/native.tests/testData/hair/`.
 */
@EnforcedProperty(ClassLevelProperty.COMPILER_OUTPUT_INTERCEPTOR, "NONE")
@EnforcedProperty(ClassLevelProperty.OPTIMIZATION_MODE, "NO")
@TestMetadata("native/native.tests/testData/hair")
@TestDataPath("\$PROJECT_ROOT")
class HairCodegenTest : AbstractNativeSimpleTest() {
    private val testDataDir = ForTestCompileRuntime.transformTestDataPath("native/native.tests/testData/hair")

    private fun runHairTest(fileName: String) {
        val testCase = generateTestCaseWithSingleFile(
            sourceFile = testDataDir.resolve(fileName),
            testKind = TestKind.STANDALONE_NO_TR,
            freeCompilerArgs = TestCompilerArgs("-Xbinary=enableHair=true"),
            extras = TestCase.NoTestRunnerExtras("main"),
        )
        val compilationResult = compileToExecutableInOneStage(testCase).assertSuccess()
        runExecutableAndVerify(testCase, TestExecutable.fromCompilationResult(testCase, compilationResult))
    }

    // Constants
    @Test fun intConstant() = runHairTest("constants/intConstant.kt")
    @Test fun longConstant() = runHairTest("constants/longConstant.kt")
    @Test fun floatConstant() = runHairTest("constants/floatConstant.kt")
    @Test fun doubleConstant() = runHairTest("constants/doubleConstant.kt")
    @Test fun booleanConstant() = runHairTest("constants/booleanConstant.kt")
    @Test fun charConstant() = runHairTest("constants/charConstant.kt")
    @Test fun nullConstant() = runHairTest("constants/nullConstant.kt")

    // Arithmetic
    @Test fun addition() = runHairTest("arithmetic/addition.kt")
    @Test fun subtraction() = runHairTest("arithmetic/subtraction.kt")
    @Test fun multiplication() = runHairTest("arithmetic/multiplication.kt")
    @Test fun bitwiseAnd() = runHairTest("arithmetic/bitwiseAnd.kt")
    @Test fun bitwiseOr() = runHairTest("arithmetic/bitwiseOr.kt")
    @Test fun bitwiseXor() = runHairTest("arithmetic/bitwiseXor.kt")
    @Test fun incrementDecrement() = runHairTest("arithmetic/incrementDecrement.kt")

    // Comparisons
    @Test fun integerComparisons() = runHairTest("comparisons/integerComparisons.kt")
    @Test fun charComparison() = runHairTest("comparisons/charComparison.kt")
    @Test fun referenceEquality() = runHairTest("comparisons/referenceEquality.kt")
    @Test fun booleanNot() = runHairTest("comparisons/booleanNot.kt")

    // Conversions
    @Test fun integerConversions() = runHairTest("conversions/integerConversions.kt")

    // Control flow
    @Test fun ifElse() = runHairTest("controlflow/ifElse.kt")
    @Test fun whenMultipleBranches() = runHairTest("controlflow/whenMultipleBranches.kt")
    @Test fun whileLoop() = runHairTest("controlflow/whileLoop.kt")
    @Test fun doWhileLoop() = runHairTest("controlflow/doWhileLoop.kt")
    @Test fun breakStatement() = runHairTest("controlflow/breakStatement.kt")
    @Test fun continueStatement() = runHairTest("controlflow/continueStatement.kt")
    @Test fun earlyReturn() = runHairTest("controlflow/earlyReturn.kt")

    // Variables
    @Test fun localVariables() = runHairTest("variables/localVariables.kt")
    @Test fun topLevelGlobal() = runHairTest("variables/topLevelGlobal.kt")

    // Types
    @Test fun isInstance() = runHairTest("types/isInstance.kt")
    @Test fun notIsInstance() = runHairTest("types/notIsInstance.kt")
    @Test fun asCast() = runHairTest("types/asCast.kt")

    // Functions
    @Test fun staticCall() = runHairTest("functions/staticCall.kt")

    // Objects
    @Test fun instanceFields() = runHairTest("objects/instanceFields.kt")
}
