/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.assignment.plugin

import org.jetbrains.kotlin.assignment.plugin.test.analysisApi.AbstractExpressionTypeTestForAssignment
import org.jetbrains.kotlin.assignment.plugin.test.analysisApi.AbstractGetOrBuildFirTestForAssignment
import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.test.utils.CUSTOM_TEST_DATA_EXTENSION_PATTERN

fun main(args: Array<String>) {
    val testsRoot = args[0]
    generateTestGroupSuiteWithJUnit5(args) {
        testGroup(testsRoot, "plugins/assign-plugin/testData") {
            testClass<AbstractFirAssignmentPluginDiagnosticTest> {
                model("diagnostics", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
            }
            testClass<AbstractLLAssignmentPluginDiagnosticTest> {
                model("diagnostics", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
            }
            testClass<AbstractFirLightTreeBlackBoxCodegenTestForAssignmentPlugin> {
                model("codegen", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
            }
            testClass<AbstractExpressionTypeTestForAssignment> {
                model("analysisApi/expressionType", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
            }
            testClass<AbstractGetOrBuildFirTestForAssignment> {
                model("analysisApi/getOrBuildFir", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
            }
        }
    }
}
