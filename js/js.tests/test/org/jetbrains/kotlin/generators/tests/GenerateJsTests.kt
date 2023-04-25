/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.impl.generateTestGroupSuite
import org.jetbrains.kotlin.incremental.AbstractJsIrES6InvalidationTest
import org.jetbrains.kotlin.incremental.AbstractJsIrInvalidationTest
import org.jetbrains.kotlin.incremental.AbstractJsFirInvalidationTest
import org.jetbrains.kotlin.js.test.*
import org.jetbrains.kotlin.js.test.fir.*
import org.jetbrains.kotlin.js.test.ir.*
import org.jetbrains.kotlin.js.testOld.AbstractDceTest
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.js.test.fir.AbstractFirLightTreeJsIrTextTest
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

        testGroup("js/js.tests/tests-gen", "compiler/testData") {
            testClass<AbstractJsPartialLinkageWithICTestCase> {
                model("klibABI/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false)
            }
        }
        testGroup("js/js.tests/tests-gen", "compiler/testData") {
            testClass<AbstractJsPartialLinkageNoICTestCase> {
                model("klibABI/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false)
            }
        }
        testGroup("js/js.tests/tests-gen", "compiler/testData") {
            testClass<AbstractFirJsPartialLinkageNoICTestCase> {
                model("klibABI/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false)
            }
        }

        testGroup("js/js.tests/tests-gen", "compiler/testData/binaryCompatibility", testRunnerMethodName = "runTest0") {
            testClass<AbstractClassicJsKlibEvolutionTest> {
                model("klibEvolution", targetBackend = TargetBackend.JS_IR)
            }
            testClass<AbstractFirJsKlibEvolutionTest> {
                model("klibEvolution", targetBackend = TargetBackend.JS_IR)
            }
        }
    }

    generateTestGroupSuiteWithJUnit5(args) {
        testGroup("js/js.tests/tests-gen", "js/js.translator/testData") {
            testClass<AbstractJsIrInvalidationTest> {
                model("incremental/invalidation/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false)
            }

            testClass<AbstractJsIrES6InvalidationTest> {
                model("incremental/invalidation/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR_ES6, recursive = false)
            }

            testClass<AbstractJsFirInvalidationTest> {
                model("incremental/invalidation/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false)
            }
        }

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

            testClass<AbstractFirPsiJsDiagnosticTest>(suiteTestClassName = "FirPsiJsOldFrontendDiagnosticsTestGenerated") {
                model("diagnostics/testsWithJsStdLib", pattern = "^([^_](.+))\\.kt$", excludedPattern = excludedFirTestdataPattern)
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

            // see todo on AbstractFirJsSteppingTest
//            testClass<AbstractFirJsSteppingTest> {
//                model("debug/stepping")
//            }
        }
    }
}
