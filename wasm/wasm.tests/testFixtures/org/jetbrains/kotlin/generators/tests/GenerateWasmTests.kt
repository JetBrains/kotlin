/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.incremental.AbstractFirWasmInvalidationTest
import org.jetbrains.kotlin.incremental.AbstractFirWasmInvalidationWithPLTest
import org.jetbrains.kotlin.wasm.test.*
import org.jetbrains.kotlin.wasm.test.diagnostics.*

fun main(args: Array<String>) {
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
        testGroup("wasm/wasm.tests/tests-gen", "compiler/testData/klib/partial-linkage") {
            testClass<AbstractWasmPartialLinkageNoICTestCase> {
                model(pattern = "^([^_](.+))$", recursive = false)
            }
            testClass<AbstractWasmPartialLinkageWithICTestCase> {
                model(pattern = "^([^_](.+))$", recursive = false)
            }
        }

        testGroup("wasm/wasm.tests/tests-gen", "js/js.translator/testData/incremental") {
            testClass<AbstractFirWasmInvalidationTest> {
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
        }
    }

    generateTestGroupSuiteWithJUnit5(args) {
        testGroup("wasm/wasm.tests/tests-gen", "compiler/testData/diagnostics") {
            testClass<AbstractDiagnosticsWasmTest> {
                model("wasmTests", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
            }

            testClass<AbstractDiagnosticsFirWasmTest> {
                model("wasmTests", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
            }

            testClass<AbstractDiagnosticsWasmWasiTest> {
                model("wasmWasiTests", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
            }

            testClass<AbstractDiagnosticsFirWasmWasiTest> {
                model("wasmWasiTests", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
            }

            testClass<AbstractDiagnosticsFirWasmKlibTest> {
                model("wasmDiagnosticsKlibTests", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
            }
        }

        testGroup("wasm/wasm.tests/tests-gen", "js/js.translator/testData/box", testRunnerMethodName = "runTest0") {
            testClass<AbstractFirWasmJsTranslatorTest> {
                model("main", pattern = jsTranslatorTestPattern)
                model("native/", pattern = jsTranslatorTestPattern)
                model("esModules/", pattern = jsTranslatorTestPattern, excludeDirs = jsTranslatorEsModulesExcludedDirs)
                model("jsQualifier/", pattern = jsTranslatorTestPattern)
                model("reflection/", pattern = jsTranslatorReflectionPattern)
                model("kotlin.test/", pattern = jsTranslatorTestPattern)
            }
        }

        testGroup("wasm/wasm.tests/tests-gen", "compiler/testData", testRunnerMethodName = "runTest0") {
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

            testClass<AbstractFirWasmWasiCodegenBoxTest> {
                model("codegen/boxWasmWasi")
                model("codegen/box", pattern = jsTranslatorTestPattern, excludeDirs = jvmOnlyBoxTests + k1BoxTestDir)
                model("codegen/boxInline")
            }

            testClass<AbstractFirWasmSpecCodegenBoxTest> {
                model("codegen/box", pattern = jsTranslatorTestPattern, excludeDirs = jvmOnlyBoxTests + k1BoxTestDir)
                model("codegen/boxInline")
            }

            testClass<AbstractFirWasmSpecCodegenBoxCoroutineTest> {
                model("codegen/box/coroutines", pattern = jsTranslatorTestPattern, excludeDirs = jvmOnlyBoxTests + k1BoxTestDir)
            }

            testClass<AbstractFirWasmSpecCodegenBoxFailingTest> {
                // Hardcoded failing tests extracted from
                // "Test Results - failed tests FirWasmSpecCodegenBoxTestGenerated.xml".
                // Keep this section in sync when failures change.

                // codegen/box/bridges (root)
                model(
                    "codegen/box/bridges",
                    pattern = "^(diamond|manyTypeArgumentsSubstitutedSuccessively|test13|test5)\\.kt$"
                )
                // codegen/box/bridges/substitutionInSuperClass
                model(
                    "codegen/box/bridges/substitutionInSuperClass",
                    pattern = "^(boundedTypeArguments|upperBound)\\.kt$"
                )

                // codegen/box/closures/capturedVarsOptimization
                model(
                    "codegen/box/closures/capturedVarsOptimization",
                    pattern = "^(closureBoxedVarOptimizations)\\.kt$"
                )

                // codegen/box/collections
                model(
                    "codegen/box/collections",
                    pattern = "^(inheritFromAbstractMutableListInt)\\.kt$"
                )

                // codegen/box/coroutines (root)
                model(
                    "codegen/box/coroutines",
                    pattern = "^(dispatchResume|kt21080)\\.kt$"
                )
                // codegen/box/coroutines/controlFlow
                model(
                    "codegen/box/coroutines/controlFlow",
                    pattern = "^(doubleBreak|throwFromFinally)\\.kt$"
                )
                // codegen/box/coroutines/inlineClasses/direct
                model(
                    "codegen/box/coroutines/inlineClasses/direct",
                    pattern = "^(boxUnboxInsideCoroutine_nonLocalReturn|genericOverrideSuspendFun|genericOverrideSuspendFun_Any|genericOverrideSuspendFun_Any_NullableInlineClassUpperBound|genericOverrideSuspendFun_Int|genericOverrideSuspendFun_NullableAny|genericOverrideSuspendFun_NullableAny_null|genericOverrideSuspendFun_NullableInt|genericOverrideSuspendFun_NullableInt_null)\\.kt$"
                )
                // codegen/box/coroutines/inlineClasses/resume
                model(
                    "codegen/box/coroutines/inlineClasses/resume",
                    pattern = "^(boxUnboxInsideCoroutine_nonLocalReturn)\\.kt$"
                )
                // codegen/box/coroutines/inlineClasses/resumeWithException
                model(
                    "codegen/box/coroutines/inlineClasses/resumeWithException",
                    pattern = "^(boxTypeParameterOfSuperType|boxUnboxInsideCoroutine_nonLocalReturn)\\.kt$"
                )
                // codegen/box/coroutines/intrinsicSemantics
                model(
                    "codegen/box/coroutines/intrinsicSemantics",
                    pattern = "^(intercepted|releaseIntercepted|startCoroutine|startCoroutineUninterceptedOrReturn|startCoroutineUninterceptedOrReturnInterception|suspendCoroutineUninterceptedOrReturn)\\.kt$"
                )
                // codegen/box/coroutines/stackUnwinding
                model(
                    "codegen/box/coroutines/stackUnwinding",
                    pattern = "^(suspendInCycle)\\.kt$"
                )
                // codegen/box/coroutines/suspendFunctionAsCoroutine
                model(
                    "codegen/box/coroutines/suspendFunctionAsCoroutine",
                    pattern = "^(dispatchResume)\\.kt$"
                )

                // codegen/box/delegatedProperty
                model(
                    "codegen/box/delegatedProperty",
                    pattern = "^(genericSetValueViaSyntheticAccessor)\\.kt$"
                )

                // codegen/box/funInterface
                model(
                    "codegen/box/funInterface",
                    pattern = "^(primitiveConversions|samConstructorExplicitInvocation)\\.kt$"
                )

                // codegen/box/inference
                model(
                    "codegen/box/inference",
                    pattern = "^(overrideGenericDefaultMethod)\\.kt$"
                )

                // codegen/box/inlineClasses (root)
                model(
                    "codegen/box/inlineClasses",
                    pattern = "^(checkBoxingForComplexClassHierarchy|checkBoxingForComplexClassHierarchyGeneric|defaultInterfaceMethodsInInlineClass|defaultInterfaceMethodsInInlineClassGeneric|kt38680a|kt38680aGeneric|kt38680b|kt38680bGeneric)\\.kt$"
                )
                // codegen/box/inlineClasses/interfaceMethodCalls
                model(
                    "codegen/box/inlineClasses/interfaceMethodCalls",
                    pattern = "^(genericDefaultInterfaceExtensionFunCall|genericDefaultInterfaceExtensionFunCallGeneric|genericDefaultInterfaceMethodCall|genericDefaultInterfaceMethodCallGeneric)\\.kt$"
                )

                // codegen/box/interfaceCallsNCasts
                model(
                    "codegen/box/interfaceCallsNCasts",
                    pattern = "^(diamond)\\.kt$"
                )

                // codegen/box/operatorConventions
                model(
                    "codegen/box/operatorConventions",
                    pattern = "^(kt20387)\\.kt$"
                )

                // codegen/box/strings
                model(
                    "codegen/box/strings",
                    pattern = "^(kt13213)\\.kt$"
                )

                // codegen/box/syntheticAccessors
                model(
                    "codegen/box/syntheticAccessors",
                    pattern = "^(accessorForGenericMethod)\\.kt$"
                )

                // codegen/box/traits
                model(
                    "codegen/box/traits",
                    pattern = "^(kt2541|traitImplGenericDelegation)\\.kt$"
                )

                // codegen/box/typealias
                model(
                    "codegen/box/typealias",
                    pattern = "^(simple)\\.kt$"
                )

                // codegen/boxInline/anonymousObject (root)
                model(
                    "codegen/boxInline/anonymousObject",
                    pattern = "^(fakeOverrideGenericBase|fakeOverrideReferenceGenericBase)\\.kt$"
                )

                // codegen/boxInline/anonymousObject/properRecapturingInClass
                model(
                    "codegen/boxInline/anonymousObject/properRecapturingInClass",
                    pattern = "^(inlinelambdaChain|lambdaChain|lambdaChainSimple|lambdaChainSimple_2|lambdaChain_2|lambdaChain_3)\\.kt$"
                )
            }

            testClass<AbstractFirWasmWasiCodegenBoxWithInlinedFunInKlibTest> {
                model("codegen/boxWasmWasi")
                model("codegen/box", pattern = jsTranslatorTestPattern, excludeDirs = jvmOnlyBoxTests + k1BoxTestDir)
                model("codegen/boxInline")
            }

            testClass<AbstractFirWasmJsSteppingTest> {
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
        testGroup("wasm/wasm.tests/tests-gen", "js/js.translator/testData", testRunnerMethodName = "runTest0") {
            testClass<AbstractFirWasmTypeScriptExportTest> {
                model("typescript-export/wasm/")
            }
        }

        testGroup("wasm/wasm.tests/tests-gen", "compiler/testData/klib/syntheticAccessors", testRunnerMethodName = "runTest0") {
            testClass<AbstractFirWasmJsCodegenBoxWithInlinedFunInKlibTest>(
                suiteTestClassName = "WasmJsSynthAccBoxTestGenerated"
            ) {
                model()
            }
        }
    }
}
