/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.model.annotation
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil.KT_WITHOUT_UNDERSCORE_PREFIX
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil.WITHOUT_UNDERSCORE_PREFIX
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.js.test.JsEs6Test
import org.jetbrains.kotlin.js.test.JsInlineAnonymousFunctionsTest
import org.jetbrains.kotlin.js.test.runners.*
import org.jetbrains.kotlin.js.test.runners.tsexport.*
import org.jetbrains.kotlin.test.utils.CUSTOM_TEST_DATA_EXTENSION_PATTERN
import org.junit.jupiter.api.Tag

fun main(args: Array<String>) {
    val testsRoot = args[0]
    System.setProperty("java.awt.headless", "true")

    val jvmOnlyBoxTests = listOf("compileKotlinAgainstKotlin")
    val k1BoxTestDir = "multiplatform/k1"
    val k2BoxTestDir = "multiplatform/k2"
    val excludedFirTestdataPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX

    // TODO: repair these tests
    //generateTestDataForReservedWords()

    generateTestGroupSuiteWithJUnit5(args) {
        testGroup(testsRoot, "compiler/testData/klib/partial-linkage") {
            testClass<AbstractJsPartialLinkageWithICTestCase> {
                model(pattern = WITHOUT_UNDERSCORE_PREFIX, recursive = false)
            }
            testClass<AbstractJsPartialLinkageNoICTestCase> {
                model(pattern = WITHOUT_UNDERSCORE_PREFIX, recursive = false)
            }
            testClass<AbstractJsPartialLinkageNoICES6TestCase>(annotations = listOf(es6())) {
                model(pattern = WITHOUT_UNDERSCORE_PREFIX, recursive = false)
            }
        }

        testGroup(testsRoot, "compiler/testData/klib/syntheticAccessors") {
            testClass<AbstractJsKlibSyntheticAccessorTest> {
                model()
            }
            testClass<AbstractJsKlibSyntheticAccessorsBoxTest>(
                suiteTestClassName = "JsKlibSyntheticAccessorsBoxTestGenerated"
            ) {
                model()
            }
        }

        testGroup(testsRoot, "js/js.translator/testData/incremental") {
            testClass<AbstractJsInvalidationPerFileTest> {
                model("invalidation/", pattern = WITHOUT_UNDERSCORE_PREFIX, recursive = false)
            }

            testClass<AbstractJsInvalidationPerModuleTest> {
                model("invalidation/", pattern = WITHOUT_UNDERSCORE_PREFIX, recursive = false)
            }

            testClass<AbstractJsES6InvalidationPerFileTest>(annotations = listOf(es6())) {
                model("invalidation/", pattern = WITHOUT_UNDERSCORE_PREFIX, recursive = false)
            }

            testClass<AbstractJsES6InvalidationPerModuleTest>(annotations = listOf(es6())) {
                model("invalidation/", pattern = WITHOUT_UNDERSCORE_PREFIX, recursive = false)
            }

            testClass<AbstractJsInvalidationPerFileWithPLTest> {
                model("invalidationWithPL/", pattern = WITHOUT_UNDERSCORE_PREFIX, recursive = false)
            }

            testClass<AbstractJsInvalidationPerModuleWithPLTest> {
                model("invalidationWithPL/", pattern = WITHOUT_UNDERSCORE_PREFIX, recursive = false)
            }
        }

        testGroup(testsRoot, "js/js.translator/testData/sourcemap", testRunnerMethodName = "runTest0") {
            testClass<AbstractSourceMapGenerationSmokeTest> {
                model()
            }
        }

        testGroup(testsRoot, "js/js.translator/testData/multiModuleOrder/", testRunnerMethodName = "runTest0") {
            testClass<AbstractFirMultiModuleOrderTest> {
                model()
            }
        }

        testGroup(testsRoot, "js/js.translator/testData/box", testRunnerMethodName = "runTest0") {
            testClass<AbstractPsiJsBoxTest> {
                model(pattern = KT_WITHOUT_UNDERSCORE_PREFIX, excludeDirs = listOf("es6classes"))
            }

            testClass<AbstractLightTreeJsBoxTest> {
                model(pattern = KT_WITHOUT_UNDERSCORE_PREFIX, excludeDirs = listOf("es6classes"))
            }

            testClass<AbstractJsES6BoxTest>(annotations = listOf(es6())) {
                model(pattern = KT_WITHOUT_UNDERSCORE_PREFIX)
            }

            testClass<AbstractJsBoxWithInlineAnonymousFunctionsTest>(annotations = listOf(inlineAnonymousFunctions())) {
                model(pattern = KT_WITHOUT_UNDERSCORE_PREFIX, excludeDirs = listOf("es6classes"))
            }

            testClass<AbstractJsES6BoxWithInlineAnonymousFunctionsTest>(annotations = listOf(es6(), inlineAnonymousFunctions())) {
                model(pattern = KT_WITHOUT_UNDERSCORE_PREFIX)
            }

            testClass<AbstractJsSwcBoxTest>(annotations = listOf(swcTag())) {
                model(pattern = KT_WITHOUT_UNDERSCORE_PREFIX)
            }
        }

        testGroup(testsRoot, "js/js.translator/testData/typescript-export/js", testRunnerMethodName = "runTest0") {
            testClass<AbstractJsTypeScriptExportTest> {
                model(pattern = KT_WITHOUT_UNDERSCORE_PREFIX)
            }

            testClass<AbstractJsTypeScriptWholeFileExportTest> {
                model(pattern = KT_WITHOUT_UNDERSCORE_PREFIX)
            }

            testClass<AbstractJsES6TypeScriptExportTest>(annotations = listOf(es6())) {
                model(pattern = KT_WITHOUT_UNDERSCORE_PREFIX)
            }

            testClass<AbstractJsES6TypeScriptWholeFileExportTest>(annotations = listOf(es6())) {
                model(pattern = KT_WITHOUT_UNDERSCORE_PREFIX)
            }

            testClass<AbstractJsAnalysisApiTypeScriptExportTest> {
                model(pattern = KT_WITHOUT_UNDERSCORE_PREFIX)
            }

            testClass<AbstractJsES6AnalysisApiTypeScriptExportTest>(annotations = listOf(es6())) {
                model(pattern = KT_WITHOUT_UNDERSCORE_PREFIX)
            }

            testClass<AbstractJsAnalysisApiTypeScriptWholeFileExportTest> {
                model(pattern = KT_WITHOUT_UNDERSCORE_PREFIX)
            }

            testClass<AbstractJsES6AnalysisApiTypeScriptWholeFileExportTest>(annotations = listOf(es6())) {
                model(pattern = KT_WITHOUT_UNDERSCORE_PREFIX)
            }
        }

        testGroup(testsRoot, "js/js.translator/testData/lineNumbers", testRunnerMethodName = "runTest0") {
            testClass<AbstractJsLineNumberTest> {
                model()
            }
        }

        testGroup(testsRoot, "compiler/testData/codegen", testRunnerMethodName = "runTest0") {
            testClass<AbstractJsLightTreeBlackBoxCodegenWithSeparateKmpCompilationTest> {
                model("box/$k2BoxTestDir")
            }

            testClass<AbstractJsCodegenBoxTest> {
                model("box", excludeDirs = jvmOnlyBoxTests + k1BoxTestDir)
            }

            testClass<AbstractJsCodegenBoxWithInlineAnonymousFunctionsTest>(annotations = listOf(inlineAnonymousFunctions())) {
                model("box", excludeDirs = jvmOnlyBoxTests + k1BoxTestDir)
            }

            testClass<AbstractJsCodegenBoxInlineTest> {
                model("boxInline")
            }

            testClass<AbstractJsCodegenBoxInlineWithInlineAnonymousFunctionsTest>(annotations = listOf(inlineAnonymousFunctions())) {
                model("boxInline")
            }

            testClass<AbstractJsSwcCodegenBoxTest>(annotations = listOf(swcTag())) {
                model("box", excludeDirs = jvmOnlyBoxTests + k1BoxTestDir)
            }

            testClass<AbstractJsSwcCodegenBoxInlineTest>(annotations = listOf(swcTag())) {
                model("boxInline")
            }

            testClass<AbstractFirJsLightTreeHeaderModeCodegenTest> {
                model("box", excludeDirs = jvmOnlyBoxTests + k1BoxTestDir)
            }

            testClass<AbstractJsES6CodegenBoxTest>(annotations = listOf(es6())) {
                model("box", excludeDirs = jvmOnlyBoxTests + k1BoxTestDir, smokeTest = true)
            }

            testClass<AbstractJsES6CodegenBoxWithInlineAnonymousFunctionsTest>(annotations = listOf(es6(), inlineAnonymousFunctions())) {
                model("box", excludeDirs = jvmOnlyBoxTests + k1BoxTestDir)
            }

            testClass<AbstractJsCodegenSplittingTest> {
                model("box")
                model("boxInline")
            }

            testClass<AbstractJsES6CodegenInlineTest>(annotations = listOf(es6())) {
                model("boxInline")
            }

            testClass<AbstractJsES6CodegenBoxInlineWithInlineAnonymousFunctionsTest>(
                annotations = listOf(
                    es6(),
                    inlineAnonymousFunctions(),
                )
            ) {
                model("boxInline")
            }

            testClass<AbstractJsCodegenWasmJsInteropTest> {
                model("boxWasmJsInterop")
            }

            testClass<AbstractJsES6CodegenWasmJsInteropTest>(annotations = listOf(es6())) {
                model("boxWasmJsInterop")
            }

            testClass<AbstractJsIrDeserializationCodegenBoxTest> {
                model("box", excludeDirs = jvmOnlyBoxTests + k1BoxTestDir)
                model("boxInline")
            }
        }

        testGroup(testsRoot, "compiler/testData/debug", testRunnerMethodName = "runTest0") {
            testClass<AbstractJsSteppingTest> {
                model("stepping")
            }

            testClass<AbstractJsSteppingSplitTest> {
                model("stepping")
            }
        }

        testGroup(testsRoot, "compiler/testData/diagnostics", testRunnerMethodName = "runTest0") {
            testClass<AbstractPsiJsDiagnosticWithBackendTest>(suiteTestClassName = "PsiJsKlibDiagnosticsTestGenerated") {
                model(
                    relativeRootPath = "klibSerializationTests",
                    pattern = KT_WITHOUT_UNDERSCORE_PREFIX,
                    excludedPattern = excludedFirTestdataPattern,
                )
                model(
                    relativeRootPath = "testsWithJsStdLib",
                    pattern = KT_WITHOUT_UNDERSCORE_PREFIX,
                    excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN,
                )
                model(
                    relativeRootPath = "testsWithJsStdLibAndBackendCompilation",
                    pattern = KT_WITHOUT_UNDERSCORE_PREFIX,
                    excludedPattern = excludedFirTestdataPattern,
                )
            }

            testClass<AbstractLightTreeJsDiagnosticWithBackendTest>(suiteTestClassName = "LightTreeJsKlibDiagnosticsTestGenerated") {
                model(
                    relativeRootPath = "klibSerializationTests",
                    pattern = KT_WITHOUT_UNDERSCORE_PREFIX,
                    excludedPattern = excludedFirTestdataPattern,
                )
                model(
                    relativeRootPath = "testsWithJsStdLib",
                    pattern = KT_WITHOUT_UNDERSCORE_PREFIX,
                    excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN,
                )
                model(
                    relativeRootPath = "testsWithJsStdLibAndBackendCompilation",
                    pattern = KT_WITHOUT_UNDERSCORE_PREFIX,
                    excludedPattern = excludedFirTestdataPattern,
                )
                model(
                    relativeRootPath = "testsWithAnyBackend",
                    pattern = KT_WITHOUT_UNDERSCORE_PREFIX,
                    excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN,
                )
                model(
                    relativeRootPath = "irInliner",
                    pattern = KT_WITHOUT_UNDERSCORE_PREFIX,
                    excludedPattern = excludedFirTestdataPattern,
                )
            }
        }

        testGroup(testsRoot, "compiler/testData/ir/irText", testRunnerMethodName = "runTest0") {
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

        testGroup(testsRoot, "compiler/testData/loadJava", testRunnerMethodName = "runTest0") {
            testClass<AbstractLoadCompiledJsKotlinTest> {
                model("compiledKotlin", extension = "kt")
                model("compiledKotlinWithStdlib", extension = "kt")
            }
        }
    }
}

private fun es6() = annotation<JsEs6Test>()

private fun inlineAnonymousFunctions() = annotation<JsInlineAnonymousFunctionsTest>()

private fun swcTag() = annotation(Tag::class.java, "swc")
