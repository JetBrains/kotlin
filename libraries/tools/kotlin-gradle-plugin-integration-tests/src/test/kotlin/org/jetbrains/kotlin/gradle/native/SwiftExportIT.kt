/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.runProcess
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.test.assertContains

@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@DisplayName("Tests for Swift Export")
@SwiftExportGradlePluginTests
@GradleTestVersions(minVersion = TestVersions.Gradle.G_7_0)
class SwiftExportIT : KGPBaseTest() {

    @DisplayName("embedAndSign executes normally when Swift Export is enabled")
    @GradleTest
    fun testEmbedAnsSignExecutionWithSwiftExportEnabled(
        gradleVersion: GradleVersion,
        @TempDir testBuildDir: Path,
    ) {
        nativeProject(
            "simpleSwiftExport",
            gradleVersion,
        ) {
            projectPath.enableSwiftExport()

            build(
                ":shared:embedAndSignAppleFrameworkForXcode",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir)
            ) {
                assertTasksExecuted(":shared:iosArm64DebugSwiftExport")
                assertTasksExecuted(":shared:iosArm64MainKlibrary")
                assertTasksExecuted(":shared:compileSwiftExportMainKotlinIosArm64")
                assertTasksExecuted(":shared:linkSwiftExportBinaryDebugStaticIosArm64")
                assertTasksExecuted(":shared:iosArm64DebugGenerateSPMPackage")
                assertTasksExecuted(":shared:iosArm64DebugBuildSPMPackage")
                assertTasksExecuted(":shared:mergeIosDebugSwiftExportLibraries")
                assertTasksExecuted(":shared:copyDebugSPMIntermediates")
                assertTasksSkipped(":shared:embedAndSignAppleFrameworkForXcode")

                assertDirectoryInProjectExists("shared/build/MergedLibraries/ios/Debug")
                assertDirectoryInProjectExists("shared/build/SPMBuild/iosArm64/Debug")
                assertDirectoryInProjectExists("shared/build/SPMDerivedData")
                assertDirectoryInProjectExists("shared/build/SPMPackage/iosArm64/Debug")
                assertDirectoryInProjectExists("shared/build/SwiftExport/iosArm64/Debug")
            }
        }
    }

    @DisplayName("check Swift Export incremental build")
    @GradleTest
    fun testSwiftExportIncrementalBuild(
        gradleVersion: GradleVersion,
        @TempDir testBuildDir: Path,
    ) {
        nativeProject(
            "simpleSwiftExport",
            gradleVersion,
        ) {
            projectPath.enableSwiftExport()

            build(
                ":shared:embedAndSignAppleFrameworkForXcode",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir)
            ) {
                assertTasksExecuted(":shared:copyDebugSPMIntermediates")
            }

            val swiftExportKt = projectPath
                .resolve("shared/src/commonMain/kotlin")
                .resolve("com/github/jetbrains/swiftexport/SwiftExport.kt")

            swiftExportKt.appendText(
                """

                    fun barbarbar(): Int = 145
                """.trimIndent()
            )

            build(
                ":shared:embedAndSignAppleFrameworkForXcode",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir)
            ) {
                assertTasksExecuted(":shared:copyDebugSPMIntermediates")
            }

            val builtProductsDir = projectPath.resolve("shared/build/builtProductsDir")

            val nmOutput = runProcess(
                listOf("nm", "libShared.a"),
                builtProductsDir.toFile()
            )

            assert(nmOutput.isSuccessful) { "nm call was not successfull" }
            assert(nmOutput.output.contains("com_github_jetbrains_swiftexport_barbarbar")) {
                "barbarbar function is missing in libShared.a"
            }
        }
    }

    @DisplayName("check Swift Export fat binary build")
    @GradleTest
    fun testSwiftExportFatBinaryBuild(
        gradleVersion: GradleVersion,
        @TempDir testBuildDir: Path,
    ) {
        nativeProject(
            "simpleSwiftExport",
            gradleVersion,
        ) {
            projectPath.enableSwiftExport()

            build(
                ":shared:embedAndSignAppleFrameworkForXcode",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir, listOf("arm64", "x86_64"), "iphonesimulator")
            ) {
                assertTasksExecuted(":shared:copyDebugSPMIntermediates")
            }

            val builtProductsDir = projectPath.resolve("shared/build/builtProductsDir")

            val lipoOutput = runProcess(
                listOf("lipo", "-archs", "libShared.a"),
                builtProductsDir.toFile()
            )

            assert(lipoOutput.isSuccessful) { "lipo -archs call was not successfull" }

            val libraryArchs = lipoOutput.output.lines().joinToString("").split(" ")

            assertContains(libraryArchs, "arm64", "libShared.a doesn't contain arm64")
            assertContains(libraryArchs, "x86_64", "libShared.a doesn't contain x86_64")
        }
    }

    @DisplayName("Swift Export with multiple source sets")
    @GradleTest
    fun testMultipleSourceSetsWithSwiftExportEnabled(
        gradleVersion: GradleVersion,
        @TempDir testBuildDir: Path,
    ) {
        nativeProject(
            "simpleSwiftExport",
            gradleVersion,
        ) {
            projectPath.enableSwiftExport()

            build(
                ":shared:embedAndSignAppleFrameworkForXcode",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir)
            ) {
                assertTasksExecuted(":shared:copyDebugSPMIntermediates")
                assertDirectoryInProjectExists("shared/build/SwiftExport/iosArm64/Debug")
            }

            val swiftFile = projectPath
                .resolve("shared/build/SwiftExport/iosArm64/Debug/files/Shared/Shared.swift")
                .readText()

            assert(swiftFile.contains("iosBar()")) { "Swift file doesn't contain iosBar() from iosMain source set" }
            assert(swiftFile.contains("iosFoo()")) { "Swift file doesn't contain iosFoo() from iosMain source set" }

            assert(swiftFile.contains("bar()")) { "Swift file doesn't contain bar() from commonMain source set" }
            assert(swiftFile.contains("foo()")) { "Swift file doesn't contain foo() from commonMain source set" }
            assert(swiftFile.contains("foobar(")) { "Swift file doesn't contain foobar( from commonMain source set" }
        }
    }

    @DisplayName("check Swift Export contains symbols for different API surfaces")
    @GradleTest
    fun testSwiftExportMultipleAPISurfaces(
        gradleVersion: GradleVersion,
        @TempDir testBuildDir: Path,
    ) {
        nativeProject(
            "simpleSwiftExport",
            gradleVersion,
        ) {
            projectPath.enableSwiftExport()

            build(
                ":shared:embedAndSignAppleFrameworkForXcode",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir, listOf("arm64", "x86_64"), "iphonesimulator")
            ) {
                assertTasksExecuted(":shared:copyDebugSPMIntermediates")
            }

            val builtProductsDir = projectPath.resolve("shared/build/builtProductsDir").toFile()

            //get x64 slice
            runProcess(
                listOf("lipo", "-thin", "x86_64", "libShared.a", "-output", "libShared_x86.a"),
                builtProductsDir
            )

            //get arm64 slice
            runProcess(
                listOf("lipo", "-thin", "arm64", "libShared.a", "-output", "libShared_arm.a"),
                builtProductsDir
            )

            val x64Symbols = runProcess(
                listOf("nm", "libShared_x86.a"),
                builtProductsDir
            )

            val arm64Symbols = runProcess(
                listOf("nm", "libShared_arm.a"),
                builtProductsDir
            )

            assert(x64Symbols.output.contains("iosX64Bar")) {
                "Doesn't contain iosX64Bar() from iosX64Main API surface"
            }

            assert(arm64Symbols.output.contains("iosSimulatorArm64Bar")) {
                "Doesn't contain iosSimulatorArm64Bar() from iosSimulatorArm64Main API surface"
            }

            val sdkVersion = runProcess(
                listOf("xcrun", "--sdk", "iphonesimulator", "--show-sdk-version"),
                projectPath.toFile()
            )

            assert(sdkVersion.isSuccessful)

            // Check arm64 compilation
            val arm64Compilation = swiftCompile(
                projectPath.toFile(),
                builtProductsDir,
                "arm64-apple-ios${sdkVersion.output.trim()}-simulator"
            )

            // Check x86_64 compilation
            val x64Compilation = swiftCompile(
                projectPath.toFile(),
                builtProductsDir,
                "x86_64-apple-ios${sdkVersion.output.trim()}-simulator"
            )

            assert(arm64Compilation.isSuccessful)
            assert(x64Compilation.isSuccessful)
        }
    }
}

