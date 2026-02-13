/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.assignment.plugin

import org.jetbrains.kotlin.generators.dsl.TestGroup
import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil

fun main(args: Array<String>) {
    val testsRoot = args[0]
    generateTestGroupSuiteWithJUnit5(args) {
        testGroup(testsRoot = testsRoot, testDataRoot = "plugins/assign-plugin/testData") {
            testClass<AbstractAssignmentPluginDiagnosticTest> {
                model("diagnostics", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
            }
            testClass<AbstractFirPsiAssignmentPluginDiagnosticTest> {
                model("diagnostics", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
            }
            testClass<AbstractIrBlackBoxCodegenTestAssignmentPlugin> {
                model("codegen", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
            }
            testClass<AbstractFirLightTreeBlackBoxCodegenTestForAssignmentPlugin> {
                model("codegen", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
            }
        }

        testGroup(testsRoot = testsRoot, testDataRoot = "plugins/assign-plugin/testData") {
            run {
                fun TestGroup.TestClass.diagnosticsModelInit() {
                    model("diagnostics", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
                    //model("firMembers")
                }

                testClass<AbstractLLAssignDiagnosticsTest> {
                    diagnosticsModelInit()
                }

                testClass<AbstractLLReversedAssignDiagnosticsTest> {
                    diagnosticsModelInit()
                }
            }

            run {
                fun TestGroup.TestClass.blackBoxModelInit() {
                    //model("boxIr")
                    model("codegen")
                }

                testClass<AbstractLLAssignBlackBoxTest> {
                    blackBoxModelInit()
                }

                testClass<AbstractLLReversedAssignBlackBoxTest> {
                    blackBoxModelInit()
                }
            }
        }
    }
}
