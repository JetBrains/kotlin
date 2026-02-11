/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.include
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.kotlin.gradle.util.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.appendText
import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@DisplayName("Tests for Swift Export")
@SwiftExportGradlePluginTests
class SwiftExportIT : KGPBaseTest() {

    // USR expectations are hardcoded. See docs/swift-export/testing-usr-stability.md.

    private companion object {
        private const val DEFAULT_IOS_DEPLOYMENT_TARGET = "14.1"
    }

    @DisplayName("embedSwiftExportForXcode fail")
    @GradleTest
    fun shouldFailWithExecutingEmbedSwiftExportForXcode(
        gradleVersion: GradleVersion,
    ) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                }
            }
            buildAndFail(":embedSwiftExportForXcode") {
                assertOutputContains("Please run the embedSwiftExportForXcode task from Xcode")
                assertOutputDoesNotContain("ConfigurationCacheProblemsException: Configuration cache problems found in this build")
            }
        }
    }

    @DisplayName("embedSwiftExport executes normally")
    @GradleTest
    fun testSwiftExportExecutionWithSwiftExportEnabled(
        gradleVersion: GradleVersion,
        @TempDir testBuildDir: Path,
    ) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            settingsBuildScriptInjection {
                settings.rootProject.name = "shared"
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                }
            }

            build(
                ":embedSwiftExportForXcode",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir)
            ) {
                assertTasksExecuted(":iosArm64DebugSwiftExport")
                assertTasksExecuted(":iosArm64MainKlibrary")
                assertTasksExecuted(":compileKotlinIosArm64")
                assertTasksExecuted(":compileSwiftExportMainKotlinIosArm64")
                assertTasksExecuted(":linkSwiftExportBinaryDebugStaticIosArm64")
                assertTasksExecuted(":iosArm64DebugGenerateSPMPackage")
                assertTasksExecuted(":iosArm64DebugBuildSPMPackage")
                assertTasksExecuted(":mergeIosDebugSwiftExportLibraries")
                assertTasksExecuted(":copyDebugSPMIntermediates")
                assertTasksExecuted(":embedSwiftExportForXcode")

                assertDirectoryInProjectExists("build/MergedLibraries/ios/Debug")
                assertDirectoryInProjectExists("build/SPMBuild/iosArm64/Debug")
                assertDirectoryInProjectExists("build/SPMDerivedData")
                assertDirectoryInProjectExists("build/SPMPackage/iosArm64/Debug")
                assertDirectoryInProjectExists("build/SwiftExport/iosArm64/Debug")

                val buildProductsDir = this@project.gradleRunner.environment?.get("BUILT_PRODUCTS_DIR")?.let { File(it) }
                assertNotNull(buildProductsDir)

                val exportedKotlinPackagesSwiftModule = buildProductsDir.resolve("ExportedKotlinPackages.swiftmodule")
                val kotlinRuntime = buildProductsDir.resolve("KotlinRuntime")
                val libShared = buildProductsDir.resolve("libShared.a")
                val sharedSwiftModule = buildProductsDir.resolve("Shared.swiftmodule")
                val sharedBridgeShared = buildProductsDir.resolve("SharedBridge_Shared")

                assertDirectoriesExist(
                    exportedKotlinPackagesSwiftModule.toPath(),
                    kotlinRuntime.toPath(),
                    sharedSwiftModule.toPath(),
                    sharedBridgeShared.toPath()
                )

                assertFileExists(libShared.toPath())

                assertHasDiagnostic(KotlinToolingDiagnostics.ExperimentalFeatureWarning, "Swift Export")
            }
        }
    }

    @DisplayName("check Swift Export incremental build")
    @GradleTest
    fun testSwiftExportIncrementalBuild(
        gradleVersion: GradleVersion,
        @TempDir testBuildDir: Path,
    ) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            settingsBuildScriptInjection {
                settings.rootProject.name = "shared"
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                }
            }

            val source = projectPath.resolve("src/commonMain/kotlin").createDirectories().resolve("Foo.kt")

            source.writeText(
                """
                    package com.github.jetbrains.swiftexport
                    
                    fun functionToRemove(): Int = 4444
                """.trimIndent()
            )

            build(
                ":embedSwiftExportForXcode",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir)
            ) {
                assertTasksExecuted(":copyDebugSPMIntermediates")
            }

            source.appendText(
                """

                    fun barbarbar(): Int = 145
                """.trimIndent()
            )

            source.replaceText("fun functionToRemove(): Int = 4444", "")

            build(
                ":embedSwiftExportForXcode",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir)
            ) {
                assertTasksExecuted(":copyDebugSPMIntermediates")
            }

            val builtProductsDir = projectPath.resolve("build/builtProductsDir")

            val nmOutput = runProcess(
                listOf("nm", "libShared.a"),
                builtProductsDir.toFile()
            )

            assert(nmOutput.isSuccessful) { "nm call was not successfull" }
            assert(nmOutput.output.contains("com_github_jetbrains_swiftexport_barbarbar")) {
                "barbarbar function is missing in libShared.a"
            }
            assert(nmOutput.output.contains("com_github_jetbrains_swiftexport_functionToRemove").not()) {
                "functionToRemove function is present in libShared.a"
            }

            // Verify Swift module API surface using symbolgraph-extract
            assertSwiftModuleSymbols(
                workingDir = projectPath.toFile(),
                moduleName = "Shared",
                target = "arm64-apple-ios$DEFAULT_IOS_DEPLOYMENT_TARGET",
                sdk = "iphoneos",
                searchPaths = listOf(builtProductsDir.toFile()),
                expectedSymbols = setOf(
                    SwiftSymbol(
                        "s:22ExportedKotlinPackages3comO6githubO9jetbrainsO11swiftexportO6SharedE9barbarbars5Int32VyFZ",
                        listOf("barbarbar()")
                    )
                )
            )
        }
    }

    @DisplayName("check Swift Export fat binary build")
    @GradleTest
    fun testSwiftExportFatBinaryBuild(
        gradleVersion: GradleVersion,
        @TempDir testBuildDir: Path,
    ) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            settingsBuildScriptInjection {
                settings.rootProject.name = "shared"
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosSimulatorArm64()
                    @Suppress("DEPRECATION") // fixme: KT-81704 Cleanup tests after apple x64 family deprecation
                    iosX64()

                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                }
            }
            build(
                ":embedSwiftExportForXcode",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir, listOf("arm64", "x86_64"), "iphonesimulator"),
            ) {
                assertTasksExecuted(":copyDebugSPMIntermediates")
            }

            val builtProductsDir = projectPath.resolve("build/builtProductsDir")

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
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            settingsBuildScriptInjection {
                settings.rootProject.name = "shared"
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()

                    sourceSets.commonMain.get().compileSource(
                        """
                            fun bar() {}
                            fun foo() {}
                            fun foobar() {}
                        """.trimIndent()
                    )

                    sourceSets.iosMain.get().compileSource(
                        """
                            fun iosBar() {}
                            fun iosFoo() {}
                        """.trimIndent()
                    )
                }
            }
            build(
                ":iosArm64DebugSwiftExport",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir)
            ) {
                assertTasksExecuted(":iosArm64DebugSwiftExport")
                assertDirectoryInProjectExists("build/SwiftExport/iosArm64/Debug")
            }

            val swiftFile = projectPath
                .resolve("build/SwiftExport/iosArm64/Debug/files/Shared/Shared.swift")
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
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            settingsBuildScriptInjection {
                settings.rootProject.name = "shared"
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosSimulatorArm64()
                    @Suppress("DEPRECATION") // fixme: KT-81704 Cleanup tests after apple x64 family deprecation
                    iosX64()
                    iosArm64()

                    sourceSets.iosArm64Main.get().compileSource("fun iosArm64Bar() {}")
                    sourceSets.iosSimulatorArm64Main.get().compileSource("fun iosSimulatorArm64Bar() {}")
                    sourceSets.iosX64Main.get().compileSource("fun iosX64Bar() {}")
                }
            }
            build(
                ":embedSwiftExportForXcode",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir, listOf("arm64", "x86_64"), "iphonesimulator"),
            ) {
                assertTasksExecuted(":copyDebugSPMIntermediates")
            }

            val builtProductsDir = projectPath.resolve("build/builtProductsDir").toFile()

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

            val consumer = projectPath.resolve("Consumer.swift").also {
                it.writeText(swiftConsumerSource)
            }.toFile()

            // Check arm64 compilation
            val arm64Compilation = swiftCompile(
                projectPath.toFile(),
                builtProductsDir,
                consumer,
                "arm64-apple-ios${sdkVersion.output.trim()}-simulator"
            )

            // Check x86_64 compilation
            val x64Compilation = swiftCompile(
                projectPath.toFile(),
                builtProductsDir,
                consumer,
                "x86_64-apple-ios${sdkVersion.output.trim()}-simulator"
            )

            assert(arm64Compilation.isSuccessful)
            assert(x64Compilation.isSuccessful)

            // Verify Swift module API surface for arm64 (iosSimulatorArm64)
            assertSwiftModuleSymbols(
                workingDir = projectPath.toFile(),
                moduleName = "Shared",
                target = "arm64-apple-ios${sdkVersion.output.trim()}-simulator",
                sdk = "iphonesimulator",
                searchPaths = listOf(builtProductsDir),
                expectedSymbols = setOf(SwiftSymbol("s:6Shared20iosSimulatorArm64BaryyF", listOf("iosSimulatorArm64Bar()")))
            )

            // Verify Swift module API surface for x86_64 (iosX64)
            assertSwiftModuleSymbols(
                workingDir = projectPath.toFile(),
                moduleName = "Shared",
                target = "x86_64-apple-ios${sdkVersion.output.trim()}-simulator",
                sdk = "iphonesimulator",
                searchPaths = listOf(builtProductsDir),
                expectedSymbols = setOf(SwiftSymbol("s:6Shared9iosX64BaryyF", listOf("iosX64Bar()")))
            )
        }
    }

    @DisplayName("Test exporting transitive dependencies")
    @GradleTest
    fun testExportingTransitiveDependencies(
        gradleVersion: GradleVersion,
        @TempDir testBuildDir: Path,
    ) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            settingsBuildScriptInjection {
                settings.rootProject.name = "shared"
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()

                    sourceSets.commonMain {
                        compileSource(
                            """
                            import org.foo.One
                            fun foo(): One = One()
                            """.trimIndent()
                        )

                        dependencies {
                            implementation(project(":dep-one"))
                            implementation(project(":dep-two"))
                        }
                    }
                }
            }

            val subprojectOne = project("empty", gradleVersion) {
                buildScriptInjection {
                    project.applyMultiplatform {
                        iosArm64()
                        sourceSets.commonMain.get().compileSource(
                            """
                            package org.foo
                            class One
                            """.trimIndent()
                        )
                    }
                }
            }

            val subprojectTwo = project("empty", gradleVersion) {
                buildScriptInjection {
                    project.applyMultiplatform {
                        iosArm64()
                        sourceSets.commonMain.get().compileSource(
                            """
                            package org.bar
                            class Two
                            """.trimIndent()
                        )
                    }
                }
            }

            include(subprojectOne, "dep-one")
            include(subprojectTwo, "dep-two")

            // dep-two is intentionally not part of the exported API surface.
            build(
                ":embedSwiftExportForXcode",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir)
            ) {
                assertTasksExecuted(":dep-one:compileKotlinIosArm64")
                assertTasksExecuted(":dep-two:compileKotlinIosArm64")
                assertTasksExecuted(":compileKotlinIosArm64")

                val sharedPath = projectPath.resolve("build/SwiftExport/iosArm64/Debug/files/Shared")
                val depOnePath = projectPath.resolve("build/SwiftExport/iosArm64/Debug/files/SharedDepOne")
                val depTwoPath = projectPath.resolve("build/SwiftExport/iosArm64/Debug/files/SharedDepTwo")

                assertDirectoryExists(sharedPath)
                assertDirectoryExists(depOnePath)
                assertDirectoryDoesNotExist(depTwoPath)

                val modulesFile = projectPath.resolve("build/SwiftExport/iosArm64/Debug/modules/Shared.json")
                assertFileExists(modulesFile)

                val modules = parseJsonToMap(modulesFile).getNestedList("modules")
                assertNotNull(modules)

                val actualModules = modules.map { it["name"]?.jsonPrimitive?.content ?: "" }.toSet()

                assertEquals(
                    setOf("Shared", "SharedDepOne", "ExportedKotlinPackages", "KotlinRuntimeSupport"),
                    actualModules
                )
            }

            val builtProductsDir = projectPath.resolve("build/builtProductsDir").toFile()
            // Verify Swift module API surface for all exported modules
            // - Shared: exports foo() which returns One from SharedDepOne
            // - SharedDepOne: exports the One class
            // - ExportedKotlinPackages: contains package-namespaced declarations (empty in this test)
            // - KotlinRuntimeSupport: exposes Swift-side runtime helpers
            assertAllSwiftModuleSymbols(
                workingDir = projectPath.toFile(),
                builtProductsDir = builtProductsDir,
                target = "arm64-apple-ios$DEFAULT_IOS_DEPLOYMENT_TARGET",
                sdk = "iphoneos",
                expectedSymbolsByModule = mapOf(
                    "Shared" to setOf(
                        SwiftSymbol(
                            "s:6Shared3foo22ExportedKotlinPackages3orgOABO0A6DepOneE0H0CyF",
                            listOf("foo()")
                        )
                    ),
                    "SharedDepOne" to setOf(
                        SwiftSymbol(
                            "s:22ExportedKotlinPackages3orgO3fooO12SharedDepOneE0H0C",
                            listOf("org.foo.One")
                        ),
                        SwiftSymbol(
                            "s:22ExportedKotlinPackages3orgO3fooO12SharedDepOneE0H0CAHycfc",
                            listOf("org.foo.One.init()")
                        )
                    ),
                    "ExportedKotlinPackages" to setOf(
                        SwiftSymbol("s:22ExportedKotlinPackages3orgO", listOf("org")),
                        SwiftSymbol("s:22ExportedKotlinPackages3orgO3fooO", listOf("org.foo"))
                    ),
                    // KotlinError exports the struct and its declared members.
                    "KotlinRuntimeSupport" to setOf(
                        SwiftSymbol("s:20KotlinRuntimeSupport0A5ErrorV", listOf("KotlinError")),
                        SwiftSymbol(
                            "s:20KotlinRuntimeSupport0A5ErrorV7wrappedACSo0A4BaseC_tcfc",
                            listOf("KotlinError.init(wrapped:)")
                        ),
                        SwiftSymbol(
                            "s:20KotlinRuntimeSupport0A5ErrorV7wrappedSo0A4BaseCvp",
                            listOf("KotlinError.wrapped")
                        ),
                        SwiftSymbol(
                            "s:20KotlinRuntimeSupport0A5ErrorV11descriptionSSvp",
                            listOf("KotlinError.description")
                        )
                    )
                )
            )
        }
    }

    @DisplayName("Test dependency resolution with misconfigured repository")
    @GradleTest
    fun testDependencyResolutionWithMisconfiguredRepository(
        gradleVersion: GradleVersion,
        @TempDir testBuildDir: Path,
    ) {
        // Publish dependency
        val multiplatformLibrary = publishMultiplatformLibrary(gradleVersion) {
            iosArm64()
            sourceSets.commonMain.get().compileSource(
                """
                            package org.foo
                            class One
                            """.trimIndent()
            )
        }

        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            settingsBuildScriptInjection {
                settings.rootProject.name = "shared"
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()

                    sourceSets.commonMain {
                        compileSource(
                            """
                            import org.foo.One
                            fun foo(): One = One()
                            """.trimIndent()
                        )
                        dependencies {
                            implementation(multiplatformLibrary.rootCoordinate)
                        }
                    }
                }
            }

            buildAndFail(
                ":embedSwiftExportForXcode",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir)
            ) {
                assertOutputContains("Could not find ${multiplatformLibrary.rootCoordinate}")
            }
        }
    }

    @DisplayName("Test build with new iOS apis which are not available in the minimum deployment target")
    @GradleTest
    fun testNewApiAvailability(
        gradleVersion: GradleVersion,
        @TempDir testBuildDir: Path,
    ) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            settingsBuildScriptInjection {
                settings.rootProject.name = "shared"
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    // UIContentUnavailableConfiguration is available only starting iOS sdk 17
                    sourceSets.iosMain.get().compileSource(
                        """
                            import platform.UIKit.UIContentUnavailableConfiguration
                            fun demo(): UIContentUnavailableConfiguration = UIContentUnavailableConfiguration.emptyConfiguration()
                        """.trimIndent()
                    )
                }
            }

            // Build with iOS sdk 14, it should fail
            buildAndFail(
                ":embedSwiftExportForXcode",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir, iphoneOsDeploymentTarget = "14.0")
            ) {
                assertTasksFailed(":iosArm64DebugBuildSPMPackage")
            }

            // Build with iOS sdk 17, it should succeed
            build(
                ":embedSwiftExportForXcode",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir, iphoneOsDeploymentTarget = "17.0")
            ) {
                assertTasksExecuted(":iosArm64DebugBuildSPMPackage")
            }

            // Verify Swift module API surface using symbolgraph-extract
            val builtProductsDir = projectPath.resolve("build/builtProductsDir")
            assertSwiftModuleSymbols(
                workingDir = projectPath.toFile(),
                moduleName = "Shared",
                target = "arm64-apple-ios17.0",
                sdk = "iphoneos",
                searchPaths = listOf(builtProductsDir.toFile()),
                expectedSymbols = setOf(SwiftSymbol("s:6Shared4demo5UIKit33UIContentUnavailableConfigurationVyF", listOf("demo()")))
            )
        }
    }
}
