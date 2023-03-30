/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.impl.generateTestGroupSuite
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.wasm.test.*
import org.jetbrains.kotlin.wasm.test.diagnostics.AbstractDiagnosticsWasmTest

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    val jvmOnlyBoxTests = listOf(
        "compileKotlinAgainstKotlin",
    )

    generateTestGroupSuite(args) {
        val jsTranslatorTestPattern = "^([^_](.+))\\.kt$"
        testGroup("wasm/wasm.tests/tests-gen", "js/js.translator/testData", testRunnerMethodName = "runTest0") {
            testClass<AbstractJsTranslatorWasmTest> {
                model("box/main", pattern = jsTranslatorTestPattern, targetBackend = TargetBackend.WASM)
                model("box/native/", pattern = jsTranslatorTestPattern, targetBackend = TargetBackend.WASM)
                model("box/esModules/", pattern = jsTranslatorTestPattern, targetBackend = TargetBackend.WASM,
                    excludeDirs = listOf(
                        // JsExport is not supported for classes
                        "jsExport", "native", "export",
                        // Multimodal infra is not supported. Also, we don't use ES modules for cross-module refs in Wasm
                        "crossModuleRef", "crossModuleRefPerFile", "crossModuleRefPerModule"
                    )
                )
                model("box/jsQualifier/", pattern = jsTranslatorTestPattern, targetBackend = TargetBackend.WASM)
                model("box/reflection/", pattern = "^(findAssociatedObject(InSeparatedFile)?)\\.kt$", targetBackend = TargetBackend.WASM)
            }

            testClass<AbstractJsTranslatorUnitWasmTest> {
                model("box/kotlin.test/", pattern = jsTranslatorTestPattern, targetBackend = TargetBackend.WASM)
            }
        }

        testGroup("wasm/wasm.tests/tests-gen", "compiler/testData", testRunnerMethodName = "runTest0") {
            testClass<AbstractIrCodegenBoxWasmTest> {
                model("codegen/box", targetBackend = TargetBackend.WASM, pattern = jsTranslatorTestPattern, excludeDirs = jvmOnlyBoxTests)
            }

            testClass<AbstractIrCodegenBoxInlineWasmTest> {
                model("codegen/boxInline", targetBackend = TargetBackend.WASM)
            }

            testClass<AbstractIrCodegenWasmJsInteropWasmTest> {
                model("codegen/boxWasmJsInterop", targetBackend = TargetBackend.WASM)
            }
        }
    }

    generateTestGroupSuiteWithJUnit5(args) {
        testGroup("wasm/wasm.tests/tests-gen", "compiler/testData") {
            testClass<AbstractDiagnosticsWasmTest> {
                model("diagnostics/wasmTests")
            }
        }
    }
}