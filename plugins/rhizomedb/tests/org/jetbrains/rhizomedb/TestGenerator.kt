/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.rhizomedb.runners.AbstractRhizomedbFirLightTreeAsmLikeInstructionsListingTest
import org.jetbrains.rhizomedb.runners.AbstractRhizomedbFirPsiDiagnosticTest

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    val excludedFirTestdataPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX

    generateTestGroupSuiteWithJUnit5(args) {
        testGroup(
            "plugins/rhizomedb/tests-gen",
            "plugins/rhizomedb/testData"
        ) {
            // ------------------------------- diagnostics -------------------------------
//            testClass<AbstractSerializationPluginDiagnosticTest>() {
//                model("diagnostics", excludedPattern = excludedFirTestdataPattern)
//            }
//
            testClass<AbstractRhizomedbFirPsiDiagnosticTest> {
                model("diagnostics", excludedPattern = excludedFirTestdataPattern)
                model("firMembers")
            }

            // ------------------------------- asm instructions -------------------------------

            testClass<AbstractRhizomedbFirLightTreeAsmLikeInstructionsListingTest> {
                model("codegen")
            }
        }
    }
}
