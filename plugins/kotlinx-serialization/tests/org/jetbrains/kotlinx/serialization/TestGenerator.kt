/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization

import org.jetbrains.kotlin.generators.TestGroup
import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlinx.serialization.matrix.cases.enumsTestMatrix
import org.jetbrains.kotlinx.serialization.matrix.testMatrix
import org.jetbrains.kotlinx.serialization.runners.*

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    val excludedFirTestdataPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX

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

            testClass<AbstractSerializationFirLightTreeBlackBoxTest> {
                model("boxIr")
                model("firMembers")
            }

            testClass<AbstractSerializationJdk11IrBoxTest> {
                model("jdk11BoxIr")
            }

            testClass<AbstractSerializationJdk11FirLightTreeBoxTest> {
                model("jdk11BoxIr")
            }

            testClass<AbstractSerializationWithoutRuntimeIrBoxTest> {
                model("boxWithoutRuntime")
            }

            testClass<AbstractSerializationWithoutRuntimeFirLightTreeBoxTest> {
                model("boxWithoutRuntime")
            }

            testClass<AbstractSerializationIrJsBoxTest> {
                model("boxIr")
            }

            testClass<AbstractSerializationFirJsBoxTest> {
                model("boxIr")
            }

            // ------------------------------- code compile -------------------------------

            testClass<AbstractCompilerFacilityTestForSerialization> {
                model("compilerFacility")
            }

            testMatrix {
                add("enums") { enumsTestMatrix() }
            }
        }

        testGroup(testsRoot = "plugins/kotlinx-serialization/tests-gen", testDataRoot = "plugins/kotlinx-serialization/testData") {
            run {
                fun TestGroup.TestClass.diagnosticsModelInit() {
                    model("diagnostics", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
                    model("firMembers")
                }

                testClass<AbstractLLSerializationDiagnosticsTest> {
                    diagnosticsModelInit()
                }

                testClass<AbstractLLFirReversedSerializationDiagnosticTest> {
                    diagnosticsModelInit()
                }
            }

            run {
                fun TestGroup.TestClass.blackBoxModelInit() {
                    model("boxIr")
                    model("codegen")
                }

                testClass<AbstractLLFirSerializationBlackBoxCodegenBasedTest> {
                    blackBoxModelInit()
                }

                testClass<AbstractLLFirReversedSerializationBlackBoxCodegenBasedTest> {
                    blackBoxModelInit()
                }
            }
        }
    }
}
