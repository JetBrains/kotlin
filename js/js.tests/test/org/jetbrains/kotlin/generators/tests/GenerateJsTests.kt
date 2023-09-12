/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.impl.generateTestGroupSuite
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.js.test.*
import org.jetbrains.kotlin.js.test.fir.*
import org.jetbrains.kotlin.js.test.ir.*
import org.jetbrains.kotlin.js.testOld.AbstractDceTest
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.js.test.fir.AbstractFirLightTreeJsIrTextTest
import org.jetbrains.kotlin.js.test.ir.AbstractMultiModuleOrderTest
import org.jetbrains.kotlin.js.testOld.klib.AbstractClassicJsKlibEvolutionTest
import org.jetbrains.kotlin.js.testOld.klib.AbstractFirJsKlibEvolutionTest

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
            testClass<AbstractDceTest> {
                model("dce/", pattern = "(.+)\\.js", targetBackend = TargetBackend.JS)
            }
        }

        testGroup("js/js.tests/tests-gen", "compiler/testData", testRunnerMethodName = "runTest0") {
            testClass<AbstractClassicJsKlibEvolutionTest> {
                model("klib/evolution", targetBackend = TargetBackend.JS_IR)
            }
            testClass<AbstractFirJsKlibEvolutionTest> {
                model("klib/evolution", targetBackend = TargetBackend.JS_IR)
            }
        }
    }

    generateTestGroupSuiteWithJUnit5(args) {
        testGroup("js/js.tests/tests-gen", "compiler/testData") {
            testClass<AbstractJsPartialLinkageWithICTestCase> {
                model("klib/partial-linkage/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false)
            }
        }
        testGroup("js/js.tests/tests-gen", "compiler/testData") {
            testClass<AbstractJsPartialLinkageNoICTestCase> {
                model("klib/partial-linkage/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false)
            }
        }
        testGroup("js/js.tests/tests-gen", "compiler/testData") {
            testClass<AbstractJsPartialLinkageNoICES6TestCase> {
                model("klib/partial-linkage/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR_ES6, recursive = false)
            }
        }
        testGroup("js/js.tests/tests-gen", "compiler/testData") {
            testClass<AbstractFirJsPartialLinkageNoICTestCase> {
                model("klib/partial-linkage/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false)
            }
        }

        testGroup("js/js.tests/tests-gen", "js/js.translator/testData") {
            testClass<AbstractJsIrInvalidationPerFileTest> {
                model("incremental/invalidation/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false)
            }

            testClass<AbstractJsIrInvalidationPerModuleTest> {
                model("incremental/invalidation/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false)
            }

            testClass<AbstractJsIrES6InvalidationPerFileTest> {
                model("incremental/invalidation/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR_ES6, recursive = false)
            }

            testClass<AbstractJsIrES6InvalidationPerModuleTest> {
                model("incremental/invalidation/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR_ES6, recursive = false)
            }

            testClass<AbstractJsFirInvalidationPerFileTest> {
                model("incremental/invalidation/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false)
            }

            testClass<AbstractJsFirInvalidationPerModuleTest> {
                model("incremental/invalidation/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false)
            }

            testClass<AbstractJsIrInvalidationPerFileWithPLTest> {
                model("incremental/invalidationWithPL/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false)
            }

            testClass<AbstractJsIrInvalidationPerModuleWithPLTest> {
                model("incremental/invalidationWithPL/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false)
            }
        }

        testGroup("js/js.tests/tests-gen", "js/js.translator/testData", testRunnerMethodName = "runTest0") {

            testClass<AbstractSourceMapGenerationSmokeTest> {
                model("sourcemap/")
            }

            testClass<AbstractMultiModuleOrderTest> {
                model("multiModuleOrder/")
            }

            testClass<AbstractWebDemoExamplesTest> {
                model("webDemoExamples/")
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

            testClass<AbstractFirJsES6BoxTest> {
                model("box/", pattern = "^([^_](.+))\\.kt$")
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

            testClass<AbstractFirPsiJsDiagnosticTest>(suiteTestClassName = "FirPsiJsOldFrontendDiagnosticsTestGenerated") {
                model(
                    relativeRootPath = "diagnostics/testsWithJsStdLib",
                    pattern = "^([^_](.+))\\.kt$",
                    excludedPattern = excludedFirTestdataPattern,
                    targetBackend = TargetBackend.JS_IR
                )
            }

            testClass<AbstractFirPsiJsDiagnosticWithBackendTest>(suiteTestClassName = "FirPsiJsOldFrontendDiagnosticsWithBackendTestGenerated") {
                model(
                    relativeRootPath = "diagnostics/testsWithJsStdLibAndBackendCompilation",
                    pattern = "^([^_](.+))\\.kt$",
                    excludedPattern = excludedFirTestdataPattern,
                    targetBackend = TargetBackend.JS_IR
                )
            }

            testClass<AbstractDiagnosticsTestWithJsStdLib>(suiteTestClassName = "DiagnosticsWithJsStdLibTestGenerated") {
                model(
                    relativeRootPath = "diagnostics/testsWithJsStdLib",
                    pattern = "^([^_](.+))\\.kt$",
                    excludedPattern = excludedFirTestdataPattern,
                    targetBackend = TargetBackend.JS_IR
                )
            }

            testClass<AbstractDiagnosticsTestWithJsStdLibWithBackend>(suiteTestClassName = "DiagnosticsWithJsStdLibAndBackendTestGenerated") {
                model(
                    relativeRootPath = "diagnostics/testsWithJsStdLibAndBackendCompilation",
                    pattern = "^([^_](.+))\\.kt$",
                    excludedPattern = excludedFirTestdataPattern,
                    targetBackend = TargetBackend.JS_IR
                )
            }

            testClass<AbstractClassicJsIrTextTest> {
                model("ir/irText")
            }

            testClass<AbstractFirLightTreeJsIrTextTest> {
                model("ir/irText")
            }

            testClass<AbstractFirPsiJsIrTextTest> {
                model("ir/irText")
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

            testClass<AbstractFirJsES6CodegenBoxTest> {
                model("codegen/box", excludeDirs = jvmOnlyBoxTests)
            }

            testClass<AbstractFirJsES6CodegenBoxErrorTest> {
                model("codegen/boxError", excludeDirs = jvmOnlyBoxTests)
            }

            testClass<AbstractFirJsES6CodegenInlineTest> {
                model("codegen/boxInline")
            }

            testClass<AbstractFirJsES6CodegenWasmJsInteropTest> {
                model("codegen/boxWasmJsInterop")
            }

            // see todo on AbstractFirJsSteppingTest
//            testClass<AbstractFirJsSteppingTest> {
//                model("debug/stepping")
//            }

            testClass<AbstractFirLoadK2CompiledJsKotlinTest> {
                model("loadJava/compiledKotlin", extension = "kt")
                model("loadJava/compiledKotlinWithStdlib", extension = "kt")
            }
        }
    }
}
