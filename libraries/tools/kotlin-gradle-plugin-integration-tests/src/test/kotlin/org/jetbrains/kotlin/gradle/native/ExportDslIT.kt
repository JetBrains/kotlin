/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import kotlinx.serialization.json.jsonPrimitive
import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsSeverity.ERROR
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsSeverity.FATAL
import org.jetbrains.kotlin.gradle.plugin.mpp.export.SwiftExportVisibility
import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.include
import org.jetbrains.kotlin.gradle.util.SwiftSymbol
import org.jetbrains.kotlin.gradle.util.assertSwiftModuleSymbols
import org.jetbrains.kotlin.gradle.util.getNestedList
import org.jetbrains.kotlin.gradle.util.parseJsonToMap
import org.jetbrains.kotlin.gradle.util.swiftClassNames
import org.jetbrains.kotlin.gradle.util.swiftExportEmbedAndSignEnvVariables
import org.jetbrains.kotlin.gradle.util.swiftTypealiases
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@DisplayName("Tests for the export DSL")
@SwiftExportGradlePluginTests
@OptIn(ExperimentalSwiftExportDsl::class)
class ExportDslIT : KGPBaseTest() {

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
                assertConfigurationCacheStored()
            }
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

    @DisplayName("KT-90135: An override with an invalid module name fails the build with the diagnostic")
    @GradleTest
    fun testOverrideWithInvalidModuleNameFailsTheBuild(
        gradleVersion: GradleVersion,
        @TempDir testBuildDir: Path,
    ) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
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
                        configure(project.dependencies.project(mapOf("path" to ":sub"))) {
                            moduleName.set("not-a-valid-swift-module")
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

            buildAndFail(
                ":$EMBED_SWIFT_EXPORT_TASK_NAME",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir)
            ) {
                assertTasksFailed(":iosArm64SwiftExport")
                // As an ERROR the task went on and failed later inside the Swift Export worker.
                assertHasDiagnostic(KotlinToolingDiagnostics.SwiftExportInvalidModuleName, expectedSeverity = FATAL)
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
                assertTasksExecuted(":iosArm64SwiftExport")

                val files = projectPath.resolve("build/SwiftExport/iosArm64/output/files")
                // The derived name for `:sub` would have been `Sub`.
                assertDirectoryDoesNotExist(files.resolve("Sub"))
                val renamedSwift = files.resolve("Renamed/Renamed.swift")
                assertFileExists(renamedSwift)

                // Flattening the package exposes `com.example.sub.One` at the top level of the module.
                assertEquals("ExportedKotlinPackages.com.example.sub.One", renamedSwift.swiftTypealiases()["One"])

                val modules = parseJsonToMap(projectPath.resolve("build/SwiftExport/iosArm64/output/modules.json"))
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
                assertTasksExecuted(":iosArm64SwiftExport")

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

    @DisplayName("KT-89480: Changing a dependency visibility override invalidates the Swift Export task")
    @GradleTest
    fun testDependencyVisibilityOverrideIsTrackedAsTaskInput(
        gradleVersion: GradleVersion,
        @TempDir testBuildDir: Path,
    ) {
        val visibilityProperty = "subVisibilityOverride"
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            settingsBuildScriptInjection {
                settings.rootProject.name = "shared"
            }
            buildScriptInjection {
                // The visibility override is read from a Gradle property so that it can be changed between builds.
                // Crucially, none of the task's tracked file inputs (the sources or the resolved klib) change when
                // only this property changes.
                val visibilityOverride = project.providers.gradleProperty(visibilityProperty)
                    .map { SwiftExportVisibility.valueOf(it) }
                    .orElse(SwiftExportVisibility.EXPOSED)
                project.applyMultiplatform {
                    iosArm64()
                    sourceSets.commonMain {
                        compileSource(
                            """
                            fun makeOne(): com.example.sub.One = com.example.sub.One()
                            """.trimIndent()
                        )
                        dependencies {
                            // Only transitively exported by default; the override below is what makes it FULL/HIDDEN.
                            implementation(project(":sub"))
                        }
                    }
                }
                export.swift {
                    moduleName.set("Shared")
                    xcodeIntegration {
                        configure(project.dependencies.project(mapOf("path" to ":sub"))) {
                            visibility.set(visibilityOverride)
                        }
                        // KT-90135: configure() only selects dependencies, so a selector that matches nothing
                        // is reported but breaks nothing.
                        configure("unknown:qa:1.0", SwiftExportVisibility.HIDDEN)
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

            val builtProductsDir = projectPath.resolve("build/builtProductsDir")

            // 1) First build with the override set to EXPOSED: the whole public API of `:sub` is translated.
            build(
                ":$EMBED_SWIFT_EXPORT_TASK_NAME",
                "-P$visibilityProperty=EXPOSED",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir),
            ) {
                assertTasksExecuted(":iosArm64SwiftExport")
                assertHasDiagnostic(KotlinToolingDiagnostics.SwiftExportModuleResolutionError, expectedSeverity = ERROR)
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

            // 2) Re-running with the same override keeps the task UP-TO-DATE, as expected.
            build(
                ":$EMBED_SWIFT_EXPORT_TASK_NAME",
                "-P$visibilityProperty=EXPOSED",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir),
            ) {
                assertTasksUpToDate(":iosArm64SwiftExport")
            }

            // 3) Change ONLY the visibility override to HIDDEN. The override drives the task's output and is tracked
            // via the `dependencyOptionsOverridesInputs` task input, so up-to-date checking sees the change: the
            // task re-executes and now emits only an empty stub for the referenced `One`, dropping the unreferenced
            // `TwoNeverReferenced` and `One`'s own synthetic constructor.
            build(
                ":$EMBED_SWIFT_EXPORT_TASK_NAME",
                "-P$visibilityProperty=HIDDEN",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir),
            ) {
                assertTasksExecuted(":iosArm64SwiftExport")
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
            }
        }
    }

    @DisplayName("KT-90141: Promoting a transitive dependency to a direct api dependency invalidates the Swift Export task")
    @GradleTest
    fun testPromotingTransitiveDependencyToDirectApiIsTrackedAsTaskInput(
        gradleVersion: GradleVersion,
        @TempDir testBuildDir: Path,
    ) {
        val directApiProperty = "directSubApi"
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            settingsBuildScriptInjection {
                settings.rootProject.name = "shared"
            }
            buildScriptInjection {
                // Toggled between builds: the klib set the task sees is the same either way, only the direct edge changes.
                val directSubApi = project.providers.gradleProperty(directApiProperty).isPresent
                project.applyMultiplatform {
                    iosArm64()
                    sourceSets.commonMain {
                        compileStubSourceWithSourceSetName()
                        dependencies {
                            api(project(":mid"))
                            if (directSubApi) api(project(":sub"))
                        }
                    }
                }
                export.swift {
                    moduleName.set("Shared")
                    xcodeIntegration()
                }
            }

            val sub = project("empty", gradleVersion) {
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
            val mid = project("empty", gradleVersion) {
                buildScriptInjection {
                    project.applyMultiplatform {
                        iosArm64()
                        sourceSets.commonMain {
                            compileSource(
                                """
                                package com.example.mid
                                fun makeOne(): com.example.sub.One = com.example.sub.One()
                                """.trimIndent()
                            )
                            dependencies {
                                api(project(":sub"))
                            }
                        }
                    }
                }
            }

            include(sub, "sub")
            include(mid, "mid")

            val swiftApiFiles = {
                projectPath.resolve("build/SwiftExport/iosArm64/output/files").toFile().walk()
                    .filter { it.extension == "swift" }.map { it.toPath() }.toList()
            }

            build(
                ":iosArm64SwiftExport",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir),
            ) {
                assertTasksExecuted(":iosArm64SwiftExport")
                assertFalse(swiftApiFiles().any { "TwoNeverReferenced" in it.readText() })
            }

            build(
                ":iosArm64SwiftExport",
                "-P$directApiProperty",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir),
            ) {
                assertTasksExecuted(":iosArm64SwiftExport")
                assertContains(swiftApiFiles().flatMap { it.swiftClassNames() }, "TwoNeverReferenced")
            }
        }
    }

    private companion object {
        const val EMBED_SWIFT_EXPORT_TASK_NAME = "embedSwiftExportForXcode"
        const val IOS_DEPLOYMENT_TARGET = "18.0"
    }
}
