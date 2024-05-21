/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.appendText
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.readText

@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@DisplayName("Tests for Swift Export")
@SwiftExportGradlePluginTests
@GradleTestVersions(minVersion = TestVersions.Gradle.G_7_0)
@OptIn(EnvironmentalVariablesOverride::class)
class SwiftExportIT : KGPBaseTest() {

    @DisplayName("embedAndSign executes normally when Swift Export is enabled")
    @OptIn(EnvironmentalVariablesOverride::class)
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

            val environmentVariables = EnvironmentalVariables(
                "CONFIGURATION" to "Debug",
                "SDK_NAME" to "iphoneos123",
                "ARCHS" to "arm64",
                "ONLY_ACTIVE_ARCH" to "YES",
                "TARGET_BUILD_DIR" to testBuildDir.toString(),
                "FRAMEWORKS_FOLDER_PATH" to "build/xcode-derived",
                "PLATFORM_NAME" to "iphoneos",
                "BUILT_PRODUCTS_DIR" to projectPath.resolve("shared/build/builtProductsDir").toString(),
            )

            build(":shared:embedAndSignAppleFrameworkForXcode", environmentVariables = environmentVariables) {
                assertTasksSkipped(":shared:assembleDebugAppleFrameworkForXcodeIosArm64")
                assertTasksExecuted(":shared:iosArm64DebugSwiftExport")
                assertTasksExecuted(":shared:iosArm64MainKlibrary")
                assertTasksExecuted(":shared:compileSwiftExportMainKotlinIosArm64")
                assertTasksExecuted(":shared:linkSwiftExportBinaryDebugStaticIosArm64")
                assertTasksExecuted(":shared:iosArm64DebugGenerateSPMPackage")
                assertTasksExecuted(":shared:iosArm64DebugBuildSPMPackage")
                assertTasksExecuted(":shared:iosArm64DebugCopySPMIntermediates")
                assertTasksSkipped(":shared:embedAndSignAppleFrameworkForXcode")

                assertDirectoryInProjectExists("shared/build/iosArm64DebugSPMPackage")
                assertDirectoryInProjectExists("shared/build/iosArm64DebugSwiftExport")
            }
        }
    }

    @DisplayName("Swift Export with multiple source sets")
    @OptIn(EnvironmentalVariablesOverride::class)
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

            val environmentVariables = EnvironmentalVariables(
                "CONFIGURATION" to "Debug",
                "SDK_NAME" to "iphoneos123",
                "ARCHS" to "arm64",
                "ONLY_ACTIVE_ARCH" to "YES",
                "TARGET_BUILD_DIR" to testBuildDir.toString(),
                "FRAMEWORKS_FOLDER_PATH" to "build/xcode-derived",
                "PLATFORM_NAME" to "iphoneos",
                "BUILT_PRODUCTS_DIR" to projectPath.resolve("shared/build/builtProductsDir").toString(),
            )

            build(":shared:embedAndSignAppleFrameworkForXcode", environmentVariables = environmentVariables) {
                assertTasksExecuted(":shared:iosArm64DebugCopySPMIntermediates")
                assertDirectoryInProjectExists("shared/build/iosArm64DebugSwiftExport")
            }

            val swiftFile = projectPath
                .resolve("shared/build/iosArm64DebugSwiftExport/files/Shared.swift")
                .readText()

            assert(swiftFile.contains("iosBar()")) { "Swift file doesn't contain iosBar() from iosMain source set" }
            assert(swiftFile.contains("iosFoo()")) { "Swift file doesn't contain iosFoo() from iosMain source set" }

            assert(swiftFile.contains("bar()")) { "Swift file doesn't contain bar() from commonMain source set" }
            assert(swiftFile.contains("foo()")) { "Swift file doesn't contain foo() from commonMain source set" }
            assert(swiftFile.contains("foobar(")) { "Swift file doesn't contain foobar( from commonMain source set" }
        }
    }
}

internal fun Path.enableSwiftExport() {
    resolve("local.properties")
        .also { if (!it.exists()) it.createFile() }
        .appendText(
            """
            
            kotlin.swift-export.enabled=true
            """.trimIndent()
        )
}