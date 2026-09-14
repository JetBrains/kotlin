/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import kotlinx.serialization.json.jsonPrimitive
import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.export.ExperimentalExportDsl
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.mpp.export.SwiftExportVisibility
import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.include
import org.jetbrains.kotlin.gradle.util.SwiftSymbol
import org.jetbrains.kotlin.gradle.util.assertSwiftModuleSymbols
import org.jetbrains.kotlin.gradle.util.getNestedList
import org.jetbrains.kotlin.gradle.util.parseJsonToMap
import org.jetbrains.kotlin.gradle.util.swiftExportEmbedAndSignEnvVariables
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@DisplayName("Tests for the export DSL")
@SwiftExportGradlePluginTests
@OptIn(ExperimentalExportDsl::class, ExperimentalSwiftExportDsl::class)
class ExportDslIT : KGPBaseTest() {

    @DisplayName("embedSwiftExportForXcode is registered when the Xcode integration is activated")
    @GradleTest
    fun testXcodeIntegrationRegistersEmbedTask(
        gradleVersion: GradleVersion,
    ) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                }
                export.swift {
                    moduleName.set("Shared")
                    xcodeIntegration()
                }
            }

            assertTrue(isEmbedSwiftExportTaskRegistered())
        }
    }

    @DisplayName("embedSwiftExportForXcode is not registered when the export DSL is used without the Xcode integration")
    @GradleTest
    fun testExportDslWithoutXcodeIntegrationDoesNotRegisterEmbedTask(
        gradleVersion: GradleVersion,
    ) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                }
                export.swift {
                    moduleName.set("Shared")
                }
            }

            assertFalse(isEmbedSwiftExportTaskRegistered())
        }
    }

    @DisplayName("The Xcode integration can be activated independently of the module configuration order")
    @GradleTest
    fun testXcodeIntegrationActivationIsOrderIndependent(
        gradleVersion: GradleVersion,
    ) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                export.swift {
                    xcodeIntegration()
                }
                project.applyMultiplatform {
                    iosArm64()
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                }
                export.swift {
                    moduleName.set("Shared")
                }
            }

            assertTrue(isEmbedSwiftExportTaskRegistered())
        }
    }

    @DisplayName("embedSwiftExportForXcode fails with an actionable error outside of Xcode in an activated project")
    @GradleTest
    fun testActivatedProjectFailsOutsideOfXcode(
        gradleVersion: GradleVersion,
    ) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                }
                export.swift {
                    xcodeIntegration()
                }
            }

            buildAndFail(":$EMBED_SWIFT_EXPORT_TASK_NAME") {
                assertOutputContains("Please run the $EMBED_SWIFT_EXPORT_TASK_NAME task from Xcode")
            }
        }
    }

    @DisplayName("embedSwiftExportForXcode executes normally in an activated project")
    @GradleTest
    fun testActivatedProjectExecutesEmbedTask(
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
                export.swift {
                    xcodeIntegration()
                }
            }

            build(
                ":$EMBED_SWIFT_EXPORT_TASK_NAME",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir)
            ) {
                assertTasksExecuted(":iosArm64DebugSwiftExport")
                assertTasksExecuted(":$EMBED_SWIFT_EXPORT_TASK_NAME")
            }
        }
    }

    @DisplayName("A legacy swiftExport build still configures and reports the deprecation")
    @GradleTest
    fun testLegacyDslStillWorksAndReportsDeprecation(
        gradleVersion: GradleVersion,
    ) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                }
                swiftExport.moduleName.set("Legacy")
            }

            build("help") {
                assertHasDiagnostic(KotlinToolingDiagnostics.DeprecatedSwiftExportDsl)
                assertNoDiagnostic(KotlinToolingDiagnostics.ConflictingSwiftExportDsls)
            }

            assertTrue(isEmbedSwiftExportTaskRegistered())
        }
    }

    @DisplayName("Configuring both Swift Export DSLs fails the build")
    @GradleTest
    fun testConfiguringBothDslsFailsTheBuild(
        gradleVersion: GradleVersion,
    ) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                }
                swiftExport.moduleName.set("Legacy")
                export.swift {
                    moduleName.set("Shared")
                    xcodeIntegration()
                }
            }

            buildAndFail(":compileKotlinIosArm64") {
                assertHasDiagnostic(KotlinToolingDiagnostics.ConflictingSwiftExportDsls)
                assertNoDiagnostic(KotlinToolingDiagnostics.DeprecatedSwiftExportDsl)
            }
        }
    }

    @DisplayName("An override colliding with the root module name fails the build with the diagnostic")
    @GradleTest
    fun testDuplicateModuleNameFailsTheBuild(
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
                        compileStubSourceWithSourceSetName()
                        dependencies {
                            api(project(":sub"))
                        }
                    }
                }
                export.swift {
                    moduleName.set("Shared")
                    xcodeIntegration {
                        // Collides with the root module's own name, set above.
                        configure(project.dependencies.project(mapOf("path" to ":sub"))) {
                            moduleName.set("Shared")
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

            include(subproject, "sub")

            // The diagnostic is FATAL because it is reported after checkKotlinGradlePluginConfigurationErrors has
            // run, so it has to fail the build on its own.
            buildAndFail(
                ":$EMBED_SWIFT_EXPORT_TASK_NAME",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir)
            ) {
                assertHasDiagnostic(KotlinToolingDiagnostics.SwiftExportDuplicateModuleNames)
            }
        }
    }

    @DisplayName("A dependency override renames the module and flattens its package in the generated Swift")
    @GradleTest
    fun testDependencyOverrideIsAppliedEndToEnd(
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
                            fun makeOne(): com.example.sub.One = com.example.sub.One()
                            """.trimIndent()
                        )
                        dependencies {
                            api(project(":sub"))
                        }
                    }
                }
                export.swift {
                    moduleName.set("Shared")
                    xcodeIntegration {
                        configure(project.dependencies.project(mapOf("path" to ":sub"))) {
                            moduleName.set("Renamed")
                            rootPackage.set("com.example.sub")
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
                            package com.example.sub
                            class One
                            """.trimIndent()
                        )
                    }
                }
            }

            include(subproject, "sub")

            build(
                ":$EMBED_SWIFT_EXPORT_TASK_NAME",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir)
            ) {
                assertTasksExecuted(":iosArm64DebugSwiftExport")

                val files = projectPath.resolve("build/SwiftExport/iosArm64/Debug/files")
                // The derived name for `:sub` would have been `Sub`.
                assertDirectoryDoesNotExist(files.resolve("Sub"))
                val renamedSwift = files.resolve("Renamed/Renamed.swift")
                assertFileExists(renamedSwift)

                // Flattening the package exposes `com.example.sub.One` at the top level of the module.
                assertContains(renamedSwift.readText(), "public typealias One = ExportedKotlinPackages.com.example.sub.One")

                val modules = parseJsonToMap(projectPath.resolve("build/SwiftExport/iosArm64/Debug/modules/Shared.json"))
                    .getNestedList("modules")
                    .orEmpty()
                    .map { it["name"]?.jsonPrimitive?.content }
                assertContains(modules, "Renamed")
                assertFalse(modules.contains("Sub"), modules.toString())
            }
        }
    }

    @DisplayName("A hidden dependency is translated to empty stubs instead of a real Swift API")
    @GradleTest
    fun testHiddenDependencyIsTranslatedToStubs(
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
                            fun makeOne(): com.example.sub.One = com.example.sub.One()
                            """.trimIndent()
                        )
                        dependencies {
                            // Would be fully exported without the override.
                            api(project(":sub"))
                        }
                    }
                }
                export.swift {
                    moduleName.set("Shared")
                    xcodeIntegration {
                        configure(
                            project.dependencies.project(mapOf("path" to ":sub")),
                            SwiftExportVisibility.HIDDEN,
                        )
                    }
                }
            }

            val subproject = project("empty", gradleVersion) {
                buildScriptInjection {
                    project.applyMultiplatform {
                        iosArm64()
                        sourceSets.commonMain.get().compileSource(
                            """
                            package com.example.sub
                            class One {
                                fun memberOfHiddenClass(): Int = 42
                            }
                            class UnreferencedFromShared
                            """.trimIndent()
                        )
                    }
                }
            }

            include(subproject, "sub")

            build(
                ":$EMBED_SWIFT_EXPORT_TASK_NAME",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir)
            ) {
                assertTasksExecuted(":iosArm64DebugSwiftExport")

                // Hiding doesn't remove the module.
                val builtProductsDir = projectPath.resolve("build/builtProductsDir")
                assertDirectoriesExist(builtProductsDir.resolve("Sub.swiftmodule"))

                // `One` is a stub without members; `UnreferencedFromShared` isn't there at all.
                assertSwiftModuleSymbols(
                    workingDir = projectPath.toFile(),
                    moduleName = "Sub",
                    target = "arm64-apple-ios$IOS_DEPLOYMENT_TARGET",
                    searchPaths = listOf(builtProductsDir.toFile()),
                    expectedSymbols = setOf(
                        SwiftSymbol(
                            demangledId = "(extension in Sub):ExportedKotlinPackages.com.example.sub.One",
                            pathComponents = listOf("com", "example", "sub", "One")
                        ),
                    )
                )

                // The declaration referring to the hidden type is still exported.
                assertSwiftModuleSymbols(
                    workingDir = projectPath.toFile(),
                    moduleName = "Shared",
                    target = "arm64-apple-ios$IOS_DEPLOYMENT_TARGET",
                    searchPaths = listOf(builtProductsDir.toFile()),
                    expectedSymbols = setOf(
                        SwiftSymbol(
                            demangledId = "Shared.makeOne() -> (extension in Sub):ExportedKotlinPackages.com.example.sub.One",
                            pathComponents = listOf("makeOne()")
                        ),
                    )
                )
            }
        }
    }

    @DisplayName("An exposed transitive dependency is fully exported under its api-derived name")
    @GradleTest
    fun testExposedTransitiveDependencyIsFullyExported(
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
                            fun makeOne(): com.example.sub.One = com.example.sub.One()
                            """.trimIndent()
                        )
                        dependencies {
                            // Only transitively exported by default.
                            implementation(project(":sub"))
                        }
                    }
                }
                export.swift {
                    moduleName.set("Shared")
                    xcodeIntegration {
                        configure(
                            project.dependencies.project(mapOf("path" to ":sub")),
                            SwiftExportVisibility.EXPOSED,
                        )
                    }
                }
            }

            val subproject = project("empty", gradleVersion) {
                buildScriptInjection {
                    project.applyMultiplatform {
                        iosArm64()
                        sourceSets.commonMain.get().compileSource(
                            """
                            package com.example.sub
                            class One
                            class TwoNeverReferenced
                            """.trimIndent()
                        )
                    }
                }
            }

            include(subproject, "sub")

            build(
                ":$EMBED_SWIFT_EXPORT_TASK_NAME",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir)
            ) {
                assertTasksExecuted(":iosArm64DebugSwiftExport")

                // Named from the project path, as with `api(project(":sub"))`.
                val builtProductsDir = projectPath.resolve("build/builtProductsDir")
                assertDirectoriesExist(builtProductsDir.resolve("Sub.swiftmodule"))

                // Full export, so the unreferenced class is there too.
                assertSwiftModuleSymbols(
                    workingDir = projectPath.toFile(),
                    moduleName = "Sub",
                    target = "arm64-apple-ios$IOS_DEPLOYMENT_TARGET",
                    searchPaths = listOf(builtProductsDir.toFile()),
                    expectedSymbols = setOf(
                        SwiftSymbol(
                            demangledId = "(extension in Sub):ExportedKotlinPackages.com.example.sub.One",
                            pathComponents = listOf("com", "example", "sub", "One")
                        ),
                        SwiftSymbol(
                            demangledId = "(extension in Sub):ExportedKotlinPackages.com.example.sub.One.init() -> (extension in Sub):ExportedKotlinPackages.com.example.sub.One",
                            pathComponents = listOf("com", "example", "sub", "One", "init()")
                        ),
                        SwiftSymbol(
                            demangledId = "(extension in Sub):ExportedKotlinPackages.com.example.sub.TwoNeverReferenced",
                            pathComponents = listOf("com", "example", "sub", "TwoNeverReferenced")
                        ),
                        SwiftSymbol(
                            demangledId = "(extension in Sub):ExportedKotlinPackages.com.example.sub.TwoNeverReferenced.init() -> (extension in Sub):ExportedKotlinPackages.com.example.sub.TwoNeverReferenced",
                            pathComponents = listOf("com", "example", "sub", "TwoNeverReferenced", "init()")
                        ),
                    )
                )
            }
        }
    }

    private fun TestProject.isEmbedSwiftExportTaskRegistered(): Boolean = buildScriptReturn {
        project.tasks.findByName(EMBED_SWIFT_EXPORT_TASK_NAME) != null
    }.buildAndReturn()

    private companion object {
        const val EMBED_SWIFT_EXPORT_TASK_NAME = "embedSwiftExportForXcode"
        const val IOS_DEPLOYMENT_TARGET = "18.0"
    }
}
