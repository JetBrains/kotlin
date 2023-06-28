/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlinx.serialization.runners.*

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    val excludedFirTestdataPattern = "^(.+)\\.fir\\.kts?\$"

    generateTestGroupSuiteWithJUnit5(args) {
        testGroup(
            "plugins/kotlinx-serialization/tests-gen",
            "plugins/kotlinx-serialization/testData"
        ) {
            // ------------------------------- diagnostics -------------------------------
            testClass<AbstractSerializationPluginDiagnosticTest>() {
                model("diagnostics", excludedPattern = excludedFirTestdataPattern)
            }

            testClass<AbstractSerializationFirPsiDiagnosticTest> {
                model("diagnostics", excludedPattern = excludedFirTestdataPattern)
                model("firMembers")
            }

            // ------------------------------- asm instructions -------------------------------

            testClass<AbstractSerializationAsmLikeInstructionsListingTest> {
                model("codegen")
            }

            testClass<AbstractSerializationIrAsmLikeInstructionsListingTest> {
                model("codegen")
            }

            testClass<AbstractSerializationFirLightTreeAsmLikeInstructionsListingTest> {
                model("codegen")
            }

            // ------------------------------- box -------------------------------

            testClass<AbstractSerializationIrBoxTest> {
                model("boxIr")
            }

            testClass<AbstractSerializationJdk11IrBoxTest> {
                model("jdk11BoxIr")
            }

            testClass<AbstractSerializationFirLightTreeBlackBoxTest> {
                model("boxIr")
                model("firMembers")
            }

            testClass<AbstractSerializationWithoutRuntimeIrBoxTest> {
                model("boxWithoutRuntime")
            }

            testClass<AbstractSerializationIrJsBoxTest> {
                model("boxIr")
            }

            testClass<AbstractSerializationFirJsBoxTest> {
                model("boxIr")
            }
        }
    }
}
