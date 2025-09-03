/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil

fun main(args: Array<String>) {
    generateTestGroupSuiteWithJUnit5(args) {
        testGroup("plugins/power-assert/tests-gen", "plugins/power-assert/testData") {
            testClass<AbstractIrBlackBoxCodegenTestForPowerAssert> {
                model("codegen", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
            }
            testClass<AbstractFirLightTreeBlackBoxCodegenTestForPowerAssert> {
                model("codegen", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
            }
        }
    }
}
