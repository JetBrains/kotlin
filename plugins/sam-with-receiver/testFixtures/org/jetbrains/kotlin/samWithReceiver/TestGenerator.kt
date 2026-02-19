/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.samWithReceiver

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil

fun main(args: Array<String>) {
    val testsRoot = args[0]
    generateTestGroupSuiteWithJUnit5(args) {
        testGroup(testsRoot, "plugins/sam-with-receiver/testData") {
            testClass<AbstractSamWithReceiverTest> {
                model("diagnostics", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
            }
            testClass<AbstractFirPsiSamWithReceiverDiagnosticTest> {
                model("diagnostics", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
            }
            testClass<AbstractIrBlackBoxCodegenTestForSamWithReceiver> {
                model("codegen", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
            }
            testClass<AbstractFirLightTreeBlackBoxCodegenTestForSamWithReceiver> {
                model("codegen", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
            }
        }
    }
}
