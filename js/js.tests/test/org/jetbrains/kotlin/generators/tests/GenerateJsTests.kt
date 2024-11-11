/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.impl.generateTestGroupSuite
import org.jetbrains.kotlin.generators.model.annotation
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.js.test.fir.*
import org.jetbrains.kotlin.js.test.ir.*
import org.jetbrains.kotlin.js.testOld.klib.AbstractClassicJsKlibEvolutionTest
import org.jetbrains.kotlin.js.testOld.klib.AbstractFirJsKlibEvolutionTest
import org.jetbrains.kotlin.test.TargetBackend
import org.junit.jupiter.api.Disabled
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

    generateTestGroupSuite(args) {
        testGroup("js/js.tests/tests-gen", "compiler/testData/klib/evolution", testRunnerMethodName = "runTest0") {
            testClass<AbstractClassicJsKlibEvolutionTest>(annotations = listOf(*legacyFrontend())) {
                model(targetBackend = TargetBackend.JS_IR)
            }
            testClass<AbstractFirJsKlibEvolutionTest> {
                model(targetBackend = TargetBackend.JS_IR)
            }
        }
    }

    generateTestGroupSuiteWithJUnit5(args) {
        testGroup("js/js.tests/tests-gen", "compiler/testData/klib/partial-linkage") {
            testClass<AbstractJsPartialLinkageWithICTestCase>(annotations = listOf(*legacyFrontend())) {
                model(pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false)
            }
            testClass<AbstractJsPartialLinkageNoICTestCase>(annotations = listOf(*legacyFrontend())) {
                model(pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false)
            }
            testClass<AbstractJsPartialLinkageNoICES6TestCase>(annotations = listOf(*legacyFrontend(), *es6())) {
                model(pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR_ES6, recursive = false)
            }
            testClass<AbstractFirJsPartialLinkageNoICTestCase> {
                model(pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false)
            }
        }

        testGroup("js/js.tests/tests-gen", "compiler/testData/klib/syntheticAccessors") {
            testClass<AbstractFirJsKlibSyntheticAccessorsTest> {
                model()
            }
            testClass<AbstractFirJsCodegenBoxWithInlinedFunInKlibTest>(
                suiteTestClassName = "FirJsKlibSyntheticAccessorsBoxTestGenerated"
            ) {
                model()
            }
        }

        testGroup("js/js.tests/klib-compatibility/tests-gen", "compiler/testData/klib/versionCompatibility") {
            testClass<AbstractJsKlibCompatibilityNoICTestCase>(annotations = listOf(*legacyFrontend())) {
                model(pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false)
            }
            testClass<AbstractJsKlibCompatibilityNoICES6TestCase>(annotations = listOf(*legacyFrontend(), *es6())) {
                model(pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR_ES6, recursive = false)
            }
            testClass<AbstractJsKlibCompatibilityWithICTestCase>(annotations = listOf(*legacyFrontend())) {
                model(pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false)
            }
        }

        testGroup("js/js.tests/tests-gen", "js/js.translator/testData/incremental") {
            testClass<AbstractJsIrInvalidationPerFileTest>(annotations = listOf(*legacyFrontend())) {
                model("invalidation/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false)
            }

            testClass<AbstractJsIrInvalidationPerModuleTest>(annotations = listOf(*legacyFrontend())) {
                model("invalidation/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false)
            }

            testClass<AbstractJsIrES6InvalidationPerFileTest>(annotations = listOf(*legacyFrontend(), *es6())) {
                model("invalidation/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR_ES6, recursive = false)
            }

            testClass<AbstractJsIrES6InvalidationPerModuleTest>(annotations = listOf(*legacyFrontend(), *es6())) {
                model("invalidation/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR_ES6, recursive = false)
            }

            testClass<AbstractJsFirInvalidationPerFileTest> {
                model("invalidation/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false)
            }

            testClass<AbstractJsFirInvalidationPerModuleTest> {
                model("invalidation/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false)
            }

            testClass<AbstractJsFirES6InvalidationPerFileTest>(annotations = listOf(*es6())) {
                model("invalidation/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR_ES6, recursive = false)
            }

            testClass<AbstractJsFirES6InvalidationPerModuleTest>(annotations = listOf(*es6())) {
                model("invalidation/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR_ES6, recursive = false)
            }

            testClass<AbstractJsIrInvalidationPerFileWithPLTest>(annotations = listOf(*legacyFrontend())) {
                model("invalidationWithPL/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false)
            }

            testClass<AbstractJsIrInvalidationPerModuleWithPLTest>(annotations = listOf(*legacyFrontend())) {
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
            testClass<AbstractSourceMapGenerationSmokeTest>(annotations = listOf(*legacyFrontend())) {
                model()
            }
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
        }

        testGroup("js/js.tests/tests-gen", "compiler/testData/codegen", testRunnerMethodName = "runTest0") {
            testClass<AbstractIrJsCodegenBoxTest>(annotations = listOf(*legacyFrontend())) {
                model("box", excludeDirs = jvmOnlyBoxTests + k2BoxTestDir)
            }

            testClass<AbstractIrJsES6CodegenBoxTest>(annotations = listOf(*legacyFrontend(), *es6())) {
                model("box", excludeDirs = jvmOnlyBoxTests + k2BoxTestDir)
            }

            testClass<AbstractFirJsCodegenBoxTest> {
                model("box", excludeDirs = jvmOnlyBoxTests + k1BoxTestDir)
            }

            testClass<AbstractFirJsCodegenBoxWithInlinedFunInKlibTest> {
                model("box", excludeDirs = jvmOnlyBoxTests + k1BoxTestDir)
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

            testClass<AbstractFirJsES6CodegenInlineTest>(annotations = listOf(*es6())) {
                model("boxInline")
            }

            testClass<AbstractIrCodegenWasmJsInteropJsTest>(annotations = listOf(*legacyFrontend())) {
                model("boxWasmJsInterop")
            }

            testClass<AbstractFirJsCodegenWasmJsInteropTest> {
                model("boxWasmJsInterop")
            }

            testClass<AbstractFirJsES6CodegenWasmJsInteropTest>(annotations = listOf(*es6())) {
                model("boxWasmJsInterop")
            }

            testClass<AbstractFirJsIrDeserializationCodegenBoxTest> {
                model("box", excludeDirs = jvmOnlyBoxTests + k1BoxTestDir + irInterpreterTests)
                model("boxInline")
            }

            testClass<AbstractFirJsIrDeserializationCodegenBoxWithInlinedFunInKlibTest> {
                model("box", excludeDirs = jvmOnlyBoxTests + k1BoxTestDir + irInterpreterTests)
                model("boxInline")
            }
        }

        testGroup("js/js.tests/tests-gen", "compiler/testData/debug", testRunnerMethodName = "runTest0") {
            testClass<AbstractIrJsSteppingTest>(annotations = listOf(*legacyFrontend())) {
                model("stepping")
            }

            testClass<AbstractFirJsSteppingTest> {
                model("stepping")
            }

            testClass<AbstractIrJsLocalVariableTest>(
                annotations = listOf(
                    *legacyFrontend(),
                    annotation(Disabled::class.java, "value" to "flaky, see KTI-1959"),
                )
            ) {
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

            testClass<AbstractDiagnosticsTestWithJsStdLib>(
                suiteTestClassName = "DiagnosticsWithJsStdLibTestGenerated",
                annotations = listOf(*legacyFrontend()),
            ) {
                model(
                    relativeRootPath = "testsWithJsStdLib",
                    pattern = "^([^_](.+))\\.kt$",
                    excludedPattern = excludedFirTestdataPattern,
                    targetBackend = TargetBackend.JS_IR
                )
            }

            testClass<AbstractDiagnosticsTestWithJsStdLibWithBackend>(
                suiteTestClassName = "DiagnosticsWithJsStdLibAndBackendTestGenerated",
                annotations = listOf(*legacyFrontend()),
            ) {
                model(
                    relativeRootPath = "testsWithJsStdLibAndBackendCompilation",
                    pattern = "^([^_](.+))\\.kt$",
                    excludedPattern = excludedFirTestdataPattern,
                    targetBackend = TargetBackend.JS_IR
                )
            }

            testClass<AbstractFirJsDiagnosticWithIrInlinerTest>(suiteTestClassName = "FirJsDiagnosticWithIrInlinerTestGenerated") {
                model(
                    relativeRootPath = "irInliner",
                    pattern = "^([^_](.+))\\.kt$",
                    targetBackend = TargetBackend.JS_IR
                )
            }
        }

        testGroup("js/js.tests/tests-gen", "compiler/testData/ir/irText", testRunnerMethodName = "runTest0") {
            testClass<AbstractClassicJsIrTextTest>(annotations = listOf(*legacyFrontend())) {
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

private fun legacyFrontend() = arrayOf(
    annotation(Tag::class.java, "legacy-frontend")
)

private fun es6() = arrayOf(
    annotation(Tag::class.java, "es6")
)
