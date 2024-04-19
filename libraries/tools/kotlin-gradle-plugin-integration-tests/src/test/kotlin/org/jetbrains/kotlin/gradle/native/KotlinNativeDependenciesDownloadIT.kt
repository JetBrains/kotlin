/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.assertProcessRunResult
import org.jetbrains.kotlin.gradle.util.runProcess
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

// We temporarily disable it for windows until a proper fix is found for this issue: KT-62761
@OsCondition(
    supportedOn = [OS.MAC, OS.LINUX], enabledOnCI = [OS.MAC, OS.LINUX]
)
@DisplayName("This test class contains different scenarios with downloading dependencies for Kotlin Native Compiler during build.")
@NativeGradlePluginTests
class KotlinNativeDependenciesDownloadIT : KGPBaseTest() {

    @TempDir
    lateinit var konanDataTempDir: Path

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.withBundledKotlinNative().copy(
            // For each test in this class, we need to provide an isolated .konan directory,
            // so we create it within each test project folder
            konanDataDir = konanDataTempDir
        )

    @DisplayName("Gradle should download dependencies before compile execution")
    @GradleTest
    fun shouldDownloadDependenciesBeforeCompilerExecution(gradleVersion: GradleVersion) {
        nativeProject("mpp-default-hierarchy", gradleVersion) {
            build("assemble") {
                assertOutputContains("Downloading dependency for Kotlin Native")
                assertOutputDoesNotContain("(KonanProperties) Downloading dependency")
            }
        }
    }

    @DisplayName("Compiler should download dependencies when Kotlin Native Toolchain disabled ")
    @GradleTest
    fun checkCompilerDownloadsDependenciesWhenToochainDisabled(gradleVersion: GradleVersion) {
        nativeProject("mpp-default-hierarchy", gradleVersion) {

            build(
                "assemble", buildOptions = defaultBuildOptions.copy(
                    freeArgs = listOf("-Pkotlin.native.toolchain.enabled=false"),
                )
            ) {
                // Linux downloads dependencies because on CI tests on Linux are launched against default K/N version
                // (which doesn't contain `b38cc9c02407e3ae726d2b16751c1bdc78550cb4 [native] Make KonanConfig initialization more lazy` yet),
                // but MacOS tests are launched on CI against freshly built-version (which does contain this commit)
                //
                // This test is expected to fail on advancing K/N version, after which the difference should be gone
                // and only `assertOutputDoesNotContain` can be left
                if (HostManager.hostIsMac) {
                    assertOutputDoesNotContain("(KonanProperties) Downloading dependency")
                } else {
                    assertOutputContains("(KonanProperties) Downloading dependency")
                }
                assertOutputDoesNotContain("Downloading dependency for Kotlin Native")
            }
        }
    }

    //This test uses internal server for native dependencies
    @DisplayName("checks that native dependencies are not corrupted")
    @GradleTest
    fun testNativeDependencies(gradleVersion: GradleVersion) {
        testNativeDependencies("native-simple-project", "assemble", gradleVersion)
    }

    @OptIn(EnvironmentalVariablesOverride::class)
    private fun testNativeDependencies(projectName: String, task: String, gradleVersion: GradleVersion) {
        val konanDirectory = workingDir.resolve("konan")
        nativeProject(
            projectName, gradleVersion,
            environmentVariables = EnvironmentalVariables(Pair("KONAN_USE_INTERNAL_SERVER", "1")),
            buildOptions = defaultBuildOptions.withBundledKotlinNative().copy(
                konanDataDir = konanDirectory
            ),
        ) {
            build(task) {
                val file = projectPath.resolve("new.m").toFile().also { it.createNewFile() }
                val dependencies = konanDirectory.resolve("dependencies").toFile()
                assertTrue(dependencies.exists())
                assertTrue(dependencies.listFiles() != null, "Dependencies were not downloaded")
                dependencies.listFiles()?.filter { it.name != "cache" }?.forEach {
                    val processRunResult =
                        runProcess(listOf("clang", "-Werror", "-c", file.path, "-isysroot", it.absolutePath), workingDir.toFile())
                    assertProcessRunResult(processRunResult) {
                        assertTrue(isSuccessful)
                    }
                }
            }
        }
    }


    //This test uses internal server for native dependencies
    @DisplayName("checks that macos dependencies are not corrupted")
    @GradleTest
    @OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
    fun testMacosNativeDependencies(gradleVersion: GradleVersion) {
        testNativeDependencies("KT-66982-macos-target", "compileKotlinMacosArm64", gradleVersion)
    }
}
