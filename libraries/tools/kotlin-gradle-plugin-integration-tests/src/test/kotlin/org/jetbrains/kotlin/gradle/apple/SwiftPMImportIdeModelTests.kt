/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.gradle.apple

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.ide.prepareKotlinIdeaImportTask
import org.jetbrains.kotlin.gradle.plugin.ide.Idea222Api
import org.jetbrains.kotlin.gradle.testbase.BuildOptions
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.OsCondition
import org.jetbrains.kotlin.gradle.testbase.SwiftPMImportGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
import org.jetbrains.kotlin.gradle.testbase.plugins
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.testbase.providerBuildScriptReturn
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.ignoreAccessViolations
import org.jetbrains.kotlin.gradle.uklibs.include
import org.jetbrains.kotlin.gradle.util.runProcess
import org.junit.jupiter.api.condition.OS
import kotlin.io.path.createDirectories
import kotlin.test.assertEquals

@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)
@SwiftPMImportGradlePluginTests
class SwiftPMImportIdeModelTests : KGPBaseTest() {

    @OptIn(Idea222Api::class)
    @GradleTest
    fun `hasSwiftPMDependencies - transitive SwiftPM dependencies from project dependencies - influence hasSwiftPMDependencies flag`(version: GradleVersion) {
        project("empty", version) {
            plugins {
                kotlin("multiplatform").apply(false)
            }

            val producer = project("empty", version) {
                val localPackage = projectPath.resolve("localPackage").also { it.createDirectories() }.toFile()
                runProcess(listOf("swift", "package", "init", "--type", "library"), localPackage)

                plugins {
                    kotlin("multiplatform")
                }
                buildScriptInjection {
                    project.applyMultiplatform {
                        iosArm64()

                        swiftPMDependencies {
                            localSwiftPackage(project.layout.projectDirectory.dir("localPackage"), products = listOf("localPackage"))
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

            val consumerWithDependencyHasSwiftPMDependenciesProvider = consumerWithDependency.providerBuildScriptReturn {
                project.prepareKotlinIdeaImportTask.map {
                    project.ignoreAccessViolations {
                        (project.kotlinExtension as KotlinMultiplatformExtension).swiftPMImportIdeModel?.hasSwiftPMDependencies ?: error("...")
                    }
                }
            }
            val consumerWithoutDependencyHasSwiftPMDependenciesProvider = consumerWithoutDependency.providerBuildScriptReturn {
                project.prepareKotlinIdeaImportTask.map {
                    project.ignoreAccessViolations {
                        (project.kotlinExtension as KotlinMultiplatformExtension).swiftPMImportIdeModel?.hasSwiftPMDependencies ?: error("...")
                    }
                }
            }

            include(producer, "producer")
            include(consumerWithDependency, "consumerWithDependency")
            include(consumerWithoutDependency, "consumerWithoutDependency")

            val consumerWithDependencyHasSwiftPMDependencies = consumerWithDependencyHasSwiftPMDependenciesProvider.buildAndReturn(
                "prepareKotlinIdeaImport",
                executingProject = this,
                // CC is also disabled during import
                configurationCache = BuildOptions.ConfigurationCacheValue.DISABLED
            )
            val consumerWithoutDependencyHasSwiftPMDependencies = consumerWithoutDependencyHasSwiftPMDependenciesProvider.buildAndReturn(
                "prepareKotlinIdeaImport",
                executingProject = this,
                configurationCache = BuildOptions.ConfigurationCacheValue.DISABLED
            )

            assertEquals(true, consumerWithDependencyHasSwiftPMDependencies)
            assertEquals(false, consumerWithoutDependencyHasSwiftPMDependencies)
        }
    }

}