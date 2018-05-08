/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.tests.generator.testGroup
import org.jetbrains.kotlin.js.test.AbstractDceTest
import org.jetbrains.kotlin.js.test.AbstractJsLineNumberTest
import org.jetbrains.kotlin.js.test.semantics.*
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
    }

    testGroup("js/js.tests/test", "compiler/testData", testRunnerMethodName = "runTest0") {
        testClass<AbstractJsCodegenBoxTest> {
            model("codegen/box", targetBackend = TargetBackend.JS)
        }

        testClass<AbstractNonLocalReturnsTest> {
            model("codegen/boxInline/nonLocalReturns/", targetBackend = TargetBackend.JS)
        }

        testClass<AbstractPropertyAccessorsInlineTests> {
            model("codegen/boxInline/property/", targetBackend = TargetBackend.JS)
        }

        testClass<AbstractNoInlineTests> {
            model("codegen/boxInline/noInline/", targetBackend = TargetBackend.JS)
        }

        testClass<AbstractCallableReferenceInlineTests> {
            model("codegen/boxInline/callableReference/", targetBackend = TargetBackend.JS)
        }

        testClass<AbstractEnumValuesInlineTests> {
            model("codegen/boxInline/enum/", targetBackend = TargetBackend.JS)
        }

        testClass<AbstractInlineDefaultValuesTests> {
            model("codegen/boxInline/defaultValues/", targetBackend = TargetBackend.JS)
        }

        testClass<AbstractJsLegacyPrimitiveArraysBoxTest> {
            model("codegen/box/arrays", targetBackend = TargetBackend.JS)
        }
    }
}
