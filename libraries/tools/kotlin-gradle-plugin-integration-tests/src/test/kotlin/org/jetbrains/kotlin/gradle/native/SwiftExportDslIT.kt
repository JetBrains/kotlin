/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.*
import org.jetbrains.kotlin.gradle.util.publishMultiplatformLibrary
import org.jetbrains.kotlin.gradle.util.swiftExportEmbedAndSignEnvVariables
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@DisplayName("Tests for Swift Export DSL")
@SwiftExportGradlePluginTests
@OptIn(ExperimentalSwiftExportDsl::class)
class SwiftExportDslIT : KGPBaseTest() {

    /**
     * The single top-level `public typealias <name> = ...` declaration in generated Swift, so that assertions compare
     * the whole declaration and not a substring of the file.
     */
    private fun Path.typealiasOf(name: String): String? = readText()
        .lineSequence()
        .singleOrNull { it.startsWith("public typealias $name ") }
        ?.trim()

    /**
     * Publishes a library that declares its Swift Export module name and package flatten rule.
     */
    private fun publishLibraryWithSwiftExportMetadata(
        gradleVersion: GradleVersion,
        projectName: String,
        swiftModuleName: String,
        packageToFlatten: String,
        source: String,
    ): PublishedProject = project("empty", gradleVersion) {
        plugins {
            kotlin("multiplatform")
        }
        settingsBuildScriptInjection {
            settings.rootProject.name = projectName
        }
        buildScriptInjection {
            project.applyMultiplatform {
                iosArm64()
                sourceSets.commonMain.get().compileSource(source)
                with(swiftExport) {
                    moduleName.set(swiftModuleName)
                    flattenPackage.set(packageToFlatten)
                }
            }
        }
    }.publish()

