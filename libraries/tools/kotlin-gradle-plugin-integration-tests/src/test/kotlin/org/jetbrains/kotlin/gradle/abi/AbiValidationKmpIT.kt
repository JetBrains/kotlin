/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalAbiValidation::class)

package org.jetbrains.kotlin.gradle.abi

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.abi.utils.*
import org.jetbrains.kotlin.gradle.abi.utils.AbiValidationTestDumps.FILE_FOR_FILTERS
import org.jetbrains.kotlin.gradle.abi.utils.AbiValidationTestDumps.SIMPLE_CLASS
import org.jetbrains.kotlin.gradle.abi.utils.AbiValidationTestDumps.assertDumpsEqual
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.DisplayName
import java.io.File
import kotlin.test.assertTrue

@MppGradlePluginTests
class AbiValidationKmpIT : KGPBaseTest() {

    private fun TestProject.defaultNativeTargets() {
        buildScriptInjection {
            with(kotlinMultiplatform) {
                linuxX64()
                linuxArm64()
                mingwX64()
                androidNativeArm32()
                androidNativeArm64()
                androidNativeX64()
                androidNativeX86()
            }
        }
    }

    private fun TestProject.defaultKmpProject() {
        defaultNativeTargets()
        abiValidation()
    }

    @GradleTest
    @DisplayName("Test check task with ABI validation enabled")
    fun testForEnabledAbiValidation(
        gradleVersion: GradleVersion,
    ) {
        kmpProject(gradleVersion) {
            buildScriptInjection {
                kotlinMultiplatform.jvm()
            }

            abiValidation()

            // create the reference dumps to check
            build("updateKotlinAbi")

            build("check") {
                assertTasksExecuted(":checkKotlinAbi")
            }
        }
    }

    @GradleTest
    @DisplayName("updateKotlinAbi and checkKotlinAbi for native targets")
    fun testSimple(gradleVersion: GradleVersion) {
        project("base-kotlin-multiplatform-library", gradleVersion) {
            defaultKmpProject()
            kotlinSourcesDir("commonMain").source("test/classes/SimpleClass.kt") { SIMPLE_CLASS }

            build("updateKotlinAbi") {
                assertTasksExecuted(":updateKotlinAbi")
            }

            val dumpFile = referenceKlibDumpFile()
            assertDumpsEqual(SIMPLE_DUMP_KLIB, dumpFile)

            build("checkKotlinAbi") {
                assertTasksExecuted(":checkKotlinAbi")
            }
        }
    }

    /**
     * This test is not testing all cases of filtering it just checks that the filters are passed to ABI tools correctly.
     * All the other tests on filtering should be placed in ABI Tools separately.
     */
    @GradleTest
    @DisplayName("Test passing filters to ABI tools")
    fun testFiltering(gradleVersion: GradleVersion) {
        project("base-kotlin-multiplatform-library", gradleVersion) {
            defaultKmpProject()
            kotlinSourcesDir("commonMain").source("test/classes/Filtering.kt") { FILE_FOR_FILTERS }

            abiValidation {
                filters {
                    it.exclude {
                        byNames.addAll("**.ExcludedByNameClass")
                        annotatedWith.addAll("**.Exclude")
                    }
                    it.include {
                        byNames.addAll("**.*Class", "**.IncludedByName")
                        annotatedWith.addAll("**.Include")
                    }
                }
            }

            build("updateKotlinAbi") {
                assertTasksExecuted(":updateKotlinAbi")
            }

            val dumpFile = referenceKlibDumpFile()
            assertDumpsEqual(FILTERED_DUMP_KLIB, dumpFile)
        }
    }

    @GradleTest
    @DisplayName("checkKotlinAbi should fail when a class is not in a dump")
    fun testCheckFailsWhenClassNotInDump(gradleVersion: GradleVersion) {
        project("base-kotlin-multiplatform-library", gradleVersion) {
            defaultKmpProject()
            kotlinSourcesDir("commonMain").source("test/classes/SimpleClass.kt") { SIMPLE_CLASS }
            createReferenceKlibDumpFile(EMPTY_DUMP_KLIB)

            buildAndFail("checkKotlinAbi") {
                assertTasksFailed(":checkKotlinAbi")
                assertOutputContains("+final class test.classes/SimpleClass { // ")
            }
        }
    }

