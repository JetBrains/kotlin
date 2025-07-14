/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.model.annotation
import org.jetbrains.kotlin.konan.test.abi.AbstractClassicNativeLibraryAbiReaderTest
import org.jetbrains.kotlin.konan.test.abi.AbstractFirNativeLibraryAbiReaderTest
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeCodegenBoxTest
import org.jetbrains.kotlin.konan.test.blackbox.support.ClassLevelProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.KLIB_IR_INLINER
import org.jetbrains.kotlin.konan.test.blackbox.support.TestKind
import org.jetbrains.kotlin.konan.test.blackbox.support.group.ClassicPipeline
import org.jetbrains.kotlin.konan.test.blackbox.support.group.UseExtTestCaseGroupProvider
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.CacheMode
import org.jetbrains.kotlin.konan.test.diagnostics.*
import org.jetbrains.kotlin.konan.test.evolution.AbstractNativeKlibEvolutionTest
import org.jetbrains.kotlin.konan.test.headerklib.*
import org.jetbrains.kotlin.konan.test.irText.*
import org.jetbrains.kotlin.konan.test.dump.*
import org.jetbrains.kotlin.konan.test.klib.AbstractFirKlibCrossCompilationIdentityTest
import org.jetbrains.kotlin.konan.test.klib.AbstractFirKlibCrossCompilationIdentityWithPreSerializationLoweringTest
import org.jetbrains.kotlin.konan.test.serialization.AbstractNativeIrDeserializationTest
import org.jetbrains.kotlin.konan.test.serialization.AbstractNativeIrDeserializationWithInlinedFunInKlibTest
import org.jetbrains.kotlin.konan.test.syntheticAccessors.*
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.utils.CUSTOM_TEST_DATA_EXTENSION_PATTERN
import org.junit.jupiter.api.Tag

