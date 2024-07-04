/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.incremental.AbstractFirWasmInvalidationPerFileTest
import org.jetbrains.kotlin.incremental.AbstractFirWasmInvalidationPerFileWithPLTest
import org.jetbrains.kotlin.wasm.test.AbstractWasmPartialLinkageWithICTestCase
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.wasm.test.*
import org.jetbrains.kotlin.wasm.test.diagnostics.AbstractDiagnosticsWasmTest
import org.jetbrains.kotlin.wasm.test.diagnostics.AbstractDiagnosticsFirWasmTest
import org.jetbrains.kotlin.wasm.test.diagnostics.AbstractDiagnosticsFirWasmWasiTest
import org.jetbrains.kotlin.wasm.test.diagnostics.AbstractDiagnosticsWasmWasiTest

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    // Common configuration shared between K1 and K2 tests:
    val jvmOnlyBoxTests = listOf("compileKotlinAgainstKotlin")
    val k2BoxTestDir = "multiplatform/k2"

    val jsTranslatorTestPattern = "^([^_](.+))\\.kt$"
    val jsTranslatorReflectionPattern = "^(findAssociatedObject(InSeparatedFile)?(Lazyness)?)\\.kt$"
    val jsTranslatorEsModulesExcludedDirs = listOf(
        // JsExport is not supported for classes
        "jsExport", "native", "export",
        // Multimodal infra is not supported. Also, we don't use ES modules for cross-module refs in Wasm
        "crossModuleRef", "crossModuleRefPerFile", "crossModuleRefPerModule"
    )

    generateTestGroupSuiteWithJUnit5(args) {
        testGroup("wasm/wasm.tests/tests-gen", "compiler/testData") {
            testClass<AbstractWasmPartialLinkageNoICTestCase> {
                model("klib/partial-linkage/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.WASM, recursive = false)
            }
        }

        testGroup("wasm/wasm.tests/tests-gen", "compiler/testData") {
            testClass<AbstractWasmPartialLinkageWithICTestCase> {
                model("klib/partial-linkage/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.WASM, recursive = false)
            }
        }

        testGroup("wasm/wasm.tests/tests-gen", "compiler/testData") {
            testClass<AbstractFirWasmPartialLinkageNoICTestCase> {
                model("klib/partial-linkage/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.WASM, recursive = false)
            }
        }

        testGroup("wasm/wasm.tests/tests-gen", "js/js.translator/testData") {
            testClass<AbstractFirWasmInvalidationPerFileTest> {
                val jsTargetedInvalidationTests = listOf(
                    "abstractClassWithJsExport",
                    "classWithJsExport",
                    "inlineFunctionAnnotations",
                    "interfaceWithJsExport",
                    "jsExportWithMultipleFiles",
                    "typeScriptExportsPerFile",
                    "typeScriptExportsPerModule",
                    "fileNameClash",
                    "jsCode",
                    "jsCodeWithConstString",
                    "jsModuleAnnotation",
                    "jsModuleAnnotationOnObjectWithUsage",
                    "jsName",
                    "fileNameCaseClash",
                    "jsCodeWithConstStringFromOtherModule",
                    "moveExternalDeclarationsBetweenFiles",
                    "inlineFunctionCircleUsage",
                    "jsExportReexport"
                )
                model(
                    "incremental/invalidation/",
                    pattern = "^([^_](.+))$",
                    targetBackend = TargetBackend.WASM,
                    recursive = false,
                    excludedPattern = jsTargetedInvalidationTests.joinToString("|")
                )
            }
            testClass<AbstractFirWasmInvalidationPerFileWithPLTest> {
                model(
                    "incremental/invalidationWithPL/",
                    pattern = "^([^_](.+))$",
                    targetBackend = TargetBackend.WASM,
                    recursive = false,
                )
            }
        }
    }

    generateTestGroupSuiteWithJUnit5(args) {
        testGroup("wasm/wasm.tests/tests-gen", "compiler/testData") {
            testClass<AbstractDiagnosticsWasmTest> {
                model("diagnostics/wasmTests", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
            }

            testClass<AbstractDiagnosticsFirWasmTest> {
                model("diagnostics/wasmTests", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
            }

            testClass<AbstractDiagnosticsWasmWasiTest> {
                model("diagnostics/wasmWasiTests", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
            }

            testClass<AbstractDiagnosticsFirWasmWasiTest> {
                model("diagnostics/wasmWasiTests", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
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
            testClass<AbstractFirWasmJsCodegenBoxTest> {
                model("codegen/box", pattern = jsTranslatorTestPattern, excludeDirs = jvmOnlyBoxTests)
            }

            testClass<AbstractFirWasmJsCodegenBoxInlineTest> {
                model("codegen/boxInline")
            }

            testClass<AbstractFirWasmJsCodegenInteropTest> {
                model("codegen/boxWasmJsInterop")
            }

            testClass<AbstractFirWasmWasiCodegenBoxTest> {
                model("codegen/boxWasmWasi")
            }

            testClass<AbstractFirWasmJsSteppingTest> {
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
                model("codegen/box", pattern = jsTranslatorTestPattern, excludeDirs = jvmOnlyBoxTests + k2BoxTestDir)
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

        testGroup("wasm/wasm.tests/tests-gen", "js/js.translator/testData", testRunnerMethodName = "runTest0") {
            testClass<AbstractFirWasmTypeScriptExportTest> {
                model("typescript-export/wasm/")
            }

            testClass<AbstractK1WasmTypeScriptExportTest> {
                model("typescript-export/wasm/")
            }
        }
    }
}
