/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.test

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.kapt.test.runners.AbstractIrKotlinKaptContextTest
import org.jetbrains.kotlin.kapt.test.runners.AbstractKaptStubConverterTest

fun main(args: Array<String>) {
    generateTestGroupSuiteWithJUnit5(args) {
        testGroup("plugins/kapt/kapt-compiler/tests-gen", "plugins/kapt/kapt-compiler/testData") {
            testClass<AbstractIrKotlinKaptContextTest> {
                model("kotlinRunner")
            }
            testClass<AbstractKaptStubConverterTest> {
                model("converter")
            }
            testClass<AbstractFirKaptStubConverterTest> {
                model("converter")
            }
        }
    }
}
