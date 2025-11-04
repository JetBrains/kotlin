/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.kotlin.dsl.kotlin
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.junit.jupiter.api.DisplayName

@DisplayName("Tests for Mpp unsupported host platforms")
@MppGradlePluginTests
class MppUnsupportedKotlinNativeHostIT : KGPBaseTest() {

    /**
     * A list of JVM system property arguments used to specify unsupported native host platforms.
     * These parameters configure the operating system name and architecture to unsupported values,
     * such as Linux with a RISC-V64 architecture.
     *
     * This list is utilized in tests to verify behaviors when the native host platform is
     * explicitly unsupported within the testing configuration.
     */
    private val unsupportedHostParameters = listOf("-Dos.name=Linux", "-Dos.arch=riscv64")

    @OptIn(ExperimentalWasmDsl::class)
    @DisplayName("Build multiplatform project should not fail on unsupported native host platforms")
    @GradleTest
    fun testMultiplatformProjectOnUnsupportedHostPlatform(
        gradleVersion: GradleVersion,
    ) {
        project(
            "empty",
            gradleVersion,
            // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
            buildOptions = defaultBuildOptions.copy(isolatedProjects = BuildOptions.IsolatedProjectsMode.DISABLED),
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
            var args = listOf(":compileKotlinJvm") + unsupportedHostParameters
            build(*args.toTypedArray()) {
                assertTasksExecuted(":compileKotlinJvm")
                verifyDiagnostics()
                assertDirectoryInProjectExists("build/classes/kotlin/jvm/main")
            }

            args = listOf(":compileKotlinJs") + unsupportedHostParameters
            build(*args.toTypedArray()) {
                assertTasksExecuted(":compileKotlinJs")
                verifyDiagnostics()
                assertDirectoryInProjectExists("build/classes/kotlin/js/main")
            }

            args = listOf(":compileKotlinWasmJs") + unsupportedHostParameters
            build(*args.toTypedArray()) {
                assertTasksExecuted(":compileKotlinWasmJs")
                verifyDiagnostics()
                assertDirectoryInProjectExists("build/classes/kotlin/wasmJs/main")
            }

            // Native compile
            args = listOf(":compileKotlinLinuxX64") + unsupportedHostParameters
            build(*args.toTypedArray()) {
                assertTasksSkipped(":compileKotlinLinuxX64")
                verifyDiagnostics()
            }

            args = listOf(":compileKotlinMacosArm64") + unsupportedHostParameters
            build(*args.toTypedArray()) {
                assertTasksSkipped(":compileKotlinMacosArm64")
                verifyDiagnostics()
            }

            args = listOf(":compileKotlinMingwX64") + unsupportedHostParameters
            build(*args.toTypedArray()) {
                assertTasksSkipped(":compileKotlinMingwX64")
                verifyDiagnostics()
            }

            // Assemble
            args = listOf(":assemble") + unsupportedHostParameters
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
}

private fun BuildResult.verifyDiagnostics() {
    assertHasDiagnostic(KotlinToolingDiagnostics.DisabledKotlinNativeTargets)
    assertHasDiagnostic(KotlinToolingDiagnostics.NativeHostNotSupportedError)
}