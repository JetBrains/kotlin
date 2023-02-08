/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.model.annotation
import org.jetbrains.kotlin.konan.blackboxtest.*
import org.jetbrains.kotlin.konan.blackboxtest.support.ClassLevelProperty
import org.jetbrains.kotlin.konan.blackboxtest.support.EnforcedProperty
import org.jetbrains.kotlin.konan.blackboxtest.support.group.K2Pipeline
import org.jetbrains.kotlin.konan.blackboxtest.support.group.UseExtTestCaseGroupProvider
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
                annotations = listOf(codegen(), provider<UseExtTestCaseGroupProvider>())
            ) {
                model("codegen/box", targetBackend = TargetBackend.NATIVE)
                model("codegen/boxInline", targetBackend = TargetBackend.NATIVE)
            }
        }

        testGroup("native/native.tests/tests-gen", "compiler/testData") {
            testClass<AbstractNativeCodegenBoxTest>(
                suiteTestClassName = "K2NativeCodegenBoxTestGenerated",
                annotations = listOf(codegenK2(), provider<UseExtTestCaseGroupProvider>(), provider<K2Pipeline>())
            ) {
                model("codegen/box", targetBackend = TargetBackend.NATIVE)
                model("codegen/boxInline", targetBackend = TargetBackend.NATIVE)
            }
        }

        // Samples (how to utilize the abilities of new test infrastructure).
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData") {
            testClass<AbstractNativeBlackBoxTest>(
                suiteTestClassName = "InfrastructureTestGenerated",
                annotations = listOf(infrastructure(), provider<UseStandardTestCaseGroupProvider>())
            ) {
                model("samples")
                model("samples2")
            }
        }

        // KLIB ABI tests.
        testGroup("native/native.tests/tests-gen", "compiler/testData") {
            testClass<AbstractNativeKlibABITest>(
                suiteTestClassName = "KlibABITestGenerated"
            ) {
                model("klibABI/", pattern = "^([^_](.+))$", recursive = false)
            }
        }

        // KLIB binary compatibility tests.
        testGroup("native/native.tests/tests-gen", "compiler/testData") {
            testClass<AbstractNativeKlibBinaryCompatibilityTest>(
                suiteTestClassName = "KlibBinaryCompatibilityTestGenerated"
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
                suiteTestClassName = "NativeK1LibContentsTestGenerated",
                annotations = listOf(k1libContents())
            ) {
                model("klibContents", pattern = "^([^_](.+)).kt$", recursive = true)
            }
        }
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData") {
            testClass<AbstractNativeKlibContentsTest>(
                suiteTestClassName = "NativeK2LibContentsTestGenerated",
                annotations = listOf(k2libContents(), provider<K2Pipeline>())
            ) {
                model("klibContents", pattern = "^([^_](.+)).kt$", recursive = true)
            }
        }

        // LLDB integration tests.
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData") {
            testClass<AbstractNativeBlackBoxTest>(
                suiteTestClassName = "LldbTestGenerated",
                annotations = listOf(debugger(), provider<UseStandardTestCaseGroupProvider>(), debugOnly())
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

private fun codegen() = annotation(Tag::class.java, "codegen")
private fun codegenK2() = annotation(Tag::class.java, "codegenK2")
private fun debugger() = annotation(Tag::class.java, "debugger")
private fun infrastructure() = annotation(Tag::class.java, "infrastructure")
private fun k1libContents() = annotation(Tag::class.java, "k1libContents")
private fun k2libContents() = annotation(Tag::class.java, "k2libContents")
