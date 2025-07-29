/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.wasm.test.klib.AbstractCustomWasmJsCompilerFirstPhaseTest

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    val jvmOnlyBoxTests = listOf("compileKotlinAgainstKotlin")

    generateTestGroupSuiteWithJUnit5(args) {
        testGroup("wasm/wasm.tests/klib-compatibility/tests-gen", "compiler/testData/codegen", testRunnerMethodName = "runTest") {
            testClass<AbstractCustomWasmJsCompilerFirstPhaseTest> {
                model("box", excludeDirs = jvmOnlyBoxTests)
                model("boxInline")
            }
        }
    }
}
