/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.test.TestDataAssertions.assertEqualsToFile
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

@DisplayName("Tests for checking klib cross-compilation in KGP")
@NativeGradlePluginTests
class KlibCrossCompilationNativeIT : KGPBaseTest() {
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(
            ignoreWarningModeSeverityOverride = true
        )

    @GradleTest
    @OsCondition(supportedOn = [OS.LINUX, OS.WINDOWS], enabledOnCI = [OS.LINUX, OS.WINDOWS])
    fun compileIosTargetOnNonDarwinHostWithGradlePropertyDisabled(gradleVersion: GradleVersion) {
        nativeProject("empty", gradleVersion, buildOptions = defaultBuildOptions.disableKlibsCrossCompilation()) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                }
            }
            embedDirectoryFromTestData("klibCrossCompilationWithGradlePropertyDisabled", "data")
            build(":compileKotlinIosArm64") {
                assertEqualsToFile(
                    projectPath.resolve("data/diagnostics.txt").toFile(),
                    extractProjectsAndTheirDiagnostics()
                )
                assertTasksSkipped(":compileKotlinIosArm64")
            }
        }
    }

    @GradleTest
    @OsCondition(supportedOn = [OS.LINUX, OS.WINDOWS], enabledOnCI = [OS.LINUX, OS.WINDOWS])
    fun compileIosTargetOnNonDarwinHostWithDefaultSettings(gradleVersion: GradleVersion, @TempDir konanDataDir: Path) {
        val buildOptions =
            // KT-62761: on Windows machine there are problems with removing tmp directory due to opened files
            // Thus, the logic of Kotlin Native toolchain provisioning may not be involved here, KT-72068 may not be tested
            // Consider removing the special handling for Windows after resolution of KT-62761
            if (HostManager.hostIsMingw)
                defaultBuildOptions
            else defaultBuildOptions.copy(
                // This line is required for the custom konan home location to check that it is downloaded,
                // even when the target is not supported(@see KT-72068). Even without this line the test will not fail,
                // but please don't remove while `kotlin.native.enableKlibsCrossCompilation` flag exists.
                konanDataDir = konanDataDir,
            )
        nativeProject(
            "empty",
            gradleVersion,
            buildOptions = buildOptions
        ) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64 {
                        binaries.executable()
                    }
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                    sourceSets.commonTest.get().compileStubSourceWithSourceSetName()
                }
            }
            embedDirectoryFromTestData("klibCrossCompilationDefaultSettings", "data")
            val expectedDiagnostics = projectPath.resolve("data/diagnostics.txt")

            build(":compileKotlinIosArm64") {
                assertEqualsToFile(expectedDiagnostics.toFile(), extractProjectsAndTheirDiagnostics())
                assertTasksExecuted(":compileKotlinIosArm64")
            }

            build(":linkIosArm64") {
                assertEqualsToFile(expectedDiagnostics.toFile(), extractProjectsAndTheirDiagnostics())
                // Do not assert :linkIosArm64, because it's a plain umbrella-like `org.gradle.DefaultTask` instance,
                // and it doesn't get disabled even on linuxes (see [KotlinNativeConfigureBinariesSideEffect])
                assertTasksSkipped(":linkDebugTestIosArm64")
                assertTasksSkipped(":linkReleaseExecutableIosArm64")
                assertTasksSkipped(":linkDebugExecutableIosArm64")
            }
        }
    }
}
