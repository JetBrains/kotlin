/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil

fun main(args: Array<String>) {
    generateTestGroupSuiteWithJUnit5(args) {
        testGroup("plugins/lombok/tests-gen", "plugins/lombok/testData") {
            testClass<AbstractIrBlackBoxCodegenTestForLombok> {
                model("box")
            }
            testClass<AbstractFirLightTreeBlackBoxCodegenTestForLombok> {
                model("box")
            }
            testClass<AbstractDiagnosticTestForLombok> {
                model("diagnostics/k1+k2", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
            }
            testClass<AbstractFirPsiDiagnosticTestForLombok> {
                model("diagnostics")
            }
        }
    }
}
