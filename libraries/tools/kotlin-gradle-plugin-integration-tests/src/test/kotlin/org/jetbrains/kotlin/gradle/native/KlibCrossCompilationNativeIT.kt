/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

@DisplayName("Tests for checking klib cross-compilation in KGP")
@NativeGradlePluginTests
class KlibCrossCompilationNativeIT : KGPBaseTest() {

    @GradleTest
    @TestMetadata("klibCrossCompilationDefaultSettings")
    @OsCondition(supportedOn = [OS.LINUX, OS.WINDOWS], enabledOnCI = [OS.LINUX, OS.WINDOWS])
    fun compileIosTargetOnNonDarwinHostWithDefaultSettings(gradleVersion: GradleVersion) {
        nativeProject("klibCrossCompilationDefaultSettings", gradleVersion) {
            build(":compileKotlinIosArm64") {
                assertEqualsToFile(
                    projectPath.resolve("diagnostics.txt").toFile(),
                    extractProjectsAndTheirDiagnostics()
                )
                assertTasksSkipped(":compileKotlinIosArm64")
            }
        }
    }

    @GradleTest
    @TestMetadata("klibCrossCompilationWithGradlePropertyEnabled")
    @OsCondition(supportedOn = [OS.LINUX, OS.WINDOWS], enabledOnCI = [OS.LINUX, OS.WINDOWS])
    fun compileIosTargetOnNonDarwinHostWithGradlePropertyEnabled(gradleVersion: GradleVersion, @TempDir konanDataDir: Path) {
        val buildOptions =
            // KT-62761: on Windows machine there are problems with removing tmp directory due to opened files
            // Thus, the logic of Kotlin Native toolchain provisioning may not be involved here, KT-72068 may not be tested
            // Consider removing the special handling for Windows after resolution of KT-62761
            if (HostManager.hostIsMingw)
                defaultBuildOptions
            else defaultBuildOptions.copy(
                // This line is required for the custom konan home location, to check that it is downloaded,
                // even when target is not supported(@see KT-72068). Even without this line the test will not fail,
                // but please don't remove while `kotlin.native.enableKlibsCrossCompilation` flag exists.
                konanDataDir = konanDataDir,
                // TODO: remove explicit version selection after resolution of KTI-1928
                nativeOptions = defaultBuildOptions.nativeOptions.copy(
                    version = "2.0.20",
                )
            )
        nativeProject(
            "klibCrossCompilationWithGradlePropertyEnabled",
            gradleVersion,
            buildOptions = buildOptions
        ) {

            val expectedDiagnostics = projectPath.resolve("expected-diagnostics.txt")
            if (!HostManager.hostIsMingw) {
                expectedDiagnostics.replaceText(
                    "> Configure project :",
                    """
                    |> Configure project :
                    |w: [OldNativeVersionDiagnostic | WARNING] '2.0.20' Kotlin Native is being used with an newer '${buildOptions.kotlinVersion}' Kotlin. Please adjust versions to avoid incompatibilities.
                    |#diagnostic-end
                    |    
                    """.trimMargin()
                )
            }

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
