/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.assignment.plugin

import org.jetbrains.kotlin.assignment.plugin.test.analysisApi.AbstractExpressionTypeTestForAssignment
import org.jetbrains.kotlin.assignment.plugin.test.analysisApi.AbstractGetOrBuildFirTestForAssignment
import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil

fun main(args: Array<String>) {
    val testsRoot = args[0]
    generateTestGroupSuiteWithJUnit5(args) {
        testGroup(testsRoot, "plugins/assign-plugin/testData") {
            testClass<AbstractFirAssignmentPluginDiagnosticTest> {
                model("diagnostics")
            }
            testClass<AbstractLLAssignmentPluginDiagnosticTest> {
                model("diagnostics")
            }
            testClass<AbstractFirLightTreeBlackBoxCodegenTestForAssignmentPlugin> {
                model("codegen")
            }
            testClass<AbstractExpressionTypeTestForAssignment> {
                model("analysisApi/expressionType")
            }
            testClass<AbstractGetOrBuildFirTestForAssignment> {
                model("analysisApi/getOrBuildFir")
            }
        }
    }
}
