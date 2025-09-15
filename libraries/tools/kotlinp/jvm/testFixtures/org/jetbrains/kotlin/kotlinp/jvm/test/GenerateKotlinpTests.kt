/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kotlinp.jvm.test

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5

fun main(args: Array<String>) {
    val testsRoot = args[0]

    generateTestGroupSuiteWithJUnit5(args) {
        testGroup(testsRoot, "libraries/tools/kotlinp/jvm/testData") {
            testClass<AbstractK1KotlinpTest> {
                model("")
            }
            testClass<AbstractK2KotlinpTest> {
                model("", pattern = "^(.*)\\.kts?$")
            }
        }

        testGroup("libraries/tools/kotlinp/jvm/tests-gen", "compiler/testData") {
            testClass<AbstractKotlinpCompilerTestDataTest> {
                model("loadJava/compiledKotlin", extension = "kt")
                model("loadJava/compiledKotlinWithStdlib", extension = "kt")
                model("serialization/builtinsSerializer", extension = "kt")
            }
        }
    }
}
