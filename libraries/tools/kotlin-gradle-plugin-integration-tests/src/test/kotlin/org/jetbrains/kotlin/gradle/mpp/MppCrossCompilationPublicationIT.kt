/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.PublisherConfiguration
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.setupMavenPublication
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.test.assertFalse

@MppGradlePluginTests
class MppCrossCompilationPublicationIT : KGPBaseTest() {

    @DisplayName("Cross compilation enabled, no cinterops, publish library to mavenLocal")
    @GradleTest
    fun testCrossCompilationPublicationWithoutCinterops(
        gradleVersion: GradleVersion,
    ) {
        val multiplatformLibrary = publishMultiplatformLibrary(gradleVersion) {
            assertNoDiagnostic(KotlinToolingDiagnostics.CrossCompilationWithCinterops)
        }

        val mavenUrl = multiplatformLibrary.projectPath.resolve("build/repo")
        val libraryRoot = mavenUrl.resolve("com/jetbrains/library")

        // Verify that module directories exist for each target
        val expectedModules = listOf(
            "multiplatformLibrary",
            "multiplatformLibrary-iosarm64",
            "multiplatformLibrary-jvm",
            "multiplatformLibrary-linuxx64",
            "multiplatformLibrary-macosarm64",
            "multiplatformLibrary-mingwx64"
        )

        // Check that expected modules exist
        checkExpectedModules(libraryRoot, expectedModules)
    }

    @DisplayName("Cross compilation disabled, no cinterops, publish library to mavenLocal")
    @GradleTest
    @OsCondition(supportedOn = [OS.LINUX, OS.WINDOWS], enabledOnCI = [OS.LINUX, OS.WINDOWS])
    fun testDisabledCrossCompilationPublicationWithoutCinterops(
        gradleVersion: GradleVersion,
    ) {
        val multiplatformLibrary = publishMultiplatformLibrary(
            gradleVersion,
            defaultBuildOptions.disableKlibsCrossCompilation()
        ) {
            assertNoDiagnostic(KotlinToolingDiagnostics.CrossCompilationWithCinterops)
        }

        val mavenUrl = multiplatformLibrary.projectPath.resolve("build/repo")
        val libraryRoot = mavenUrl.resolve("com/jetbrains/library")

        // Verify that only the common and JVM module directories exist
        val expectedModules = listOf(
            "multiplatformLibrary",
            "multiplatformLibrary-jvm",
            "multiplatformLibrary-linuxx64",
            "multiplatformLibrary-mingwx64"
        )

        // Check that expected modules exist
        checkExpectedModules(libraryRoot, expectedModules)

        // Verify that native modules are NOT published
        val unexpectedModules = listOf(
            "multiplatformLibrary-iosarm64",
            "multiplatformLibrary-macosarm64",
        )

        for (module in unexpectedModules) {
            val moduleDir = libraryRoot.resolve(module)
            assertFalse(moduleDir.exists(), "Native module directory should not exist when cross-compilation is disabled: $module")
        }
    }