@OptIn(EnvironmentalVariablesOverride::class)
private fun GradleProject.swiftExportEmbedAndSignEnvVariables(
    testBuildDir: Path,
    archs: List<String> = listOf("arm64"),
    sdk: String = "iphoneos123",
) = EnvironmentalVariables(
    "CONFIGURATION" to "Debug",
    "SDK_NAME" to sdk,
    "ARCHS" to archs.joinToString(" "),
    "ONLY_ACTIVE_ARCH" to "YES",
    "TARGET_BUILD_DIR" to testBuildDir.absolutePathString(),
    "FRAMEWORKS_FOLDER_PATH" to "build/xcode-derived",
    "PLATFORM_NAME" to "iphoneos",
    "BUILT_PRODUCTS_DIR" to projectPath.resolve("shared/build/builtProductsDir").absolutePathString(),
)

private fun swiftCompile(workingDir: File, libDir: File, target: String) = runProcess(
    listOf(
        "xcrun", "--sdk", "iphonesimulator", "swiftc", "./Consumer.swift",
        "-I", libDir.canonicalPath, "-target", target,
        "-Xlinker", "-L", "-Xlinker", libDir.canonicalPath, "-Xlinker", "-lShared",
        "-framework", "Foundation", "-framework", "UIKit"
    ),
    workingDir
)

internal fun Path.enableSwiftExport() {
    resolve("local.properties")
        .also { if (!it.exists()) it.createFile() }
        .appendText(
            """
            
            kotlin.swift-export.enabled=true
            """.trimIndent()
        )
}