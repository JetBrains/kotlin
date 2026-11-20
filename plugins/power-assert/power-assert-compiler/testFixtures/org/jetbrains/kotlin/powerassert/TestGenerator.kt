/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5

fun main(args: Array<String>) {
    val testsRoot = args[0]
    generateTestGroupSuiteWithJUnit5(args) {
        testGroup(testsRoot, "plugins/power-assert/power-assert-compiler/testData") {
            testClass<AbstractPowerAssertPluginFirPsiDiagnosticTest> {
                model("diagnostic")
            }
            testClass<AbstractPowerAssertBlackBoxCodegenTest> {
                model("codegen")
            }
            testClass<AbstractPowerAssertMissingRuntimeBlackBoxCodegenTest> {
                model(
                    relativeRootPath = "codegen",
                    excludeDirsRecursively = listOf("annotated"),
                )
            }
        }
    }
}
