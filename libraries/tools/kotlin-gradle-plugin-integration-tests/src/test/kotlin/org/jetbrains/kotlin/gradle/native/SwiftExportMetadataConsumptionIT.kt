/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.export.ExperimentalExportDsl
import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.*
import org.jetbrains.kotlin.gradle.util.swiftExportEmbedAndSignEnvVariables
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.assertContains
import kotlin.test.assertNotNull

@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@DisplayName("Tests for Swift Export metadata consumption with the export DSL")
@SwiftExportGradlePluginTests
@OptIn(ExperimentalExportDsl::class, ExperimentalSwiftExportDsl::class)
class SwiftExportMetadataConsumptionIT : KGPBaseTest() {

    @DisplayName(
        "Swift Export metadata consumed from a direct published dependency with moduleName override"
    )
    @GradleTest
    fun directPublishedDependencyWithModuleNameOverride(
        gradleVersion: GradleVersion,
        @TempDir testBuildDir: Path,
    ) {
        testSwiftExportMetadataConsumption(
            gradleVersion = gradleVersion,
            testBuildDir = testBuildDir,
            published = true,
            moduleNameOverride = "Foo",
        )
    }

    @DisplayName(
        "Swift Export metadata consumed from a direct published dependency with rootPackage override"
    )
    @GradleTest
    fun directPublishedDependencyWithRootPackageOverride(
        gradleVersion: GradleVersion,
        @TempDir testBuildDir: Path,
    ) {
        testSwiftExportMetadataConsumption(
            gradleVersion = gradleVersion,
            testBuildDir = testBuildDir,
            published = true,
            rootPackageOverride = "com.foo.bar",
        )
    }

    @DisplayName(
        "Swift Export metadata consumed from a direct published dependency with no overrides"
    )
    @GradleTest
    fun directPublishedDependencyWithNoOverrides(
        gradleVersion: GradleVersion,
        @TempDir testBuildDir: Path,
    ) {
        testSwiftExportMetadataConsumption(
            gradleVersion = gradleVersion,
            testBuildDir = testBuildDir,
            published = true,
        )
    }

    @DisplayName(
        "Swift Export metadata consumed from a direct published dependency with moduleName and rootPackage overrides"
    )
    @GradleTest
    fun directPublishedDependencyWithModuleNameAndRootPackageOverrides(
        gradleVersion: GradleVersion,
        @TempDir testBuildDir: Path,
    ) {
        testSwiftExportMetadataConsumption(
            gradleVersion = gradleVersion,
            testBuildDir = testBuildDir,
            published = true,
            moduleNameOverride = "Bar",
            rootPackageOverride = "com.bar.baz",
        )
    }

    private fun testSwiftExportMetadataConsumption(
        gradleVersion: GradleVersion,
        testBuildDir: Path,
        published: Boolean,
        moduleNameOverride: String? = null,
        rootPackageOverride: String? = null,
    ) {
        val rootPackage = rootPackageOverride ?: "com.foo.bar"
        val subproject = project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    export.swift {
                        this.moduleName.set(moduleNameOverride)
                        this.rootPackage.set(rootPackageOverride)
                    }

                    sourceSets.commonMain {
                        compileStubSourceWithSourceSetName()
                        compileSource(
                            """
                            package $rootPackage
                            class LibFoo
                            """.trimIndent()
                        )
                    }
                }
            }
        }
        val publishedSubproject = if (published) {
            subproject.publish(publisherConfiguration = PublisherConfiguration(group = rootPackage))
        } else {
            null
        }

        project(
            "empty",
            gradleVersion
        ) {
            if (publishedSubproject != null) {
                addPublishedProjectToRepositories(publishedSubproject)
            } else {
                include(subproject, "subproject")
            }

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
                        export.swift {
                            xcodeIntegration()
                        }

                        sourceSets.commonMain {
                            compileStubSourceWithSourceSetName()

                            dependencies {
                                if (publishedSubproject != null) {
                                    api(publishedSubproject.rootCoordinate)
                                } else {
                                    api(project(":subproject"))
                                }
                            }
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

                val expectedDependencyModuleName = when {
                    moduleNameOverride != null -> moduleNameOverride
                    published -> "ComFooBarEmpty"
                    else -> "Subproject"
                }
                assertSubdirectoriesExist(
                    buildProductsDir,
                    // Default directories.
                    "ExportedKotlinPackages.swiftmodule",
                    "KotlinRuntime",
                    // Exported :shared module.
                    "Shared.swiftmodule",
                    "SharedBridge_Shared",
                    "$expectedDependencyModuleName.swiftmodule",
                    "SharedBridge_$expectedDependencyModuleName",
                )

                assertFileExists(buildProductsDir.resolve("libShared.a"))

                if (rootPackageOverride != null) {
                    val subprojectSwiftPath =
                        projectPath.resolve("build/SwiftExport/iosArm64/Debug/files/$expectedDependencyModuleName/$expectedDependencyModuleName.swift")
                    assertContains(
                        subprojectSwiftPath.readText(),
                        "public typealias LibFoo = ExportedKotlinPackages.$rootPackageOverride.LibFoo"
                    )
                }
            }
        }
    }

    private fun assertSubdirectoriesExist(dir: File, vararg subdirNames: String) {
        val subdirPaths = subdirNames.map { dir.resolve(it).toPath() }
        assertDirectoriesExist(*subdirPaths.toTypedArray()) { defaultMessage ->
            defaultMessage + "\n" + "Contents of dir:\n" + dir.list()!!.joinToString(separator = "\n")
        }
    }
}
