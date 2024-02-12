/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.TestGroup
import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.model.AnnotationModel
import org.jetbrains.kotlin.generators.model.annotation
import org.jetbrains.kotlin.konan.test.blackbox.*
import org.jetbrains.kotlin.konan.test.blackbox.support.ClassLevelProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedHostTarget
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.group.*
import org.jetbrains.kotlin.konan.test.diagnostics.AbstractDiagnosticsNativeTest
import org.jetbrains.kotlin.konan.test.diagnostics.AbstractFirLightTreeNativeDiagnosticsTest
import org.jetbrains.kotlin.konan.test.diagnostics.AbstractFirPsiNativeDiagnosticsTest
import org.jetbrains.kotlin.konan.test.irtext.AbstractClassicNativeIrTextTest
import org.jetbrains.kotlin.konan.test.irtext.AbstractFirLightTreeNativeIrTextTest
import org.jetbrains.kotlin.konan.test.irtext.AbstractFirPsiNativeIrTextTest
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.utils.CUSTOM_TEST_DATA_EXTENSION_PATTERN
import org.junit.jupiter.api.Tag

fun main() {
    System.setProperty("java.awt.headless", "true")
    val k2BoxTestDir = listOf("multiplatform/k2")

    generateTestGroupSuiteWithJUnit5 {
        // Former konan local tests
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData") {
            testClass<AbstractNativeCodegenBoxTest>(
                suiteTestClassName = "NativeCodegenLocalTestGenerated",
                annotations = listOf(
                    provider<UseExtTestCaseGroupProvider>(),
                )
            ) {
                model("codegen", targetBackend = TargetBackend.NATIVE)
            }
            testClass<AbstractNativeCodegenBoxTest>(
                suiteTestClassName = "FirNativeCodegenLocalTestGenerated",
                annotations = listOf(
                    *frontendFir(),
                    provider<UseExtTestCaseGroupProvider>()
                )
            ) {
                model("codegen", targetBackend = TargetBackend.NATIVE)
            }
        }

        // Codegen box tests.
        testGroup("native/native.tests/tests-gen", "compiler/testData") {
            testClass<AbstractNativeCodegenBoxTest>(
                suiteTestClassName = "NativeCodegenBoxTestGenerated",
                annotations = listOf(
                    provider<UseExtTestCaseGroupProvider>(),
                )
            ) {
                model("codegen/box", targetBackend = TargetBackend.NATIVE, excludeDirs = k2BoxTestDir)
                model("codegen/boxInline", targetBackend = TargetBackend.NATIVE)
            }
            testClass<AbstractNativeCodegenBoxTest>(
                suiteTestClassName = "NativeCodegenBoxTestNoPLGenerated",
                annotations = listOf(
                    provider<UseExtTestCaseGroupProvider>(),
                    *noPartialLinkage()
                )
            ) {
                model("codegen/box", targetBackend = TargetBackend.NATIVE, excludeDirs = k2BoxTestDir)
                model("codegen/boxInline", targetBackend = TargetBackend.NATIVE)
            }
            testClass<AbstractNativeCodegenBoxTest>(
                suiteTestClassName = "FirNativeCodegenBoxTestGenerated",
                annotations = listOf(
                    *frontendFir(),
                    provider<UseExtTestCaseGroupProvider>()
                )
            ) {
                model("codegen/box", targetBackend = TargetBackend.NATIVE)
                model("codegen/boxInline", targetBackend = TargetBackend.NATIVE)
            }
            testClass<AbstractNativeCodegenBoxTest>(
                suiteTestClassName = "FirNativeCodegenBoxTestNoPLGenerated",
                annotations = listOf(
                    *frontendFir(),
                    provider<UseExtTestCaseGroupProvider>(),
                    *noPartialLinkage()
                )
            ) {
                model("codegen/box", targetBackend = TargetBackend.NATIVE)
                model("codegen/boxInline", targetBackend = TargetBackend.NATIVE)
            }
            testClass<AbstractClassicNativeIrTextTest> {
                model(
                    "ir/irText",
                    excludeDirs = listOf("declarations/multiplatform/k2")
                )
            }
            testClass<AbstractFirLightTreeNativeIrTextTest> {
                model(
                    "ir/irText",
                    excludeDirs = listOf("declarations/multiplatform/k1")
                )
            }
            testClass<AbstractFirPsiNativeIrTextTest> {
                model(
                    "ir/irText",
                    excludeDirs = listOf("declarations/multiplatform/k1")
                )
            }
        }

        // Samples (how to utilize the abilities of new test infrastructure).
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData") {
            testClass<AbstractNativeBlackBoxTest>(
                suiteTestClassName = "InfrastructureTestGenerated",
                annotations = listOf(
                    infrastructure(),
                    provider<UseStandardTestCaseGroupProvider>()
                )
            ) {
                model("samples")
                model("samples2")
            }
        }

        testGroup("native/native.tests/tests-gen", "native/native.tests/testData") {
            testClass<AbstractNativeBlackBoxTest>(
                suiteTestClassName = "FirInfrastructureTestGenerated",
                annotations = listOf(
                    infrastructure(),
                    *frontendFir(),
                    provider<UseStandardTestCaseGroupProvider>()
                )
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
                model("klib/partial-linkage/", pattern = "^([^_](.+))$", recursive = false)
            }
            testClass<AbstractNativePartialLinkageTest>(
                suiteTestClassName = "FirNativePartialLinkageTestGenerated",
                annotations = listOf(
                    *frontendFir()
                )
            ) {
                model("klib/partial-linkage/", pattern = "^([^_](.+))$", recursive = false)
            }
        }

        // KLIB evolution tests.
        testGroup("native/native.tests/tests-gen", "compiler/testData") {
            testClass<AbstractNativeKlibEvolutionTest>(
                suiteTestClassName = "NativeKlibEvolutionTestGenerated"
            ) {
                model("klib/evolution", recursive = false)
            }
            testClass<AbstractNativeKlibEvolutionTest>(
                suiteTestClassName = "FirNativeKlibEvolutionTestGenerated",
                annotations = listOf(
                    *frontendFir()
                )
            ) {
                model("klib/evolution", recursive = false)
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

            testClass<AbstractNativeCInteropExperimentalTest>(
                suiteTestClassName = "CInteropExperimentalTestGenerated"
            ) {
                model("CInterop/experimental/cases", pattern = "^([^_](.+))$", recursive = false)
            }
        }

        // ObjCExport tests.
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData") {
            testClass<AbstractNativeObjCExportTest>(
                suiteTestClassName = "ObjCExportTestGenerated"
            ) {
                model("ObjCExport", pattern = "^([^_](.+))$", recursive = false)
            }
            testClass<AbstractNativeObjCExportTest>(
                suiteTestClassName = "FirObjCExportTestGenerated",
                annotations = listOf(
                    *frontendFir()
                ),
            ) {
                model("ObjCExport", pattern = "^([^_](.+))$", recursive = false)
            }
        }

        // Dump KLIB metadata tests
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData") {
            testClass<AbstractNativeKlibDumpMetadataTest>(
                suiteTestClassName = "NativeKlibDumpMetadataTestGenerated"
            ) {
                model("klib/dump-metadata", pattern = "^([^_](.+)).kt$", recursive = true)
            }
        }
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData") {
            testClass<AbstractNativeKlibDumpMetadataTest>(
                suiteTestClassName = "FirNativeKlibDumpMetadataTestGenerated",
                annotations = listOf(
                    *frontendFir()
                )
            ) {
                model("klib/dump-metadata", pattern = "^([^_](.+)).kt$", recursive = true)
            }
        }

        // Dump KLIB IR tests
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData") {
            testClass<AbstractNativeKlibDumpIrTest>(
                suiteTestClassName = "NativeKlibDumpIrTestGenerated",
            ) {
                model("klib/dump-ir", pattern = "^([^_](.+)).kt$", recursive = true)
            }
        }
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData") {
            testClass<AbstractNativeKlibDumpIrTest>(
                suiteTestClassName = "FirNativeKlibDumpIrTestGenerated",
                annotations = listOf(
                    *frontendFir()
                )
            ) {
                model("klib/dump-ir", pattern = "^([^_](.+)).kt$", recursive = true)
            }
        }

        // Dump KLIB IR signatures tests
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData") {
            testClass<AbstractNativeKlibDumpIrSignaturesTest>(
                suiteTestClassName = "NativeKlibDumpIrSignaturesTestGenerated",
            ) {
                model("klib/dump-signatures", pattern = "^([^_](.+)).kt$", recursive = true)
            }
        }
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData") {
            testClass<AbstractNativeKlibDumpIrSignaturesTest>(
                suiteTestClassName = "FirNativeKlibDumpIrSignaturesTestGenerated",
                annotations = listOf(
                    *frontendFir()
                )
            ) {
                model("klib/dump-signatures", pattern = "^([^_](.+)).kt$", recursive = true)
            }
        }

        // Dump KLIB metadata signatures tests
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData") {
            testClass<AbstractNativeKlibDumpMetadataSignaturesTest>(
                suiteTestClassName = "NativeKlibDumpMetadataSignaturesTestGenerated",
            ) {
                model("klib/dump-signatures", pattern = "^([^_](.+)).(kt|def)$", recursive = true)
            }
        }
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData") {
            testClass<AbstractNativeKlibDumpMetadataSignaturesTest>(
                suiteTestClassName = "FirNativeKlibDumpMetadataSignaturesTestGenerated",
                annotations = listOf(
                    *frontendFir()
                )
            ) {
                model("klib/dump-signatures", pattern = "^([^_](.+)).(kt|def)$", recursive = true)
            }
        }

        // LLDB integration tests.
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData") {
            testClass<AbstractNativeBlackBoxTest>(
                suiteTestClassName = "LldbTestGenerated",
                annotations = listOf(
                    debugger(),
                    provider<UseStandardTestCaseGroupProvider>(),
                    forceDebugMode(),
                    forceHostTarget()
                )
            ) {
                model("lldb")
            }
        }
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData") {
            testClass<AbstractNativeBlackBoxTest>(
                suiteTestClassName = "FirLldbTestGenerated",
                annotations = listOf(
                    debugger(),
                    provider<UseStandardTestCaseGroupProvider>(),
                    forceDebugMode(),
                    forceHostTarget(),
                    *frontendFir()
                )
            ) {
                model("lldb")
            }
        }

        // New frontend test infrastructure tests
        testGroup(testsRoot = "native/native.tests/tests-gen", testDataRoot = "compiler/testData") {
            testClass<AbstractDiagnosticsNativeTest> {
                model(
                    "diagnostics/nativeTests",
                    excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN,
                    // There are no special native-specific diagnostics in K1 frontend.
                    // These checks happen in native backend instead, in SpecialBackendChecks class.
                    excludeDirs = listOf("specialBackendChecks"),
                )
            }

            testClass<AbstractFirPsiNativeDiagnosticsTest>(
                suiteTestClassName = "FirPsiOldFrontendNativeDiagnosticsTestGenerated",
                annotations = listOf(*frontendFir()),
            ) {
                model("diagnostics/nativeTests", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
            }

            testClass<AbstractFirLightTreeNativeDiagnosticsTest>(
                suiteTestClassName = "FirLightTreeOldFrontendNativeDiagnosticsTestGenerated",
                annotations = listOf(*frontendFir()),
            ) {
                model("diagnostics/nativeTests", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
            }
        }

        // Atomicfu compiler plugin native tests.
        testGroup("plugins/atomicfu/atomicfu-compiler/test", "plugins/atomicfu/atomicfu-compiler/testData") {
            testClass<AbstractNativeBlackBoxTest>(
                suiteTestClassName = "AtomicfuNativeTestGenerated",
                annotations = listOf(atomicfuNative(), provider<UseStandardTestCaseGroupProvider>())
            ) {
                model("nativeBox")
            }
        }

        generateTestGroupSuiteWithJUnit5 {
            testGroup("native/native.tests/tests-gen", "compiler/testData/klib/dump-abi") {
                testClass<AbstractNativeLibraryAbiReaderTest>(
                    suiteTestClassName = "NativeLibraryAbiReaderTest"
                ) {
                    model("content", targetBackend = TargetBackend.NATIVE)
                }
            }
            testGroup("native/native.tests/tests-gen", "compiler/testData/klib/dump-abi") {
                testClass<AbstractNativeLibraryAbiReaderTest>(
                    suiteTestClassName = "FirNativeLibraryAbiReaderTest",
                    annotations = listOf(
                        *frontendFir()
                    )
                ) {
                    model("content", targetBackend = TargetBackend.NATIVE)
                }
            }

            testGroup("native/native.tests/tests-gen", "compiler/testData/klib/dump-abi") {
                testClass<AbstractNativeCInteropLibraryAbiReaderTest>(
                    suiteTestClassName = "NativeCInteropLibraryAbiReaderTest"
                ) {
                    model("cinterop")
                }
            }
            testGroup("native/native.tests/tests-gen", "compiler/testData/klib/dump-abi") {
                testClass<AbstractNativeCInteropLibraryAbiReaderTest>(
                    suiteTestClassName = "FirNativeCInteropLibraryAbiReaderTest",
                    annotations = listOf(
                        *frontendFir()
                    )
                ) {
                    model("cinterop")
                }
            }
        }

        // Header klib comparison tests
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData") {
            testClass<AbstractNativeHeaderKlibComparisonTest>(
                suiteTestClassName = "NativeHeaderKlibComparisonTestGenerated",
            ) {
                model("klib/header-klibs/comparison", extension = null, recursive = false)
            }
            testClass<AbstractNativeHeaderKlibComparisonTest>(
                suiteTestClassName = "FirNativeHeaderKlibComparisonTestGenerated",
                annotations = listOf(*frontendFir()),
            ) {
                model("klib/header-klibs/comparison", extension = null, recursive = false)
            }
        }

        // Header klib compilation tests
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData") {
            testClass<AbstractNativeHeaderKlibCompilationTest>(
                suiteTestClassName = "NativeHeaderKlibCompilationTestGenerated",
            ) {
                model("klib/header-klibs/compilation", extension = null, recursive = false)
            }
            testClass<AbstractNativeHeaderKlibCompilationTest>(
                suiteTestClassName = "FirNativeHeaderKlibCompilationTestGenerated",
                annotations = listOf(*frontendFir()),
            ) {
                model("klib/header-klibs/compilation", extension = null, recursive = false)
            }
        }

        // Plain executable tests
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData") {
            testClass<AbstractNativeBlackBoxTest>(
                suiteTestClassName = "NativeStandaloneTestGenerated",
                annotations = listOf(
                    *standalone(),
                    provider<UseStandardTestCaseGroupProvider>(),
                )
            ) {
                model("standalone")
            }
            testClass<AbstractNativeBlackBoxTest>(
                suiteTestClassName = "FirNativeStandaloneTestGenerated",
                annotations = listOf(
                    *standalone(),
                    provider<UseStandardTestCaseGroupProvider>(),
                    *frontendFir(),
                )
            ) {
                model("standalone")
            }
        }
        // C Export
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData") {
            testClass<AbstractNativeCExportDynamicTest>(
                suiteTestClassName = "CExportTestDynamicGenerated"
            ) {
                model("CExport", pattern = "^([^_](.+))$", recursive = false)
            }
            testClass<AbstractNativeCExportDynamicTest>(
                suiteTestClassName = "FirCExportTestDynamicGenerated",
                annotations = listOf(
                    *frontendFir()
                ),
            ) {
                model("CExport", pattern = "^([^_](.+))$", recursive = false)
            }
            testClass<AbstractNativeCExportStaticTest>(
                suiteTestClassName = "CExportTestStaticGenerated"
            ) {
                model("CExport", pattern = "^([^_](.+))$", recursive = false)
            }
            testClass<AbstractNativeCExportStaticTest>(
                suiteTestClassName = "FirCExportTestStaticGenerated",
                annotations = listOf(
                    *frontendFir()
                ),
            ) {
                model("CExport", pattern = "^([^_](.+))$", recursive = false)
            }
        }
        // Swift Export
        testGroup(
            "native/native.tests/tests-gen/",
            "native/swift/swift-export-standalone/testData"
        ) {
            testClass<SwiftTypeCheckBaseTest>(
                suiteTestClassName = "TypeCheckSwiftExportGoldenData"
            ) {
                model("", extension = "swift", recursive = true)
            }
        }
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData") {
            testClass<AbstractNativeSwiftExportTest>(
                suiteTestClassName = "FirSwiftExportTestGenerated",
                annotations = listOf(
                    *frontendFir()
                ),
            ) {
                model("SwiftExport", pattern = "^([^_](.+))$", recursive = false)
            }
            testClass<AbstractNativeSwiftExportTest>(
                suiteTestClassName = "SwiftExportTestGenerated",
            ) {
                model("SwiftExport", pattern = "^([^_](.+))$", recursive = false)
            }
        }
    }
}

