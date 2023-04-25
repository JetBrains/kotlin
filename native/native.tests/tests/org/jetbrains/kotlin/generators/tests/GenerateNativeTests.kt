/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.model.annotation
import org.jetbrains.kotlin.konan.blackboxtest.*
import org.jetbrains.kotlin.konan.blackboxtest.support.ClassLevelProperty
import org.jetbrains.kotlin.konan.blackboxtest.support.EnforcedHostTarget
import org.jetbrains.kotlin.konan.blackboxtest.support.EnforcedProperty
import org.jetbrains.kotlin.konan.blackboxtest.support.group.FirPipeline
import org.jetbrains.kotlin.konan.blackboxtest.support.group.UseExtTestCaseGroupProvider
import org.jetbrains.kotlin.konan.blackboxtest.support.group.UsePartialLinkage
import org.jetbrains.kotlin.konan.blackboxtest.support.group.UseStandardTestCaseGroupProvider
import org.jetbrains.kotlin.test.TargetBackend
import org.junit.jupiter.api.Tag

fun main() {
    System.setProperty("java.awt.headless", "true")

    generateTestGroupSuiteWithJUnit5 {
        // Codegen box tests.
        testGroup("native/native.tests/tests-gen", "compiler/testData") {
            testClass<AbstractNativeCodegenBoxTest>(
                suiteTestClassName = "NativeCodegenBoxTestGenerated",
                annotations = listOf(
                    codegen(),
                    k1Codegen(),
                    provider<UseExtTestCaseGroupProvider>()
                )
            ) {
                model("codegen/box", targetBackend = TargetBackend.NATIVE)
                model("codegen/boxInline", targetBackend = TargetBackend.NATIVE)
            }
            testClass<AbstractNativeCodegenBoxTest>(
                suiteTestClassName = "NativeCodegenBoxTestNoPLGenerated",
                annotations = listOf(
                    codegen(),
                    k1Codegen(),
                    provider<UseExtTestCaseGroupProvider>(),
                    noPartialLinkage(),
                    noPartialLinkageMayBeSkipped()
                )
            ) {
                model("codegen/box", targetBackend = TargetBackend.NATIVE)
                model("codegen/boxInline", targetBackend = TargetBackend.NATIVE)
            }
            testClass<AbstractNativeCodegenBoxTest>(
                suiteTestClassName = "FirNativeCodegenBoxTestGenerated",
                annotations = listOf(
                    codegenK2(),
                    firCodegen(),
                    provider<UseExtTestCaseGroupProvider>(),
                    provider<FirPipeline>()
                )
            ) {
                model("codegen/box", targetBackend = TargetBackend.NATIVE)
                model("codegen/boxInline", targetBackend = TargetBackend.NATIVE)
            }
            testClass<AbstractNativeCodegenBoxTest>(
                suiteTestClassName = "FirNativeCodegenBoxTestNoPLGenerated",
                annotations = listOf(
                    codegenK2(),
                    firCodegen(),
                    provider<UseExtTestCaseGroupProvider>(),
                    provider<FirPipeline>(),
                    noPartialLinkage(),
                    noPartialLinkageMayBeSkipped()
                )
            ) {
                model("codegen/box", targetBackend = TargetBackend.NATIVE)
                model("codegen/boxInline", targetBackend = TargetBackend.NATIVE)
            }
        }

        // Samples (how to utilize the abilities of new test infrastructure).
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData") {
            testClass<AbstractNativeBlackBoxTest>(
                suiteTestClassName = "InfrastructureTestGenerated",
                annotations = listOf(infrastructure(), k1Infrastructure(), provider<UseStandardTestCaseGroupProvider>())
            ) {
                model("samples")
                model("samples2")
            }
        }

        testGroup("native/native.tests/tests-gen", "native/native.tests/testData") {
            testClass<AbstractNativeBlackBoxTest>(
                suiteTestClassName = "FirInfrastructureTestGenerated",
                annotations = listOf(infrastructure(), firInfrastructure(), provider<UseStandardTestCaseGroupProvider>(), provider<FirPipeline>())
            ) {
                model("samples")
                model("samples2")
            }
        }

        // Partial linkage tests.
        testGroup("native/native.tests/tests-gen", "compiler/testData") {
            testClass<AbstractNativePartialLinkageTest>(
                suiteTestClassName = "NativePartialLinkageTestGenerated"
            ) {
                model("klibABI/", pattern = "^([^_](.+))$", recursive = false)
            }
            testClass<AbstractNativePartialLinkageTest>(
                suiteTestClassName = "FirNativePartialLinkageTestGenerated",
                annotations = listOf(provider<FirPipeline>())
            ) {
                model("klibABI/", pattern = "^([^_](.+))$", recursive = false)
            }
        }

        // KLIB evolution tests.
        testGroup("native/native.tests/tests-gen", "compiler/testData") {
            testClass<AbstractNativeKlibEvolutionTest>(
                suiteTestClassName = "NativeKlibEvolutionTestGenerated"
            ) {
                model("binaryCompatibility/klibEvolution", recursive = false)
            }
            testClass<AbstractNativeKlibEvolutionTest>(
                suiteTestClassName = "FirNativeKlibEvolutionTestGenerated",
                annotations = listOf(provider<FirPipeline>())
            ) {
                model("binaryCompatibility/klibEvolution", recursive = false)
            }
        }

        // CInterop tests.
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData") {
            testClass<AbstractNativeCInteropFModulesTest>(
                suiteTestClassName = "CInteropFModulesTestGenerated"
            ) {
                model("CInterop/simple/simpleDefs", pattern = "^([^_](.+))$", recursive = false)
                model("CInterop/framework/frameworkDefs", pattern = "^([^_](.+))$", recursive = false)
                model("CInterop/framework.macros/macrosDefs", pattern = "^([^_](.+))$", recursive = false)
                model("CInterop/builtins/builtinsDefs", pattern = "^([^_](.+))$", recursive = false)
            }
            testClass<AbstractNativeCInteropNoFModulesTest>(
                suiteTestClassName = "CInteropNoFModulesTestGenerated"
            ) {
                model("CInterop/simple/simpleDefs", pattern = "^([^_](.+))$", recursive = false)
                model("CInterop/framework/frameworkDefs", pattern = "^([^_](.+))$", recursive = false)
                model("CInterop/framework.macros/macrosDefs", pattern = "^([^_](.+))$", recursive = false)
                model("CInterop/builtins/builtinsDefs", pattern = "^([^_](.+))$", recursive = false)
            }
            testClass<AbstractNativeCInteropKT39120Test>(
                suiteTestClassName = "CInteropKT39120TestGenerated"
            ) {
                model("CInterop/KT-39120/defs", pattern = "^([^_](.+))$", recursive = false)
            }
            testClass<AbstractNativeCInteropIncludeCategoriesTest>(
                suiteTestClassName = "CInteropIncludeCategoriesTestGenerated"
            ) {
                model("CInterop/frameworkIncludeCategories/cases", pattern = "^([^_](.+))$", recursive = false)
            }
        }

        // Klib contents tests
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData") {
            testClass<AbstractNativeKlibContentsTest>(
                suiteTestClassName = "NativeKLibContentsTestGenerated",
                annotations = listOf(k1libContents())
            ) {
                model("klibContents", pattern = "^([^_](.+)).kt$", recursive = true)
            }
        }
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData") {
            testClass<AbstractNativeKlibContentsTest>(
                suiteTestClassName = "FirNativeKLibContentsTestGenerated",
                annotations = listOf(k2libContents(), firKLibContents(), provider<FirPipeline>())
            ) {
                model("klibContents", pattern = "^([^_](.+)).kt$", recursive = true)
            }
        }

        // LLDB integration tests.
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData") {
            testClass<AbstractNativeBlackBoxTest>(
                suiteTestClassName = "LldbTestGenerated",
                annotations = listOf(
                    debugger(),
                    provider<UseStandardTestCaseGroupProvider>(),
                    debugOnly(),
                    hostOnly()
                )
            ) {
                model("lldb")
            }
        }
    }
}

