/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.model.annotation
import org.jetbrains.kotlin.konan.test.blackbox.support.group.UseDummyTestCaseGroupProvider
import org.jetbrains.kotlin.konan.test.klib.AbstractCustomNativeCompilerFirstStageTest
import org.jetbrains.kotlin.konan.test.klib.AbstractCustomNativeCompilerSecondStageTest
import org.jetbrains.kotlin.test.HeavyTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.junit.jupiter.api.parallel.Resources

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")
    val testsRoot = args[0]

    val jvmOnlyBoxTests = listOf("compileKotlinAgainstKotlin")
    val k1BoxTestDir = "multiplatform/k1"
    // KT-68747: `box/fir/inferenceWithTypeAliasFromOtherModule.kt` takes infinite time to compile. Fixed in 2.0.20
    val CUSTOM_FIRST_STAGE_EXCLUSION_PATTERN = "^inferenceWithTypeAliasFromOtherModule.kt\$"

    generateTestGroupSuiteWithJUnit5(args) {
        testGroup(testsRoot, "compiler/testData/codegen", testRunnerMethodName = "runTest") {
            testClass<AbstractCustomNativeCompilerFirstStageTest>(
                annotations = listOf(
                    provider<UseDummyTestCaseGroupProvider>(),
                    annotation(HeavyTest::class.java),
                )
            ) {
                model("box", excludeDirs = jvmOnlyBoxTests + k1BoxTestDir, excludedPattern = CUSTOM_FIRST_STAGE_EXCLUSION_PATTERN)
                model("boxInline")
            }
            testClass<AbstractCustomNativeCompilerSecondStageTest>(
                annotations = listOf(
                    provider<UseDummyTestCaseGroupProvider>(),
                    annotation(HeavyTest::class.java),
                )
            ) {
                model("box", excludeDirs = jvmOnlyBoxTests + k1BoxTestDir)
                model("boxInline")
            }

            testClass<AbstractCustomNativeCompilerFirstStageTest>(
                suiteTestClassName = "CustomNativeAggregateFirstStageTestGenerated",
                annotations = listOf(
                    annotation(HeavyTest::class.java),
                    annotation(Tag::class.java, "aggregate-first-stage"),
                    provider<UseDummyTestCaseGroupProvider>(),
                )
            ) {
                model("boxInline")
            }
            testClass<AbstractCustomNativeCompilerSecondStageTest>(
                suiteTestClassName = "CustomNativeAggregateSecondStageTestGenerated",
                annotations = listOf(
                    annotation(HeavyTest::class.java),
                    annotation(Tag::class.java, "aggregate-second-stage"),
                    provider<UseDummyTestCaseGroupProvider>(),
                )
            ) {
                model("boxInline")
            }
        }

        // Native-specific codegen/box tests based on Compiler Core testinfra
        testGroup(testsRoot, "native/native.tests/testData/codegen") {
            testClass<AbstractCustomNativeCompilerFirstStageTest>(
                suiteTestClassName = "CustomNativeSpecificFirstStageTestGenerated",
                annotations = listOf(
                    annotation(HeavyTest::class.java),
                    provider<UseDummyTestCaseGroupProvider>(),
                )
            ) {
                model()
            }
            testClass<AbstractCustomNativeCompilerSecondStageTest>(
                suiteTestClassName = "CustomNativeSpecificSecondStageTestGenerated",
                annotations = listOf(
                    annotation(HeavyTest::class.java),
                    provider<UseDummyTestCaseGroupProvider>(),
                )
            ) {
                model(
                    // only on Linux/x86: ld.lld: error: undefined symbol: stat unsupported: function is defined in a header file
                    excludedPattern = "^(1|statbuf)\\.kt\$",
                )
            }
        }
    }
}

