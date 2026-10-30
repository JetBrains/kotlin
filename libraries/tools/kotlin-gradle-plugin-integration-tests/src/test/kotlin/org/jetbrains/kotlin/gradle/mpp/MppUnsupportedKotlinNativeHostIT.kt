/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.kotlin.dsl.kotlin
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInNative
import org.junit.jupiter.api.DisplayName

@DisplayName("Tests for Mpp unsupported host platforms")
@MustRunOnChangesInNative
class MppUnsupportedKotlinNativeHostIT : KGPDaemonsBaseTest() {

    /**
     * Defines the parameters for a Linux RISCV64 host environment.
     * These parameters are used to specify the operating system name and architecture.
     */
    private val linuxRiscv64HostParameters = listOf("-Dos.name=Linux", "-Dos.arch=riscv64")
    private val disabledLinuxArm64HostParameters = listOf(
        "-Dos.name=Linux", "-Dos.arch=aarch64", "-Dkotlin.native.experimental.linuxArm64Host=false"
    )

    @DisplayName("Build multiplatform project should not fail on unsupported native host platforms")
    @GradleTest
    fun testMultiplatformProjectOnUnsupportedHostPlatform(
        gradleVersion: GradleVersion,
    ) = testMultiplatformProjectOnUnsupportedHostPlatform(gradleVersion, linuxRiscv64HostParameters)

    @DisplayName("Build multiplatform project with experimental Linux ARM64 host support disabled")
    @GradleTest
    fun testMultiplatformProjectOnDisabledLinuxArm64HostPlatform(
        gradleVersion: GradleVersion,
    ) = testMultiplatformProjectOnUnsupportedHostPlatform(gradleVersion, disabledLinuxArm64HostParameters)

    private fun testMultiplatformProjectOnUnsupportedHostPlatform(
        gradleVersion: GradleVersion,
        hostParameters: List<String>,
    ) {
        project(
            "empty",
            gradleVersion,
            // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
            buildOptions = defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899(),
        ) {
            plugins {
                kotlin("multiplatform")
            }
            settingsBuildScriptInjection {
                settings.rootProject.name = "mpp"
            }
            buildScriptInjection {
                with(project) {
                    applyMultiplatform {
                        // non-native targets
                        jvm()
                        js { browser() }
                        wasmJs { browser() }

                        // native targets
                        linuxX64()
                        mingwX64()
                        macosArm64()

                        sourceSets.commonMain.get().compileStubSourceWithSourceSetName()

                        sourceSets.jvmMain.get().compileStubSourceWithSourceSetName()
                        sourceSets.jsMain.get().compileStubSourceWithSourceSetName()
                        sourceSets.wasmJsMain.get().compileStubSourceWithSourceSetName()

                        sourceSets.linuxMain.get().compileStubSourceWithSourceSetName()
                        sourceSets.mingwMain.get().compileStubSourceWithSourceSetName()
                        sourceSets.macosMain.get().compileStubSourceWithSourceSetName()
                    }
                }
            }

            // JVM and JS compile
            var args = listOf(":compileKotlinJvm") + hostParameters
            build(*args.toTypedArray()) {
                assertTasksExecuted(":compileKotlinJvm")
                verifyDiagnostics()
                assertDirectoryInProjectExists("build/classes/kotlin/jvm/main")
            }

            args = listOf(":compileKotlinJs") + hostParameters
            build(*args.toTypedArray()) {
                assertTasksExecuted(":compileKotlinJs")
                verifyDiagnostics()
                assertDirectoryInProjectExists("build/classes/kotlin/js/main")
            }

            args = listOf(":compileKotlinWasmJs") + hostParameters
            build(*args.toTypedArray()) {
                assertTasksExecuted(":compileKotlinWasmJs")
                verifyDiagnostics()
                assertDirectoryInProjectExists("build/classes/kotlin/wasmJs/main")
            }

            // Native compile
            args = listOf(":compileKotlinLinuxX64") + hostParameters
            build(*args.toTypedArray()) {
                assertTasksSkipped(":compileKotlinLinuxX64")
                verifyDiagnostics()
            }

            args = listOf(":compileKotlinMacosArm64") + hostParameters
            build(*args.toTypedArray()) {
                assertTasksSkipped(":compileKotlinMacosArm64")
                verifyDiagnostics()
            }

            args = listOf(":compileKotlinMingwX64") + hostParameters
            build(*args.toTypedArray()) {
                assertTasksSkipped(":compileKotlinMingwX64")
                verifyDiagnostics()
            }

            // Assemble
            args = listOf(":assemble") + hostParameters
            build(*args.toTypedArray()) {
                assertTasksSkipped(
                    ":compileKotlinMingwX64",
                    ":compileKotlinLinuxX64",
                    ":compileKotlinMacosArm64",
                    ":compileNativeMainKotlinMetadata"
                )
                assertTasksExecuted(
                    ":compileCommonMainKotlinMetadata",
                    ":assemble"
                )
                assertTasksUpToDate(
                    ":compileKotlinJvm",
                    ":compileKotlinJs",
                    ":compileKotlinWasmJs"
                )
                assertTasksAreNotInTaskGraph(
                    ":commonizeNativeDistribution",
                    ":downloadKotlinNativeDistribution"
                )
                verifyDiagnostics()
            }
        }
    }

    @DisplayName("KT-81443 configuration cache with disabled iOS targets on an unsupported host")
    @GradleTest
    fun testConfigurationCacheOnUnsupportedHostPlatform(
        gradleVersion: GradleVersion,
    ) = testConfigurationCacheOnUnsupportedHostPlatform(gradleVersion, linuxRiscv64HostParameters)

    @DisplayName("KT-81443 configuration cache with experimental Linux ARM64 host support disabled")
    @GradleTest
    fun testConfigurationCacheOnDisabledLinuxArm64HostPlatform(
        gradleVersion: GradleVersion,
    ) = testConfigurationCacheOnUnsupportedHostPlatform(gradleVersion, disabledLinuxArm64HostParameters)

    private fun testConfigurationCacheOnUnsupportedHostPlatform(
        gradleVersion: GradleVersion,
        hostParameters: List<String>,
    ) {
        project(
            "empty",
            gradleVersion
        ) {
            plugins {
                kotlin("multiplatform")
            }
            settingsBuildScriptInjection {
                settings.rootProject.name = "mpp"
            }
            buildScriptInjection {
                with(project) {
                    applyMultiplatform {
                        jvm()
                        iosArm64().binaries.framework()

                        sourceSets.commonMain.get().compileStubSourceWithSourceSetName()

                        sourceSets.jvmMain.get().compileStubSourceWithSourceSetName()
                        sourceSets.iosMain.get().compileStubSourceWithSourceSetName()
                    }
                }
            }

            val args = listOf(":build", "--configuration-cache") + hostParameters
            build(*args.toTypedArray()) {
                assertTasksExecuted(":build")
                assertTasksSkipped(":compileKotlinIosArm64")
                assertTasksAreNotInTaskGraph(":downloadKotlinNativeDistribution")
                assertConfigurationCacheStored()
            }
            build(*args.toTypedArray()) {
                assertConfigurationCacheReused()
            }
        }
    }
}

private fun BuildResult.verifyDiagnostics() {
    assertHasDiagnostic(KotlinToolingDiagnostics.DisabledKotlinNativeTargets)
    assertHasDiagnostic(KotlinToolingDiagnostics.NativeHostNotSupportedError)
}
