/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.model.annotation
import org.jetbrains.kotlin.konan.test.abi.AbstractNativeLibraryAbiReaderTest
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeCodegenBoxTest
import org.jetbrains.kotlin.konan.test.blackbox.support.ClassLevelProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.KLIB_IR_INLINER
import org.jetbrains.kotlin.konan.test.blackbox.support.TestKind
import org.jetbrains.kotlin.konan.test.blackbox.support.group.UseExtTestCaseGroupProvider
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.CacheMode
import org.jetbrains.kotlin.konan.test.diagnostics.*
import org.jetbrains.kotlin.konan.test.dump.AbstractNativeKlibDumpIrSignaturesTest
import org.jetbrains.kotlin.konan.test.dump.AbstractNativeKlibDumpIrTest
import org.jetbrains.kotlin.konan.test.dump.AbstractNativeKlibDumpMetadataSignaturesTest
import org.jetbrains.kotlin.konan.test.dump.AbstractNativeKlibDumpMetadataTest
import org.jetbrains.kotlin.konan.test.headerklib.AbstractNativeHeaderKlibComparisonTest
import org.jetbrains.kotlin.konan.test.headerklib.AbstractNativeHeaderKlibCompilationTest
import org.jetbrains.kotlin.konan.test.irText.AbstractLightTreeNativeIrTextTest
import org.jetbrains.kotlin.konan.test.irText.AbstractPsiNativeIrTextTest
import org.jetbrains.kotlin.konan.test.klib.AbstractKlibCrossCompilationIdentityTest
import org.jetbrains.kotlin.konan.test.klib.AbstractKlibCrossCompilationIdentityWithPreSerializationLoweringTest
import org.jetbrains.kotlin.konan.test.serialization.AbstractNativeIrDeserializationTest
import org.jetbrains.kotlin.konan.test.serialization.AbstractNativeIrDeserializationWithInlinedFunInKlibTest
import org.jetbrains.kotlin.konan.test.syntheticAccessors.AbstractNativeKlibSyntheticAccessorTest
import org.jetbrains.kotlin.konan.test.dump.AbstractNativeLoadCompiledKotlinTest
import org.junit.jupiter.api.Tag

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")
    val k1BoxTestDir = listOf("multiplatform/k1")
    val testsRoot = args[0]

    generateTestGroupSuiteWithJUnit5(args) {
        // irText tests
        testGroup(testsRoot, "compiler/testData/ir/irText") {
            testClass<AbstractLightTreeNativeIrTextTest> {
                model()
            }
            testClass<AbstractPsiNativeIrTextTest> {
                model()
            }
        }

        // New frontend test infrastructure tests
        testGroup(testsRoot = testsRoot, testDataRoot = "compiler/testData/diagnostics") {
            testClass<AbstractPsiNativeDiagnosticsWithBackendTestBase>(
                suiteTestClassName = "PsiNativeKlibDiagnosticsTestGenerated",
                annotations = listOf(klib())
            ) {
                model("klibSerializationTests")
                // KT-67300: TODO: extract specialBackendChecks into own test runner, invoking Native backend facade at the end
                model("nativeTests")
            }

            testClass<AbstractLightTreeNativeDiagnosticsWithBackendTestBase>(
                suiteTestClassName = "LightTreeNativeKlibDiagnosticsTestGenerated",
                annotations = listOf(klib())
            ) {
                model("klibSerializationTests")
                // KT-67300: TODO: extract specialBackendChecks into own test runner, invoking Native backend facade at the end
                model("nativeTests")
            }

            testClass<AbstractNativeDiagnosticsWithBackendWithInlinedFunInKlibTestBase>(
                suiteTestClassName = "NativeKlibDiagnosticsWithInlinedFunInKlibTestGenerated",
                annotations = listOf(klib())
            ) {
                model("klibSerializationTests")
                // KT-67300: TODO: extract specialBackendChecks into own test runner, invoking Native backend facade at the end
                model("nativeTests")
                model("testsWithAnyBackend")
            }
        }

        // Dump KLIB metadata tests
        testGroup(testsRoot, "native/native.tests/testData/klib/dump-metadata") {
            testClass<AbstractNativeKlibDumpMetadataTest> {
                model(pattern = "^([^_](.+)).kt$", recursive = true)
            }
        }

        // Dump KLIB IR tests
        testGroup(testsRoot, "native/native.tests/testData/klib/dump-ir") {
            testClass<AbstractNativeKlibDumpIrTest> {
                model(pattern = "^([^_](.+)).kt$", recursive = true)
            }
        }

        // Dump KLIB IR signatures tests
        testGroup(testsRoot, "native/native.tests/testData/klib/dump-signatures") {
            testClass<AbstractNativeKlibDumpIrSignaturesTest> {
                model(pattern = "^([^_](.+)).kt$", recursive = true)
            }
        }

        // Dump KLIB metadata signatures tests
        testGroup(testsRoot, "native/native.tests/testData/klib/dump-signatures") {
            testClass<AbstractNativeKlibDumpMetadataSignaturesTest> {
                model(pattern = "^([^_](.+)).(kt|def)$", recursive = true)
            }
        }

        // Header klib comparison tests
        testGroup(testsRoot, "native/native.tests/testData/klib/header-klibs/comparison") {
            testClass<AbstractNativeHeaderKlibComparisonTest> {
                model(extension = null, recursive = false)
            }
        }

        // Header klib compilation tests
        testGroup(testsRoot, "native/native.tests/testData/klib/header-klibs/compilation") {
            testClass<AbstractNativeHeaderKlibCompilationTest> {
                model(extension = null, recursive = false)
            }
        }

        // Codegen/box tests for IR Inliner at 1st phase, invoked before K2 Klib Serializer
        testGroup(testsRoot, "compiler/testData/codegen") {
            testClass<AbstractNativeCodegenBoxTest>(
                suiteTestClassName = "NativeCodegenBoxWithInlinedFunInKlibTestGenerated",
                annotations = listOf(
                    klibIrInliner(),
                    provider<UseExtTestCaseGroupProvider>()
                )
            ) {
                model("box", excludeDirs = k1BoxTestDir)
                model("boxInline")
            }
            testClass<AbstractNativeIrDeserializationTest> {
                model("box", excludeDirs = k1BoxTestDir)
                model("boxInline")
            }
            testClass<AbstractNativeIrDeserializationWithInlinedFunInKlibTest> {
                model("box", excludeDirs = k1BoxTestDir)
                model("boxInline")
            }
        }

        // Codegen/box tests for synthetic accessor tests
        testGroup(testsRoot, "compiler/testData/klib/syntheticAccessors") {
            testClass<AbstractNativeCodegenBoxTest>(
                suiteTestClassName = "NativeKlibSyntheticAccessorsBoxTestGenerated",
                annotations = listOf(
                    klibIrInliner(),
                    provider<UseExtTestCaseGroupProvider>(),
                )
            ) {
                model()
            }
        }

        // KLIB synthetic accessor tests.
        testGroup(testsRoot, "compiler/testData/klib/syntheticAccessors") {
            testClass<AbstractNativeKlibSyntheticAccessorTest>(
                annotations = listOf(
                    *klibSyntheticAccessors(),
                    klibIrInliner(),
                )
            ) {
                model()
            }
        }

        // KLIB cross-compilation tests.
        testGroup(testsRoot, "native/native.tests/testData/klib/cross-compilation/identity") {
            testClass<AbstractKlibCrossCompilationIdentityTest> {
                model()
            }
            testClass<AbstractKlibCrossCompilationIdentityWithPreSerializationLoweringTest> {
                model()
            }
        }

        testGroup(testsRoot, "compiler/testData/klib/dump-abi/content") {
            testClass<AbstractNativeLibraryAbiReaderTest> {
                model()
            }
        }

        testGroup(testsRoot, "compiler/testData/loadJava", testRunnerMethodName = "runTest0") {
            testClass<AbstractNativeLoadCompiledKotlinTest> {
                model("compiledKotlin", extension = "kt")
                model("compiledKotlinWithStdlib", extension = "kt")
            }
        }
    }
}

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
