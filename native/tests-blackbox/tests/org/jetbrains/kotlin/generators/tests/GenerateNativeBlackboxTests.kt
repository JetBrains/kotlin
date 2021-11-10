/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.konan.blackboxtest.AbstractNativeBlackBoxTest

fun main() {
    System.setProperty("java.awt.headless", "true")

    generateTestGroupSuiteWithJUnit5 {
        testGroup("native/tests-blackbox/tests-gen", "native/tests-blackbox/testData") {
            testClass<AbstractNativeBlackBoxTest> {
                model("samples")
                model("samples2")
            }
        }
    }
}
