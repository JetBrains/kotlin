/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.model.annotation
import org.jetbrains.kotlin.konan.blackboxtest.AbstractNativeBlackBoxTest
import org.jetbrains.kotlin.konan.blackboxtest.group.ExtTestCaseGroupProvider

fun main() {
    System.setProperty("java.awt.headless", "true")

    generateTestGroupSuiteWithJUnit5 {
        testGroup("native/tests-blackbox/ext-tests-gen", "compiler/testData") {
            testClass<AbstractNativeBlackBoxTest>(
                suiteTestClassName = "NativeExtBlackBoxTestGenerated",
                annotations = listOf(
                    annotation(CustomNativeBlackBoxTestCaseGroupProvider::class.java, ExtTestCaseGroupProvider::class.java)
                )
            ) {
                model("codegen/box")
                model("codegen/boxInline")
            }
        }
    }
}
