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
import org.jetbrains.kotlin.konan.test.diagnostics.*
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
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData/codegen") {
            testClass<AbstractNativeCodegenBoxTest>(
                suiteTestClassName = "NativeCodegenLocalTestGenerated",
                annotations = listOf(
                    provider<UseExtTestCaseGroupProvider>(),
                )
            ) {
                model(targetBackend = TargetBackend.NATIVE)
            }
            testClass<AbstractNativeCodegenBoxTest>(
                suiteTestClassName = "FirNativeCodegenLocalTestGenerated",
                annotations = listOf(
                    *frontendFir(),
                    provider<UseExtTestCaseGroupProvider>()
                )
            ) {
                model(targetBackend = TargetBackend.NATIVE)
            }
        }

        // Codegen box tests.
        testGroup("native/native.tests/tests-gen", "compiler/testData/codegen") {
            testClass<AbstractNativeCodegenBoxTest>(
                suiteTestClassName = "NativeCodegenBoxTestGenerated",
                annotations = listOf(
                    provider<UseExtTestCaseGroupProvider>(),
                )
            ) {
                model("box", targetBackend = TargetBackend.NATIVE, excludeDirs = k2BoxTestDir)
                model("boxInline", targetBackend = TargetBackend.NATIVE)
            }
            testClass<AbstractNativeCodegenBoxTest>(
                suiteTestClassName = "NativeCodegenBoxTestNoPLGenerated",
                annotations = listOf(
                    provider<UseExtTestCaseGroupProvider>(),
                    *noPartialLinkage()
                )
            ) {
                model("box", targetBackend = TargetBackend.NATIVE, excludeDirs = k2BoxTestDir)
                model("boxInline", targetBackend = TargetBackend.NATIVE)
            }
            testClass<AbstractNativeCodegenBoxTest>(
                suiteTestClassName = "FirNativeCodegenBoxTestGenerated",
                annotations = listOf(
                    *frontendFir(),
                    provider<UseExtTestCaseGroupProvider>()
                )
            ) {
                model("box", targetBackend = TargetBackend.NATIVE)
                model("boxInline", targetBackend = TargetBackend.NATIVE)
            }
            testClass<AbstractNativeCodegenBoxTest>(
                suiteTestClassName = "FirNativeCodegenBoxTestNoPLGenerated",
                annotations = listOf(
                    *frontendFir(),
                    provider<UseExtTestCaseGroupProvider>(),
                    *noPartialLinkage()
                )
            ) {
                model("box", targetBackend = TargetBackend.NATIVE)
                model("boxInline", targetBackend = TargetBackend.NATIVE)
            }
        }

        // irText tests
        testGroup("native/native.tests/tests-gen", "compiler/testData/ir/irText") {
            testClass<AbstractClassicNativeIrTextTest> {
                model(excludeDirs = listOf("declarations/multiplatform/k2"))
            }
            testClass<AbstractFirLightTreeNativeIrTextTest> {
                model(excludeDirs = listOf("declarations/multiplatform/k1"))
            }
            testClass<AbstractFirPsiNativeIrTextTest> {
                model(excludeDirs = listOf("declarations/multiplatform/k1"))
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
        testGroup("native/native.tests/tests-gen", "compiler/testData/klib/partial-linkage") {
            testClass<AbstractNativePartialLinkageTest>(
                suiteTestClassName = "NativePartialLinkageTestGenerated"
            ) {
                model(pattern = "^([^_](.+))$", recursive = false)
            }
            testClass<AbstractNativePartialLinkageTest>(
                suiteTestClassName = "FirNativePartialLinkageTestGenerated",
                annotations = listOf(
                    *frontendFir()
                )
            ) {
                model(pattern = "^([^_](.+))$", recursive = false)
            }
        }

        // Klib Compatibility tests.
        testGroup("native/native.tests/klib-compatibility/tests-gen", "compiler/testData/klib/versionCompatibility") {
            testClass<AbstractNativeKlibCompatibilityTest>(
                suiteTestClassName = "NativeKlibCompatibilityTestGenerated"
            ) {
                model(pattern = "^([^_](.+))$", recursive = false)
            }
            testClass<AbstractNativeKlibCompatibilityTest>(
                suiteTestClassName = "FirNativeKlibCompatibilityTestGenerated",
                annotations = listOf(
                    *frontendFir()
                )
            ) {
                model(pattern = "^([^_](.+))$", recursive = false)
            }
        }

        // KLIB evolution tests.
        testGroup("native/native.tests/tests-gen", "compiler/testData/klib/evolution") {
            testClass<AbstractNativeKlibEvolutionTest>(
                suiteTestClassName = "NativeKlibEvolutionTestGenerated"
            ) {
                model(recursive = false)
            }
            testClass<AbstractNativeKlibEvolutionTest>(
                suiteTestClassName = "FirNativeKlibEvolutionTestGenerated",
                annotations = listOf(
                    *frontendFir()
                )
            ) {
                model(recursive = false)
            }
        }

        // KLIB synthetic accessor tests.
        testGroup("native/native.tests/tests-gen", "compiler/testData/klib/syntheticAccessors") {
            testClass<AbstractNativeKlibSyntheticAccessorTest>(
                suiteTestClassName = "ClassicNativeKlibSyntheticAccessorTestGenerated",
                annotations = listOf(
                    *klibSyntheticAccessors(),
                )
            ) {
                model(targetBackend = TargetBackend.NATIVE)
            }
            testClass<AbstractNativeKlibSyntheticAccessorTest>(
                suiteTestClassName = "FirNativeKlibSyntheticAccessorTestGenerated",
                annotations = listOf(
                    *klibSyntheticAccessors(),
                    *frontendFir(),
                )
            ) {
                model(targetBackend = TargetBackend.NATIVE)
            }
        }

        // CInterop tests.
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData/CInterop") {
            testClass<AbstractNativeCInteropFModulesTest>(
                suiteTestClassName = "CInteropFModulesTestGenerated"
            ) {
                model("simple/simpleDefs", pattern = "^([^_](.+))$", recursive = false)
                model("framework/frameworkDefs", pattern = "^([^_](.+))$", recursive = false)
                model("framework.macros/macrosDefs", pattern = "^([^_](.+))$", recursive = false)
                model("builtins/builtinsDefs", pattern = "^([^_](.+))$", recursive = false)
            }
            testClass<AbstractNativeCInteropNoFModulesTest>(
                suiteTestClassName = "CInteropNoFModulesTestGenerated"
            ) {
                model("simple/simpleDefs", pattern = "^([^_](.+))$", recursive = false)
                model("framework/frameworkDefs", pattern = "^([^_](.+))$", recursive = false)
                model("framework.macros/macrosDefs", pattern = "^([^_](.+))$", recursive = false)
                model("builtins/builtinsDefs", pattern = "^([^_](.+))$", recursive = false)
            }
            testClass<AbstractNativeCInteropKT39120Test>(
                suiteTestClassName = "CInteropKT39120TestGenerated"
            ) {
                model("KT-39120/defs", pattern = "^([^_](.+))$", recursive = false)
            }
            testClass<AbstractNativeCInteropIncludeCategoriesTest>(
                suiteTestClassName = "CInteropIncludeCategoriesTestGenerated"
            ) {
                model("frameworkIncludeCategories/cases", pattern = "^([^_](.+))$", recursive = false)
            }

            testClass<AbstractNativeCInteropExperimentalTest>(
                suiteTestClassName = "CInteropExperimentalTestGenerated"
            ) {
                model("experimental/cases", pattern = "^([^_](.+))$", recursive = false)
            }
        }

        // ObjCExport tests.
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData/ObjCExport") {
            testClass<AbstractNativeObjCExportTest>(
                suiteTestClassName = "ObjCExportTestGenerated"
            ) {
                model(pattern = "^([^_](.+))$", recursive = false)
            }
            testClass<AbstractNativeObjCExportTest>(
                suiteTestClassName = "FirObjCExportTestGenerated",
                annotations = listOf(
                    *frontendFir()
                ),
            ) {
                model(pattern = "^([^_](.+))$", recursive = false)
            }
        }

        // Dump KLIB metadata tests
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData/klib/dump-metadata") {
            testClass<AbstractNativeKlibDumpMetadataTest>(
                suiteTestClassName = "NativeKlibDumpMetadataTestGenerated"
            ) {
                model(pattern = "^([^_](.+)).kt$", recursive = true)
            }
            testClass<AbstractNativeKlibDumpMetadataTest>(
                suiteTestClassName = "FirNativeKlibDumpMetadataTestGenerated",
                annotations = listOf(
                    *frontendFir()
                )
            ) {
                model(pattern = "^([^_](.+)).kt$", recursive = true)
            }
        }

        // Dump KLIB IR tests
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData/klib/dump-ir") {
            testClass<AbstractNativeKlibDumpIrTest>(
                suiteTestClassName = "NativeKlibDumpIrTestGenerated",
            ) {
                model(pattern = "^([^_](.+)).kt$", recursive = true)
            }
            testClass<AbstractNativeKlibDumpIrTest>(
                suiteTestClassName = "FirNativeKlibDumpIrTestGenerated",
                annotations = listOf(
                    *frontendFir()
                )
            ) {
                model(pattern = "^([^_](.+)).kt$", recursive = true)
            }
        }

        // Dump KLIB IR signatures tests
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData/klib/dump-signatures") {
            testClass<AbstractNativeKlibDumpIrSignaturesTest>(
                suiteTestClassName = "NativeKlibDumpIrSignaturesTestGenerated",
            ) {
                model(pattern = "^([^_](.+)).kt$", recursive = true)
            }
            testClass<AbstractNativeKlibDumpIrSignaturesTest>(
                suiteTestClassName = "FirNativeKlibDumpIrSignaturesTestGenerated",
                annotations = listOf(
                    *frontendFir()
                )
            ) {
                model(pattern = "^([^_](.+)).kt$", recursive = true)
            }
        }

        // Dump KLIB metadata signatures tests
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData/klib/dump-signatures") {
            testClass<AbstractNativeKlibDumpMetadataSignaturesTest>(
                suiteTestClassName = "NativeKlibDumpMetadataSignaturesTestGenerated",
            ) {
                model(pattern = "^([^_](.+)).(kt|def)$", recursive = true)
            }
            testClass<AbstractNativeKlibDumpMetadataSignaturesTest>(
                suiteTestClassName = "FirNativeKlibDumpMetadataSignaturesTestGenerated",
                annotations = listOf(
                    *frontendFir()
                )
            ) {
                model(pattern = "^([^_](.+)).(kt|def)$", recursive = true)
            }
        }

        // LLDB integration tests.
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData/lldb") {
            testClass<AbstractNativeBlackBoxTest>(
                suiteTestClassName = "LldbTestGenerated",
                annotations = listOf(
                    debugger(),
                    provider<UseStandardTestCaseGroupProvider>(),
                    forceDebugMode(),
                    forceHostTarget()
                )
            ) {
                model()
            }
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
                model()
            }
        }

        // New frontend test infrastructure tests
        testGroup(testsRoot = "native/native.tests/tests-gen", testDataRoot = "compiler/testData/diagnostics") {
            testClass<AbstractDiagnosticsNativeTest> {
                model(
                    "nativeTests",
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
                model("nativeTests", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
            }

            testClass<AbstractFirLightTreeNativeDiagnosticsTest>(
                suiteTestClassName = "FirLightTreeOldFrontendNativeDiagnosticsTestGenerated",
                annotations = listOf(*frontendFir()),
            ) {
                model("nativeTests", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
            }

            testClass<AbstractFirPsiNativeDiagnosticsWithBackendTestBase>(
                suiteTestClassName = "FirPsiNativeKlibDiagnosticsTestGenerated",
                annotations = listOf(*frontendFir(), klib())
            ) {
                model("klibSerializationTests")
                // KT-67300: TODO: extract specialBackendChecks into own test runner, invoking Native backend facade at the end
                model("nativeTests/specialBackendChecks")
            }

            testClass<AbstractFirLightTreeNativeDiagnosticsWithBackendTestBase>(
                suiteTestClassName = "FirLightTreeNativeKlibDiagnosticsTestGenerated",
                annotations = listOf(*frontendFir(), klib())
            ) {
                model("klibSerializationTests")
                // KT-67300: TODO: extract specialBackendChecks into own test runner, invoking Native backend facade at the end
                model("nativeTests/specialBackendChecks")
            }
        }

        // Atomicfu compiler plugin native tests.
        testGroup("plugins/atomicfu/atomicfu-compiler/test", "plugins/atomicfu/atomicfu-compiler/testData/nativeBox") {
            testClass<AbstractNativeBlackBoxTest>(
                suiteTestClassName = "AtomicfuNativeTestGenerated",
                annotations = listOf(*atomicfuNative(), provider<UseStandardTestCaseGroupProvider>())
            ) {
                model()
            }
        }

        generateTestGroupSuiteWithJUnit5 {
            testGroup("native/native.tests/tests-gen", "compiler/testData/klib/dump-abi/content") {
                testClass<AbstractNativeLibraryAbiReaderTest>(
                    suiteTestClassName = "NativeLibraryAbiReaderTest"
                ) {
                    model(targetBackend = TargetBackend.NATIVE)
                }
                testClass<AbstractNativeLibraryAbiReaderTest>(
                    suiteTestClassName = "FirNativeLibraryAbiReaderTest",
                    annotations = listOf(
                        *frontendFir()
                    )
                ) {
                    model(targetBackend = TargetBackend.NATIVE)
                }
            }

            testGroup("native/native.tests/tests-gen", "compiler/testData/klib/dump-abi/cinterop") {
                testClass<AbstractNativeCInteropLibraryAbiReaderTest>(
                    suiteTestClassName = "NativeCInteropLibraryAbiReaderTest"
                ) {
                    model()
                }
                testClass<AbstractNativeCInteropLibraryAbiReaderTest>(
                    suiteTestClassName = "FirNativeCInteropLibraryAbiReaderTest",
                    annotations = listOf(
                        *frontendFir()
                    )
                ) {
                    model()
                }
            }
        }

        // Header klib comparison tests
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData/klib/header-klibs/comparison") {
            testClass<AbstractNativeHeaderKlibComparisonTest>(
                suiteTestClassName = "NativeHeaderKlibComparisonTestGenerated",
            ) {
                model(extension = null, recursive = false)
            }
            testClass<AbstractNativeHeaderKlibComparisonTest>(
                suiteTestClassName = "FirNativeHeaderKlibComparisonTestGenerated",
                annotations = listOf(*frontendFir()),
            ) {
                model(extension = null, recursive = false)
            }
        }

        // Header klib compilation tests
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData/klib/header-klibs/compilation") {
            testClass<AbstractNativeHeaderKlibCompilationTest>(
                suiteTestClassName = "NativeHeaderKlibCompilationTestGenerated",
            ) {
                model(extension = null, recursive = false)
            }
            testClass<AbstractNativeHeaderKlibCompilationTest>(
                suiteTestClassName = "FirNativeHeaderKlibCompilationTestGenerated",
                annotations = listOf(*frontendFir()),
            ) {
                model(extension = null, recursive = false)
            }
        }

        // Plain executable tests
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData/standalone") {
            testClass<AbstractNativeBlackBoxTest>(
                suiteTestClassName = "NativeStandaloneTestGenerated",
                annotations = listOf(
                    *standalone(),
                    provider<UseStandardTestCaseGroupProvider>(),
                )
            ) {
                model()
            }
            testClass<AbstractNativeBlackBoxTest>(
                suiteTestClassName = "FirNativeStandaloneTestGenerated",
                annotations = listOf(
                    *standalone(),
                    provider<UseStandardTestCaseGroupProvider>(),
                    *frontendFir(),
                )
            ) {
                model()
            }
        }
        val binaryLibraryKinds = mapOf(
            "Static" to binaryLibraryKind("STATIC"),
            "Dynamic" to binaryLibraryKind("DYNAMIC"),
        )
        val frontendFlags = mapOf(
            "Classic" to arrayOf(),
            "Fir" to frontendFir(),
        )
        // C Export
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData/CExport") {
            val cinterfaceModes = mapOf(
                "InterfaceV1" to cinterfaceMode("V1"),
                "InterfaceNone" to cinterfaceMode("NONE")
            )
            binaryLibraryKinds.forEach { binaryKind ->
                frontendFlags.forEach { frontend ->
                    cinterfaceModes.forEach { cinterfaceMode ->
                        val frontendKey = if (frontend.key == "Classic") "" else frontend.key
                        val suiteTestClassName = "${frontendKey}CExport${binaryKind.key}${cinterfaceMode.key}TestGenerated"
                        testClass<AbstractNativeCExportTest>(
                            suiteTestClassName,
                            annotations = listOf(
                                binaryKind.value,
                                cinterfaceMode.value,
                                *frontend.value
                            )
                        ) {
                            model(cinterfaceMode.key, pattern = "^([^_](.+))$", recursive = false)
                        }
                    }
                }
            }
        }
        // Swift Export
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData/SwiftExport") {
            testClass<AbstractNativeSwiftExportExecutionTest>(
                suiteTestClassName = "SwiftExportExecutionTestGenerated",
                annotations = listOf(
                    *frontendFir(),
                    provider<UseStandardTestCaseGroupProvider>(),
                ),
            ) {
                model(pattern = "^([^_](.+))$", recursive = false)
            }
        }
        // Stress tests
        testGroup("native/native.tests/stress/tests-gen", "native/native.tests/stress/testData") {
            testClass<AbstractNativeBlackBoxTest>(
                suiteTestClassName = "NativeStressTestGenerated",
                annotations = listOf(
                    *stress(),
                    provider<UseStandardTestCaseGroupProvider>(),
                )
            ) {
                model()
            }
            testClass<AbstractNativeBlackBoxTest>(
                suiteTestClassName = "FirNativeStressTestGenerated",
                annotations = listOf(
                    *stress(),
                    provider<UseStandardTestCaseGroupProvider>(),
                    *frontendFir(),
                )
            ) {
                model()
            }
        }
        // GC tests
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData/gc") {
            testClass<AbstractNativeBlackBoxTest>(
                suiteTestClassName = "NativeGCTestGenerated",
                annotations = listOf(
                    *gc(),
                    provider<UseStandardTestCaseGroupProvider>(),
                )
            ) {
                model()
            }
            testClass<AbstractNativeBlackBoxTest>(
                suiteTestClassName = "FirNativeGCTestGenerated",
                annotations = listOf(
                    *gc(),
                    provider<UseStandardTestCaseGroupProvider>(),
                    *frontendFir(),
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

fun frontendFir() = arrayOf(
    annotation(Tag::class.java, "frontend-fir"),
    annotation(FirPipeline::class.java)
)

private fun klib() = annotation(Tag::class.java, "klib")
private fun debugger() = annotation(Tag::class.java, "debugger")
private fun infrastructure() = annotation(Tag::class.java, "infrastructure")
private fun atomicfuNative() = arrayOf(
    annotation(Tag::class.java, "atomicfu-native"),
    annotation(EnforcedHostTarget::class.java), // TODO(KT-65977): Make atomicfu tests run on all targets.
)
private fun standalone() = arrayOf(
    annotation(Tag::class.java, "standalone"),
    annotation(
        EnforcedProperty::class.java,
        "property" to ClassLevelProperty.TEST_KIND,
        "propertyValue" to "STANDALONE_NO_TR"
    )
)

private fun klibSyntheticAccessors() = arrayOf(
    annotation(
        EnforcedProperty::class.java,
        "property" to ClassLevelProperty.TEST_KIND,
        "propertyValue" to "STANDALONE"
    ),
    provider<UseExtTestCaseGroupProvider>(),
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
private fun stress() = arrayOf(
    annotation(Tag::class.java, "stress"),
    annotation(
        EnforcedProperty::class.java,
        "property" to ClassLevelProperty.EXECUTION_TIMEOUT,
        "propertyValue" to "15m"
    )
)