/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.allopen

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil

fun main(args: Array<String>) {
    val testsRoot = args[0]
    generateTestGroupSuiteWithJUnit5(args) {
        testGroup(testsRoot, "plugins/allopen/testData") {
            testClass<AbstractIrBytecodeListingTestForAllOpen> {
                model("bytecodeListing", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
            }
            testClass<AbstractFirPsiBytecodeListingTestForAllOpen> {
                model("bytecodeListing", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
            }
            testClass<AbstractDiagnosticTestForAllOpenBase> {
                model("diagnostics", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
            }
            testClass<AbstractFirLightTreeDiagnosticTestForAllOpen> {
                model("diagnostics", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
            }
            testClass<AbstractFirPsiDiagnosticTestForAllOpen> {
                model("diagnostics", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
            }
        }
    }
}
