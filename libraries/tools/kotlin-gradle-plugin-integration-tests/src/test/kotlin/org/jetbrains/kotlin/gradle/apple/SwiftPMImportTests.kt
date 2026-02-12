/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.apple

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.ide.prepareKotlinIdeaImportTask
import org.jetbrains.kotlin.gradle.plugin.ide.Idea222Api
import org.jetbrains.kotlin.gradle.testbase.BuildOptions
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.NativeGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.OsCondition
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
import org.jetbrains.kotlin.gradle.testbase.plugins
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.testbase.providerBuildScriptReturn
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.ignoreAccessViolations
import org.jetbrains.kotlin.gradle.uklibs.include
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.assertEquals

@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)
@NativeGradlePluginTests
class SwiftPMImportTests : KGPBaseTest() {

    @OptIn(Idea222Api::class)
    @GradleTest
    fun `hasSwiftPMDependencies - transitive SwiftPM dependencies from project dependencies - influence hasSwiftPMDependencies flag`(version: GradleVersion, @TempDir temp: Path) {
        val localPackagePath = temp.resolve("localPackage").also { it.createDirectories() }
        localPackagePath.resolve("Package.swift").writeText(
            """
                // swift-tools-version: 5.9
                import PackageDescription

                let package = Package(
                    name: "localPackage",
                    platforms: [.macOS(.v13)],
                    products: [
                        .library(
                            name: "localPackage",
                            targets: ["Foo"]
                        ),
                    ],
                    targets: [
                        .target(name: "Foo"),
                    ]
                )
            """.trimIndent()
        )
        localPackagePath.resolve("Sources/Foo").also { it.createDirectories() }.resolve("Foo.swift").writeText("")
        val localPackageFilePath = localPackagePath.toFile()

        project("empty", version) {
            plugins {
                kotlin("multiplatform").apply(false)
            }
            val producer = project("empty", version) {
                plugins {
                    kotlin("multiplatform")
                }
                buildScriptInjection {
                    project.applyMultiplatform {
                        iosArm64()

                        swiftPMDependencies {
                            localPackage(localPackageFilePath, products = listOf("localPackage"))
                        }
                    }
                }
            }

            val consumerWithoutDependency = project("empty", version) {
                plugins {
                    kotlin("multiplatform")
                }
                buildScriptInjection {
                    project.applyMultiplatform {
                        iosArm64()
                    }
                }
            }

            val consumerWithDependency = project("empty", version) {
                plugins {
                    kotlin("multiplatform")
                }
                buildScriptInjection {
                    project.applyMultiplatform {
                        iosArm64()
                        sourceSets.commonMain.dependencies { implementation(project(":producer")) }
                    }
                }
            }

            include(producer, "producer")
            include(consumerWithDependency, "consumerWithDependency")
            include(consumerWithoutDependency, "consumerWithoutDependency")

            val consumerWithDependencyHasSwiftPMDependencies = consumerWithDependency.providerBuildScriptReturn {
                project.prepareKotlinIdeaImportTask.map {
                    project.ignoreAccessViolations {
                        (project.kotlinExtension as KotlinMultiplatformExtension).hasSwiftPMDependencies
                    }
                }
            }.buildAndReturn(
                "prepareKotlinIdeaImport",
                executingProject = this,
                // CC is also disabled during import
                configurationCache = BuildOptions.ConfigurationCacheValue.DISABLED
            )
            val consumerWithoutDependencyHasSwiftPMDependencies = consumerWithoutDependency.providerBuildScriptReturn {
                project.prepareKotlinIdeaImportTask.map {
                    project.ignoreAccessViolations {
                        (project.kotlinExtension as KotlinMultiplatformExtension).hasSwiftPMDependencies
                    }
                }
            }.buildAndReturn(
                "prepareKotlinIdeaImport",
                executingProject = this,
                configurationCache = BuildOptions.ConfigurationCacheValue.DISABLED
            )

            assertEquals(true, consumerWithDependencyHasSwiftPMDependencies)
            assertEquals(false, consumerWithoutDependencyHasSwiftPMDependencies)
        }
    }

}