fun main() {
    System.setProperty("java.awt.headless", "true")
    val k1BoxTestDir = listOf("multiplatform/k1")

    generateTestGroupSuiteWithJUnit5 {
        // irText tests
        testGroup("native/native.tests/klib-ir-inliner/tests-gen", "compiler/testData/ir/irText") {
            testClass<AbstractClassicNativeIrTextTest>(
                annotations = listOf(*frontendClassic()),
            ) {
                model(excludeDirs = listOf("declarations/multiplatform/k2"))
            }
            testClass<AbstractFirLightTreeNativeIrTextTest>(
                annotations = listOf(*frontendClassic()),
            ) {
                model(excludeDirs = listOf("declarations/multiplatform/k1"))
            }
            testClass<AbstractFirPsiNativeIrTextTest>(
                annotations = listOf(*frontendClassic()),
            ) {
                model(excludeDirs = listOf("declarations/multiplatform/k1"))
            }
        }

        // New frontend test infrastructure tests
        testGroup(testsRoot = "native/native.tests/klib-ir-inliner/tests-gen", testDataRoot = "compiler/testData/diagnostics") {
            testClass<AbstractDiagnosticsNativeTest>(
                annotations = listOf(*frontendClassic()),
            ) {
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
            ) {
                model("nativeTests", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
            }

            testClass<AbstractFirLightTreeNativeDiagnosticsTest>(
                suiteTestClassName = "FirLightTreeOldFrontendNativeDiagnosticsTestGenerated",
            ) {
                model("nativeTests", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
            }

            testClass<AbstractFirPsiNativeDiagnosticsWithBackendTestBase>(
                suiteTestClassName = "FirPsiNativeKlibDiagnosticsTestGenerated",
                annotations = listOf(klib())
            ) {
                model("klibSerializationTests")
                // KT-67300: TODO: extract specialBackendChecks into own test runner, invoking Native backend facade at the end
                model("nativeTests/specialBackendChecks")
            }

            testClass<AbstractFirLightTreeNativeDiagnosticsWithBackendTestBase>(
                suiteTestClassName = "FirLightTreeNativeKlibDiagnosticsTestGenerated",
                annotations = listOf(klib())
            ) {
                model("klibSerializationTests")
                // KT-67300: TODO: extract specialBackendChecks into own test runner, invoking Native backend facade at the end
                model("nativeTests/specialBackendChecks")
            }

            testClass<AbstractFirNativeDiagnosticsWithBackendWithInlinedFunInKlibTestBase>(
                suiteTestClassName = "FirNativeKlibDiagnosticsWithInlinedFunInKlibTestGenerated",
                annotations = listOf(klib())
            ) {
                model("klibSerializationTests")
                // KT-67300: TODO: extract specialBackendChecks into own test runner, invoking Native backend facade at the end
                model("nativeTests/specialBackendChecks")
                model("testsWithAnyBackend")
            }
        }

        // Dump KLIB metadata tests
        testGroup("native/native.tests/klib-ir-inliner/tests-gen", "native/native.tests/testData/klib/dump-metadata") {
            testClass<AbstractNativeKlibDumpMetadataTest>(
                suiteTestClassName = "NativeKlibDumpMetadataTestGenerated",
                annotations = listOf(
                    *frontendClassic()
                )
            ) {
                model(pattern = "^([^_](.+)).kt$", recursive = true)
            }
            testClass<AbstractNativeKlibDumpMetadataTest>(
                suiteTestClassName = "FirNativeKlibDumpMetadataTestGenerated",
            ) {
                model(pattern = "^([^_](.+)).kt$", recursive = true)
            }
        }

        // Dump KLIB IR tests
        testGroup("native/native.tests/klib-ir-inliner/tests-gen", "native/native.tests/testData/klib/dump-ir") {
            testClass<AbstractNativeKlibDumpIrTest>(
                suiteTestClassName = "NativeKlibDumpIrTestGenerated",
                annotations = listOf(
                    *frontendClassic()
                )
            ) {
                model(pattern = "^([^_](.+)).kt$", recursive = true)
            }
            testClass<AbstractNativeKlibDumpIrTest>(
                suiteTestClassName = "FirNativeKlibDumpIrTestGenerated",
            ) {
                model(pattern = "^([^_](.+)).kt$", recursive = true)
            }
        }

        // Dump KLIB IR signatures tests
        testGroup("native/native.tests/klib-ir-inliner/tests-gen", "native/native.tests/testData/klib/dump-signatures") {
            testClass<AbstractNativeKlibDumpIrSignaturesTest>(
                suiteTestClassName = "NativeKlibDumpIrSignaturesTestGenerated",
                annotations = listOf(
                    *frontendClassic()
                )
            ) {
                model(pattern = "^([^_](.+)).kt$", recursive = true)
            }
            testClass<AbstractNativeKlibDumpIrSignaturesTest>(
                suiteTestClassName = "FirNativeKlibDumpIrSignaturesTestGenerated",
            ) {
                model(pattern = "^([^_](.+)).kt$", recursive = true)
            }
        }

        // Dump KLIB metadata signatures tests
        testGroup("native/native.tests/klib-ir-inliner/tests-gen", "native/native.tests/testData/klib/dump-signatures") {
            testClass<AbstractNativeKlibDumpMetadataSignaturesTest>(
                suiteTestClassName = "NativeKlibDumpMetadataSignaturesTestGenerated",
                annotations = listOf(
                    *frontendClassic()
                )
            ) {
                model(pattern = "^([^_](.+)).(kt|def)$", recursive = true)
            }
            testClass<AbstractNativeKlibDumpMetadataSignaturesTest>(
                suiteTestClassName = "FirNativeKlibDumpMetadataSignaturesTestGenerated",
            ) {
                model(pattern = "^([^_](.+)).(kt|def)$", recursive = true)
            }
        }

        // Header klib comparison tests
        testGroup("native/native.tests/klib-ir-inliner/tests-gen", "native/native.tests/testData/klib/header-klibs/comparison") {
            testClass<AbstractNativeHeaderKlibComparisonTest>(
                suiteTestClassName = "NativeHeaderKlibComparisonTestGenerated",
                annotations = listOf(*frontendClassic()),
            ) {
                model(extension = null, recursive = false)
            }
            testClass<AbstractNativeHeaderKlibComparisonTest>(
                suiteTestClassName = "FirNativeHeaderKlibComparisonTestGenerated",
            ) {
                model(extension = null, recursive = false)
            }
        }

        // Header klib compilation tests
        testGroup("native/native.tests/klib-ir-inliner/tests-gen", "native/native.tests/testData/klib/header-klibs/compilation") {
            testClass<AbstractNativeHeaderKlibCompilationTest>(
                suiteTestClassName = "NativeHeaderKlibCompilationTestGenerated",
                annotations = listOf(*frontendClassic()),
            ) {
                model(extension = null, recursive = false)
            }
            testClass<AbstractNativeHeaderKlibCompilationTest>(
                suiteTestClassName = "FirNativeHeaderKlibCompilationTestGenerated",
            ) {
                model(extension = null, recursive = false)
            }
        }

        // KLIB evolution tests.
        testGroup("native/native.tests/klib-ir-inliner/tests-gen", "compiler/testData/klib/evolution") {
            testClass<AbstractNativeKlibEvolutionTest>(
                suiteTestClassName = "NativeKlibEvolutionTestGenerated",
                annotations = listOf(
                    *frontendClassic()
                ),
            ) {
                model(recursive = false)
            }
            testClass<AbstractNativeKlibEvolutionTest>(
                suiteTestClassName = "FirNativeKlibEvolutionTestGenerated",
            ) {
                model(recursive = false)
            }
        }

        // Codegen/box tests for IR Inliner at 1st phase, invoked before K2 Klib Serializer
        testGroup("native/native.tests/klib-ir-inliner/tests-gen", "compiler/testData/codegen") {
            testClass<AbstractNativeCodegenBoxTest>(
                suiteTestClassName = "FirNativeCodegenBoxWithInlinedFunInKlibTestGenerated",
                annotations = listOf(
                    klibIrInliner(),
                    provider<UseExtTestCaseGroupProvider>()
                )
            ) {
                model("box", targetBackend = TargetBackend.NATIVE, excludeDirs = k1BoxTestDir)
                model("boxInline", targetBackend = TargetBackend.NATIVE, excludeDirs = k1BoxTestDir)
            }
            testClass<AbstractNativeIrDeserializationTest> {
                model("box", excludeDirs = k1BoxTestDir, nativeTestInNonNativeTestInfra = true)
                model("boxInline", excludeDirs = k1BoxTestDir, nativeTestInNonNativeTestInfra = true)
            }
            testClass<AbstractNativeIrDeserializationWithInlinedFunInKlibTest> {
                model("box", excludeDirs = k1BoxTestDir, nativeTestInNonNativeTestInfra = true)
                model("boxInline", excludeDirs = k1BoxTestDir, nativeTestInNonNativeTestInfra = true)
            }
        }

        // Codegen/box tests for synthetic accessor tests
        testGroup("native/native.tests/klib-ir-inliner/tests-gen", "compiler/testData/klib/syntheticAccessors") {
            testClass<AbstractNativeCodegenBoxTest>(
                suiteTestClassName = "FirNativeKlibSyntheticAccessorsBoxTestGenerated",
                annotations = listOf(
                    klibIrInliner(),
                    provider<UseExtTestCaseGroupProvider>(),
                )
            ) {
                model(targetBackend = TargetBackend.NATIVE)
            }
        }

        // KLIB synthetic accessor tests.
        testGroup("native/native.tests/klib-ir-inliner/tests-gen", "compiler/testData/klib/syntheticAccessors") {
            testClass<AbstractFirNativeKlibSyntheticAccessorTest>(
                annotations = listOf(
                    *klibSyntheticAccessors(),
                    klibIrInliner(),
                )
            ) {
                model()
            }
        }

        // KLIB cross-compilation tests.
        testGroup("native/native.tests/klib-ir-inliner/tests-gen", "native/native.tests/testData/klib/cross-compilation/identity") {
            testClass<AbstractFirKlibCrossCompilationIdentityTest> {
                model()
            }
            testClass<AbstractFirKlibCrossCompilationIdentityWithPreSerializationLoweringTest> {
                model()
            }
        }
    }

    generateTestGroupSuiteWithJUnit5 {
        testGroup("native/native.tests/klib-ir-inliner/tests-gen", "compiler/testData/klib/dump-abi/content") {
            testClass<AbstractClassicNativeLibraryAbiReaderTest>(
                suiteTestClassName = "ClassicNativeLibraryAbiReaderTestGenerated",
                annotations = listOf(
                    *frontendClassic()
                ),
            ) {
                model()
            }
            testClass<AbstractFirNativeLibraryAbiReaderTest>(
                suiteTestClassName = "FirNativeLibraryAbiReaderTestGenerated",
            ) {
                model()
            }
        }
    }
}

fun frontendClassic() = arrayOf(
    annotation(ClassicPipeline::class.java)
)

private fun klib() = annotation(Tag::class.java, "klib")
fun klibIrInliner() = annotation(Tag::class.java, KLIB_IR_INLINER)
private fun klibSyntheticAccessors() = arrayOf(
    annotation(
        EnforcedProperty::class.java,
        "property" to ClassLevelProperty.TEST_KIND,
        "propertyValue" to TestKind.STANDALONE.name
    ),
    annotation(
        EnforcedProperty::class.java,
        "property" to ClassLevelProperty.CACHE_MODE,
        "propertyValue" to CacheMode.Alias.NO.name
    ),
    provider<UseExtTestCaseGroupProvider>(),
)
