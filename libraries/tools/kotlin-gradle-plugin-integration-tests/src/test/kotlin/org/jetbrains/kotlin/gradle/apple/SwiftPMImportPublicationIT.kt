/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.gradle.apple

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMDependency
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.PublisherConfiguration
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.publish
import org.junit.jupiter.api.condition.OS
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMImportMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.deserializeSwiftPMImportMetadata
import kotlin.test.assertEquals
import kotlin.test.assertFalse


@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)
@OptIn(EnvironmentalVariablesOverride::class)
@SwiftPMImportGradlePluginTests
class SwiftPMImportPublicationIT : KGPBaseTest() {

    @GradleTest
    fun `check published library with remote spm dependency`(version: GradleVersion) {
        val producer = project("empty", version) {
            plugins {
                kotlin("multiplatform")
            }

            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    iosSimulatorArm64()

                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()

                    swiftPMDependencies {
                        iosMinimumDeploymentTarget.set("18.0")
                        swiftPackage(
                            url = url("https://github.com/apple/swift-protobuf.git"),
                            version = exact("1.32.0"),
                            products = listOf(),
                        )
                    }
                }
            }
        }.publish(publisherConfiguration = PublisherConfiguration(group = "dependency"))

        producer.assertSwiftPMMetadataVariantExistsInRootComponent()

        val spmMetadata = producer.rootComponent.swiftPmMetadata
        assertFileExists(spmMetadata, "SwiftPM metadata file should exist")

        assertEquals(
            SwiftPMImportMetadata(
                konanTargets = setOf("ios_arm64", "ios_simulator_arm64"),
                iosDeploymentVersion = "18.0",
                macosDeploymentVersion = null,
                watchosDeploymentVersion = null,
                tvosDeploymentVersion = null,
                isModulesDiscoveryEnabled = true,
                dependencies = setOf(
                    SwiftPMDependency.Remote(
                        repository = SwiftPMDependency.Remote.Repository.Url("https://github.com/apple/swift-protobuf.git"),
                        version = SwiftPMDependency.Remote.Version.Exact("1.32.0"),
                        products = listOf(),
                        cinteropClangModules = emptyList(),
                        packageName = "swift-protobuf",
                        traits = setOf()
                    )
                ),
            ),
            deserializeSwiftPMImportMetadata(spmMetadata.inputStream())
        )
    }

    @GradleTest
    fun `check publishing library with local swift package`(version: GradleVersion) {
        val producer = project("empty", version) {
            val localSwiftPackageRelativePath = "../localSwiftPackage"
            val localPackageDir = projectPath.resolve(localSwiftPackageRelativePath)
            val targetName = "LocalSwiftPackage"

            createLocalSwiftPackage(localPackageDir, packageName = targetName)

            plugins {
                kotlin("multiplatform")
            }

            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    iosSimulatorArm64()

                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()

                    swiftPMDependencies {
                        localSwiftPackage(
                            directory = project.layout.projectDirectory.dir(localSwiftPackageRelativePath),
                            products = listOf(targetName),
                        )
                    }
                }
            }

        }.publish(publisherConfiguration = PublisherConfiguration(group = "dependency"))

        val absolutePackagePath = producer.repository.parentFile.resolve("../localSwiftPackage").canonicalFile

        producer.assertSwiftPMMetadataVariantExistsInRootComponent()

        val spmMetadata = producer.rootComponent.swiftPmMetadata
        assertFileExists(spmMetadata, "SwiftPM metadata file should exist")

        assertEquals(
            SwiftPMImportMetadata(
                konanTargets = setOf("ios_arm64", "ios_simulator_arm64"),
                iosDeploymentVersion = null,
                macosDeploymentVersion = null,
                watchosDeploymentVersion = null,
                tvosDeploymentVersion = null,
                isModulesDiscoveryEnabled = true,
                dependencies = setOf(
                    SwiftPMDependency.Local(
                        absolutePath = absolutePackagePath,
                        products = listOf(
                            SwiftPMDependency.Product(
                                name = "LocalSwiftPackage",
                                cinteropClangModules = emptySet(),
                                platformConstraints = null
                            )
                        ),
                        cinteropClangModules = listOf(
                            SwiftPMDependency.CinteropClangModule(
                                name = "LocalSwiftPackage",
                                platformConstraints = null
                            )
                        ),
                        packageName = "localSwiftPackage",
                        traits = setOf()
                    )
                ),
            ),
            deserializeSwiftPMImportMetadata(spmMetadata.inputStream())
        )
    }

    @GradleTest
    fun `check library without spm dependency does not publish spm artifact in root component`(version: GradleVersion) {
        val producer = project("empty", version) {
            plugins {
                kotlin("multiplatform")
            }

            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    iosSimulatorArm64()

                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                }
            }

        }.publish(publisherConfiguration = PublisherConfiguration(group = "dependency"))

        assertFalse(producer.rootComponent.swiftPmMetadata.exists(), "SwiftPM metadata file should be published")
    }
}
