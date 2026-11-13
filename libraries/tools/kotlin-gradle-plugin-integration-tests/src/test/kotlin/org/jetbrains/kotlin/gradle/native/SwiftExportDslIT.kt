/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION") // Tests the deprecated legacy Swift Export DSL on purpose.

package org.jetbrains.kotlin.gradle.native

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.*
import org.jetbrains.kotlin.gradle.util.publishMultiplatformLibrary
import org.jetbrains.kotlin.gradle.util.swiftExportEmbedAndSignEnvVariables
import org.jetbrains.kotlin.gradle.util.swiftSourceLines
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText
import kotlin.test.assertContains
import kotlin.test.assertNotNull

@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@DisplayName("Tests for Swift Export DSL")
@SwiftExportGradlePluginTests
@OptIn(ExperimentalSwiftExportDsl::class)
class SwiftExportDslIT : KGPBaseTest() {

    @DisplayName("Changing an exported module name override invalidates the Swift Export task")
    @GradleTest
    @Suppress("DEPRECATION") // Tests the deprecated legacy Swift Export DSL on purpose.
    fun testExportedModuleNameOverrideIsTrackedAsTaskInput(
        gradleVersion: GradleVersion,
        @TempDir testBuildDir: Path,
    ) {
        val exportedLibrary = publishMultiplatformLibrary(gradleVersion) {
            iosArm64()
            sourceSets.commonMain.get().compileSource(
                """
                package org.foo
                class One
                """.trimIndent()
            )
        }

        val moduleNameProperty = "exportedModuleNameOverride"
        val initialModuleName = "InitialExportedModule"
        val renamedModuleName = "RenamedExportedModule"
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            settingsBuildScriptInjection {
                settings.rootProject.name = "shared"
            }
            addPublishedProjectToRepositories(exportedLibrary)

            // Only Serializable values may be captured inside the build script injection below.
            val exportedLibraryCoordinate = exportedLibrary.rootCoordinate

            buildScriptInjection {
                // The exported module name is read from a Gradle property so that it can be changed between builds.
                // Crucially, none of the task's tracked file inputs (the sources, the resolved klib, or the resolved
                // export configuration) change when only this property changes.
                val moduleNameOverride = project.providers.gradleProperty(moduleNameProperty).orElse(initialModuleName)
                project.applyMultiplatform {
                    iosArm64()
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()

                    with(swiftExport) {
                        export(exportedLibraryCoordinate) {
                            moduleName.set(moduleNameOverride)
                        }
                    }
                }
            }

            val initialModuleDir = "build/SwiftExport/iosArm64/output/files/$initialModuleName"
            val renamedModuleDir = "build/SwiftExport/iosArm64/output/files/$renamedModuleName"

            // 1) First run with the initial override: the task executes and emits the exported module under its name.
            build(
                ":iosArm64SwiftExport",
                "-P$moduleNameProperty=$initialModuleName",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir),
            ) {
                assertTasksExecuted(":iosArm64SwiftExport")
                assertDirectoryInProjectExists(initialModuleDir)
            }

            // 2) Re-running with the same override keeps the task UP-TO-DATE, as expected.
            build(
                ":iosArm64SwiftExport",
                "-P$moduleNameProperty=$initialModuleName",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir),
            ) {
                assertTasksUpToDate(":iosArm64SwiftExport")
            }

            // No run produced this file, so the next run must delete it along with the rest of the directory.
            projectPath.resolve("build/SwiftExport/iosArm64/output/Stale/Stale.kt").createParentDirectories().writeText("")

            // 3) Change ONLY the exported module name override. The override drives the task's output and is tracked
            // via the `exportedModulesInputs` task input, so up-to-date checking sees the change: the task re-executes
            // and emits the exported module under its new name, while the stale directory is gone.
            build(
                ":iosArm64SwiftExport",
                "-P$moduleNameProperty=$renamedModuleName",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir),
            ) {
                assertTasksExecuted(":iosArm64SwiftExport")
                assertDirectoryInProjectExists(renamedModuleDir)
                assertDirectoryInProjectDoesNotExist(initialModuleDir)
                assertDirectoryInProjectDoesNotExist("build/SwiftExport/iosArm64/output/Stale")
            }
        }
    }

    @DisplayName("embedSwiftExport exports project and published modules under custom and derived names without a dependency block")
    @GradleTest
    fun testSwiftExportDSLWithoutDependencies(
        gradleVersion: GradleVersion,
        @TempDir testBuildDir: Path,
    ) {
        val multiplatformLibrary = publishMultiplatformLibrary(gradleVersion)

        project(
            "empty",
            gradleVersion
        ) {
            plugins {
                kotlin("multiplatform")
            }
            addPublishedProjectToRepositories(multiplatformLibrary)
            // Only Serializable values may be captured inside the build script injection below.
            val multiplatformLibraryCoordinate = multiplatformLibrary.rootCoordinate

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
                            export(dependencies.project(mapOf("path" to ":not-good-looking-project-name")))
                            export(multiplatformLibraryCoordinate)
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

                assertDirectoriesExist(
                    *listOf(
                        "ExportedKotlinPackages.swiftmodule",
                        "KotlinRuntime",
                        "CustomShared.swiftmodule",
                        "SharedBridge_CustomShared",
                        "CustomSubproject.swiftmodule",
                        "SharedBridge_CustomSubproject",
                        // Derived from the project path.
                        "NotGoodLookingProjectName.swiftmodule",
                        "SharedBridge_NotGoodLookingProjectName",
                        // Derived from the published coordinates.
                        "FooMultiplatformLibrary.swiftmodule",
                    ).map { buildProductsDir.resolve(it).toPath() }.toTypedArray()
                )
                assertFileExists(buildProductsDir.resolve("libCustomShared.a").toPath())
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
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir)
            ) {
                val sharedSwiftPath = projectPath.resolve("build/SwiftExport/iosArm64/output/files/Shared/Shared.swift")
                assertContains(
                    sharedSwiftPath.swiftSourceLines(),
                    "public static func iosSuspendFunction() async throws -> Swift.Int32 {",
                )
            }
        }
    }
}
