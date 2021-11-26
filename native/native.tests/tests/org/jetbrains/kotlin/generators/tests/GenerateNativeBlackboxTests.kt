/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.model.annotation
import org.jetbrains.kotlin.konan.blackboxtest.AbstractNativeBlackBoxTest
import org.jetbrains.kotlin.konan.blackboxtest.support.group.NativeBlackBoxTestCaseGroupProvider
import org.jetbrains.kotlin.konan.blackboxtest.support.group.ExtTestCaseGroupProvider

fun main() {
    System.setProperty("java.awt.headless", "true")

    generateTestGroupSuiteWithJUnit5 {
        // External box tests.
        testGroup("native/native.tests/tests-gen", "compiler/testData") {
            testClass<AbstractNativeBlackBoxTest>(
                suiteTestClassName = "NativeExtBlackBoxTestGenerated",
                annotations = listOf(annotation(NativeBlackBoxTestCaseGroupProvider::class.java, ExtTestCaseGroupProvider::class.java))
            ) {
                model("codegen/box")
                model("codegen/boxInline")
            }
        }

        // Samples (how to utilize abilities of new test infrastructure).
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData") {
            testClass<AbstractNativeBlackBoxTest> {
                model("samples")
                model("samples2")
            }
        }
    }
}
