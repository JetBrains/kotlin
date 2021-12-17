/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.impl.generateTestGroupSuite
import org.jetbrains.kotlin.incremental.AbstractInvalidationTest
import org.jetbrains.kotlin.js.test.*
import org.jetbrains.kotlin.js.test.ir.*
import org.jetbrains.kotlin.js.testOld.AbstractDceTest
import org.jetbrains.kotlin.js.testOld.AbstractJsLineNumberTest
import org.jetbrains.kotlin.js.testOld.compatibility.binary.AbstractJsKlibBinaryCompatibilityTest
import org.jetbrains.kotlin.js.testOld.wasm.semantics.AbstractIrCodegenBoxWasmTest
import org.jetbrains.kotlin.js.testOld.wasm.semantics.AbstractIrCodegenWasmJsInteropWasmTest
import org.jetbrains.kotlin.js.testOld.wasm.semantics.AbstractJsTranslatorWasmTest
import org.jetbrains.kotlin.test.TargetBackend

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    val jvmOnlyBoxTests = listOf(
        "testsWithJava9",
        "testsWithJava15",
        "testsWithJava17",
    )

    // TODO: repair these tests
    //generateTestDataForReservedWords()

    generateTestGroupSuite(args) {
        testGroup("js/js.tests/tests-gen", "js/js.translator/testData", testRunnerMethodName = "runTest0") {
            testClass<AbstractJsTranslatorWasmTest> {
                model("box/main", pattern = "^([^_](.+))\\.kt$", targetBackend = TargetBackend.WASM)
                model("box/kotlin.test/", pattern = "^([^_](.+))\\.kt$", targetBackend = TargetBackend.WASM)
                model("box/native/", pattern = "^([^_](.+))\\.kt$", targetBackend = TargetBackend.WASM)
            }

            testClass<AbstractDceTest> {
                model("dce/", pattern = "(.+)\\.js", targetBackend = TargetBackend.JS)
            }

            testClass<AbstractJsLineNumberTest> {
                model("lineNumbers/", pattern = "^([^_](.+))\\.kt$", targetBackend = TargetBackend.JS)
            }
        }

        testGroup("js/js.tests/tests-gen", "compiler/testData") {
            testClass<AbstractJsKLibABITestCase> {
                model("klibABI/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false, )
            }
        }

        testGroup("js/js.tests/tests-gen", "js/js.translator/testData") {
            testClass<AbstractInvalidationTest> {
                model("incremental/invalidation/", pattern = "^([^_](.+))$", targetBackend = TargetBackend.JS_IR, recursive = false)
            }
        }

        testGroup("js/js.tests/tests-gen", "compiler/testData", testRunnerMethodName = "runTest0") {
            testClass<AbstractIrCodegenBoxWasmTest> {
                model(
                    "codegen/box", pattern = "^([^_](.+))\\.kt$", targetBackend = TargetBackend.WASM, excludeDirs = listOf(
                        // TODO: Add stdlib
                        "contracts", "platformTypes",

                        // TODO: ArrayList
                        "ranges/stepped/unsigned",

                        // TODO: Support delegated properties
                        "delegatedProperty",

                        "compileKotlinAgainstKotlin"
                    ) + jvmOnlyBoxTests
                )
            }

            testClass<AbstractIrCodegenWasmJsInteropWasmTest> {
                model("codegen/boxWasmJsInterop", targetBackend = TargetBackend.WASM)
            }
        }

        testGroup("js/js.tests/tests-gen", "compiler/testData/binaryCompatibility", testRunnerMethodName = "runTest0") {
            testClass<AbstractJsKlibBinaryCompatibilityTest> {
                model("klibEvolution", targetBackend = TargetBackend.JS_IR)
            }
        }
    }

    generateTestGroupSuiteWithJUnit5(args) {
        testGroup("js/js.tests/tests-gen", "js/js.translator/testData", testRunnerMethodName = "runTest0") {
            testClass<AbstractBoxJsTest> {
                model("box/", pattern = "^([^_](.+))\\.kt$")
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

            testClass<AbstractIrBoxJsTest> {
                model("box/", pattern = "^([^_](.+))\\.kt$")
            }

            testClass<AbstractIrJsTypeScriptExportTest> {
                model("typescript-export/", pattern = "^([^_](.+))\\.kt$")
            }

            testClass<AbstractFirIrBoxJsTest> {
                model("box/", pattern = "^([^_](.+))\\.kt$")
            }

            testClass<AbstractFirJsTest> {
                model("box/", pattern = "^([^_](.+))\\.kt$")
            }
        }

        testGroup("js/js.tests/tests-gen", "compiler/testData", testRunnerMethodName = "runTest0") {
            testClass<AbstractJsCodegenBoxTest> {
                model("codegen/box", excludeDirs = jvmOnlyBoxTests + "compileKotlinAgainstKotlin")
            }

            testClass<AbstractJsCodegenInlineTest> {
                model("codegen/boxInline")
            }

            testClass<AbstractJsLegacyPrimitiveArraysBoxTest> {
                model("codegen/box/arrays")
            }

            testClass<AbstractIrJsCodegenBoxTest> {
                model("codegen/box", excludeDirs = jvmOnlyBoxTests + "compileKotlinAgainstKotlin")
            }

            testClass<AbstractIrJsCodegenBoxErrorTest> {
                model("codegen/boxError", excludeDirs = jvmOnlyBoxTests + "compileKotlinAgainstKotlin")
            }

            testClass<AbstractIrJsCodegenInlineTest> {
                model("codegen/boxInline")
            }

            testClass<AbstractIrCodegenWasmJsInteropJsTest> {
                model("codegen/boxWasmJsInterop")
            }

            testClass<AbstractFirIrJsCodegenBoxTest> {
                model("codegen/box", excludeDirs = jvmOnlyBoxTests + "compileKotlinAgainstKotlin")
            }
        }
    }
}
