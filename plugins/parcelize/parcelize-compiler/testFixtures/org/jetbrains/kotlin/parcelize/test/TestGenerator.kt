/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.test

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.parcelize.test.runners.*

fun main(args: Array<String>) {
    generateTestGroupSuiteWithJUnit5(args) {
        testGroup("plugins/parcelize/parcelize-compiler/tests-gen", "plugins/parcelize/parcelize-compiler/testData") {
            testClass<AbstractParcelizeIrBoxTest> {
                model("box")
            }

            testClass<AbstractParcelizeFirLightTreeBoxTest> {
                model("box")
            }

            testClass<AbstractParcelizeIrBytecodeListingTest> {
                model("codegen")
            }

            testClass<AbstractFirParcelizeBytecodeListingTest> {
                model("codegen")
            }

            testClass<AbstractParcelizeDiagnosticTest> {
                model("diagnostics", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
            }

            testClass<AbstractFirPsiParcelizeDiagnosticTest> {
                model("diagnostics", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
            }
        }
    }
}
