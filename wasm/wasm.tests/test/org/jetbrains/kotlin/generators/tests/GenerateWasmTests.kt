/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.wasm.test.*
import org.jetbrains.kotlin.wasm.test.diagnostics.AbstractDiagnosticsWasmTest

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    // Common configuration shared between K1 and K2 tests:
    val jvmOnlyBoxTests = listOf(
        "compileKotlinAgainstKotlin",
    )
    val jsTranslatorTestPattern = "^([^_](.+))\\.kt$"
    val jsTranslatorReflectionPattern = "^(findAssociatedObject(InSeparatedFile)?)\\.kt$"
    val jsTranslatorEsModulesExcludedDirs = listOf(
        // JsExport is not supported for classes
        "jsExport", "native", "export",
        // Multimodal infra is not supported. Also, we don't use ES modules for cross-module refs in Wasm
        "crossModuleRef", "crossModuleRefPerFile", "crossModuleRefPerModule"
    )

    generateTestGroupSuiteWithJUnit5(args) {
        testGroup("wasm/wasm.tests/tests-gen", "compiler/testData") {
            testClass<AbstractDiagnosticsWasmTest> {
                model("diagnostics/wasmTests")
            }
        }

        testGroup("wasm/wasm.tests/tests-gen", "js/js.translator/testData", testRunnerMethodName = "runTest0") {
            testClass<AbstractFirWasmJsTranslatorTest> {
                model("box/main", pattern = jsTranslatorTestPattern)
                model("box/native/", pattern = jsTranslatorTestPattern)
                model("box/esModules/", pattern = jsTranslatorTestPattern, excludeDirs = jsTranslatorEsModulesExcludedDirs)
                model("box/jsQualifier/", pattern = jsTranslatorTestPattern)
                model("box/reflection/", pattern = jsTranslatorReflectionPattern)
                model("box/kotlin.test/", pattern = jsTranslatorTestPattern)
            }
        }

        testGroup("wasm/wasm.tests/tests-gen", "compiler/testData", testRunnerMethodName = "runTest0") {
            testClass<AbstractFirWasmCodegenBoxTest> {
                model("codegen/box", pattern = jsTranslatorTestPattern, excludeDirs = jvmOnlyBoxTests)
            }

            testClass<AbstractFirWasmCodegenBoxInlineTest> {
                model("codegen/boxInline")
            }

            testClass<AbstractFirWasmCodegenWasmJsInteropTest> {
                model("codegen/boxWasmJsInterop")
            }

            testClass<AbstractFirWasmSteppingTest> {
                model("debug/stepping")
            }
        }

        testGroup("wasm/wasm.tests/tests-gen", "js/js.translator/testData", testRunnerMethodName = "runTest0") {
            testClass<AbstractK1WasmJsTranslatorTest> {
                model("box/main", pattern = jsTranslatorTestPattern)
                model("box/native/", pattern = jsTranslatorTestPattern)
                model("box/esModules/", pattern = jsTranslatorTestPattern, excludeDirs = jsTranslatorEsModulesExcludedDirs)
                model("box/jsQualifier/", pattern = jsTranslatorTestPattern)
                model("box/reflection/", pattern = jsTranslatorReflectionPattern)
                model("box/kotlin.test/", pattern = jsTranslatorTestPattern)
            }
        }

        testGroup("wasm/wasm.tests/tests-gen", "compiler/testData", testRunnerMethodName = "runTest0") {
            testClass<AbstractK1WasmCodegenBoxTest> {
                model("codegen/box", pattern = jsTranslatorTestPattern, excludeDirs = jvmOnlyBoxTests)
            }

            testClass<AbstractK1WasmCodegenBoxInlineTest> {
                model("codegen/boxInline")
            }

            testClass<AbstractK1WasmCodegenWasmJsInteropTest> {
                model("codegen/boxWasmJsInterop")
            }

            testClass<AbstractK1WasmWasiCodegenBoxTest> {
                model("codegen/boxWasmWasi")
            }

            testClass<AbstractK1WasmSteppingTest> {
                model("debug/stepping")
            }
        }
    }
}