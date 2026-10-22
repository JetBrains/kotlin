/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalAbiValidation::class)

package org.jetbrains.kotlin.gradle.abi

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.abi.utils.*
import org.jetbrains.kotlin.gradle.abi.utils.AbiValidationTestDumps.assertDumpsEqual
import org.jetbrains.kotlin.gradle.dsl.abi.BinariesSource
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInBuildToolsApi
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInGradle

@MustRunOnChangesInGradle
@AndroidGradlePluginTests
class AbiValidationAndroidKmpIT : KGPBaseTest() {

    @AndroidTestVersions(minVersion = TestVersions.AGP.AGP_88, additionalVersions = [TestVersions.AGP.AGP_811])
    @GradleAndroidTest
    @MustRunOnChangesInBuildToolsApi // Runs ABI tool via BTA
    fun testAndroidOnly(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        androidKmpLibraryProject(gradleVersion, agpVersion, jdkVersion) {
            abiValidation()

            kotlinSourcesDir("commonMain").source("CommonClass.kt") { "class CommonClass" }
            kotlinSourcesDir("androidMain").source("AndroidClass.kt") { "class AndroidClass" }

            build("updateKotlinAbi")

            val dumpFile = referenceMixedAndroidDumpFile()
            assertFileExists(dumpFile)

            val tab = "\t"
            val expectedDump = """
                public final class AndroidClass {
                ${tab}public fun <init> ()V
                }

                public final class CommonClass {
                ${tab}public fun <init> ()V
                }


            """.trimIndent()
            assertDumpsEqual(expectedDump, dumpFile)
        }
    }

    @AndroidTestVersions(minVersion = TestVersions.AGP.AGP_88, additionalVersions = [TestVersions.AGP.AGP_811])
    @GradleAndroidTest
    @MustRunOnChangesInBuildToolsApi // Runs ABI tool via BTA
    fun testMixed(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        androidKmpLibraryProject(gradleVersion, agpVersion, jdkVersion) {
            abiValidation()
            buildScriptInjection {
                with(kotlinMultiplatform) {
                    jvm()
                }
            }

            kotlinSourcesDir("commonMain").source("CommonClass.kt") { "class CommonClass" }
            kotlinSourcesDir("androidMain").source("AndroidClass.kt") { "class AndroidClass" }
            kotlinSourcesDir("jvmMain").source("JvmClass.kt") { "class JvmClass" }

            build("updateKotlinAbi")

            val tab = "\t"

            val androidDumpFile = referenceMixedAndroidDumpFile()
            assertFileExists(androidDumpFile)
            val expectedAndroidDump = """
                public final class AndroidClass {
                ${tab}public fun <init> ()V
                }

                public final class CommonClass {
                ${tab}public fun <init> ()V
                }


            """.trimIndent()
            assertDumpsEqual(expectedAndroidDump, androidDumpFile)

            val jvmDumpFile = referenceMixedJvmDumpFile()
            assertFileExists(jvmDumpFile)
            val expectedJvmDump = """
                public final class CommonClass {
                ${tab}public fun <init> ()V
                }

                public final class JvmClass {
                ${tab}public fun <init> ()V
                }


            """.trimIndent()
            assertDumpsEqual(expectedJvmDump, jvmDumpFile)
        }
    }

    @AndroidTestVersions(minVersion = TestVersions.AGP.AGP_88, additionalVersions = [TestVersions.AGP.AGP_811])
    @GradleAndroidTest
    @MustRunOnChangesInBuildToolsApi // Runs ABI tool via BTA
    fun testCheckFailsOnAndroidAbiChange(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        androidKmpLibraryProject(gradleVersion, agpVersion, jdkVersion) {
            abiValidation()

            kotlinSourcesDir("commonMain").source("CommonClass.kt") { "class CommonClass" }
            kotlinSourcesDir("androidMain").source("AndroidClass.kt") { "class AndroidClass" }

            build("updateKotlinAbi")
            assertFileExists(referenceMixedAndroidDumpFile())
            build("checkKotlinAbi")

            kotlinSourcesDir("androidMain").source("AndroidClass.kt") {
                "class AndroidClass {\n    fun newApi(): Int = 42\n}"
            }
            buildAndFail("checkKotlinAbi") {
                assertTasksFailed(":checkKotlinAbi")
                assertOutputContains("public final fun newApi ()I")
            }
        }
    }

