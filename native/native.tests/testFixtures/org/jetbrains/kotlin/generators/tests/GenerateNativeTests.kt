/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.dsl.TestGroup
import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.model.AnnotationModel
import org.jetbrains.kotlin.generators.model.annotation
import org.jetbrains.kotlin.konan.test.blackbox.*
import org.jetbrains.kotlin.konan.test.blackbox.support.ClassLevelProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedHostTarget
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.KLIB_IR_INLINER
import org.jetbrains.kotlin.konan.test.blackbox.support.group.*
import org.junit.jupiter.api.Tag

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    val testsRoot = args[0]
    generateTestGroupSuiteWithJUnit5(args) {
        // Former konan local tests
        testGroup(testsRoot, "native/native.tests/testData/codegen") {
            testClass<AbstractNativeCodegenBoxTest>(
                suiteTestClassName = "FirNativeCodegenLocalTestGenerated",
                annotations = listOf(
                    provider<UseExtTestCaseGroupProvider>(),
                )
            ) {
                model()
            }
            testClass<AbstractNativeCodegenBoxTest>(
                suiteTestClassName = "FirNativeCodegenLocalTestWithInlinedFunInKlibGenerated",
                annotations = listOf(
                    klibIrInliner(),
                    provider<UseExtTestCaseGroupProvider>()
                )
            ) {
                model()
            }
        }

        // Samples (how to utilize the abilities of new test infrastructure).
        testGroup(testsRoot, "native/native.tests/testData") {
            testClass<AbstractNativeBlackBoxTest>(
                suiteTestClassName = "FirInfrastructureTestGenerated",
                annotations = listOf(
                    infrastructure(),
                    provider<UseStandardTestCaseGroupProvider>()
                )
            ) {
                model("samples")
                model("samples2")
            }
        }

        // Partial linkage tests.
        testGroup(testsRoot, "compiler/testData/klib/partial-linkage") {
            testClass<AbstractNativePartialLinkageTest>(
                suiteTestClassName = "NativePartialLinkageTestGenerated",
            ) {
                model(pattern = "^([^_](.+))$", recursive = false)
            }
        }

        // CInterop tests.
        testGroup(testsRoot, "native/native.tests/testData/CInterop") {
            testClass<AbstractNativeCInteropFModulesTest>(
                suiteTestClassName = "CInteropFModulesTestGenerated",
            ) {
                model("simple/simpleDefs", pattern = "^([^_](.+))$", recursive = false)
                model("framework/frameworkDefs", pattern = "^([^_](.+))$", recursive = false)
                model("framework.macros/macrosDefs", pattern = "^([^_](.+))$", recursive = false)
                model("builtins/builtinsDefs", pattern = "^([^_](.+))$", recursive = false)
                model("cCallMode/cCallMode", pattern = "^([^_](.+))$", recursive = false)
            }
            testClass<AbstractNativeCInteropNoFModulesTest>(
                suiteTestClassName = "CInteropNoFModulesTestGenerated",
            ) {
                model("simple/simpleDefs", pattern = "^([^_](.+))$", recursive = false)
                model("framework/frameworkDefs", pattern = "^([^_](.+))$", recursive = false)
                model("framework.macros/macrosDefs", pattern = "^([^_](.+))$", recursive = false)
                model("builtins/builtinsDefs", pattern = "^([^_](.+))$", recursive = false)
                model("cCallMode/cCallMode", pattern = "^([^_](.+))$", recursive = false)
            }
            testClass<AbstractNativeCInteropKT39120Test>(
                suiteTestClassName = "CInteropKT39120TestGenerated",
            ) {
                model("KT-39120/defs", pattern = "^([^_](.+))$", recursive = false)
            }
            testClass<AbstractNativeCInteropIncludeCategoriesTest>(
                suiteTestClassName = "CInteropIncludeCategoriesTestGenerated",
            ) {
                model("frameworkIncludeCategories/cases", pattern = "^([^_](.+))$", recursive = false)
            }

            testClass<AbstractNativeCInteropExperimentalTest>(
                suiteTestClassName = "CInteropExperimentalTestGenerated",
            ) {
                model("experimental/cases", pattern = "^([^_](.+))$", recursive = false)
            }
        }

        // ObjCExport tests.
        testGroup(testsRoot, "native/native.tests/testData/ObjCExport") {
            testClass<AbstractNativeObjCExportTest>(
                suiteTestClassName = "FirObjCExportTestGenerated",
            ) {
                model(pattern = "^([^_](.+))$", recursive = false)
            }
        }

        // LLDB integration tests.
        testGroup(testsRoot, "native/native.tests/testData/lldb") {
            testClass<AbstractNativeBlackBoxTest>(
                suiteTestClassName = "FirLldbTestGenerated",
                annotations = listOf(
                    debugger(),
                    provider<UseStandardTestCaseGroupProvider>(),
                    forceDebugMode(),
                    forceHostTarget(),
                )
            ) {
                model()
            }
        }

        testGroup(testsRoot, "compiler/testData/klib/dump-abi/cinterop") {
            testClass<AbstractNativeCInteropLibraryAbiReaderTest>(
                suiteTestClassName = "FirNativeCInteropLibraryAbiReaderTest",
            ) {
                model()
            }
        }

        // Plain executable tests
        testGroup(testsRoot, "native/native.tests/testData/standalone") {
            testClass<AbstractNativeBlackBoxTest>(
                suiteTestClassName = "FirNativeStandaloneTestGenerated",
                annotations = listOf(
                    *standalone(),
                    provider<UseStandardTestCaseGroupProvider>(),
                )
            ) {
                model()
            }
            testClass<AbstractNativeBlackBoxTest>(
                suiteTestClassName = "FirNativeStandaloneTestWithInlinedFunInKlibGenerated",
                annotations = listOf(
                    *standalone(),
                    provider<UseStandardTestCaseGroupProvider>(),
                    klibIrInliner(),
                )
            ) {
                model()
            }
        }
        val binaryLibraryKinds = mapOf(
            "Static" to binaryLibraryKind("STATIC"),
            "Dynamic" to binaryLibraryKind("DYNAMIC"),
        )
        // C Export
        testGroup(testsRoot, "native/native.tests/testData/CExport") {
            val cinterfaceModes = mapOf(
                "InterfaceV1" to cinterfaceMode("V1"),
                "InterfaceNone" to cinterfaceMode("NONE")
            )
            binaryLibraryKinds.forEach { binaryKind ->
                cinterfaceModes.forEach { cinterfaceMode ->
                    val suiteTestClassName = "FirCExport${binaryKind.key}${cinterfaceMode.key}TestGenerated"
                    testClass<AbstractNativeCExportTest>(
                        suiteTestClassName,
                        annotations = listOf(
                            binaryKind.value,
                            cinterfaceMode.value,
                        )
                    ) {
                        model(cinterfaceMode.key, pattern = "^([^_](.+))$", recursive = false)
                    }
                }
            }
            testClass<AbstractNativeCExportInterfaceV1HeaderTest>(
                "CExportInterfaceV1HeaderTestGenerated",
            ) {
                model("InterfaceV1HeaderTests")
            }
        }
        // GC tests
        testGroup(testsRoot, "native/native.tests/testData/gc") {
            testClass<AbstractNativeBlackBoxTest>(
                suiteTestClassName = "FirNativeGCTestGenerated",
                annotations = listOf(
                    *gc(),
                    provider<UseStandardTestCaseGroupProvider>(),
                )
            ) {
                model()
            }
        }
    }
}


