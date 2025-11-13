/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.model.annotation
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.js.test.fir.*
import org.jetbrains.kotlin.js.test.ir.*
import org.jetbrains.kotlin.test.utils.CUSTOM_TEST_DATA_EXTENSION_PATTERN
import org.junit.jupiter.api.Tag

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    val jvmOnlyBoxTests = listOf("compileKotlinAgainstKotlin")
    val k1BoxTestDir = "multiplatform/k1"
    val k2BoxTestDir = "multiplatform/k2"
    val irInterpreterTests = "involvesIrInterpreter"
    val excludedFirTestdataPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX

    // TODO: repair these tests
    //generateTestDataForReservedWords()
    generateTypeScriptJsExportOnFiles("js/js.translator/testData/typescript-export/js")

    generateTestGroupSuiteWithJUnit5(args) {
        testGroup("js/js.tests/tests-gen", "compiler/testData/klib/partial-linkage") {
            testClass<AbstractJsPartialLinkageWithICTestCase> {
                model(pattern = "^([^_](.+))$", recursive = false)
            }
            testClass<AbstractJsPartialLinkageNoICTestCase> {
                model(pattern = "^([^_](.+))$", recursive = false)
            }
            testClass<AbstractJsPartialLinkageNoICES6TestCase>(annotations = listOf(*es6())) {
                model(pattern = "^([^_](.+))$", recursive = false)
            }
        }

        testGroup("js/js.tests/tests-gen", "compiler/testData/klib/syntheticAccessors") {
            testClass<AbstractJsKlibSyntheticAccessorTest> {
                model()
            }
            testClass<AbstractJsCodegenBoxWithInlinedFunInKlibTest>(
                suiteTestClassName = "JsKlibSyntheticAccessorsBoxTestGenerated"
            ) {
                model()
            }
        }

        testGroup("js/js.tests/tests-gen", "js/js.translator/testData/incremental") {
            testClass<AbstractJsInvalidationPerFileTest> {
                model("invalidation/", pattern = "^([^_](.+))$", recursive = false)
            }

            testClass<AbstractJsInvalidationPerModuleTest> {
                model("invalidation/", pattern = "^([^_](.+))$", recursive = false)
            }

            testClass<AbstractJsES6InvalidationPerFileTest>(annotations = listOf(*es6())) {
                model("invalidation/", pattern = "^([^_](.+))$", recursive = false)
            }

            testClass<AbstractJsES6InvalidationPerModuleTest>(annotations = listOf(*es6())) {
                model("invalidation/", pattern = "^([^_](.+))$", recursive = false)
            }

            testClass<AbstractJsInvalidationPerFileWithPLTest> {
                model("invalidationWithPL/", pattern = "^([^_](.+))$", recursive = false)
            }

            testClass<AbstractJsInvalidationPerModuleWithPLTest> {
                model("invalidationWithPL/", pattern = "^([^_](.+))$", recursive = false)
            }
        }

        testGroup("js/js.tests/tests-gen", "js/js.translator/testData/sourcemap", testRunnerMethodName = "runTest0") {
            testClass<AbstractSourceMapGenerationSmokeTest> {
                model()
            }
        }

        testGroup("js/js.tests/tests-gen", "js/js.translator/testData/multiModuleOrder/", testRunnerMethodName = "runTest0") {
            testClass<AbstractFirMultiModuleOrderTest> {
                model()
            }
        }

        testGroup("js/js.tests/tests-gen", "js/js.translator/testData/box", testRunnerMethodName = "runTest0") {
            testClass<AbstractPsiJsBoxTest> {
                model(pattern = "^([^_](.+))\\.kt$", excludeDirs = listOf("es6classes"))
            }

            testClass<AbstractLightTreeJsBoxTest> {
                model(pattern = "^([^_](.+))\\.kt$", excludeDirs = listOf("es6classes"))
            }

            testClass<AbstractJsES6BoxTest>(annotations = listOf(*es6())) {
                model(pattern = "^([^_](.+))\\.kt$")
            }
        }

        testGroup("js/js.tests/tests-gen", "js/js.translator/testData/typescript-export/js", testRunnerMethodName = "runTest0") {
            testClass<AbstractJsTypeScriptExportTest> {
                model(pattern = "^([^_](.+))\\.kt$")
            }

            testClass<AbstractJsES6TypeScriptExportTest>(annotations = listOf(*es6())) {
                model(pattern = "^([^_](.+))\\.kt$")
            }

            testClass<AbstractJsTypeScriptExportWithInlinedFunInKlibTest>(annotations = listOf(*es6())) {
                model(pattern = "^([^_](.+))\\.kt$")
            }

            testClass<AbstractJsAnalysisApiTypeScriptExportTest> {
                model(pattern = "^([^_](.+))\\.kt$")
            }

            testClass<AbstractJsES6AnalysisApiTypeScriptExportTest>(annotations = listOf(*es6())) {
                model(pattern = "^([^_](.+))\\.kt$")
            }
        }

        testGroup("js/js.tests/tests-gen", "js/js.translator/testData/lineNumbers", testRunnerMethodName = "runTest0") {
            testClass<AbstractJsLineNumberTest> {
                model()
            }
            testClass<AbstractJsLineNumberWithInlinedFunInKlibTest> {
                model()
            }
        }

        testGroup("js/js.tests/tests-gen", "compiler/testData/codegen", testRunnerMethodName = "runTest0") {
            testClass<AbstractJsLightTreeBlackBoxCodegenWithSeparateKmpCompilationTest> {
                model("box/$k2BoxTestDir")
            }

            testClass<AbstractJsCodegenBoxTest> {
                model("box", excludeDirs = jvmOnlyBoxTests + k1BoxTestDir)
            }

            testClass<AbstractJsCodegenBoxWithInlinedFunInKlibTest> {
                model("box", excludeDirs = jvmOnlyBoxTests + k1BoxTestDir)
                model("boxInline")
            }

            testClass<AbstractJsES6CodegenBoxTest>(annotations = listOf(*es6())) {
                model("box", excludeDirs = jvmOnlyBoxTests + k1BoxTestDir)
            }

            testClass<AbstractJsCodegenInlineTest> {
                model("boxInline")
            }

            testClass<AbstractJsCodegenInlineWithInlinedFunInKlibTest> {
                model("boxInline")
            }

            testClass<AbstractJsCodegenSplittingInlineWithInlinedFunInKlibTest> {
                model("box")
                model("boxInline")
            }

            testClass<AbstractJsES6CodegenInlineTest>(annotations = listOf(*es6())) {
                model("boxInline")
            }

            testClass<AbstractJsCodegenWasmJsInteropTest> {
                model("boxWasmJsInterop")
            }

            testClass<AbstractJsCodegenWasmJsInteropWithInlinedFunInKlibTest> {
                model("boxWasmJsInterop")
            }

            testClass<AbstractJsES6CodegenWasmJsInteropTest>(annotations = listOf(*es6())) {
                model("boxWasmJsInterop")
            }

            testClass<AbstractJsIrDeserializationCodegenBoxTest> {
                model("box", excludeDirs = jvmOnlyBoxTests + k1BoxTestDir + irInterpreterTests)
                model("boxInline")
            }

            testClass<AbstractJsIrDeserializationCodegenBoxWithInlinedFunInKlibTest> {
                model("box", excludeDirs = jvmOnlyBoxTests + k1BoxTestDir + irInterpreterTests)
                model("boxInline")
            }
        }

        testGroup("js/js.tests/tests-gen", "compiler/testData/debug", testRunnerMethodName = "runTest0") {
            testClass<AbstractJsSteppingTest> {
                model("stepping")
            }

            testClass<AbstractJsSteppingWithInlinedFunInKlibTest> {
                model("stepping")
            }

            testClass<AbstractJsSteppingSplitTest> {
                model("stepping")
            }

            testClass<AbstractJsSteppingSplitWithInlinedFunInKlibTest> {
                model("stepping")
            }
        }

        testGroup("js/js.tests/tests-gen", "compiler/testData/diagnostics", testRunnerMethodName = "runTest0") {
            testClass<AbstractPsiJsDiagnosticWithBackendTest>(suiteTestClassName = "PsiJsKlibDiagnosticsTestGenerated") {
                model(
                    relativeRootPath = "klibSerializationTests",
                    pattern = "^([^_](.+))\\.kt$",
                    excludedPattern = excludedFirTestdataPattern,
                )
            }

            testClass<AbstractPsiJsDiagnosticTest>(suiteTestClassName = "PsiJsOldFrontendDiagnosticsTestGenerated") {
                model(
                    relativeRootPath = "testsWithJsStdLib",
                    pattern = "^([^_](.+))\\.kt$",
                    excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN,
                )
            }

            testClass<AbstractLightTreeJsDiagnosticTest>(suiteTestClassName = "LightTreeJsOldFrontendDiagnosticsTestGenerated") {
                model(
                    relativeRootPath = "testsWithJsStdLib",
                    pattern = "^([^_](.+))\\.kt$",
                    excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN,
                )
            }

            testClass<AbstractPsiJsDiagnosticWithBackendTest>(suiteTestClassName = "PsiJsOldFrontendDiagnosticsWithBackendTestGenerated") {
                model(
                    relativeRootPath = "testsWithJsStdLibAndBackendCompilation",
                    pattern = "^([^_](.+))\\.kt$",
                    excludedPattern = excludedFirTestdataPattern,
                )
            }

            testClass<AbstractLightTreeJsDiagnosticWithBackendTest>(suiteTestClassName = "LightTreeJsOldFrontendDiagnosticsWithBackendTestGenerated") {
                model(
                    relativeRootPath = "testsWithJsStdLibAndBackendCompilation",
                    pattern = "^([^_](.+))\\.kt$",
                    excludedPattern = excludedFirTestdataPattern,
                )
                model(
                    relativeRootPath = "testsWithAnyBackend",
                    pattern = "^([^_](.+))\\.kt$",
                    excludedPattern = excludedFirTestdataPattern,
                )
            }

            testClass<AbstractJsDiagnosticWithBackendWithInlinedFunInKlibTestBase>(suiteTestClassName = "JsOldFrontendDiagnosticsWithBackendWithInlinedFunInKlibTestGenerated") {
                model(
                    relativeRootPath = "testsWithJsStdLibAndBackendCompilation",
                    pattern = "^([^_](.+))\\.kt$",
                    excludedPattern = excludedFirTestdataPattern,
                )
                model(
                    relativeRootPath = "testsWithAnyBackend",
                    pattern = "^([^_](.+))\\.kt$",
                    excludedPattern = excludedFirTestdataPattern,
                )
            }

            testClass<AbstractJsDiagnosticWithIrInlinerTest>(suiteTestClassName = "JsDiagnosticWithIrInlinerTestGenerated") {
                model(
                    relativeRootPath = "irInliner",
                    pattern = "^([^_](.+))\\.kt$",
                )
            }
        }

        testGroup("js/js.tests/tests-gen", "compiler/testData/ir/irText", testRunnerMethodName = "runTest0") {
            testClass<AbstractLightTreeJsIrTextTest> {
                model(
                    excludeDirs = listOf("declarations/multiplatform/k1")
                )
            }

            testClass<AbstractPsiJsIrTextTest> {
                model(
                    excludeDirs = listOf("declarations/multiplatform/k1")
                )
            }
        }

        testGroup("js/js.tests/tests-gen", "compiler/testData/loadJava", testRunnerMethodName = "runTest0") {
            testClass<AbstractLoadCompiledJsKotlinTest> {
                model("compiledKotlin", extension = "kt")
                model("compiledKotlinWithStdlib", extension = "kt")
            }
        }
    }
}

private fun es6() = arrayOf(
    annotation(Tag::class.java, "es6")
)