private inline fun <reified T : Annotation> provider() = annotation(T::class.java)

private fun forceDebugMode() = annotation(
    EnforcedProperty::class.java,
    "property" to ClassLevelProperty.OPTIMIZATION_MODE,
    "propertyValue" to "DEBUG"
)

private fun forceHostTarget() = annotation(EnforcedHostTarget::class.java)

private fun noPartialLinkage() = arrayOf(
    annotation(UsePartialLinkage::class.java, "mode" to UsePartialLinkage.Mode.DISABLED),
    // This is a special tag to mark codegen box tests with disabled partial linkage that may be skipped in slow TC configurations:
    annotation(Tag::class.java, "no-partial-linkage-may-be-skipped")
)

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

private fun frontendFir() = arrayOf(
    annotation(Tag::class.java, "frontend-fir"),
    annotation(FirPipeline::class.java)
)

private fun debugger() = annotation(Tag::class.java, "debugger")
private fun infrastructure() = annotation(Tag::class.java, "infrastructure")
private fun k1libContents() = annotation(Tag::class.java, "k1libContents")
private fun k2libContents() = annotation(Tag::class.java, "k2libContents")
private fun atomicfuNative() = annotation(Tag::class.java, "atomicfu-native")
private fun standalone() = arrayOf(
    annotation(Tag::class.java, "standalone"),
    annotation(
        EnforcedProperty::class.java,
        "property" to ClassLevelProperty.TEST_KIND,
        "propertyValue" to "STANDALONE_NO_TR"
    )
)