    @DisplayName("embedSwiftExport executes normally when export module is defined in Swift Export DSL")
    @GradleTest
    fun testSwiftExportDSLExportModule(
        gradleVersion: GradleVersion,
        @TempDir testBuildDir: Path,
    ) {
        project(
            "empty",
            gradleVersion
        ) {
            plugins {
                kotlin("multiplatform")
            }
            settingsBuildScriptInjection {
                settings.rootProject.name = "shared"
            }
            buildScriptInjection {
                with(project) {
                    applyMultiplatform {
                        iosArm64()
                        with(swiftExport) {
                            export(dependencies.project(mapOf("path" to ":subproject")))
                            export(dependencies.project(mapOf("path" to ":not-good-looking-project-name")))
                        }

                        sourceSets.commonMain {
                            compileStubSourceWithSourceSetName()

                            dependencies {
                                implementation(project(":subproject"))
                                implementation(project(":not-good-looking-project-name"))
                            }
                        }
                    }
                }
            }

            val subprojectOne = project("empty", gradleVersion) {
                buildScriptInjection {
                    project.applyMultiplatform {
                        iosArm64()
                        sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                    }
                }
            }

            val subprojectTwo = project("empty", gradleVersion) {
                buildScriptInjection {
                    project.applyMultiplatform {
                        iosArm64()
                        sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                    }
                }
            }

            include(subprojectOne, "subproject")
            include(subprojectTwo, "not-good-looking-project-name")

            build(
                ":embedSwiftExportForXcode",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir)
            ) {
                val buildProductsDir = this@project.gradleRunner.environment?.get("BUILT_PRODUCTS_DIR")?.let { File(it) }
                assertNotNull(buildProductsDir)

                val exportedKotlinPackagesSwiftModule = buildProductsDir.resolve("ExportedKotlinPackages.swiftmodule")
                val kotlinRuntime = buildProductsDir.resolve("KotlinRuntime")
                val libShared = buildProductsDir.resolve("libShared.a")
                val notGoodLookingProjectSwiftModule = buildProductsDir.resolve("NotGoodLookingProjectName.swiftmodule")
                val sharedBridgeNotGoodLookingProject = buildProductsDir.resolve("SharedBridge_NotGoodLookingProjectName")
                val sharedSwiftModule = buildProductsDir.resolve("Shared.swiftmodule")
                val sharedBridgeShared = buildProductsDir.resolve("SharedBridge_Shared")
                val subprojectSwiftModule = buildProductsDir.resolve("Subproject.swiftmodule")
                val sharedBridgeSubproject = buildProductsDir.resolve("SharedBridge_Subproject")

                assertDirectoriesExist(
                    exportedKotlinPackagesSwiftModule.toPath(),
                    kotlinRuntime.toPath(),
                    sharedSwiftModule.toPath(),
                    sharedBridgeShared.toPath(),
                    notGoodLookingProjectSwiftModule.toPath(),
                    sharedBridgeNotGoodLookingProject.toPath(),
                    subprojectSwiftModule.toPath(),
                    sharedBridgeSubproject.toPath()
                )

                assertFileExists(libShared.toPath())
            }
        }
    }

    @DisplayName("embedSwiftExport executes normally when custom module name is defined in Swift Export DSL")
    @GradleTest
    fun testSwiftExportDSLCustomModuleName(
        gradleVersion: GradleVersion,
        @TempDir testBuildDir: Path,
    ) {
        project(
            "empty",
            gradleVersion
        ) {
            plugins {
                kotlin("multiplatform")
            }
            settingsBuildScriptInjection {
                settings.rootProject.name = "shared"
            }
            buildScriptInjection {
                with(project) {
                    applyMultiplatform {
                        iosArm64()
                        with(swiftExport) {
                            moduleName.set("CustomShared")
                            export(dependencies.project(mapOf("path" to ":subproject"))) {
                                moduleName.set("CustomSubproject")
                            }
                        }

                        sourceSets.commonMain {
                            compileStubSourceWithSourceSetName()

                            dependencies {
                                implementation(project(":subproject"))
                            }
                        }
                    }
                }
            }

            val subproject = project("empty", gradleVersion) {
                buildScriptInjection {
                    project.applyMultiplatform {
                        iosArm64()
                        sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                    }
                }
            }

            include(subproject, "subproject")

            build(
                ":embedSwiftExportForXcode",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir)
            ) {
                val buildProductsDir = this@project.gradleRunner.environment?.get("BUILT_PRODUCTS_DIR")?.let { File(it) }
                assertNotNull(buildProductsDir)

                val exportedKotlinPackagesSwiftModule = buildProductsDir.resolve("ExportedKotlinPackages.swiftmodule")
                val kotlinRuntime = buildProductsDir.resolve("KotlinRuntime")
                val libCustomShared = buildProductsDir.resolve("libCustomShared.a")
                val sharedSwiftModule = buildProductsDir.resolve("CustomShared.swiftmodule")
                val sharedBridgeShared = buildProductsDir.resolve("SharedBridge_CustomShared")
                val subprojectSwiftModule = buildProductsDir.resolve("CustomSubproject.swiftmodule")
                val sharedBridgeSubproject = buildProductsDir.resolve("SharedBridge_CustomSubproject")

                assertDirectoriesExist(
                    exportedKotlinPackagesSwiftModule.toPath(),
                    kotlinRuntime.toPath(),
                    sharedSwiftModule.toPath(),
                    sharedBridgeShared.toPath(),
                    subprojectSwiftModule.toPath(),
                    sharedBridgeSubproject.toPath()
                )

                assertFileExists(libCustomShared.toPath())
            }
        }
    }

    @DisplayName("embedSwiftExport executes normally when package flatten rule is defined in Swift Export DSL")
    @GradleTest
    fun testSwiftExportDSLWithPackageFlatteringRuleEnabled(
        gradleVersion: GradleVersion,
        @TempDir testBuildDir: Path,
    ) {
        project(
            "empty",
            gradleVersion
        ) {
            plugins {
                kotlin("multiplatform")
            }
            settingsBuildScriptInjection {
                settings.rootProject.name = "shared"
            }
            buildScriptInjection {
                with(project) {
                    applyMultiplatform {
                        iosArm64()
                        with(swiftExport) {
                            flattenPackage.set("com.github.jetbrains.swiftexport")
                            export(dependencies.project(mapOf("path" to ":subproject"))) {
                                flattenPackage.set("com.subproject.library")
                            }
                        }

                        sourceSets.commonMain {
                            compileSource(
                                """
                                    package com.github.jetbrains.swiftexport
                                    class MyKotlinClass()
                                """.trimIndent()
                            )

                            dependencies {
                                implementation(project(":subproject"))
                            }
                        }
                    }
                }
            }

            val subproject = project("empty", gradleVersion) {
                buildScriptInjection {
                    project.applyMultiplatform {
                        iosArm64()
                        sourceSets.commonMain.get().compileSource(
                            """
                                package com.subproject.library
                                class LibFoo()
                            """.trimIndent()
                        )
                    }
                }
            }

            include(subproject, "subproject")

            build(
                ":embedSwiftExportForXcode",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir)
            ) {
                val sharedSwiftPath = projectPath.resolve("build/SwiftExport/iosArm64/Debug/files/Shared/Shared.swift")
                assertContains(
                    sharedSwiftPath.readText(),
                    "public typealias MyKotlinClass = ExportedKotlinPackages.com.github.jetbrains.swiftexport.MyKotlinClass"
                )

                val subprojectSwiftPath = projectPath.resolve("build/SwiftExport/iosArm64/Debug/files/Subproject/Subproject.swift")
                assertContains(
                    subprojectSwiftPath.readText(),
                    "public typealias LibFoo = ExportedKotlinPackages.com.subproject.library.LibFoo"
                )
            }
        }
    }

    @DisplayName("embedSwiftExport executes normally when external dependency is defined in Swift Export DSL")
    @GradleTest
    fun testSwiftExportDSLWithExternalDependency(
        gradleVersion: GradleVersion,
        @TempDir testBuildDir: Path,
    ) {
        // Publish dependency
        val multiplatformLibrary = publishMultiplatformLibrary(gradleVersion)

        project(
            "empty",
            gradleVersion,
        ) {
            plugins {
                kotlin("multiplatform")
            }
            addPublishedProjectToRepositories(multiplatformLibrary)

            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                    with(swiftExport) {
                        export(multiplatformLibrary.rootCoordinate)
                    }
                }
            }

            build(
                ":embedSwiftExportForXcode",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir)
            ) {
                val buildProductsDir = this@project.gradleRunner.environment?.get("BUILT_PRODUCTS_DIR")?.let { File(it) }
                assertNotNull(buildProductsDir)

                val multiplatformLibrarySwiftModule = buildProductsDir.resolve("FooMultiplatformLibrary.swiftmodule")
                assertDirectoryExists(multiplatformLibrarySwiftModule.toPath(), "FooMultiplatformLibrary.swiftmodule doesn't exist")
            }
        }
    }

    @DisplayName("Swift Export metadata published by a dependency is applied by its consumer")
    @GradleTest
    fun testSwiftExportDSLWithPublishedMetadata(
        gradleVersion: GradleVersion,
        @TempDir testBuildDir: Path,
    ) {
        // Exported as "Foo", not as "FooMetadataLibrary" derived from the coordinates
        val metadataLibrary = publishLibraryWithSwiftExportMetadata(
            gradleVersion = gradleVersion,
            projectName = "metadataLibrary",
            swiftModuleName = "Foo",
            packageToFlatten = "org.bar.foo",
            source = """
                package org.bar.foo
                class LibFoo
            """.trimIndent(),
        )

        // The consumer overrides the published module name of this one
        val overriddenLibrary = publishLibraryWithSwiftExportMetadata(
            gradleVersion = gradleVersion,
            projectName = "overriddenLibrary",
            swiftModuleName = "PublishedName",
            packageToFlatten = "org.bar.overridden",
            source = """
                package org.bar.overridden
                class LibOverridden
            """.trimIndent(),
        )

        project(
            "empty",
            gradleVersion,
        ) {
            plugins {
                kotlin("multiplatform")
            }
            settingsBuildScriptInjection {
                settings.rootProject.name = "shared"
            }
            addPublishedProjectToRepositories(metadataLibrary)
            addPublishedProjectToRepositories(overriddenLibrary)

            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                    with(swiftExport) {
                        export(metadataLibrary.rootCoordinate)
                        export(overriddenLibrary.rootCoordinate) {
                            moduleName.set("ConsumerName")
                        }
                    }
                }
            }

            build(
                ":embedSwiftExportForXcode",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir)
            ) {
                val buildProductsDir = this@project.gradleRunner.environment?.get("BUILT_PRODUCTS_DIR")?.let { File(it) }
                assertNotNull(buildProductsDir)

                val exportedModules = projectPath.resolve("build/SwiftExport/iosArm64/Debug/files")

                // The published module name wins over the one derived from the coordinates
                assertDirectoryExists(buildProductsDir.resolve("Foo.swiftmodule").toPath(), "Foo.swiftmodule doesn't exist")
                assertFalse(
                    buildProductsDir.resolve("FooMetadataLibrary.swiftmodule").exists(),
                    "The dependency should not be exported under the name derived from its coordinates"
                )

                assertEquals(
                    "public typealias LibFoo = ExportedKotlinPackages.org.bar.foo.LibFoo",
                    exportedModules.resolve("Foo/Foo.swift").typealiasOf("LibFoo"),
                    "The published package flatten rule should be applied to the exported dependency"
                )

                // Per property, what the consumer configures wins over the published metadata
                assertDirectoryExists(
                    buildProductsDir.resolve("ConsumerName.swiftmodule").toPath(),
                    "ConsumerName.swiftmodule doesn't exist"
                )
                assertFalse(
                    buildProductsDir.resolve("PublishedName.swiftmodule").exists(),
                    "The module name configured by the consumer should win over the published one"
                )
                assertEquals(
                    "public typealias LibOverridden = ExportedKotlinPackages.org.bar.overridden.LibOverridden",
                    exportedModules.resolve("ConsumerName/ConsumerName.swift").typealiasOf("LibOverridden"),
                    "The published package flatten rule should still be applied when only the module name is overridden"
                )
            }
        }
    }

    @DisplayName("embedSwiftExport executes normally when no dependency block defined")
    @GradleTest
    fun testSwiftExportDSLWithoutDependencies(
        gradleVersion: GradleVersion,
        @TempDir testBuildDir: Path,
    ) {
        project(
            "empty",
            gradleVersion
        ) {
            plugins {
                kotlin("multiplatform")
            }
            settingsBuildScriptInjection {
                settings.rootProject.name = "shared"
            }
            buildScriptInjection {
                with(project) {
                    applyMultiplatform {
                        iosArm64()
                        with(swiftExport) {
                            export(dependencies.project(mapOf("path" to ":subproject")))
                            export(dependencies.project(mapOf("path" to ":not-good-looking-project-name")))
                        }
                    }
                }
            }

            val subprojectOne = project("empty", gradleVersion) {
                buildScriptInjection {
                    project.applyMultiplatform {
                        iosArm64()
                        sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                    }
                }
            }

            val subprojectTwo = project("empty", gradleVersion) {
                buildScriptInjection {
                    project.applyMultiplatform {
                        iosArm64()
                        sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                    }
                }
            }

            include(subprojectOne, "subproject")
            include(subprojectTwo, "not-good-looking-project-name")

            build(
                ":embedSwiftExportForXcode",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir)
            ) {
                val buildProductsDir = this@project.gradleRunner.environment?.get("BUILT_PRODUCTS_DIR")?.let { File(it) }
                assertNotNull(buildProductsDir)

                val sharedSwiftModule = buildProductsDir.resolve("Shared.swiftmodule")
                val subprojectSwiftModule = buildProductsDir.resolve("Subproject.swiftmodule")
                val notGoodLookingProjectSwiftModule = buildProductsDir.resolve("NotGoodLookingProjectName.swiftmodule")

                assertDirectoryExists(sharedSwiftModule.toPath(), "Shared.swiftmodule doesn't exist")
                assertDirectoryExists(subprojectSwiftModule.toPath(), "Subproject.swiftmodule doesn't exist")
                assertDirectoryExists(notGoodLookingProjectSwiftModule.toPath(), "NotGoodLookingProjectName.swiftmodule doesn't exist")
            }
        }
    }

    @DisplayName("embedSwiftExport executes normally when kotlinx.coroutines enabled")
    @GradleTest
    fun testSwiftExportCoroutines(
        gradleVersion: GradleVersion,
        @TempDir testBuildDir: Path,
    ) {
        project(
            "empty",
            gradleVersion
        ) {
            plugins {
                kotlin("multiplatform")
            }
            settingsBuildScriptInjection {
                settings.rootProject.name = "shared"
            }
            buildScriptInjection {
                with(project) {
                    applyMultiplatform {
                        iosArm64()
                        with(swiftExport) { }

                        sourceSets.commonMain {
                            compileSource(
                                """
                                package org.foo
                                import kotlinx.coroutines.*
                                suspend fun iosSuspendFunction(): Int = 5
                                """.trimIndent()
                            )
                            dependencies {
                                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
                            }
                        }
                    }
                }
            }

            // Coroutines export is only supported for iOS 18.0 and above
            build(
                ":embedSwiftExportForXcode",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir, iphoneOsDeploymentTarget = "18.0")
            ) {
                val buildProductsDir = this@project.gradleRunner.environment?.get("BUILT_PRODUCTS_DIR")?.let { File(it) }
                assertNotNull(buildProductsDir)

                assertOutputDoesNotContain("Coroutine support is enabled, but no `kotlinx-coroutines-core` module was found in path. Please add kotlinx-coroutines as a dependency to your project, or disable coroutines support to silence this warning.")

                val sharedSwiftPath = projectPath.resolve("build/SwiftExport/iosArm64/Debug/files/Shared/Shared.swift")
                assertContains(
                    sharedSwiftPath.readText(),
                    "public static func iosSuspendFunction() async throws -> Swift.Int32"
                )
            }
        }
    }
}
