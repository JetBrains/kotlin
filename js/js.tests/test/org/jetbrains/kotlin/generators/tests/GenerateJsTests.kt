/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.impl.generateTestGroupSuite
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.js.test.fir.*
import org.jetbrains.kotlin.js.test.ir.*
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.js.test.fir.AbstractFirLightTreeJsIrTextTest
import org.jetbrains.kotlin.js.test.ir.AbstractMultiModuleOrderTest
import org.jetbrains.kotlin.js.testOld.klib.AbstractClassicJsKlibEvolutionTest
import org.jetbrains.kotlin.js.testOld.klib.AbstractFirJsKlibEvolutionTest

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    val jvmOnlyBoxTests = listOf("compileKotlinAgainstKotlin")
    val k2BoxTestDir = "multiplatform/k2"
    val excludedFirTestdataPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX

    // TODO: repair these tests
    //generateTestDataForReservedWords()

    generateTestGroupSuite(args) {
        testGroup("js/js.tests/tests-gen", "compiler/testData/klib/evolution", testRunnerMethodName = "runTest0") {
            testClass<AbstractClassicJsKlibEvolutionTest> {
                model(targetBackend = TargetBackend.JS_IR)
            }
            testClass<AbstractFirJsKlibEvolutionTest> {
                model(targetBackend = TargetBackend.JS_IR)
            }
        }
    }

    generateTestGroupSuiteWithJUnit5(args) {
        testGroup("js/js.tests/tests-gen", "compiler/testData/klib/partial-linkage") {
            testClass<AbstractJsPartialLinkageWithICTestCase> {
                model(pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false)
            }
            testClass<AbstractJsPartialLinkageNoICTestCase> {
                model(pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false)
            }
            testClass<AbstractJsPartialLinkageNoICES6TestCase> {
                model(pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR_ES6, recursive = false)
            }
            testClass<AbstractFirJsPartialLinkageNoICTestCase> {
                model(pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false)
            }
        }

        testGroup("js/js.tests/tests-gen", "js/js.translator/testData/incremental") {
            testClass<AbstractJsIrInvalidationPerFileTest> {
                model("invalidation/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false)
            }

            testClass<AbstractJsIrInvalidationPerModuleTest> {
                model("invalidation/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false)
            }

            testClass<AbstractJsIrES6InvalidationPerFileTest> {
                model("invalidation/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR_ES6, recursive = false)
            }

            testClass<AbstractJsIrES6InvalidationPerModuleTest> {
                model("invalidation/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR_ES6, recursive = false)
            }

            testClass<AbstractJsFirInvalidationPerFileTest> {
                model("invalidation/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false)
            }

            testClass<AbstractJsFirInvalidationPerModuleTest> {
                model("invalidation/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false)
            }

            testClass<AbstractJsFirES6InvalidationPerFileTest> {
                model("invalidation/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR_ES6, recursive = false)
            }

            testClass<AbstractJsFirES6InvalidationPerModuleTest> {
                model("invalidation/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR_ES6, recursive = false)
            }

            testClass<AbstractJsIrInvalidationPerFileWithPLTest> {
                model("invalidationWithPL/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false)
            }

            testClass<AbstractJsIrInvalidationPerModuleWithPLTest> {
                model("invalidationWithPL/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false)
            }

            testClass<AbstractJsFirInvalidationPerFileWithPLTest> {
                model("invalidationWithPL/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false)
            }

            testClass<AbstractJsFirInvalidationPerModuleWithPLTest> {
                model("invalidationWithPL/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false)
            }
        }

        testGroup("js/js.tests/tests-gen", "js/js.translator/testData/sourcemap", testRunnerMethodName = "runTest0") {
            testClass<AbstractSourceMapGenerationSmokeTest> {
                model()
            }
            testClass<AbstractFirSourceMapGenerationSmokeTest> {
                model()
            }
        }

        testGroup("js/js.tests/tests-gen", "js/js.translator/testData/multiModuleOrder/", testRunnerMethodName = "runTest0") {
            testClass<AbstractMultiModuleOrderTest> {
                model()
            }
            testClass<AbstractFirMultiModuleOrderTest> {
                model()
            }
        }

        testGroup("js/js.tests/tests-gen", "js/js.translator/testData/box", testRunnerMethodName = "runTest0") {
            testClass<AbstractIrBoxJsTest> {
                model(pattern = "^([^_](.+))\\.kt$", excludeDirs = listOf("es6classes"))
            }

            testClass<AbstractIrBoxJsES6Test> {
                model(pattern = "^([^_](.+))\\.kt$")
            }

            testClass<AbstractFirPsiJsBoxTest> {
                model(pattern = "^([^_](.+))\\.kt$", excludeDirs = listOf("es6classes"))
            }

            testClass<AbstractFirLightTreeJsBoxTest> {
                model(pattern = "^([^_](.+))\\.kt$", excludeDirs = listOf("es6classes"))
            }

            testClass<AbstractFirJsES6BoxTest> {
                model(pattern = "^([^_](.+))\\.kt$")
            }
        }

        testGroup("js/js.tests/tests-gen", "js/js.translator/testData/typescript-export/js", testRunnerMethodName = "runTest0") {
            testClass<AbstractIrJsTypeScriptExportTest> {
                model(pattern = "^([^_](.+))\\.kt$")
            }

            testClass<AbstractIrJsES6TypeScriptExportTest> {
                model(pattern = "^([^_](.+))\\.kt$")
            }

            testClass<AbstractFirJsTypeScriptExportTest> {
                model(pattern = "^([^_](.+))\\.kt$")
            }

            testClass<AbstractFirJsES6TypeScriptExportTest> {
                model(pattern = "^([^_](.+))\\.kt$")
            }
        }

        testGroup("js/js.tests/tests-gen", "js/js.translator/testData/webDemoExamples", testRunnerMethodName = "runTest0") {
            testClass<AbstractWebDemoExamplesTest> {
                model()
            }
        }

        testGroup("js/js.tests/tests-gen", "js/js.translator/testData/lineNumbers", testRunnerMethodName = "runTest0") {
            testClass<AbstractJsIrLineNumberTest> {
                model()
            }

            testClass<AbstractFirJsLineNumberTest> {
                model()
            }
        }

        testGroup("js/js.tests/tests-gen", "compiler/testData/codegen", testRunnerMethodName = "runTest0") {
            testClass<AbstractIrJsCodegenBoxTest> {
                model("box", excludeDirs = jvmOnlyBoxTests + k2BoxTestDir)
            }

            testClass<AbstractIrJsES6CodegenBoxTest> {
                model("box", excludeDirs = jvmOnlyBoxTests + k2BoxTestDir)
            }

            testClass<AbstractFirJsCodegenBoxTest> {
                model("box", excludeDirs = jvmOnlyBoxTests)
            }

            testClass<AbstractFirJsES6CodegenBoxTest> {
                model("box", excludeDirs = jvmOnlyBoxTests)
            }

            testClass<AbstractIrJsCodegenInlineTest> {
                model("boxInline")
            }

            testClass<AbstractIrJsES6CodegenInlineTest> {
                model("boxInline")
            }

            testClass<AbstractFirJsCodegenInlineTest> {
                model("boxInline")
            }

            testClass<AbstractFirJsES6CodegenInlineTest> {
                model("boxInline")
            }

            testClass<AbstractIrCodegenWasmJsInteropJsTest> {
                model("boxWasmJsInterop")
            }

            testClass<AbstractFirJsCodegenWasmJsInteropTest> {
                model("boxWasmJsInterop")
            }

            testClass<AbstractFirJsES6CodegenWasmJsInteropTest> {
                model("boxWasmJsInterop")
            }
        }

        testGroup("js/js.tests/tests-gen", "compiler/testData/debug", testRunnerMethodName = "runTest0") {
            testClass<AbstractIrJsSteppingTest> {
                model("stepping")
            }

            testClass<AbstractFirJsSteppingTest> {
                model("stepping")
            }

            testClass<AbstractIrJsLocalVariableTest> {
                // The tests in the 'inlineScopes' directory are meant to test a JVM backend
                // specific feature, so there is no reason to enable them for JS.
                model("localVariables", excludeDirs = listOf("inlineScopes"))
            }
        }

        testGroup("js/js.tests/tests-gen", "compiler/testData/diagnostics", testRunnerMethodName = "runTest0") {
            testClass<AbstractFirPsiJsDiagnosticWithBackendTest>(suiteTestClassName = "FirPsiJsKlibDiagnosticsTestGenerated") {
                model(
                    relativeRootPath = "klibSerializationTests",
                    pattern = "^([^_](.+))\\.kt$",
                    excludedPattern = excludedFirTestdataPattern,
                    targetBackend = TargetBackend.JS_IR
                )
            }

            testClass<AbstractFirPsiJsDiagnosticTest>(suiteTestClassName = "FirPsiJsOldFrontendDiagnosticsTestGenerated") {
                model(
                    relativeRootPath = "testsWithJsStdLib",
                    pattern = "^([^_](.+))\\.kt$",
                    excludedPattern = excludedFirTestdataPattern,
                    targetBackend = TargetBackend.JS_IR
                )
            }

            testClass<AbstractFirLightTreeJsDiagnosticTest>(suiteTestClassName = "FirLightTreeJsOldFrontendDiagnosticsTestGenerated") {
                model(
                    relativeRootPath = "testsWithJsStdLib",
                    pattern = "^([^_](.+))\\.kt$",
                    excludedPattern = excludedFirTestdataPattern,
                    targetBackend = TargetBackend.JS_IR
                )
            }

            testClass<AbstractFirPsiJsDiagnosticWithBackendTest>(suiteTestClassName = "FirPsiJsOldFrontendDiagnosticsWithBackendTestGenerated") {
                model(
                    relativeRootPath = "testsWithJsStdLibAndBackendCompilation",
                    pattern = "^([^_](.+))\\.kt$",
                    excludedPattern = excludedFirTestdataPattern,
                    targetBackend = TargetBackend.JS_IR
                )
            }

            testClass<AbstractFirLightTreeJsDiagnosticWithBackendTest>(suiteTestClassName = "FirLightTreeJsOldFrontendDiagnosticsWithBackendTestGenerated") {
                model(
                    relativeRootPath = "testsWithJsStdLibAndBackendCompilation",
                    pattern = "^([^_](.+))\\.kt$",
                    excludedPattern = excludedFirTestdataPattern,
                    targetBackend = TargetBackend.JS_IR
                )
            }

            testClass<AbstractDiagnosticsTestWithJsStdLib>(suiteTestClassName = "DiagnosticsWithJsStdLibTestGenerated") {
                model(
                    relativeRootPath = "testsWithJsStdLib",
                    pattern = "^([^_](.+))\\.kt$",
                    excludedPattern = excludedFirTestdataPattern,
                    targetBackend = TargetBackend.JS_IR
                )
            }

            testClass<AbstractDiagnosticsTestWithJsStdLibWithBackend>(suiteTestClassName = "DiagnosticsWithJsStdLibAndBackendTestGenerated") {
                model(
                    relativeRootPath = "testsWithJsStdLibAndBackendCompilation",
                    pattern = "^([^_](.+))\\.kt$",
                    excludedPattern = excludedFirTestdataPattern,
                    targetBackend = TargetBackend.JS_IR
                )
            }
        }

        testGroup("js/js.tests/tests-gen", "compiler/testData/ir/irText", testRunnerMethodName = "runTest0") {
            testClass<AbstractClassicJsIrTextTest> {
                model(
                    excludeDirs = listOf("declarations/multiplatform/k2")
                )
            }

            testClass<AbstractFirLightTreeJsIrTextTest> {
                model(
                    excludeDirs = listOf("declarations/multiplatform/k1")
                )
            }

            testClass<AbstractFirPsiJsIrTextTest> {
                model(
                    excludeDirs = listOf("declarations/multiplatform/k1")
                )
            }
        }

        testGroup("js/js.tests/tests-gen", "compiler/testData/loadJava", testRunnerMethodName = "runTest0") {
            testClass<AbstractFirLoadK2CompiledJsKotlinTest> {
                model("compiledKotlin", extension = "kt")
                model("compiledKotlinWithStdlib", extension = "kt")
            }
        }
    }
}
