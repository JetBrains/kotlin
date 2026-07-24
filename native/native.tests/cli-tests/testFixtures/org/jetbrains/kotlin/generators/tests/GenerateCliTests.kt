/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.konan.test.cli.AbstractKlibToolCliTest
import org.jetbrains.kotlin.konan.test.cli.AbstractNativeCliTest

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")
    val mainClassName = TestGeneratorUtil.getMainClassName()

    val testsRoot = args[0]
    generateTestGroupSuiteWithJUnit5(args, mainClassName) {
        testGroup(testsRoot, "native/native.tests/cli-tests/testData") {
            testClass<AbstractNativeCliTest> {
                model("cli", extension = "args", testMethod = "doNativeTest", recursive = false)
            }
            testClass<AbstractKlibToolCliTest> {
                model("cli/klib-tool", extension = "args", testMethod = "doKlibToolTest", recursive = false)
            }
        }
    }
}