inline fun <reified T : Annotation> provider() = annotation(T::class.java)

private fun forceDebugMode() = annotation(
    EnforcedProperty::class.java,
    "property" to ClassLevelProperty.OPTIMIZATION_MODE,
    "propertyValue" to "DEBUG"
)

fun forceHostTarget() = annotation(EnforcedHostTarget::class.java)

// The concrete tests disabled in one-stage mode.
@Suppress("SameParameterValue")
private fun TestGroup.disabledInOneStageMode(vararg unexpandedPaths: String): AnnotationModel {
    require(unexpandedPaths.isNotEmpty()) { "No unexpanded paths specified" }

    return annotation(
        DisabledTestsIfProperty::class.java,
        "sourceLocations" to unexpandedPaths.map { unexpandedPath -> "$testDataRoot/$unexpandedPath" }.toTypedArray(),
        "property" to ClassLevelProperty.TEST_MODE,
        "propertyValue" to "ONE_STAGE_MULTI_MODULE"
    )
}

private fun debugger() = annotation(Tag::class.java, "debugger")
private fun infrastructure() = annotation(Tag::class.java, "infrastructure")
fun standalone() = arrayOf(
    annotation(Tag::class.java, "standalone"),
    annotation(
        EnforcedProperty::class.java,
        "property" to ClassLevelProperty.TEST_KIND,
        "propertyValue" to "STANDALONE_NO_TR"
    )
)

private fun binaryLibraryKind(kind: String = "DYNAMIC") = annotation(
    EnforcedProperty::class.java,
    "property" to ClassLevelProperty.BINARY_LIBRARY_KIND,
    "propertyValue" to kind
)
private fun cinterfaceMode(mode: String = "V1") = annotation(
    EnforcedProperty::class.java,
    "property" to ClassLevelProperty.C_INTERFACE_MODE,
    "propertyValue" to mode
)
private fun gc() = arrayOf(
    annotation(Tag::class.java, "gc"),
)
fun klibIrInliner() = annotation(Tag::class.java, KLIB_IR_INLINER)
