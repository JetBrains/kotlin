/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.plugins

import org.jetbrains.kotlin.generators.dsl.junit4.generateTestGroupSuiteWithJUnit4
import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil

fun main(args: Array<String>) {
    val mainClassName = TestGeneratorUtil.getMainClassName()
    generateTestGroupSuiteWithJUnit4(args, mainClassName) {
        testGroup("plugins/plugins-interactions-testing/tests-gen", "plugins/plugins-interactions-testing/testData") {
            testClass<AbstractPluginCliTests> {
                model("cli", extension = "args", testMethod = "doJvmTest", recursive = false)
            }
        }
    }

    generateTestGroupSuiteWithJUnit5(args) {
        testGroup("plugins/plugins-interactions-testing/tests-gen", "plugins/plugins-interactions-testing/testData") {
            testClass<AbstractPluginInteractionFirBlackBoxCodegenTest> {
                model("box", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
            }
        }
    }
}
