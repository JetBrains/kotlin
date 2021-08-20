/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization

import org.jetbrains.kotlin.test.generators.generateTestGroupSuiteWithJUnit5

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    generateTestGroupSuiteWithJUnit5(args) {
        testGroup(
            "plugins/kotlin-serialization/kotlin-serialization-compiler/test",
            "plugins/kotlin-serialization/kotlin-serialization-compiler/testData"
        ) {

            // New test infrastructure ONLY
            testClass<AbstractSerializationIrBoxTest> {
                model("boxIr")
            }
        }
    }
}