    @GradleTest
    @DisplayName("updateKotlinAbi should include target-specific sources")
    fun testUpdateIncludesTargetSpecificSources(gradleVersion: GradleVersion) {
        project("base-kotlin-multiplatform-library", gradleVersion) {
            defaultKmpProject()
            kotlinSourcesDir("commonMain").source("test/classes/SimpleClass.kt") { SIMPLE_CLASS }

            kotlinSourcesDir("linuxArm64Main").source("ExtraClassLinuxArm64.kt") { EXTRA_CLASS_LINUX_ARM64 }

            build("updateKotlinAbi") {
                assertTasksExecuted(":updateKotlinAbi")
            }

            assertDumpsEqual(SIMPLE_DUMP_KLIB + EXTRA_DUMP_LINUX_ARM64, referenceKlibDumpFile())
        }
    }

    @GradleTest
    @DisplayName("updateKotlinAbi should succeed if a class listed in ignoredClasses is not found")
    fun testUpdateSucceedsIfIgnoredClassNotFound(gradleVersion: GradleVersion) {
        project("base-kotlin-multiplatform-library", gradleVersion) {
            defaultKmpProject()
            abiValidation {
                filters {
                    it.exclude {
                        byNames.addAll("com.company.BuildConfig")
                    }
                }
            }
            kotlinSourcesDir("commonMain").source("test/classes/SimpleClass.kt") { SIMPLE_CLASS }

            build("updateKotlinAbi") {
                assertTasksExecuted(":updateKotlinAbi")
            }

            assertDumpsEqual(SIMPLE_DUMP_KLIB, referenceKlibDumpFile())
        }
    }

    @GradleTest
    @DisplayName("updateKotlinAbi and checkKotlinAbi should work for Apple targets")
    fun testAppleTargets(gradleVersion: GradleVersion) {
        Assumptions.assumeTrue(HostManager().isEnabled(KonanTarget.MACOS_ARM64))
        project("base-kotlin-multiplatform-library", gradleVersion) {
            defaultNativeTargets()
            buildScriptInjection {
                with(kotlinMultiplatform) {
                    macosArm64()
                    iosX64()
                    iosArm64()
                    iosSimulatorArm64()
                    tvosArm64()
                    tvosSimulatorArm64()
                    watchosArm32()
                    watchosArm64()
                    watchosSimulatorArm64()
                    watchosDeviceArm64()
                }
            }
            abiValidation()
            kotlinSourcesDir("commonMain").source("test/classes/SimpleClass.kt") { SIMPLE_CLASS }

            build("updateKotlinAbi") {
                assertTasksExecuted(":updateKotlinAbi")
            }

            assertDumpsEqual(APPLE_DUMP_KLIB, referenceKlibDumpFile())

            build("checkKotlinAbi") {
                assertTasksExecuted(":checkKotlinAbi")
            }
        }
    }

    @GradleTest
    @DisplayName("Test simple project with JVM target only")
    fun testSimpleJvmOnly(
        gradleVersion: GradleVersion,
    ) {
        project(
            "base-kotlin-multiplatform-library",
            gradleVersion
        ) {
            buildScriptInjection {
                with(kotlinMultiplatform) {
                    jvm()
                }
            }
            abiValidation()

            kotlinSourcesDir("commonMain").source("org/jetbrains/tests/Sources.kt") { AbiValidationTestDumps.SIMPLE_CLASS }

            build("updateKotlinAbi")

            val jvmDumpFile = referenceJvmDumpFile()
            assertDumpsEqual(AbiValidationTestDumps.SIMPLE_DUMP_JVM, jvmDumpFile)

            build("checkKotlinAbi")

            // update dump
            jvmDumpFile.writeText("wrong ABI")
            buildAndFail("checkKotlinAbi") {
                assertTasksFailed(":checkKotlinAbi")
            }
        }
    }

