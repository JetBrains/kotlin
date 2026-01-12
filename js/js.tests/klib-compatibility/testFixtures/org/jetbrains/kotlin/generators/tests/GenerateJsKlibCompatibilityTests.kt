/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.model.annotation
import org.jetbrains.kotlin.js.test.klib.AbstractCustomJsCompilerFirstStageTest
import org.jetbrains.kotlin.js.test.klib.AbstractCustomJsCompilerSecondStageTest
import org.jetbrains.kotlin.test.HeavyTest

fun main(args: Array<String>) {
    val testsRoot = args[0]
    System.setProperty("java.awt.headless", "true")

    val jvmOnlyBoxTests = listOf("compileKotlinAgainstKotlin")
    val k1BoxTestDir = "multiplatform/k1"
    // KT-68538: `box/inference/pcla/nestedNonExhaustiveIf.kt` times out with first stage version 2.0.0, and it's not convenient to add a timeout to test runner,
    //           so this test is simply excluded from klib compatibility testing. Fixed in 2.0.20
    // KT-68747: `box/fir/inferenceWithTypeAliasFromOtherModule.kt` takes infinite time to compile. Fixed in 2.0.20
    val CUSTOM_FIRST_STAGE_EXCLUSION_PATTERN = "^(nestedNonExhaustiveIf|inferenceWithTypeAliasFromOtherModule).kt\$"

    generateTestGroupSuiteWithJUnit5(args) {
        testGroup(testsRoot, "compiler/testData/codegen", testRunnerMethodName = "runTest") {
            testClass<AbstractCustomJsCompilerFirstStageTest>(
                annotations = listOf(annotation(HeavyTest::class.java))
            ) {
                model("box", excludeDirs = jvmOnlyBoxTests + k1BoxTestDir, excludedPattern = CUSTOM_FIRST_STAGE_EXCLUSION_PATTERN)
                model("boxInline")
            }
        }
        testGroup(testsRoot, "compiler/testData/codegen", testRunnerMethodName = "runTest") {
            testClass<AbstractCustomJsCompilerSecondStageTest>(
                annotations = listOf(annotation(HeavyTest::class.java))
            ) {
                model("box", excludeDirs = jvmOnlyBoxTests + k1BoxTestDir)
                model("boxInline")
            }
        }
    }
}
