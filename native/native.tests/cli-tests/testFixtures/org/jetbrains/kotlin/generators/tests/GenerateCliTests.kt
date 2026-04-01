/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.dsl.junit4.generateTestGroupSuiteWithJUnit4
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.konan.test.cli.AbstractNativeCliTest

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")
    val mainClassName = TestGeneratorUtil.getMainClassName()

    val testsRoot = args[0]
    generateTestGroupSuiteWithJUnit4(args, mainClassName) {
        testGroup(testsRoot, "native/native.tests/cli-tests/testData") {
            testClass<AbstractNativeCliTest> {
                model("cli", extension = "args", testMethod = "doNativeTest", recursive = false)
            }
        }
    }
}
