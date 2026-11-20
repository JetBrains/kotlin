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
import org.junit.jupiter.api.Tag

fun main(args: Array<String>) {
    val testsRoot = args[0]
    generateTestGroupSuiteWithJUnit5(args) {
        testGroup(testsRoot, "plugins/plugin-sandbox/testData") {
            testClass<AbstractFirLightTreePluginDiagnosticTest> {
                model("diagnostics")
                model("intentionallyEmpty")
                model("intentionallyNonExistent")
            }

            testClass<AbstractFirLightTreeJvmPluginIrTextTest> {
                model("irText")
            }

            testClass<AbstractFirLightTreeJvmPluginIrTextTestWithoutPlugins> {
                model("irText")
            }

            testClass<AbstractLightTreeJsPluginIrTextTest> {
                model("irText")
            }

            testClass<AbstractLightTreeJsPluginIrTextTestWithoutPlugins> {
                model("irText")
            }

            testClass<AbstractFirJvmLightTreePluginBlackBoxCodegenTest> {
                model("box")
            }

            testClass<AbstractFirJvmLightTreePluginBlackBoxCodegenTestWithoutPlugins> {
                model("box")
            }

            testClass<AbstractFirJvmLightTreePluginBlackBoxCodegenWithSeparateKmpCompilationTest> {
                model("box")
            }

            testClass<AbstractJsLightTreePluginBlackBoxCodegenTest> {
                model("box")
            }

            testClass<AbstractJsLightTreePluginBlackBoxCodegenWithSeparateKmpCompilationTest> {
                model("box")
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

            testClass<AbstractFirMetadataPluginSandboxTest>(suiteTestClassName = "EmptyTestGenerated") {
                model("intentionallyEmpty")
            }

            testClass<AbstractFirMetadataPluginSandboxTest>(suiteTestClassName = "NonExistentTestGenerated") {
                model("intentionallyNonExistent")
            }

            testClass<AbstractNativeCodegenBoxTest>(
                suiteTestClassName = "PluginSandboxNativeTestGenerated",
                annotations = listOf(
                    standalone(),
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
