/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.gradle.apple

import kotlinx.serialization.ExperimentalSerializationApi
import org.gradle.api.tasks.StopExecutionException
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.register
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.createKotlinExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.FetchSyntheticImportProjectPackages
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.GenerateSyntheticLinkageImportProject
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.GenerateSyntheticLinkageImportProject.SyntheticProductType
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMDependency
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMDependencyIdentifier
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMImportMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.TransitiveSwiftPMDependencies
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.locateOrRegisterSwiftPMDependenciesExtension
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.GradleTestVersions
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.NativeGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.OsCondition
import org.jetbrains.kotlin.gradle.testbase.SwiftPMImportGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.TestVersions
import org.jetbrains.kotlin.gradle.testbase.assertOutputContains
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
import org.jetbrains.kotlin.gradle.testbase.plugins
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.jupiter.api.condition.OS

@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)
@SwiftPMImportGradlePluginTests
class FetchSyntheticImportProjectPackagesTests : KGPBaseTest() {

    @GradleTestVersions(minVersion = TestVersions.Gradle.G_8_0)
    @GradleTest
    fun `fetch task - invalidates - on subpackage manifest changes`(version: GradleVersion) {
        project("empty", version) {
            plugins {
                kotlin("multiplatform").apply(false)
            }

            buildScriptInjection {
                project.createKotlinExtension(KotlinMultiplatformExtension::class)
                val extension = project.locateOrRegisterSwiftPMDependenciesExtension()
                val generation = project.tasks.register<GenerateSyntheticLinkageImportProject>("packageGeneration") {
                    configureWithExtension(extension)
                    konanTargets.set(setOf(KonanTarget.IOS_ARM64))
                    dependencyIdentifierToImportedSwiftPMDependencies.set(
                        TransitiveSwiftPMDependencies(
                            mapOf(
                                SwiftPMDependencyIdentifier("dep") to SwiftPMImportMetadata(
                                    iosDeploymentVersion = "123.0",
                                    macosDeploymentVersion = "234.0",
                                    watchosDeploymentVersion = null,
                                    tvosDeploymentVersion = null,
                                    isModulesDiscoveryEnabled = true,
                                    dependencies = setOf(
                                        SwiftPMDependency.Remote(
                                            repository = SwiftPMDependency.Remote.Repository.Url("https://foo.bar/baz"),
                                            version = SwiftPMDependency.Remote.Version.Exact(project.property("transitiveDepVersion") as String),
                                            products = listOf(
                                                SwiftPMDependency.Product("dep"),
                                            ),
                                            cinteropClangModules = emptyList(),
                                            packageName = "baz",
                                            traits = setOf()
                                        )
                                    ),
                                ),
                            )
                        )
                    )
                    syntheticProductType.set(SyntheticProductType.INFERRED)
                }

                project.tasks.register<FetchSyntheticImportProjectPackages>("packageFetch") {
                    syntheticImportProjectRoot.set(generation.map { it.syntheticImportProjectRoot.get() })
                    doFirst { throw StopExecutionException() }
                }
            }

            build("packageFetch", "-PtransitiveDepVersion=1.0.0") {
                // For some reason "doFirst { throw StopExecutionException() }" prevents regular task(path) from returning the task path
                assert(tasks.single { it.path == ":packageFetch" }.outcome == TaskOutcome.SUCCESS)
            }
            build("packageFetch", "-PtransitiveDepVersion=1.0.0") {
                assert(tasks.single { it.path == ":packageFetch" }.outcome == TaskOutcome.UP_TO_DATE)
            }
            build("packageFetch", "-PtransitiveDepVersion=1.0.1") {
                assert(tasks.single { it.path == ":packageFetch" }.outcome == TaskOutcome.SUCCESS)
                assertOutputContains("subpackages/dep/Package.swift has changed")
            }
        }
    }
}
