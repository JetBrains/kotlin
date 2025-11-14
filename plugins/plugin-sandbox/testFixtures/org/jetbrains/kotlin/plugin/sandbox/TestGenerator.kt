/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.sandbox

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.model.annotation
import org.jetbrains.kotlin.generators.tests.provider
import org.jetbrains.kotlin.generators.tests.standalone
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeCodegenBoxTest
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedHostTarget
import org.jetbrains.kotlin.konan.test.blackbox.support.group.UseExtTestCaseGroupProvider
import org.jetbrains.kotlin.test.utils.CUSTOM_TEST_DATA_EXTENSION_PATTERN
import org.junit.jupiter.api.Tag

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

            testClass<AbstractJsLightTreePluginBlackBoxCodegenTest> {
                model("box", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
            }

            testClass<AbstractJsLightTreePluginBlackBoxCodegenWithSeparateKmpCompilationTest> {
                model("box", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
            }

            testClass<AbstractFirLoadK2CompiledWithPluginJvmKotlinTest> {
                model("firLoadK2Compiled")
            }

            testClass<AbstractLoadCompiledWithPluginJsKotlinTest> {
                model("firLoadK2Compiled")
            }

            testClass<AbstractFirMetadataPluginSandboxTest> {
                model("metadata")
            }

            testClass<AbstractNativeCodegenBoxTest>(
                suiteTestClassName = "PluginSandboxNativeTestGenerated",
                annotations = listOf(
                    *standalone(),
                    annotation(Tag::class.java, "sandbox-native"),
                    provider<UseExtTestCaseGroupProvider>(),
                    provider<EnforcedHostTarget>(),
                )
            ) {
                model("box")
            }
        }
    }
}
