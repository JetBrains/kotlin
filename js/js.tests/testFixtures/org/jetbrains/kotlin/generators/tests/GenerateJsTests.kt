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
            testClass<AbstractFirJsKlibSyntheticAccessorTest> {
                model()
            }
            testClass<AbstractFirJsCodegenBoxWithInlinedFunInKlibTest>(
                suiteTestClassName = "FirJsKlibSyntheticAccessorsBoxTestGenerated"
            ) {
                model()
            }
        }

        testGroup("js/js.tests/tests-gen", "js/js.translator/testData/incremental") {
            testClass<AbstractJsFirInvalidationPerFileTest> {
                model("invalidation/", pattern = "^([^_](.+))$", recursive = false)
            }

            testClass<AbstractJsFirInvalidationPerModuleTest> {
                model("invalidation/", pattern = "^([^_](.+))$", recursive = false)
            }

            testClass<AbstractJsFirES6InvalidationPerFileTest>(annotations = listOf(*es6())) {
                model("invalidation/", pattern = "^([^_](.+))$", recursive = false)
            }

            testClass<AbstractJsFirES6InvalidationPerModuleTest>(annotations = listOf(*es6())) {
                model("invalidation/", pattern = "^([^_](.+))$", recursive = false)
            }

            testClass<AbstractJsFirInvalidationPerFileWithPLTest> {
                model("invalidationWithPL/", pattern = "^([^_](.+))$", recursive = false)
            }

            testClass<AbstractJsFirInvalidationPerModuleWithPLTest> {
                model("invalidationWithPL/", pattern = "^([^_](.+))$", recursive = false)
            }
        }

        testGroup("js/js.tests/tests-gen", "js/js.translator/testData/sourcemap", testRunnerMethodName = "runTest0") {
            testClass<AbstractFirSourceMapGenerationSmokeTest> {
                model()
            }
        }

        testGroup("js/js.tests/tests-gen", "js/js.translator/testData/multiModuleOrder/", testRunnerMethodName = "runTest0") {
            testClass<AbstractMultiModuleOrderTest>(annotations = listOf(*legacyFrontend())) {
                model()
            }
            testClass<AbstractFirMultiModuleOrderTest> {
                model()
            }
        }

        testGroup("js/js.tests/tests-gen", "js/js.translator/testData/box", testRunnerMethodName = "runTest0") {
            testClass<AbstractIrBoxJsTest>(annotations = listOf(*legacyFrontend())) {
                model(pattern = "^([^_](.+))\\.kt$", excludeDirs = listOf("es6classes"))
            }

            testClass<AbstractIrBoxJsES6Test>(annotations = listOf(*legacyFrontend(), *es6())) {
                model(pattern = "^([^_](.+))\\.kt$")
            }

            testClass<AbstractFirPsiJsBoxTest> {
                model(pattern = "^([^_](.+))\\.kt$", excludeDirs = listOf("es6classes"))
            }

            testClass<AbstractFirLightTreeJsBoxTest> {
                model(pattern = "^([^_](.+))\\.kt$", excludeDirs = listOf("es6classes"))
            }

            testClass<AbstractFirJsES6BoxTest>(annotations = listOf(*es6())) {
                model(pattern = "^([^_](.+))\\.kt$")
            }
        }

        testGroup("js/js.tests/tests-gen", "js/js.translator/testData/typescript-export/js", testRunnerMethodName = "runTest0") {
            testClass<AbstractIrJsTypeScriptExportTest>(annotations = listOf(*legacyFrontend())) {
                model(pattern = "^([^_](.+))\\.kt$")
            }

            testClass<AbstractIrJsES6TypeScriptExportTest>(annotations = listOf(*legacyFrontend(), *es6())) {
                model(pattern = "^([^_](.+))\\.kt$")
            }

            testClass<AbstractFirJsTypeScriptExportTest> {
                model(pattern = "^([^_](.+))\\.kt$")
            }

            testClass<AbstractFirJsES6TypeScriptExportTest>(annotations = listOf(*es6())) {
                model(pattern = "^([^_](.+))\\.kt$")
            }

            testClass<AbstractFirJsTypeScriptExportWithInlinedFunInKlibTest>(annotations = listOf(*es6())) {
                model(pattern = "^([^_](.+))\\.kt$")
            }
        }

        testGroup("js/js.tests/tests-gen", "js/js.translator/testData/webDemoExamples", testRunnerMethodName = "runTest0") {
            testClass<AbstractWebDemoExamplesTest>(annotations = listOf(*legacyFrontend())) {
                model()
            }
        }

        testGroup("js/js.tests/tests-gen", "js/js.translator/testData/lineNumbers", testRunnerMethodName = "runTest0") {
            testClass<AbstractJsIrLineNumberTest>(annotations = listOf(*legacyFrontend())) {
                model()
            }

            testClass<AbstractFirJsLineNumberTest> {
                model()
            }
            testClass<AbstractFirJsLineNumberWithInlinedFunInKlibTest> {
                model()
            }
        }

        testGroup("js/js.tests/tests-gen", "compiler/testData/codegen", testRunnerMethodName = "runTest0") {
            testClass<AbstractIrJsCodegenBoxTest>(annotations = listOf(*legacyFrontend())) {
                model("box", excludeDirs = jvmOnlyBoxTests + k2BoxTestDir)
            }

            testClass<AbstractJsLightTreeBlackBoxCodegenWithSeparateKmpCompilationTest> {
                model("box/$k2BoxTestDir")
            }

            testClass<AbstractIrJsES6CodegenBoxTest>(annotations = listOf(*legacyFrontend(), *es6())) {
                model("box", excludeDirs = jvmOnlyBoxTests + k2BoxTestDir)
            }

            testClass<AbstractFirJsCodegenBoxTest> {
                model("box", excludeDirs = jvmOnlyBoxTests + k1BoxTestDir)
            }

            testClass<AbstractFirJsCodegenBoxWithInlinedFunInKlibTest> {
                model("box", excludeDirs = jvmOnlyBoxTests + k1BoxTestDir)
                model("boxInline")
            }

            testClass<AbstractFirJsES6CodegenBoxTest>(annotations = listOf(*es6())) {
                model("box", excludeDirs = jvmOnlyBoxTests + k1BoxTestDir)
            }

            testClass<AbstractIrJsCodegenInlineTest>(annotations = listOf(*legacyFrontend())) {
                model("boxInline")
            }

            testClass<AbstractIrJsES6CodegenInlineTest>(annotations = listOf(*legacyFrontend(), *es6())) {
                model("boxInline")
            }

            testClass<AbstractFirJsCodegenInlineTest> {
                model("boxInline")
            }

            testClass<AbstractFirJsCodegenInlineWithInlinedFunInKlibTest> {
                model("boxInline")
            }

            testClass<AbstractFirJsCodegenSplittingInlineWithInlinedFunInKlibTest> {
                model("box")
                model("boxInline")
            }

            testClass<AbstractFirJsES6CodegenInlineTest>(annotations = listOf(*es6())) {
                model("boxInline")
            }

            testClass<AbstractIrCodegenWasmJsInteropJsTest>(annotations = listOf(*legacyFrontend())) {
                model("boxWasmJsInterop")
            }

            testClass<AbstractFirJsCodegenWasmJsInteropTest> {
                model("boxWasmJsInterop")
            }

            testClass<AbstractFirJsCodegenWasmJsInteropWithInlinedFunInKlibTest> {
                model("boxWasmJsInterop")
            }

            testClass<AbstractFirJsES6CodegenWasmJsInteropTest>(annotations = listOf(*es6())) {
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
            testClass<AbstractFirJsSteppingTest> {
                model("stepping")
            }

            testClass<AbstractFirJsSteppingWithInlinedFunInKlibTest> {
                model("stepping")
            }

            testClass<AbstractFirJsSteppingSplitTest> {
                model("stepping")
            }

            testClass<AbstractFirJsSteppingSplitWithInlinedFunInKlibTest> {
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
                    excludedPattern = excludedFirTestdataPattern,
                )
            }

            testClass<AbstractLightTreeJsDiagnosticTest>(suiteTestClassName = "LightTreeJsOldFrontendDiagnosticsTestGenerated") {
                model(
                    relativeRootPath = "testsWithJsStdLib",
                    pattern = "^([^_](.+))\\.kt$",
                    excludedPattern = excludedFirTestdataPattern,
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
            testClass<AbstractFirLoadK2CompiledJsKotlinTest> {
                model("compiledKotlin", extension = "kt")
                model("compiledKotlinWithStdlib", extension = "kt")
            }
        }
    }
}

private fun legacyFrontend() = arrayOf(
    annotation(Tag::class.java, "legacy-frontend")
)

private fun es6() = arrayOf(
    annotation(Tag::class.java, "es6")
)