    @DisplayName("Cross compilation enabled, with Linux, Windows and macOS cinterops, publish library to mavenLocal")
    @GradleTest
    fun testCrossCompilationPublicationWithCinterops(
        gradleVersion: GradleVersion,
    ) {
        val multiplatformLibrary = publishMultiplatformLibrary(
            gradleVersion,
            test = {
                embedDirectoryFromTestData("cinterop-lib/cinterop", "src/nativeInterop/cinterop")
                embedDirectoryFromTestData("cinterop-lib/src", "include")
                embedDirectoryFromTestData("cinterop-lib/libs", "libs")
            },
            kmpSetup = {
                macosArm64()
                mingwX64()
                linuxX64()

                setupCInteropForTarget("mylib", KonanTarget.MACOS_ARM64, "mylib_macos.def")
                setupCInteropForTarget("mylib", KonanTarget.LINUX_X64, "mylib_linux.def")
                setupCInteropForTarget("mylib", KonanTarget.MINGW_X64, "mylib_windows.def")
            },
            assertions = {
                if (HostManager.hostIsMac) {
                    assertNoDiagnostic(KotlinToolingDiagnostics.CrossCompilationWithCinterops)
                } else {
                    assertHasDiagnostic(KotlinToolingDiagnostics.CrossCompilationWithCinterops)
                }
            }
        )

        val mavenUrl = multiplatformLibrary.projectPath.resolve("build/repo")
        val libraryRoot = mavenUrl.resolve("com/jetbrains/library")

        val expectedModules = if (HostManager.hostIsMac) {
            listOf(
                "multiplatformLibrary",
                "multiplatformLibrary-linuxx64",
                "multiplatformLibrary-macosarm64",
                "multiplatformLibrary-mingwx64"
            )
        } else {
            listOf(
                "multiplatformLibrary",
                "multiplatformLibrary-linuxx64",
                "multiplatformLibrary-mingwx64"
            )
        }

        // Check that expected modules exist
        checkExpectedModules(libraryRoot, expectedModules)

        val macosInteropKlib = libraryRoot
            .resolve("multiplatformLibrary-macosarm64")
            .resolve("1.0")
            .resolve("multiplatformLibrary-macosarm64-1.0-cinterop-mylib.klib")

        if (HostManager.hostIsMac) {
            assertFileExists(macosInteropKlib, "Expected cinterop klib not found for macOS host")
        } else {
            assertFileNotExists(macosInteropKlib, "Unexpected cinterop klib published on non-macOS host")
        }

        val linuxInteropKlib = libraryRoot
            .resolve("multiplatformLibrary-linuxx64")
            .resolve("1.0")
            .resolve("multiplatformLibrary-linuxx64-1.0-cinterop-mylib.klib")

        assertFileExists(linuxInteropKlib, "Expected cinterop klib not found for macOS host")

        val windowsInteropKlib = libraryRoot
            .resolve("multiplatformLibrary-mingwx64")
            .resolve("1.0")
            .resolve("multiplatformLibrary-mingwx64-1.0-cinterop-mylib.klib")

        assertFileExists(windowsInteropKlib, "Expected cinterop klib not found for macOS host")
    }
}

private fun KGPBaseTest.publishMultiplatformLibrary(
    gradleVersion: GradleVersion,
    buildOptions: BuildOptions = defaultBuildOptions,
    test: TestProject.() -> Unit = {},
    kmpSetup: KotlinMultiplatformExtension.() -> Unit = {
        jvm()
        iosArm64()
        macosArm64()
        mingwX64()
        linuxX64()
    },
    assertions: BuildResult.() -> Unit = {},
) = project(
    "emptyKts",
    gradleVersion,
    buildOptions = buildOptions
) {
    test()
    addKgpToBuildScriptCompilationClasspath()
    settingsBuildScriptInjection {
        settings.rootProject.name = "multiplatformLibrary"
    }
    buildScriptInjection {
        project.applyMultiplatform {
            sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
            kmpSetup()
        }

        project.setupMavenPublication(
            "CrossTest",
            PublisherConfiguration(
                "com.jetbrains.library",
                "1.0",
                "build/repo"
            )
        )
    }

    build(
        "publishAllPublicationsToCrossTestRepository",
        assertions = assertions
    )
}

private fun KotlinMultiplatformExtension.setupCInteropForTarget(
    name: String,
    konanTarget: KonanTarget,
    defFileName: String,
) {
    targets.withType(KotlinNativeTarget::class.java)
        .matching { it.konanTarget == konanTarget }
        .configureEach { target ->
            target.compilations
                .getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
                .cinterops
                .create(name) { interop ->
                    interop.defFile(project.file("src/nativeInterop/cinterop/$defFileName"))
                    interop.includeDirs(project.file("include"))
                }
        }
}

private fun checkExpectedModules(libraryRoot: Path, expectedModules: List<String>) {
    for (module in expectedModules) {
        val moduleDir = libraryRoot.resolve(module)
        assertDirectoryExists(moduleDir, "Module directory not found: $module")

        // Verify version directory exists
        val versionDir = moduleDir.resolve("1.0")
        assertDirectoryExists(versionDir, "Version directory not found for $module")

        // For native targets, verify KLIB exists
        if (module != "multiplatformLibrary" && module != "multiplatformLibrary-jvm") {
            val klibFile = versionDir.resolve("$module-1.0.klib")
            assertFileExists(klibFile, "KLIB file not found for $module")
        }

        // Check for module metadata
        val metadataFile = versionDir.resolve("$module-1.0.module")
        assertFileExists(metadataFile, "Module metadata not found for $module")

        // Check for POM file
        val pomFile = versionDir.resolve("$module-1.0.pom")
        assertFileExists(pomFile, "POM file not found for $module")
    }
}