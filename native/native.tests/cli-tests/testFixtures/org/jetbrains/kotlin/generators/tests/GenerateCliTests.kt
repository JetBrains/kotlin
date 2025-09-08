/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.impl.generateTestGroupSuite
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.konan.test.cli.AbstractNativeCliTest

fun main() {
    System.setProperty("java.awt.headless", "true")
    val mainClassName = TestGeneratorUtil.getMainClassName()

    generateTestGroupSuite(arrayOf(), mainClassName) {
        testGroup("native/native.tests/cli-tests/tests-gen", "native/native.tests/cli-tests/testData") {
            testClass<AbstractNativeCliTest> {
                model("cli", extension = "args", testMethod = "doNativeTest", recursive = false)
            }
        }
    }
}