private inline fun <reified T : Annotation> provider() = annotation(T::class.java)

private fun debugOnly() = annotation(
    EnforcedProperty::class.java,
    "property" to ClassLevelProperty.OPTIMIZATION_MODE,
    "propertyValue" to "DEBUG"
)

private fun hostOnly() = provider<EnforcedHostTarget>()

private fun noPartialLinkage() = annotation(
    UsePartialLinkage::class.java,
    "mode" to UsePartialLinkage.Mode.DISABLED
)

// This is a special tag to mark codegen box tests with disabled partial linkage that may be skipped in slow TC configurations.
private fun noPartialLinkageMayBeSkipped() = annotation(Tag::class.java, "no-partial-linkage-may-be-skipped")

private fun codegen() = annotation(Tag::class.java, "codegen")
private fun k1Codegen() = annotation(Tag::class.java, "k1Codegen")
private fun codegenK2() = annotation(Tag::class.java, "codegenK2")
private fun firCodegen() = annotation(Tag::class.java, "firCodegen")
private fun debugger() = annotation(Tag::class.java, "debugger")
private fun infrastructure() = annotation(Tag::class.java, "infrastructure")
private fun k1Infrastructure() = annotation(Tag::class.java, "k1Infrastructure")
private fun firInfrastructure() = annotation(Tag::class.java, "firInfrastructure")
private fun k1libContents() = annotation(Tag::class.java, "k1libContents")
private fun k2libContents() = annotation(Tag::class.java, "k2libContents")
private fun firKLibContents() = annotation(Tag::class.java, "firKlibContents")