    @GradleTest
    @DisplayName("Test empty project with JVM target only")
    fun testEmptyJvmOnly(
        gradleVersion: GradleVersion,
    ) {
        project(
            "base-kotlin-multiplatform-library",
            gradleVersion
        ) {
            buildScriptInjection {
                with(kotlinMultiplatform) {
                    jvm()
                }
            }
            abiValidation()

            // check dump file not exists
            buildAndFail("checkKotlinAbi") {
                assertTasksFailed(":checkKotlinAbi")
            }

            build("updateKotlinAbi")

            build("checkKotlinAbi")
        }
    }

    @GradleTest
    @DisplayName("Test updateKotlinAbi and checkKotlinAbi with an unsupported target")
    fun testUnsupportedTarget(gradleVersion: GradleVersion) {
        project("base-kotlin-multiplatform-library", gradleVersion) {
            defaultKmpProject()
            kotlinSourcesDir("commonMain").source("test/classes/SimpleClass.kt") { SIMPLE_CLASS }

            // generate dump with all targets
            build("updateKotlinAbi")
            assertDumpsEqual(SIMPLE_DUMP_KLIB, referenceKlibDumpFile())

            // check with one target banned - should still pass
            build(
                "checkKotlinAbi",
                buildOptions = buildOptions.copy(freeArgs = listOf("-P$BANNED_TARGETS_PROPERTY_NAME=linuxArm64"))
            ) {
                assertTasksExecuted(":checkKotlinAbi")
            }
        }
    }

    @GradleTest
    @DisplayName("internalDumpKotlinAbi should fail for unsupported targets with strict mode")
    fun testUnsupportedTargetProhibited(gradleVersion: GradleVersion) {
        project("base-kotlin-multiplatform-library", gradleVersion) {
            defaultNativeTargets()
            abiValidation {
                keepLocallyUnsupportedTargets.set(false)
            }
            kotlinSourcesDir("commonMain").source("test/classes/SimpleClass.kt") { SIMPLE_CLASS }

            createReferenceKlibDumpFile(SIMPLE_DUMP_KLIB)

            buildAndFail(
                "checkKotlinAbi",
                buildOptions = buildOptions.copy(freeArgs = listOf("-P$BANNED_TARGETS_PROPERTY_NAME=linuxArm64"))
            ) {
                assertTasksFailed(":internalDumpKotlinAbi")
            }
        }
    }

    @GradleTest
    @DisplayName("klibDump should infer a dump for unsupported target from similar enough target")
    fun testInfers(gradleVersion: GradleVersion) {
        project("base-kotlin-multiplatform-library", gradleVersion) {
            defaultKmpProject()
            kotlinSourcesDir("commonMain").source("test/classes/SimpleClass.kt") { SIMPLE_CLASS }
            // real linuxArm64 classes will be skipped
            kotlinSourcesDir("linuxArm64Main").source("ExtraClassLinuxArm64.kt") { EXTRA_CLASS_LINUX_ARM64 }

            // generate dump with linuxArm64 banned - should infer from common
            build(
                "updateKotlinAbi",
                buildOptions = buildOptions.copy(freeArgs = listOf("-P$BANNED_TARGETS_PROPERTY_NAME=linuxArm64"))
            ) {
                assertTasksExecuted(":updateKotlinAbi")
            }

            val dumpFile = referenceKlibDumpFile()
            assertDumpsEqual(SIMPLE_DUMP_KLIB, dumpFile)
        }
    }

