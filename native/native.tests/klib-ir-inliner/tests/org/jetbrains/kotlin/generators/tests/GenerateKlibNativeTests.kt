/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.model.annotation
import org.jetbrains.kotlin.konan.test.blackbox.support.group.FirPipeline
import org.jetbrains.kotlin.konan.test.diagnostics.*
import org.jetbrains.kotlin.konan.test.inlining.AbstractNativeUnboundIrSerializationTest
import org.jetbrains.kotlin.konan.test.irText.*
import org.jetbrains.kotlin.konan.test.dump.*
import org.jetbrains.kotlin.test.utils.CUSTOM_TEST_DATA_EXTENSION_PATTERN
import org.junit.jupiter.api.Tag

fun main() {
    System.setProperty("java.awt.headless", "true")
    val k1BoxTestDir = listOf("multiplatform/k1")

    generateTestGroupSuiteWithJUnit5 {
        // irText tests
        testGroup("native/native.tests/klib-ir-inliner/tests-gen", "compiler/testData/ir/irText") {
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

        // New frontend test infrastructure tests
        testGroup(testsRoot = "native/native.tests/klib-ir-inliner/tests-gen", testDataRoot = "compiler/testData/diagnostics") {
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

        // New frontend test infrastructure tests
        testGroup(testsRoot = "native/native.tests/klib-ir-inliner/tests-gen", testDataRoot = "compiler/testData") {
            testClass<AbstractNativeUnboundIrSerializationTest>(
                suiteTestClassName = "NativeUnboundIrSerializationTestGenerated",
                annotations = listOf(*frontendFir(), klib())
            ) {
                /*
                 * Note: "all-files" tests are consciously not generated to have a more clear picture of test coverage:
                 * - Some test data files don't have inline functions. There is basically nothing to test in them.
                 *   So, such tests end up in "ignored" (gray) state.
                 * - The tests that fail are "failed" (red).
                 * - Successful tests (with really processed inline functions) are "successful" (green).
                 */
                model("codegen/box", skipTestAllFilesCheck = true, excludeDirs = k1BoxTestDir)
                model("codegen/boxInline", skipTestAllFilesCheck = true)
            }
        }

        // Dump KLIB metadata tests
        testGroup("native/native.tests/klib-ir-inliner/tests-gen", "native/native.tests/testData/klib/dump-metadata") {
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
        testGroup("native/native.tests/klib-ir-inliner/tests-gen", "native/native.tests/testData/klib/dump-ir") {
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
        testGroup("native/native.tests/klib-ir-inliner/tests-gen", "native/native.tests/testData/klib/dump-signatures") {
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
        testGroup("native/native.tests/klib-ir-inliner/tests-gen", "native/native.tests/testData/klib/dump-signatures") {
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


    }
}

fun frontendFir() = arrayOf(
    annotation(Tag::class.java, "frontend-fir"),
    annotation(FirPipeline::class.java)
)

private fun klib() = annotation(Tag::class.java, "klib")
