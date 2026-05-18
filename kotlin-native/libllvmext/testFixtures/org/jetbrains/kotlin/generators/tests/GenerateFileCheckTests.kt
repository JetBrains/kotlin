/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.backend.konan.llvm.AbstractKotlinPassesFileCheckTest

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    val testsRoot = args[0]
    generateTestGroupSuiteWithJUnit5(args) {
        testGroup(testsRoot, "kotlin-native/libllvmext/testData/fileCheck") {
            testClass<AbstractKotlinPassesFileCheckTest> {
                model(extension = "ll")
            }
        }
    }
}
