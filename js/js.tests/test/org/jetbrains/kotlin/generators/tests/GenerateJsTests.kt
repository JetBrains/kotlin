/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.impl.generateTestGroupSuite
import org.jetbrains.kotlin.incremental.AbstractInvalidationTest
import org.jetbrains.kotlin.incremental.AbstractJsIrES6InvalidationTest
import org.jetbrains.kotlin.incremental.AbstractJsIrInvalidationTest
import org.jetbrains.kotlin.js.test.*
import org.jetbrains.kotlin.js.test.fir.*
import org.jetbrains.kotlin.js.test.ir.*
import org.jetbrains.kotlin.js.testOld.AbstractDceTest
import org.jetbrains.kotlin.js.testOld.compatibility.binary.AbstractJsKlibBinaryCompatibilityTest
import org.jetbrains.kotlin.js.testOld.wasm.semantics.*
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.runners.ir.AbstractFir2IrJsTextTest

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    val jvmOnlyBoxTests = listOf(
        "compileKotlinAgainstKotlin",
    )

    val excludedFirTestdataPattern = "^(.+)\\.fir\\.kts?\$"

    // TODO: repair these tests
    //generateTestDataForReservedWords()

    generateTestGroupSuite(args) {
        testGroup("js/js.tests/tests-gen", "js/js.translator/testData", testRunnerMethodName = "runTest0") {
            testClass<AbstractJsTranslatorWasmTest> {
                model("box/main", pattern = "^([^_](.+))\\.kt$", targetBackend = TargetBackend.WASM)
                model("box/native/", pattern = "^([^_](.+))\\.kt$", targetBackend = TargetBackend.WASM)
                model("box/esModules/", pattern = "^([^_](.+))\\.kt$", targetBackend = TargetBackend.WASM,
                    excludeDirs = listOf(
                        // JsExport is not supported for classes
                        "jsExport", "native", "export",
                        // Multimodal infra is not supported. Also, we don't use ES modules for cross-module refs in Wasm
                        "crossModuleRef", "crossModuleRefPerFile", "crossModuleRefPerModule"
                    )
                )
                model("box/jsQualifier/", pattern = "^([^_](.+))\\.kt$", targetBackend = TargetBackend.WASM)
            }

            testClass<AbstractJsTranslatorUnitWasmTest> {
                model("box/kotlin.test/", pattern = "^([^_](.+))\\.kt$", targetBackend = TargetBackend.WASM)
            }

            testClass<AbstractDceTest> {
                model("dce/", pattern = "(.+)\\.js", targetBackend = TargetBackend.JS)
            }
        }

        testGroup("js/js.tests/tests-gen", "compiler/testData") {
            testClass<AbstractJsKLibABIWithICTestCase> {
                model("klibABI/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false, )
            }
        }
        testGroup("js/js.tests/tests-gen", "compiler/testData") {
            testClass<AbstractJsKLibABINoICTestCase> {
                model("klibABI/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false, )
            }
        }

        testGroup("js/js.tests/tests-gen", "js/js.translator/testData") {
            testClass<AbstractJsIrInvalidationTest> {
                model("incremental/invalidation/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false)
            }

            testClass<AbstractJsIrES6InvalidationTest> {
                model("incremental/invalidation/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR_ES6, recursive = false)
            }
        }

        testGroup("js/js.tests/tests-gen", "compiler/testData", testRunnerMethodName = "runTest0") {
            testClass<AbstractIrCodegenBoxWasmTest> {
                model(
                    "codegen/box", pattern = "^([^_](.+))\\.kt$", targetBackend = TargetBackend.WASM, excludeDirs = jvmOnlyBoxTests
                )
            }

            testClass<AbstractIrCodegenBoxInlineWasmTest> {
                model("codegen/boxInline", targetBackend = TargetBackend.WASM)
            }

            testClass<AbstractIrCodegenWasmJsInteropWasmTest> {
                model("codegen/boxWasmJsInterop", targetBackend = TargetBackend.WASM)
            }
        }

        testGroup("js/js.tests/tests-gen", "compiler/testData/binaryCompatibility", testRunnerMethodName = "runTest0") {
            testClass<AbstractJsKlibBinaryCompatibilityTest> {
                model("klibEvolution", targetBackend = TargetBackend.JS_IR)
            }
        }
    }

    generateTestGroupSuiteWithJUnit5(args) {
        testGroup("js/js.tests/tests-gen", "js/js.translator/testData", testRunnerMethodName = "runTest0") {
            testClass<AbstractBoxJsTest> {
                model("box/", pattern = "^([^_](.+))\\.kt$", excludeDirs = listOf("closure/inlineAnonymousFunctions", "es6classes"))
            }

            testClass<AbstractSourceMapGenerationSmokeTest> {
                model("sourcemap/")
            }

            testClass<AbstractOutputPrefixPostfixTest> {
                model("outputPrefixPostfix/")
            }

            testClass<AbstractMultiModuleOrderTest> {
                model("multiModuleOrder/")
            }

            testClass<AbstractWebDemoExamplesTest> {
                model("webDemoExamples/")
            }

            testClass<AbstractJsLineNumberTest> {
                model("lineNumbers/")
            }

            testClass<AbstractIrBoxJsTest> {
                model("box/", pattern = "^([^_](.+))\\.kt$", excludeDirs = listOf("es6classes"))
            }

            testClass<AbstractIrBoxJsES6Test> {
                model("box/", pattern = "^([^_](.+))\\.kt$")
            }

            testClass<AbstractIrJsTypeScriptExportTest> {
                model("typescript-export/", pattern = "^([^_](.+))\\.kt$")
            }

            testClass<AbstractJsIrLineNumberTest> {
                model("lineNumbers/")
            }

            testClass<AbstractFirJsBoxTest> {
                model("box/", pattern = "^([^_](.+))\\.kt$", excludeDirs = listOf("es6classes"))
            }

            // see todo on defining class
//            testClass<AbstractFirJsTypeScriptExportTest> {
//                model("typescript-export/", pattern = "^([^_](.+))\\.kt$")
//            }

            // see todo on defining class
//            testClass<AbstractJsFirLineNumberTest> {
//                model("lineNumbers/")
//            }
        }

        testGroup("js/js.tests/tests-gen", "compiler/testData", testRunnerMethodName = "runTest0") {
            testClass<AbstractJsCodegenBoxTest> {
                model("codegen/box", excludeDirs = jvmOnlyBoxTests)
            }

            testClass<AbstractJsCodegenInlineTest> {
                model("codegen/boxInline")
            }

            testClass<AbstractJsLegacyPrimitiveArraysBoxTest> {
                model("codegen/box/arrays")
            }

            testClass<AbstractIrJsCodegenBoxTest> {
                model("codegen/box", excludeDirs = jvmOnlyBoxTests)
            }

            testClass<AbstractIrJsCodegenBoxErrorTest> {
                model("codegen/boxError", excludeDirs = jvmOnlyBoxTests)
            }

            testClass<AbstractIrJsCodegenInlineTest> {
                model("codegen/boxInline")
            }

            testClass<AbstractIrJsES6CodegenBoxTest> {
                model("codegen/box", excludeDirs = jvmOnlyBoxTests)
            }

            testClass<AbstractIrJsES6CodegenBoxErrorTest> {
                model("codegen/boxError", excludeDirs = jvmOnlyBoxTests)
            }

            testClass<AbstractIrJsES6CodegenInlineTest> {
                model("codegen/boxInline")
            }

            testClass<AbstractIrCodegenWasmJsInteropJsTest> {
                model("codegen/boxWasmJsInterop")
            }

            testClass<AbstractIrJsSteppingTest> {
                model("debug/stepping")
            }

            testClass<AbstractIrJsLocalVariableTest> {
                model("debug/localVariables")
            }

            testClass<AbstractFirJsDiagnosticTest>(suiteTestClassName = "FirJsOldFrontendDiagnosticsTestGenerated") {
                model("diagnostics/testsWithJsStdLib", pattern = "^([^_](.+))\\.kt$", excludedPattern = excludedFirTestdataPattern)
            }

            testClass<AbstractFir2IrJsTextTest>(
                suiteTestClassName = "Fir2IrJsTextTestGenerated"
            ) {
                model("ir/irJsText")
            }

            testClass<AbstractFirJsCodegenBoxTest> {
                model("codegen/box", excludeDirs = jvmOnlyBoxTests)
            }

            testClass<AbstractFirJsCodegenBoxErrorTest> {
                model("codegen/boxError", excludeDirs = jvmOnlyBoxTests)
            }

            testClass<AbstractFirJsCodegenInlineTest> {
                model("codegen/boxInline")
            }

            testClass<AbstractFirJsCodegenWasmJsInteropTest> {
                model("codegen/boxWasmJsInterop")
            }

            // see todo on AbstractFirJsSteppingTest
//            testClass<AbstractFirJsSteppingTest> {
//                model("debug/stepping")
//            }
        }
    }
}