    @GradleTest
    @DisplayName("infer a dump for a target with custom name")
    fun testInferForTargetWithCustomName(gradleVersion: GradleVersion) {
        project("base-kotlin-multiplatform-library", gradleVersion) {
            buildScriptInjection {
                with(kotlinMultiplatform) {
                    linuxX64("linux")
                    linuxArm64()
                    mingwX64()
                    androidNativeArm32()
                    androidNativeArm64()
                    androidNativeX64()
                    androidNativeX86()
                }
            }
            abiValidation()
            kotlinSourcesDir("commonMain").source("test/classes/SimpleClass.kt") { SIMPLE_CLASS }
            // linuxARm64 specific classes will be skipped
            kotlinSourcesDir("linuxMain").source("ExtraClassLinuxArm64.kt") { EXTRA_CLASS_LINUX_ARM64 }

            build(
                "updateKotlinAbi",
                buildOptions = buildOptions.copy(freeArgs = listOf("-P$BANNED_TARGETS_PROPERTY_NAME=linux"))
            ) {
                assertTasksExecuted(":updateKotlinAbi")
            }

            assertDumpsEqual(RENAMED_TARGET_KLIB_DUMP, referenceKlibDumpFile())
        }
    }

    @GradleTest
    @DisplayName("klibDump should fail when the only target in the project is disabled")
    fun testKlibDumpFailsWhenOnlyTargetDisabled(gradleVersion: GradleVersion) {
        project("base-kotlin-multiplatform-library", gradleVersion) {
            buildScriptInjection {
                kotlinMultiplatform.linuxArm64()
            }
            abiValidation()
            kotlinSourcesDir("commonMain").source("test/classes/SimpleClass.kt") { SIMPLE_CLASS }
            kotlinSourcesDir("linuxArm64Main").source("ExtraClassLinuxArm64.kt") { EXTRA_CLASS_LINUX_ARM64 }

            buildAndFail(
                "updateKotlinAbi",
                buildOptions = buildOptions.copy(freeArgs = listOf("-P$BANNED_TARGETS_PROPERTY_NAME=linuxArm64"))
            ) {
                assertTasksFailed(":internalDumpKotlinAbi")
                assertOutputContains(
                    "The target linuxArm64 is not supported by the host compiler " +
                            "and there are no targets similar to linuxArm64 to infer a dump from it."
                )
            }
        }
    }

    @GradleTest
    @DisplayName("updateKotlinAbi and checkKotlinAbi if all klib-targets are unavailable")
    fun testAllKlibTargetsUnavailable(gradleVersion: GradleVersion) {
        val allTargetsBanned = "-P$BANNED_TARGETS_PROPERTY_NAME=linuxArm64,linuxX64,mingwX64," +
                "androidNativeArm32,androidNativeArm64,androidNativeX64,androidNativeX86"

        project("base-kotlin-multiplatform-library", gradleVersion) {
            defaultKmpProject()
            kotlinSourcesDir("commonMain").source("test/classes/SimpleClass.kt") { SIMPLE_CLASS }

            // dump should fail
            buildAndFail(
                "updateKotlinAbi",
                buildOptions = buildOptions.copy(freeArgs = listOf(allTargetsBanned))
            ) {
                assertOutputContains("is not supported by the host compiler and there are no targets similar to")
            }

            createReferenceKlibDumpFile(SIMPLE_DUMP_KLIB)

            build(
                "checkKotlinAbi",
                buildOptions = buildOptions.copy(freeArgs = listOf(allTargetsBanned))
            ) {
                assertTasksExecuted(":checkKotlinAbi")
            }
        }
    }

