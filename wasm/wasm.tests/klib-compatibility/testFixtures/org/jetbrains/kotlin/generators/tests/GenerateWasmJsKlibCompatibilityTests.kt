/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.model.AnnotationModel
import org.jetbrains.kotlin.generators.model.annotation
import org.jetbrains.kotlin.wasm.test.klib.AbstractCustomWasmJsCompilerFirstStageTest
import org.jetbrains.kotlin.test.HeavyTest
import org.jetbrains.kotlin.wasm.test.klib.AbstractCustomWasmJsCompilerSecondStageTest
import org.junit.jupiter.api.Tag

fun main(args: Array<String>) {
    val testsRoot = args[0]
    System.setProperty("java.awt.headless", "true")

    val jvmOnlyBoxTests = listOf("compileKotlinAgainstKotlin")
    val k1BoxTestDir = "multiplatform/k1"

    generateTestGroupSuiteWithJUnit5(args) {
        testGroup(testsRoot, "compiler/testData/codegen", testRunnerMethodName = "runTest") {
            testClass<AbstractCustomWasmJsCompilerFirstStageTest>(
                annotations = listOf(annotation(HeavyTest::class.java))
            ) {
                model("box", excludeDirs = jvmOnlyBoxTests + k1BoxTestDir)
                model("boxInline")
            }

            testClass<AbstractCustomWasmJsCompilerFirstStageTest>(
                suiteTestClassName = "CustomWasmJsAggregateFirstStageTestGenerated",
                annotations = listOf(
                    annotation(HeavyTest::class.java),
                    aggregate(),
                )
            ) {
                model("boxInline")
            }

            testClass<AbstractCustomWasmJsCompilerSecondStageTest>(
                annotations = listOf(annotation(HeavyTest::class.java))
            ) {
                model("box", excludeDirs = jvmOnlyBoxTests + k1BoxTestDir)
                model("boxInline")
            }

            testClass<AbstractCustomWasmJsCompilerSecondStageTest>(
                suiteTestClassName = "CustomWasmJsAggregateSecondStageTestGenerated",
                annotations = listOf(
                    annotation(HeavyTest::class.java),
                    aggregate(),
                )
            ) {
                model("boxInline")
            }
        }
    }
}

private fun aggregate(): AnnotationModel = annotation(Tag::class.java, "aggregate")
