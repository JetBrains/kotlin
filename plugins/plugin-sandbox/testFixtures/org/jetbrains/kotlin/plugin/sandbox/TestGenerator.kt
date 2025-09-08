/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.sandbox

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.test.utils.CUSTOM_TEST_DATA_EXTENSION_PATTERN

fun main() {
    generateTestGroupSuiteWithJUnit5 {
        testGroup("plugins/plugin-sandbox/tests-gen", "plugins/plugin-sandbox/testData") {
            testClass<AbstractFirPsiPluginDiagnosticTest> {
                model("diagnostics", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
            }

            testClass<AbstractFirJvmLightTreePluginBlackBoxCodegenTest> {
                model("box", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
            }

            testClass<AbstractFirJvmLightTreePluginBlackBoxCodegenWithSeparateKmpCompilationTest> {
                model("box", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
            }

            testClass<AbstractFirJsLightTreePluginBlackBoxCodegenTest> {
                model("box", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
            }

            testClass<AbstractFirJsLightTreePluginBlackBoxCodegenWithSeparateKmpCompilationTest> {
                model("box", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
            }

            testClass<AbstractFirLoadK2CompiledWithPluginJvmKotlinTest> {
                model("firLoadK2Compiled")
            }

            testClass<AbstractFirLoadK2CompiledWithPluginJsKotlinTest> {
                model("firLoadK2Compiled")
            }

            testClass<AbstractFirMetadataPluginSandboxTest> {
                model("metadata")
            }
        }
    }
}
