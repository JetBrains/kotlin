/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.tests.generator.testGroup
import org.jetbrains.kotlin.js.test.AbstractDceTest
import org.jetbrains.kotlin.js.test.AbstractJsLineNumberTest
import org.jetbrains.kotlin.js.test.ir.semantics.*
import org.jetbrains.kotlin.js.test.semantics.*
import org.jetbrains.kotlin.js.test.wasm.semantics.AbstractIrWasmBoxWasmTest
import org.jetbrains.kotlin.test.TargetBackend

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    // TODO: repair these tests
    //generateTestDataForReservedWords()

    testGroup("js/js.tests/test", "js/js.translator/testData", testRunnerMethodName = "runTest0") {
        testClass<AbstractBoxJsTest> {
            model("box/", pattern = "^([^_](.+))\\.kt$", targetBackend = TargetBackend.JS)
        }

        testClass<AbstractIrBoxJsTest> {
            model("box/", pattern = "^([^_](.+))\\.kt$", targetBackend = TargetBackend.JS_IR)
        }

        testClass<AbstractIrJsTypeScriptExportTest> {
            model("typescript-export/", pattern = "^([^_](.+))\\.kt$", targetBackend = TargetBackend.JS_IR)
        }

        testClass<AbstractSourceMapGenerationSmokeTest> {
            model("sourcemap/", pattern = "^([^_](.+))\\.kt$", targetBackend = TargetBackend.JS)
        }

        testClass<AbstractOutputPrefixPostfixTest> {
            model("outputPrefixPostfix/", pattern = "^([^_](.+))\\.kt$", targetBackend = TargetBackend.JS)
        }

        testClass<AbstractDceTest> {
            model("dce/", pattern = "(.+)\\.js", targetBackend = TargetBackend.JS)
        }

        testClass<AbstractJsLineNumberTest> {
            model("lineNumbers/", pattern = "^([^_](.+))\\.kt$", targetBackend = TargetBackend.JS)
        }

        testClass<AbstractIrWasmBoxWasmTest> {
            model("wasmBox", pattern = "^([^_](.+))\\.kt$", targetBackend = TargetBackend.WASM)
        }

        testClass<AbstractIrWasmBoxJsTest> {
            model("wasmBox", pattern = "^([^_](.+))\\.kt$", targetBackend = TargetBackend.JS_IR)
        }
    }

    testGroup("js/js.tests/test", "compiler/testData", testRunnerMethodName = "runTest0") {
        testClass<AbstractJsCodegenBoxTest> {
            model("codegen/box", targetBackend = TargetBackend.JS)
        }

        testClass<AbstractIrJsCodegenBoxTest> {
            model("codegen/box", targetBackend = TargetBackend.JS_IR)
        }

        testClass<AbstractJsCodegenInlineTest> {
            model("codegen/boxInline/", targetBackend = TargetBackend.JS)
        }

        testClass<AbstractIrJsCodegenInlineTest> {
            model("codegen/boxInline/", targetBackend = TargetBackend.JS_IR)
        }

        testClass<AbstractJsLegacyPrimitiveArraysBoxTest> {
            model("codegen/box/arrays", targetBackend = TargetBackend.JS)
        }
    }
}