    /**
     * Test Federation: Gradle only. Fails on a KGP diagnostic before the ABI tool runs.
     */
    @AndroidTestVersions(minVersion = TestVersions.AGP.AGP_88, additionalVersions = [TestVersions.AGP.AGP_811])
    @GradleAndroidTest
    fun testMavenPublicationsRejectedForAndroidTarget(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        androidKmpLibraryProject(gradleVersion, agpVersion, jdkVersion) {
            buildScriptInjection {
                project.plugins.apply("maven-publish")
            }
            abiValidation {
                binariesSource.set(BinariesSource.MAVEN_PUBLICATIONS)
            }

            kotlinSourcesDir("commonMain").source("CommonClass.kt") { "class CommonClass" }
            kotlinSourcesDir("androidMain").source("AndroidClass.kt") { "class AndroidClass" }

            buildAndFail("updateKotlinAbi") {
                assertOutputContains("ABI Validation: Android target unsupported with Maven binary sources mode")
            }
        }
    }

    @AndroidTestVersions(minVersion = TestVersions.AGP.AGP_88, additionalVersions = [TestVersions.AGP.AGP_811])
    @GradleAndroidTest
    @MustRunOnChangesInBuildToolsApi // Runs ABI tool via BTA
    fun testAndroidWithNativeTarget(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        androidKmpLibraryProject(gradleVersion, agpVersion, jdkVersion) {
            abiValidation()
            buildScriptInjection {
                with(kotlinMultiplatform) {
                    linuxX64()
                }
            }

            kotlinSourcesDir("commonMain").source("CommonClass.kt") { "class CommonClass" }
            kotlinSourcesDir("androidMain").source("AndroidClass.kt") { "class AndroidClass" }

            build("updateKotlinAbi")

            // The android multiplatform library target (JVM-kind) is dumped through the dedicated
            // Android branch into api/android/, including commonMain + androidMain declarations.
            val androidDumpFile = referenceMixedAndroidDumpFile()
            assertFileExists(androidDumpFile)
            assertFileContains(
                androidDumpFile.toPath(),
                "public final class AndroidClass",
                "public final class CommonClass",
            )

            // The native target is processed independently as a klib ABI dump; it contains the
            // shared commonMain declarations but not the android-only ones.
            val klibDumpFile = referenceKlibDumpFile()
            assertFileExists(klibDumpFile)
            assertFileContains(klibDumpFile.toPath(), "CommonClass")
        }
    }

    @AndroidTestVersions(minVersion = TestVersions.AGP.AGP_88, additionalVersions = [TestVersions.AGP.AGP_811])
    @GradleAndroidTest
    @MustRunOnChangesInBuildToolsApi // Runs ABI tool via BTA
    fun testNonTestCompilationsExcludeTestSources(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        androidKmpLibraryProject(gradleVersion, agpVersion, jdkVersion) {
            abiValidation {
                binariesSource.set(BinariesSource.NON_TEST_COMPILATIONS)
            }
            // Test compilations are disabled by default in com.android.kotlin.multiplatform.library,
            // so enable them explicitly to verify they are excluded from the ABI dump.
            buildScriptInjection {
                kotlinMultiplatform.androidLibrary {
                    withHostTest {}
                    withDeviceTest {}
                }
                kotlinMultiplatform.apply {
                    val customMain = sourceSets.create("customMain").apply {
                        dependsOn(sourceSets.getByName("commonMain"))
                    }
                    sourceSets.getByName("androidMain").dependsOn(customMain)
                }
            }

            // Non-test source sets that feed the main compilation - all must appear in the dump.
            kotlinSourcesDir("commonMain").source("CommonClass.kt") { "class CommonClass" }
            kotlinSourcesDir("customMain").source("CustomClass.kt") { "class CustomClass" }
            kotlinSourcesDir("androidMain").source("AndroidClass.kt") { "class AndroidClass" }
            // Public declarations in the test compilations - none must appear in the dump.
            kotlinSourcesDir("androidHostTest").source("HostTestClass.kt") { "class HostTestClass" }
            kotlinSourcesDir("androidDeviceTest").source("DeviceTestClass.kt") { "class DeviceTestClass" }

            build("updateKotlinAbi")

            val dumpFile = referenceMixedAndroidDumpFile()
            assertFileExists(dumpFile)

            // The dump contains only the non-test declarations (commonMain + customMain + androidMain);
            // both the host-test and device-test compilations are excluded.
            val tab = "\t"
            val expectedDump = """
                public final class AndroidClass {
                ${tab}public fun <init> ()V
                }

                public final class CommonClass {
                ${tab}public fun <init> ()V
                }

                public final class CustomClass {
                ${tab}public fun <init> ()V
                }


            """.trimIndent()
            assertDumpsEqual(expectedDump, dumpFile)
        }
    }

}
