/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.incremental.AbstractFirWasmInvalidationMultiModuleTest
import org.jetbrains.kotlin.incremental.AbstractFirWasmInvalidationSingleModuleTest
import org.jetbrains.kotlin.incremental.AbstractFirWasmInvalidationTest
import org.jetbrains.kotlin.incremental.AbstractFirWasmInvalidationWithPLMultiModuleTest
import org.jetbrains.kotlin.incremental.AbstractFirWasmInvalidationWithPLSingleModuleTest
import org.jetbrains.kotlin.incremental.AbstractFirWasmInvalidationWithPLTest
import org.jetbrains.kotlin.test.utils.CUSTOM_TEST_DATA_EXTENSION_PATTERN
import org.jetbrains.kotlin.wasm.test.*
import org.jetbrains.kotlin.wasm.test.diagnostics.*

fun main(args: Array<String>) {
    val testsRoot = args[0]
    System.setProperty("java.awt.headless", "true")

    // Common configuration shared between K1 and K2 tests:
    val jvmOnlyBoxTests = listOf("compileKotlinAgainstKotlin")
    val k1BoxTestDir = "multiplatform/k1"

    val jsTranslatorTestPattern = "^([^_](.+))\\.kt$"
    val jsTranslatorReflectionPattern = "^(findAssociatedObject(InSeparatedFile)?(Lazyness)?(AndDCE)?)\\.kt$"
    val jsTranslatorEsModulesExcludedDirs = listOf(
        // JsExport is not supported for classes
        "jsExport", "native", "export", "escapedIdentifiers",
        // Multimodal infra is not supported. Also, we don't use ES modules for cross-module refs in Wasm
        "crossModuleRef", "crossModuleRefPerFile", "crossModuleRefPerModule"
    )
    // TODO: Remove excludedPattern below after fix of KT-78960 (it's simpler to exclude temporarily than to split test `boxInline/innerClasses/kt12126.kt`)
    val excludedPatternForBoxInlineTestsWithInliner = "kt12126.kt"


    generateTestGroupSuiteWithJUnit5(args) {
        testGroup(testsRoot, "compiler/testData/klib/partial-linkage") {
            testClass<AbstractWasmPartialLinkageNoICTestCase> {
                model(pattern = "^([^_](.+))$", recursive = false)
            }
            testClass<AbstractWasmPartialLinkageWithICTestCase> {
                model(pattern = "^([^_](.+))$", recursive = false)
            }
        }

        testGroup(testsRoot, "js/js.translator/testData/incremental") {
            testClass<AbstractFirWasmInvalidationTest> {
                model(
                    "invalidation/",
                    pattern = "^([^_](.+))$",
                    recursive = false,
                )
            }
            testClass<AbstractFirWasmInvalidationMultiModuleTest> {
                model(
                    "invalidation/",
                    pattern = "^([^_](.+))$",
                    recursive = false,
                )
            }
            testClass<AbstractFirWasmInvalidationSingleModuleTest> {
                model(
                    "invalidation/",
                    pattern = "^([^_](.+))$",
                    recursive = false,
                )
            }
            testClass<AbstractFirWasmInvalidationWithPLTest> {
                model(
                    "invalidationWithPL/",
                    pattern = "^([^_](.+))$",
                    recursive = false,
                )
            }
            testClass<AbstractFirWasmInvalidationWithPLMultiModuleTest> {
                model(
                    "invalidationWithPL/",
                    pattern = "^([^_](.+))$",
                    recursive = false,
                )
            }
            testClass<AbstractFirWasmInvalidationWithPLSingleModuleTest> {
                model(
                    "invalidationWithPL/",
                    pattern = "^([^_](.+))$",
                    recursive = false,
                )
            }
        }
    }

    generateTestGroupSuiteWithJUnit5(args) {
        testGroup(testsRoot, "compiler/testData/diagnostics") {
            testClass<AbstractWasmJsDiagnosticTest> {
                model("wasmTests", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
                model("wasmDiagnosticsKlibTests", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
                model("testsWithAnyBackend", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
            }

            testClass<AbstractWasmJsDiagnosticWithIrInlinerTest> {
                model("wasmTests", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
                model("wasmDiagnosticsKlibTests", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
                model("irInliner", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
                model("testsWithAnyBackend", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
            }

            testClass<AbstractWasmWasiDiagnosticTest> {
                model("wasmWasiTests", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
                model("wasmDiagnosticsKlibTests/wasmExport", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
            }

            testClass<AbstractWasmWasiDiagnosticWithIrInlinerTestBase> {
                model("wasmWasiTests", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
                model("wasmDiagnosticsKlibTests/wasmExport", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
                model("irInliner", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
                model("testsWithAnyBackend", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
            }
        }

        testGroup(testsRoot, "js/js.translator/testData/box", testRunnerMethodName = "runTest0") {
            testClass<AbstractFirWasmJsTranslatorTest> {
                model("main", pattern = jsTranslatorTestPattern)
                model("native/", pattern = jsTranslatorTestPattern)
                model("esModules/", pattern = jsTranslatorTestPattern, excludeDirs = jsTranslatorEsModulesExcludedDirs)
                model("jsQualifier/", pattern = jsTranslatorTestPattern)
                model("reflection/", pattern = jsTranslatorReflectionPattern)
                model("kotlin.test/", pattern = jsTranslatorTestPattern)
            }
        }

        testGroup(testsRoot, "compiler/testData", testRunnerMethodName = "runTest0") {
            testClass<AbstractFirWasmJsCodegenSingleModuleBoxTest> {
                model("codegen/box", pattern = jsTranslatorTestPattern, excludeDirs = jvmOnlyBoxTests + k1BoxTestDir)
            }

            testClass<AbstractFirWasmJsCodegenMultiModuleBoxTest> {
                model("codegen/box", pattern = jsTranslatorTestPattern, excludeDirs = jvmOnlyBoxTests + k1BoxTestDir + "size")
            }

            testClass<AbstractFirWasmJsCodegenBoxTest> {
                model("codegen/box", pattern = jsTranslatorTestPattern, excludeDirs = jvmOnlyBoxTests + k1BoxTestDir)
            }

            testClass<AbstractFirWasmJsCodegenBoxWithInlinedFunInKlibTest> {
                model("codegen/box", pattern = jsTranslatorTestPattern, excludeDirs = jvmOnlyBoxTests + k1BoxTestDir)
                model("codegen/boxInline", pattern = jsTranslatorTestPattern, excludedPattern = excludedPatternForBoxInlineTestsWithInliner)
            }

            testClass<AbstractFirWasmJsCodegenSplittingWithInlinedFunInKlibTest> {
                model("codegen/box", pattern = jsTranslatorTestPattern, excludeDirs = jvmOnlyBoxTests + k1BoxTestDir)
                model("codegen/boxInline", pattern = jsTranslatorTestPattern, excludedPattern = excludedPatternForBoxInlineTestsWithInliner)
            }

            testClass<AbstractFirWasmJsCodegenBoxInlineTest> {
                model("codegen/boxInline")
            }

            testClass<AbstractFirWasmJsCodegenInteropTest> {
                model("codegen/boxWasmJsInterop")
            }

            testClass<AbstractFirWasmJsCodegenSingleModuleInteropTest> {
                model("codegen/boxWasmJsInterop")
            }

            testClass<AbstractFirWasmJsCodegenMultiModuleInteropTest> {
                model("codegen/boxWasmJsInterop")
            }

            testClass<AbstractFirWasmWasiCodegenBoxTest> {
                model("codegen/boxWasmWasi")
                model("codegen/box", pattern = jsTranslatorTestPattern, excludeDirs = jvmOnlyBoxTests + k1BoxTestDir)
                model("codegen/boxInline")
            }

            testClass<AbstractFirWasmWasiCodegenBoxWithInlinedFunInKlibTest> {
                model("codegen/boxWasmWasi")
                model("codegen/box", pattern = jsTranslatorTestPattern, excludeDirs = jvmOnlyBoxTests + k1BoxTestDir)
                model("codegen/boxInline")
            }

            testClass<AbstractFirWasmJsSteppingTest> {
                model("debug/stepping")
            }
            testClass<AbstractFirWasmJsSteppingSingleModuleTest> {
                model("debug/stepping")
            }
            testClass<AbstractFirWasmJsMultiModuleSteppingTest> {
                model("debug/stepping")
            }
            testClass<AbstractFirWasmJsSteppingWithInlinedFunInKlibTest> {
                model("debug/stepping")
            }
            testClass<AbstractFirWasmJsSteppingSplitTest> {
                model("debug/stepping")
            }
            testClass<AbstractFirWasmJsSteppingSplitWithInlinedFunInKlibTest> {
                model("debug/stepping")
            }
        }
        testGroup(testsRoot, "js/js.translator/testData", testRunnerMethodName = "runTest0") {
            testClass<AbstractFirWasmTypeScriptExportTest> {
                model("typescript-export/wasm/")
            }
        }
        testGroup(testsRoot, "js/js.translator/testData", testRunnerMethodName = "runTest0") {
            testClass<AbstractFirWasmTypeScriptExportSingleModuleTest> {
                model("typescript-export/wasm/")
            }
        }

        testGroup(testsRoot, "js/js.translator/testData", testRunnerMethodName = "runTest0") {
            testClass<AbstractFirWasmTypeScriptExportMultiModuleTest> {
                model("typescript-export/wasm/")
            }
        }

        testGroup(testsRoot, "compiler/testData/klib/syntheticAccessors", testRunnerMethodName = "runTest0") {
            testClass<AbstractFirWasmJsSyntheticAccessorsTest>(
                suiteTestClassName = "WasmJsSynthAccBoxTestGenerated"
            ) {
                model()
            }
            testClass<AbstractWasmJsKlibSyntheticAccessorTest>(
                suiteTestClassName = "WasmJsSynthAccTestGenerated"
            ) {
                model()
            }
        }
        testGroup(testsRoot, "compiler/testData/ir/irText", testRunnerMethodName = "runTest0") {
            testClass<AbstractWasmJsIrTextTest> {
                model(
                    excludeDirs = listOf("declarations/multiplatform/k1")
                )
            }
        }
        testGroup(testsRoot, "compiler/testData/loadJava", testRunnerMethodName = "runTest0") {
            testClass<AbstractWasmJsLoadCompiledKotlinTest> {
                model("compiledKotlin", extension = "kt")
                model("compiledKotlinWithStdlib", extension = "kt")
            }
        }
    }
}
