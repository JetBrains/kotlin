/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.SimpleSwiftExportProperties
import org.jetbrains.kotlin.gradle.util.enableSwiftExport
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.*

@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@DisplayName("Tests for Swift Export DSL")
@SwiftExportGradlePluginTests
@GradleTestVersions(minVersion = TestVersions.Gradle.G_7_4) // DefaultResolvedComponentResult with configuration cache is supported only after 7.4
class SwiftExportDslIT : KGPBaseTest() {

    @DisplayName("embedSwiftExport executes normally when only one target is enabled in Swift Export DSL")
    @GradleTest
    fun testSwiftExportDSLSingleProject(
        gradleVersion: GradleVersion,
        @TempDir testBuildDir: Path,
    ) {
        nativeProject(
            "simpleSwiftExport",
            gradleVersion,
        ) {
            projectPath.enableSwiftExport()
            projectPath.resolve("shared/src/commonMain/kotlin/com/github/jetbrains/swiftexport/Subproject.kt").deleteExisting()
            projectPath.resolve("shared/src/commonMain/kotlin/com/github/jetbrains/swiftexport/UglySubproject.kt").deleteExisting()

            build(
                ":shared:embedSwiftExportForXcode",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir),
                buildOptions = defaultBuildOptions.copy(
                    configurationCache = BuildOptions.ConfigurationCacheValue.ENABLED,
                )
            ) {
                assertTasksExecuted(":shared:compileKotlinIosArm64")
                assertTasksAreNotInTaskGraph(":subproject:compileKotlinIosArm64")
                assertTasksAreNotInTaskGraph(":not-good-looking-project-name:compileKotlinIosArm64")
                assertTasksExecuted(":shared:copyDebugSPMIntermediates")
                assertTasksSkipped(":shared:embedSwiftExportForXcode")

                val libraryPath = "shared/build/MergedLibraries/ios/Debug"
                assertDirectoryInProjectExists(libraryPath)
                assertFileExists(projectPath.resolve(libraryPath).resolve("libShared.a"))

                val sharedPath = "shared/build/SwiftExport/iosArm64/Debug/files/Shared"
                assertDirectoryInProjectExists(sharedPath)
                assertFileExists(projectPath.resolve(sharedPath).resolve("Shared.h"))
                assertFileExists(projectPath.resolve(sharedPath).resolve("Shared.kt"))
                assertFileExists(projectPath.resolve(sharedPath).resolve("Shared.swift"))
            }
        }
    }

    @DisplayName("embedSwiftExport executes normally when export module is defined in Swift Export DSL")
    @GradleTest
    fun testSwiftExportDSLExportModule(
        gradleVersion: GradleVersion,
        @TempDir testBuildDir: Path,
    ) {
        nativeProject(
            "simpleSwiftExport",
            gradleVersion,
        ) {
            projectPath.enableSwiftExport()

            build(
                ":shared:embedSwiftExportForXcode",
                "-P${SimpleSwiftExportProperties.DSL_EXPORT}",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir),
                buildOptions = defaultBuildOptions.copy(
                    configurationCache = BuildOptions.ConfigurationCacheValue.ENABLED,
                )
            ) {
                assertTasksExecuted(":shared:compileKotlinIosArm64")
                assertTasksExecuted(":subproject:compileKotlinIosArm64")
                assertTasksExecuted(":not-good-looking-project-name:compileKotlinIosArm64")
                assertTasksExecuted(":shared:copyDebugSPMIntermediates")
                assertTasksSkipped(":shared:embedSwiftExportForXcode")

                val libraryPath = "shared/build/MergedLibraries/ios/Debug"
                assertDirectoryInProjectExists(libraryPath)
                assertFileExists(projectPath.resolve(libraryPath).resolve("libShared.a"))

                val sharedPath = "shared/build/SwiftExport/iosArm64/Debug/files/Shared"
                assertDirectoryInProjectExists(sharedPath)
                assertFileExists(projectPath.resolve(sharedPath).resolve("Shared.h"))
                assertFileExists(projectPath.resolve(sharedPath).resolve("Shared.kt"))
                assertFileExists(projectPath.resolve(sharedPath).resolve("Shared.swift"))

                val subprojectPath = "shared/build/SwiftExport/iosArm64/Debug/files/Subproject"
                assertDirectoryInProjectExists(subprojectPath)
                assertFileExists(projectPath.resolve(subprojectPath).resolve("Subproject.h"))
                assertFileExists(projectPath.resolve(subprojectPath).resolve("Subproject.kt"))
                assertFileExists(projectPath.resolve(subprojectPath).resolve("Subproject.swift"))

                val uglyProjectPath = "shared/build/SwiftExport/iosArm64/Debug/files/NotGoodLookingProjectName"
                assertDirectoryInProjectExists(uglyProjectPath)
                assertFileExists(projectPath.resolve(uglyProjectPath).resolve("NotGoodLookingProjectName.h"))
                assertFileExists(projectPath.resolve(uglyProjectPath).resolve("NotGoodLookingProjectName.kt"))
                assertFileExists(projectPath.resolve(uglyProjectPath).resolve("NotGoodLookingProjectName.swift"))
            }
        }
    }

    @DisplayName("embedSwiftExport executes normally when custom module name is defined in Swift Export DSL")
    @GradleTest
    fun testSwiftExportDSLCustomModuleName(
        gradleVersion: GradleVersion,
        @TempDir testBuildDir: Path,
    ) {
        nativeProject(
            "simpleSwiftExport",
            gradleVersion,
        ) {
            projectPath.enableSwiftExport()
            projectPath.resolve("shared/src/commonMain/kotlin/com/github/jetbrains/swiftexport/UglySubproject.kt").deleteExisting()

            build(
                ":shared:embedSwiftExportForXcode",
                "-P${SimpleSwiftExportProperties.DSL_CUSTOM_NAME}",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir),
                buildOptions = defaultBuildOptions.copy(
                    configurationCache = BuildOptions.ConfigurationCacheValue.ENABLED,
                )
            ) {
                assertTasksExecuted(":shared:compileKotlinIosArm64")
                assertTasksExecuted(":subproject:compileKotlinIosArm64")
                assertTasksExecuted(":shared:copyDebugSPMIntermediates")
                assertTasksSkipped(":shared:embedSwiftExportForXcode")

                val libraryPath = "shared/build/MergedLibraries/ios/Debug"
                assertDirectoryInProjectExists(libraryPath)
                assertFileExists(projectPath.resolve(libraryPath).resolve("libCustomShared.a"))

                val sharedPath = "shared/build/SwiftExport/iosArm64/Debug/files/CustomShared"
                assertDirectoryInProjectExists(sharedPath)
                assertFileExists(projectPath.resolve(sharedPath).resolve("CustomShared.h"))
                assertFileExists(projectPath.resolve(sharedPath).resolve("CustomShared.kt"))
                assertFileExists(projectPath.resolve(sharedPath).resolve("CustomShared.swift"))

                val subprojectPath = "shared/build/SwiftExport/iosArm64/Debug/files/CustomSubProject"
                assertDirectoryInProjectExists(subprojectPath)
                assertFileExists(projectPath.resolve(subprojectPath).resolve("CustomSubProject.h"))
                assertFileExists(projectPath.resolve(subprojectPath).resolve("CustomSubProject.kt"))
                assertFileExists(projectPath.resolve(subprojectPath).resolve("CustomSubProject.swift"))
            }
        }
    }

    @DisplayName("embedSwiftExport executes normally when package flatten rule is defined in Swift Export DSL")
    @GradleTest
    fun testSwiftExportDSLWithPackageFlatteringRuleEnabled(
        gradleVersion: GradleVersion,
        @TempDir testBuildDir: Path,
    ) {
        nativeProject(
            "simpleSwiftExport",
            gradleVersion,
        ) {
            projectPath.enableSwiftExport()
            projectPath.resolve("shared/src/commonMain/kotlin/com/github/jetbrains/swiftexport/UglySubproject.kt").deleteExisting()

            build(
                ":shared:embedSwiftExportForXcode",
                "-P${SimpleSwiftExportProperties.DSL_FLATTEN_PACKAGE}",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir),
                buildOptions = defaultBuildOptions.copy(
                    configurationCache = BuildOptions.ConfigurationCacheValue.ENABLED,
                )
            ) {
                assertTasksExecuted(":shared:compileKotlinIosArm64")
                assertTasksExecuted(":subproject:compileKotlinIosArm64")
                assertTasksExecuted(":shared:copyDebugSPMIntermediates")
                assertTasksSkipped(":shared:embedSwiftExportForXcode")

                val sharedSwiftPath = projectPath.resolve("shared/build/SwiftExport/iosArm64/Debug/files/Shared/Shared.swift")
                assert(sharedSwiftPath.readText().contains("public extension ExportedKotlinPackages.com.github.jetbrains.swiftexport"))

                val subprojectSwiftPath = projectPath.resolve("shared/build/SwiftExport/iosArm64/Debug/files/Subproject/Subproject.swift")
                assert(subprojectSwiftPath.readText().contains("public extension ExportedKotlinPackages.com.subproject.library"))
            }
        }
    }
}