/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.TestGroup
import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.model.AnnotationModel
import org.jetbrains.kotlin.generators.model.annotation
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.konan.test.blackbox.*
import org.jetbrains.kotlin.konan.test.blackbox.support.ClassLevelProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedHostTarget
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.group.*
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.CompatibilityTestMode
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.PipelineType
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.TestMode
import org.jetbrains.kotlin.konan.test.klib.AbstractFirKlibCrossCompilationIdentityTest
import org.jetbrains.kotlin.test.TargetBackend
import org.junit.jupiter.api.Tag
import java.io.File

fun main() {
    System.setProperty("java.awt.headless", "true")
    val k1BoxTestDir = listOf("multiplatform/k1")
    val k2BoxTestDir = listOf("multiplatform/k2")

    generateSources()

    generateTestGroupSuiteWithJUnit5 {
        // Former konan local tests
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData/codegen") {
            testClass<AbstractNativeCodegenBoxTest>(
                suiteTestClassName = "NativeCodegenLocalTestGenerated",
                annotations = listOf(
                    *frontendClassic(),
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
                    *frontendClassic(),
                    provider<UseExtTestCaseGroupProvider>(),
                )
            ) {
                model("box", targetBackend = TargetBackend.NATIVE, excludeDirs = k2BoxTestDir)
                model("boxInline", targetBackend = TargetBackend.NATIVE)
            }
            testClass<AbstractNativeCodegenBoxTest>(
                suiteTestClassName = "NativeCodegenBoxTestNoPLGenerated",
                annotations = listOf(
                    *frontendClassic(),
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
                model("box", targetBackend = TargetBackend.NATIVE, excludeDirs = k1BoxTestDir)
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
                model("box", targetBackend = TargetBackend.NATIVE, excludeDirs = k1BoxTestDir)
                model("boxInline", targetBackend = TargetBackend.NATIVE)
            }
        }

        // Samples (how to utilize the abilities of new test infrastructure).
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData") {
            testClass<AbstractNativeBlackBoxTest>(
                suiteTestClassName = "InfrastructureTestGenerated",
                annotations = listOf(
                    *frontendClassic(),
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
                suiteTestClassName = "NativePartialLinkageTestGenerated",
                annotations = listOf(
                    *frontendClassic(),
                )
            ) {
                model(pattern = "^([^_](.+))$", recursive = false)
            }
            testClass<AbstractNativePartialLinkageTest>(
                suiteTestClassName = "FirNativePartialLinkageTestGenerated",
                annotations = listOf(
                    *frontendFir(),
                )
            ) {
                model(pattern = "^([^_](.+))$", recursive = false)
            }
        }

        // Klib Compatibility tests.
        testGroup("native/native.tests/klib-compatibility/tests-gen", "compiler/testData/klib/versionCompatibility") {
            testClass<AbstractNativeKlibCompatibilityTest>(
                suiteTestClassName = "NativeKlibCompatibilityTestGenerated",
                annotations = listOf(
                    *frontendClassic(),
                )
            ) {
                model(pattern = "^([^_](.+))$", recursive = false)
            }
            testClass<AbstractNativeKlibCompatibilityTest>(
                suiteTestClassName = "FirNativeKlibCompatibilityTestGenerated",
                annotations = listOf(
                    *frontendFir(),
                )
            ) {
                model(pattern = "^([^_](.+))$", recursive = false)
            }
        }

        // Klib backward/forward compatibility tests.
        // TODO: After 2.2.0 release, generate also backward/forward compatibility tests for "klib/syntheticAccessors"
        testGroup("native/native.tests/klib-compatibility/tests-gen", "compiler/testData/codegen") {
            testClass<AbstractNativeCodegenBoxTest>(
                suiteTestClassName = "FirNativeCodegenBoxBackwardCompatibilityTestGenerated",
                annotations = listOf(
                    *compatibilityTestMode(CompatibilityTestMode.BACKWARD),
                    *frontendFir(),
                    provider<UseExtTestCaseGroupProvider>()
                )
            ) {
                model("box", targetBackend = TargetBackend.NATIVE, excludeDirs = k1BoxTestDir)
                model("boxInline", targetBackend = TargetBackend.NATIVE)
            }
        }

        // KLIB cross-compilation tests.
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData/klib/cross-compilation/identity") {
            testClass<AbstractFirKlibCrossCompilationIdentityTest>(
                annotations = listOf(
                    *frontendFir(),
                )
            ) {
                model()
            }
        }

        // CInterop tests.
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData/CInterop") {
            testClass<AbstractNativeCInteropFModulesTest>(
                suiteTestClassName = "CInteropFModulesTestGenerated",
                annotations = listOf(
                    *frontendClassic(),
                )
            ) {
                model("simple/simpleDefs", pattern = "^([^_](.+))$", recursive = false)
                model("framework/frameworkDefs", pattern = "^([^_](.+))$", recursive = false)
                model("framework.macros/macrosDefs", pattern = "^([^_](.+))$", recursive = false)
                model("builtins/builtinsDefs", pattern = "^([^_](.+))$", recursive = false)
            }
            testClass<AbstractNativeCInteropNoFModulesTest>(
                suiteTestClassName = "CInteropNoFModulesTestGenerated",
                annotations = listOf(
                    *frontendClassic(),
                )
            ) {
                model("simple/simpleDefs", pattern = "^([^_](.+))$", recursive = false)
                model("framework/frameworkDefs", pattern = "^([^_](.+))$", recursive = false)
                model("framework.macros/macrosDefs", pattern = "^([^_](.+))$", recursive = false)
                model("builtins/builtinsDefs", pattern = "^([^_](.+))$", recursive = false)
            }
            testClass<AbstractNativeCInteropKT39120Test>(
                suiteTestClassName = "CInteropKT39120TestGenerated",
                annotations = listOf(
                    *frontendFir()
                ),
            ) {
                model("KT-39120/defs", pattern = "^([^_](.+))$", recursive = false)
            }
            testClass<AbstractNativeCInteropIncludeCategoriesTest>(
                suiteTestClassName = "CInteropIncludeCategoriesTestGenerated",
                annotations = listOf(
                    *frontendClassic(),
                )
            ) {
                model("frameworkIncludeCategories/cases", pattern = "^([^_](.+))$", recursive = false)
            }

            testClass<AbstractNativeCInteropExperimentalTest>(
                suiteTestClassName = "CInteropExperimentalTestGenerated",
                annotations = listOf(
                    *frontendClassic(),
                )
            ) {
                model("experimental/cases", pattern = "^([^_](.+))$", recursive = false)
            }
        }

        // ObjCExport tests.
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData/ObjCExport") {
            testClass<AbstractNativeObjCExportTest>(
                suiteTestClassName = "ObjCExportTestGenerated",
                annotations = listOf(
                    *frontendClassic(),
                ),
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

        // LLDB integration tests.
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData/lldb") {
            testClass<AbstractNativeBlackBoxTest>(
                suiteTestClassName = "LldbTestGenerated",
                annotations = listOf(
                    debugger(),
                    provider<UseStandardTestCaseGroupProvider>(),
                    forceDebugMode(),
                    forceHostTarget(),
                    *frontendClassic(),
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

        // Atomicfu compiler plugin native tests.
        testGroup("plugins/atomicfu/atomicfu-compiler/test", "plugins/atomicfu/atomicfu-compiler/testData/box") {
            testClass<AbstractNativeCodegenBoxTest>(
                suiteTestClassName = "AtomicfuNativeTestGenerated",
                annotations = listOf(*atomicfuNative(), *frontendClassic(), provider<UseExtTestCaseGroupProvider>())
            ) {
                model(targetBackend = TargetBackend.NATIVE)
            }
            testClass<AbstractNativeCodegenBoxTest>(
                suiteTestClassName = "AtomicfuNativeFirTestGenerated",
                annotations = listOf(*atomicfuNative(), *frontendFir(), provider<UseExtTestCaseGroupProvider>())
            ) {
                model(targetBackend = TargetBackend.NATIVE)
            }
        }

        // LitmusKt tests.
        testGroup("native/native.tests/litmus-tests/tests-gen", "native/native.tests/litmus-tests/testData") {
            testClass<AbstractNativeBlackBoxTest>(
                suiteTestClassName = "LitmusKtTestsGenerated",
                annotations = listOf(
                    litmusktNative(),
                    provider<UseStandardTestCaseGroupProvider>(),
                    forceHostTarget(),
                    *frontendClassic(),
                )
            ) {
                model("standalone")
            }
            testClass<AbstractNativeBlackBoxTest>(
                suiteTestClassName = "FirLitmusKtTestsGenerated",
                annotations = listOf(
                    litmusktNative(),
                    provider<UseStandardTestCaseGroupProvider>(),
                    forceHostTarget(),
                    *frontendFir(),
                )
            ) {
                model("standalone")
            }
        }

        generateTestGroupSuiteWithJUnit5 {
            testGroup("native/native.tests/tests-gen", "compiler/testData/klib/dump-abi/cinterop") {
                testClass<AbstractNativeCInteropLibraryAbiReaderTest>(
                    suiteTestClassName = "NativeCInteropLibraryAbiReaderTest",
                    annotations = listOf(
                        *frontendClassic(),
                    )
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

        // Plain executable tests
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData/standalone") {
            testClass<AbstractNativeBlackBoxTest>(
                suiteTestClassName = "NativeStandaloneTestGenerated",
                annotations = listOf(
                    *standalone(),
                    provider<UseStandardTestCaseGroupProvider>(),
                    *frontendClassic(),
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
            "Classic" to frontendClassic(),
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
            testClass<AbstractNativeCExportInterfaceV1HeaderTest>(
                "CExportInterfaceV1HeaderTestGenerated",
                annotations = listOf(
                    *frontendFir(),
                )
            ) {
                model("InterfaceV1HeaderTests")
            }
        }
        // Stress tests
        testGroup("native/native.tests/stress/tests-gen", "native/native.tests/stress/testData") {
            testClass<AbstractNativeBlackBoxTest>(
                suiteTestClassName = "NativeStressTestGenerated",
                annotations = listOf(
                    *stress(),
                    provider<UseStandardTestCaseGroupProvider>(),
                    *frontendClassic(),
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
                    *frontendClassic(),
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

private fun generateSources() {
    generateKt62920StressTest()
}

private fun generateKt62920StressTest() {
    val rootDir = File("native/native.tests/stress/testData")
    rootDir.resolve("kt62920.kt").writeText(buildString {
        val maxStage = 1000
        val threadsCount = 10
        // workaround hack: please keep the string below indented, otherwise the `testCompareAll()` incorrectly handles the "// FILE:" directive
        appendLine(
            """
            // This file is generated by ${TestGeneratorUtil.getMainClassName()}. DO NOT MODIFY MANUALLY
            // KIND: STANDALONE_NO_TR
            // MODULE: cinterop
            // FILE: objclib.def
            language = Objective-C
            ---
            #include <objc/NSObject.h>

            void useObject(id) {}

            // MODULE: main(cinterop)
            // FILE: main.kt
            @file:OptIn(kotlin.ExperimentalStdlibApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

            import objclib.*

            import kotlin.concurrent.AtomicInt
            import kotlin.concurrent.AtomicIntArray
            import kotlin.native.concurrent.*
            """.trimIndent()
        )

        // Define all the classes
        (1..maxStage).forEach {
            appendLine("class C$it")
        }

        // Actual test procedure
        appendLine(
            """
            const val MAX_STAGE = $maxStage

            val canRunStage = AtomicInt(0)
            val hasRunStage = AtomicIntArray(MAX_STAGE + 1)

            fun test() {
                hasRunStage.getAndIncrement(0)
            """.trimIndent()
        )

        // Define all test cases
        (1..maxStage).forEach {
            appendLine(
                """

                while (canRunStage.value != $it) {}
                useObject(C$it())
                hasRunStage.getAndIncrement($it)
                """.replaceIndent("    ")
            )
        }

        // Close the test procedure. And define test entry point.
        appendLine(
            """
            }

            fun main() {
                val workers = Array($threadsCount) { Worker.start() }

                workers.forEach { it.executeAfter(0, ::test) }

                while (hasRunStage[0] != workers.size) {}
                (1..MAX_STAGE).forEach { stage ->
                    canRunStage.value = stage
                    while (hasRunStage[stage] != workers.size) {}
                }

                workers.forEach { it.requestTermination().result }
            }
            """.trimIndent()
        )
    })
}

inline fun <reified T : Annotation> provider() = annotation(T::class.java)

private fun forceDebugMode() = annotation(
    EnforcedProperty::class.java,
    "property" to ClassLevelProperty.OPTIMIZATION_MODE,
    "propertyValue" to "DEBUG"
)

private fun forceTwoStageMultiModule() = annotation(
    EnforcedProperty::class.java,
    "property" to ClassLevelProperty.TEST_MODE,
    "propertyValue" to TestMode.TWO_STAGE_MULTI_MODULE.name
)

private fun compatibilityTestMode(mode: CompatibilityTestMode) = arrayOf(
    annotation(
        EnforcedProperty::class.java,
        "property" to ClassLevelProperty.COMPATIBILITY_TEST_MODE,
        "propertyValue" to mode.name
    ),
    annotation(
        EnforcedProperty::class.java,
        "property" to ClassLevelProperty.TEST_MODE,
        "propertyValue" to TestMode.TWO_STAGE_MULTI_MODULE.name
    ),
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
    annotation(FirPipeline::class.java)
)

fun frontendClassic() = arrayOf(
    annotation(ClassicPipeline::class.java)
)

private fun debugger() = annotation(Tag::class.java, "debugger")
private fun infrastructure() = annotation(Tag::class.java, "infrastructure")
private fun atomicfuNative() = arrayOf(
    annotation(Tag::class.java, "atomicfu-native"),
    annotation(EnforcedHostTarget::class.java), // TODO(KT-65977): Make atomicfu tests run on all targets.
)
private fun litmusktNative() = annotation(Tag::class.java, "litmuskt-native")
private fun standalone() = arrayOf(
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
private fun stress() = arrayOf(
    annotation(Tag::class.java, "stress"),
    annotation(
        EnforcedProperty::class.java,
        "property" to ClassLevelProperty.EXECUTION_TIMEOUT,
        "propertyValue" to "15m"
    )
)