    @GradleTest
    @DisplayName("checkKotlinAbi should fail with strict validation if all klib-targets are unavailable")
    fun testFailIfAllKlibTargetsUnavailable(gradleVersion: GradleVersion) {
        val allTargetsBanned = "-P$BANNED_TARGETS_PROPERTY_NAME=linuxArm64,linuxX64,mingwX64," +
                "androidNativeArm32,androidNativeArm64,androidNativeX64,androidNativeX86"

        project("base-kotlin-multiplatform-library", gradleVersion) {
            defaultNativeTargets()
            abiValidation {
                keepLocallyUnsupportedTargets.set(false)
            }
            kotlinSourcesDir("commonMain").source("test/classes/SimpleClass.kt") { SIMPLE_CLASS }

            createReferenceKlibDumpFile(SIMPLE_DUMP_KLIB)

            buildAndFail(
                "checkKotlinAbi",
                buildOptions = buildOptions.copy(freeArgs = listOf(allTargetsBanned))
            ) {
                assertTasksFailed(":internalDumpKotlinAbi")
            }
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    @GradleTest
    @DisplayName("updateKotlinAbi and checkKotlinAbi should work with web targets")
    fun testWebTargets(gradleVersion: GradleVersion) {
        project("base-kotlin-multiplatform-library", gradleVersion) {
            defaultNativeTargets()
            buildScriptInjection {
                with(kotlinMultiplatform) {
                    wasmWasi()
                    wasmJs()
                    js()
                }
            }
            abiValidation()
            kotlinSourcesDir("commonMain").source("test/classes/SimpleClass.kt") { SIMPLE_CLASS }

            build("updateKotlinAbi", buildOptions = buildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()) {
                assertTasksExecuted(":updateKotlinAbi")
            }

            assertDumpsEqual(WEB_DUMP_KLIB, referenceKlibDumpFile())

            build("checkKotlinAbi", buildOptions = buildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()) {
                assertTasksExecuted(":checkKotlinAbi")
            }
        }
    }

    @GradleTest
    @DisplayName("check dump is updated on added declaration")
    fun testUpdateDump(gradleVersion: GradleVersion) {
        project("base-kotlin-multiplatform-library", gradleVersion) {
            defaultKmpProject()
            kotlinSourcesDir("commonMain").source("test/classes/SimpleClass.kt") { SIMPLE_CLASS }

            build("updateKotlinAbi")
            assertDumpsEqual(SIMPLE_DUMP_KLIB, referenceKlibDumpFile())

            // update source by adding a declaration
            kotlinSourcesDir("commonMain").source("test/classes/SimpleClass.kt") { SIMPLE_CLASS_MODIFIED }

            buildAndFail("checkKotlinAbi") {
                assertTasksFailed(":checkKotlinAbi")
            }

            build("updateKotlinAbi")
            assertDumpsEqual(MODIFIED_DUMP_KLIB, referenceKlibDumpFile())
        }
    }

    @GradleTest
    @DisplayName("check dump is updated on a declaration added to some source sets")
    fun testUpdateDumpTargetSpecific(gradleVersion: GradleVersion) {
        project("base-kotlin-multiplatform-library", gradleVersion) {
            defaultKmpProject()
            kotlinSourcesDir("commonMain").source("test/classes/SimpleClass.kt") { SIMPLE_CLASS }

            build("updateKotlinAbi")
            assertDumpsEqual(SIMPLE_DUMP_KLIB, referenceKlibDumpFile())

            // add target-specific source
            kotlinSourcesDir("linuxArm64Main").source("ExtraClassLinuxArm64.kt") { EXTRA_CLASS_LINUX_ARM64 }

            build("updateKotlinAbi")
            assertDumpsEqual(SIMPLE_DUMP_KLIB + EXTRA_DUMP_LINUX_ARM64, referenceKlibDumpFile())
        }
    }

    @GradleTest
    @DisplayName("validation should fail on target rename")
    fun testValidationFailsOnTargetRename(gradleVersion: GradleVersion) {
        project("base-kotlin-multiplatform-library", gradleVersion) {
            defaultKmpProject()
            kotlinSourcesDir("commonMain").source("test/classes/SimpleClass.kt") { SIMPLE_CLASS }
            createReferenceKlibDumpFile(RENAMED_TARGET_KLIB_DUMP)

            buildAndFail("checkKotlinAbi") {
                assertOutputContains(
                    "  -// Targets: [androidNativeArm32, androidNativeArm64, androidNativeX64, androidNativeX86, linuxArm64, linuxX64.linux, mingwX64]"
                )
            }
        }
    }

    @GradleTest
    @DisplayName("updateKotlinAbi should not fail for empty project and should dump empty file")
    fun testUpdateForEmptyProject(gradleVersion: GradleVersion) {
        project("base-kotlin-multiplatform-library", gradleVersion) {
            defaultKmpProject()
            // sources only in test source set - no main sources
            kotlinSourcesDir("commonTest").source("test/classes/SimpleClass.kt") { SIMPLE_CLASS }

            build("updateKotlinAbi") {
                assertTasksExecuted(":updateKotlinAbi")
            }

            val dumpFile = referenceKlibDumpFile()
            assertDumpsEqual("", dumpFile)

            // now test with existing non-empty dump - should overwrite with empty
            createReferenceKlibDumpFile(SIMPLE_DUMP_KLIB)

            build("updateKotlinAbi") {
                assertTasksExecuted(":updateKotlinAbi")
            }
            assertDumpsEqual("", dumpFile)
        }
    }

    @GradleTest
    @DisplayName("updateKotlinAbi should not fail if there is only one target")
    fun testUpdateWithSingleTarget(gradleVersion: GradleVersion) {
        project("base-kotlin-multiplatform-library", gradleVersion) {
            defaultKmpProject()
            kotlinSourcesDir("commonTest").source("test/classes/SimpleClass.kt") { SIMPLE_CLASS }
            kotlinSourcesDir("linuxX64Main").source("test/classes/SimpleClass.kt") { SIMPLE_CLASS }

            build("updateKotlinAbi") {
                assertTasksExecuted(":updateKotlinAbi")
            }

            assertDumpsEqual(LINUX_ONLY_X64_DUMP_KLIB, referenceKlibDumpFile())
        }
    }

    @GradleTest
    @DisplayName("checkKotlinAbi should fail for empty project without a dump file")
    fun testUpdateFailsForEmptyProjectWithoutDump(gradleVersion: GradleVersion) {
        project("base-kotlin-multiplatform-library", gradleVersion) {
            defaultKmpProject()
            kotlinSourcesDir("commonTest").source("test/classes/SimpleClass.kt") { SIMPLE_CLASS }

            buildAndFail("checkKotlinAbi") {
                assertTasksFailed(":checkKotlinAbi")
                assertOutputContains(
                    "Expected file with ABI declarations 'api${File.separator}base-kotlin-multiplatform-library.klib.api' does not exist."
                )
            }
        }
    }

    @GradleTest
    @DisplayName("updateKotlinAbi and checkKotlinAbi for a project with generated sources only")
    fun testGeneratedSources(gradleVersion: GradleVersion) {
        project("base-kotlin-multiplatform-library", gradleVersion) {
            defaultKmpProject()
            buildScriptInjection {
                abstract class GenerateSourcesTask : DefaultTask() {
                    @get:OutputDirectory
                    abstract val outputDirectory: DirectoryProperty

                    @TaskAction
                    fun generate() {
                        outputDirectory.asFile.get().mkdirs()
                        outputDirectory.file("Generated.kt").get().asFile.writeText(
                            "public class Generated { public fun helloCreator(): Int = 42 }"
                        )
                    }
                }

                val srcgen = project.tasks.register("generateSources", GenerateSourcesTask::class.java)
                srcgen.configure {
                    it.outputDirectory.set(project.layout.buildDirectory.get().dir("generated").dir("kotlin"))
                }
                kotlinMultiplatform.sourceSets.getByName("commonMain") {
                    @OptIn(ExperimentalKotlinGradlePluginApi::class)
                    it.generatedKotlin.srcDir(srcgen)
                }
            }

            build("updateKotlinAbi", buildOptions = buildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()) {
                assertTasksExecuted(":updateKotlinAbi")
            }

            assertDumpsEqual(GENERATED_SOURCES_DUMP_KLIB, referenceKlibDumpFile())

            build("checkKotlinAbi", buildOptions = buildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()) {
                assertTasksExecuted(":checkKotlinAbi")
            }
        }
    }

    @GradleTest
    @DisplayName("checkKotlinAbi should fail after target removal")
    fun testFailAfterTargetRemoval(gradleVersion: GradleVersion) {
        project("base-kotlin-multiplatform-library", gradleVersion) {
            // only a single native target
            buildScriptInjection {
                kotlinMultiplatform.linuxArm64()
            }
            abiValidation()
            kotlinSourcesDir("commonMain").source("test/classes/SimpleClass.kt") { SIMPLE_CLASS }
            // dump was created for multiple native targets
            createReferenceKlibDumpFile(SIMPLE_DUMP_KLIB)

            buildAndFail("checkKotlinAbi") {
                assertTasksFailed(":checkKotlinAbi")
                assertOutputContains(
                    "-// Targets: [androidNativeArm32, androidNativeArm64, androidNativeX64, " +
                            "androidNativeX86, linuxArm64, linuxX64, mingwX64]"
                )
                assertOutputContains("+// Targets: [linuxArm64]")
            }
        }
    }
}

// Sources
private val SIMPLE_CLASS_MODIFIED = """
    package test.classes
    
    class SimpleClass {
        fun foo() = 42
        fun bar() = 42
    }
""".trimIndent()

private val EXTRA_CLASS_LINUX_ARM64 = """
    package org.different.pack
    fun linuxArm64Specific(): Int = 42
""".trimIndent()

// Dumps

private val EXTRA_DUMP_LINUX_ARM64 = """

// Targets: [linuxArm64]
final fun org.different.pack/linuxArm64Specific(): kotlin/Int // org.different.pack/linuxArm64Specific|linuxArm64Specific(){}[0]

""".trimIndent()


val SIMPLE_DUMP_KLIB = """
        // Klib ABI Dump
        // Targets: [androidNativeArm32, androidNativeArm64, androidNativeX64, androidNativeX86, linuxArm64, linuxX64, mingwX64]
        // Rendering settings:
        // - Signature version: 2
        // - Show manifest properties: true
        // - Show declarations: true

        // Library unique name: <base-kotlin-multiplatform-library>
        final class test.classes/SimpleClass { // test.classes/SimpleClass|null[0]
            constructor <init>() // test.classes/SimpleClass.<init>|<init>(){}[0]

            final fun foo(): kotlin/Int // test.classes/SimpleClass.foo|foo(){}[0]
        }

    """.trimIndent()

val FILTERED_DUMP_KLIB = """
    // Klib ABI Dump
    // Targets: [androidNativeArm32, androidNativeArm64, androidNativeX64, androidNativeX86, linuxArm64, linuxX64, mingwX64]
    // Rendering settings:
    // - Signature version: 2
    // - Show manifest properties: true
    // - Show declarations: true

    // Library unique name: <base-kotlin-multiplatform-library>
    final class test.classes/FirstClass // test.classes/FirstClass|null[0]

    final class test.classes/IncludedByName // test.classes/IncludedByName|null[0]

    final class test.classes/SecondClass // test.classes/SecondClass|null[0]

""".trimIndent()

val EMPTY_DUMP_KLIB = """
        // Klib ABI Dump
        // Targets: [mingwX64]
        // Rendering settings:
        // - Signature version: 2
        // - Show manifest properties: true
        // - Show declarations: true
    """.trimIndent()

private val APPLE_DUMP_KLIB = """
    // Klib ABI Dump
    // Targets: [androidNativeArm32, androidNativeArm64, androidNativeX64, androidNativeX86, iosArm64, iosSimulatorArm64, iosX64, linuxArm64, linuxX64, macosArm64, mingwX64, tvosArm64, tvosSimulatorArm64, watchosArm32, watchosArm64, watchosDeviceArm64, watchosSimulatorArm64]
    // Rendering settings:
    // - Signature version: 2
    // - Show manifest properties: true
    // - Show declarations: true

    // Library unique name: <base-kotlin-multiplatform-library>
    final class test.classes/SimpleClass { // test.classes/SimpleClass|null[0]
        constructor <init>() // test.classes/SimpleClass.<init>|<init>(){}[0]

        final fun foo(): kotlin/Int // test.classes/SimpleClass.foo|foo(){}[0]
    }

""".trimIndent()

private val RENAMED_TARGET_KLIB_DUMP = """
    // Klib ABI Dump
    // Targets: [androidNativeArm32, androidNativeArm64, androidNativeX64, androidNativeX86, linuxArm64, linuxX64.linux, mingwX64]
    // Rendering settings:
    // - Signature version: 2
    // - Show manifest properties: true
    // - Show declarations: true

    // Library unique name: <base-kotlin-multiplatform-library>
    final class test.classes/SimpleClass { // test.classes/SimpleClass|null[0]
        constructor <init>() // test.classes/SimpleClass.<init>|<init>(){}[0]

        final fun foo(): kotlin/Int // test.classes/SimpleClass.foo|foo(){}[0]
    }

""".trimIndent()

private val MODIFIED_DUMP_KLIB = """
    // Klib ABI Dump
    // Targets: [androidNativeArm32, androidNativeArm64, androidNativeX64, androidNativeX86, linuxArm64, linuxX64, mingwX64]
    // Rendering settings:
    // - Signature version: 2
    // - Show manifest properties: true
    // - Show declarations: true

    // Library unique name: <base-kotlin-multiplatform-library>
    final class test.classes/SimpleClass { // test.classes/SimpleClass|null[0]
        constructor <init>() // test.classes/SimpleClass.<init>|<init>(){}[0]

        final fun bar(): kotlin/Int // test.classes/SimpleClass.bar|bar(){}[0]
        final fun foo(): kotlin/Int // test.classes/SimpleClass.foo|foo(){}[0]
    }

""".trimIndent()

private val LINUX_ONLY_X64_DUMP_KLIB = """
    // Klib ABI Dump
    // Targets: [linuxX64]
    // Rendering settings:
    // - Signature version: 2
    // - Show manifest properties: true
    // - Show declarations: true

    // Library unique name: <base-kotlin-multiplatform-library>
    final class test.classes/SimpleClass { // test.classes/SimpleClass|null[0]
        constructor <init>() // test.classes/SimpleClass.<init>|<init>(){}[0]

        final fun foo(): kotlin/Int // test.classes/SimpleClass.foo|foo(){}[0]
    }

""".trimIndent()

private val WEB_DUMP_KLIB = """
    // Klib ABI Dump
    // Targets: [androidNativeArm32, androidNativeArm64, androidNativeX64, androidNativeX86, js, linuxArm64, linuxX64, mingwX64, wasmJs, wasmWasi]
    // Rendering settings:
    // - Signature version: 2
    // - Show manifest properties: true
    // - Show declarations: true

    // Library unique name: <base-kotlin-multiplatform-library>
    final class test.classes/SimpleClass { // test.classes/SimpleClass|null[0]
        constructor <init>() // test.classes/SimpleClass.<init>|<init>(){}[0]

        final fun foo(): kotlin/Int // test.classes/SimpleClass.foo|foo(){}[0]
    }

""".trimIndent()

private val GENERATED_SOURCES_DUMP_KLIB = """
    // Klib ABI Dump
    // Targets: [androidNativeArm32, androidNativeArm64, androidNativeX64, androidNativeX86, linuxArm64, linuxX64, mingwX64]
    // Rendering settings:
    // - Signature version: 2
    // - Show manifest properties: true
    // - Show declarations: true

    // Library unique name: <base-kotlin-multiplatform-library>
    final class /Generated { // /Generated|null[0]
        constructor <init>() // /Generated.<init>|<init>(){}[0]

        final fun helloCreator(): kotlin/Int // /Generated.helloCreator|helloCreator(){}[0]
    }

""".trimIndent()

private const val BANNED_TARGETS_PROPERTY_NAME = "kotlin.internal.abi.validation.klib.targets.disabled.for.testing"

