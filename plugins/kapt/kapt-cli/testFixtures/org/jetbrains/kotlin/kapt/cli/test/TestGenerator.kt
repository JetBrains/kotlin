/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.cli.test

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5

fun main(args: Array<String>) {
    generateTestGroupSuiteWithJUnit5(args) {
        testGroup("plugins/kapt/kapt-cli/tests-gen", "plugins/kapt/kapt-cli/testData") {
            testClass<AbstractArgumentParsingTest> {
                model("argumentParsing", extension = "txt")
            }
            testClass<AbstractKaptToolIntegrationTest> {
                model("integration", recursive = false, extension = null)
            }
        }
    }
}
