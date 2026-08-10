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
import org.jetbrains.kotlin.gradle.uklibs.addPublishedProjectToRepositories
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.publish
import org.junit.jupiter.api.condition.OS
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMImportMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.deserializeSwiftPMImportMetadata
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertFalse


@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)
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

    @GradleTest
    fun `transitive iOS-only SwiftPM dependency is constrained on a macOS consumer`(version: GradleVersion) {
        val localPackageName = "iOSOnlyLib"
        val producer = project("empty", version) {
            val localSwiftPackageRelativePath = "../$localPackageName"
            val localPackageDir = projectPath.resolve(localSwiftPackageRelativePath)
            createLocalSwiftPackage(localPackageDir, packageName = localPackageName)
            // UIKit only exists on iOS, so a macOS build of this package fails to compile.
            localPackageDir.resolve("Sources/$localPackageName/$localPackageName.swift").writeText(
                """
                    import UIKit

                    @objc public class LocalHelper: NSObject {
                        @objc public static func greeting() -> String {
                            return UIColor.red.description
                        }
                    }
                """.trimIndent()
            )

            plugins { kotlin("multiplatform") }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    iosSimulatorArm64()

                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()

                    swiftPMDependencies {
                        localSwiftPackage(
                            directory = project.layout.projectDirectory.dir(localSwiftPackageRelativePath),
                            products = listOf(localPackageName),
                        )
                    }
                }
            }
        }.publish(publisherConfiguration = PublisherConfiguration(group = "dependency"))

        project("emptyxcode", version) {
            plugins { kotlin("multiplatform") }

            addPublishedProjectToRepositories(producer)

            buildScriptInjection {
                project.applyMultiplatform {
                    listOf(
                        iosArm64(),
                        iosSimulatorArm64(),
                        macosArm64(),
                    ).forEach {
                        // Only a dynamic framework's link task builds the synthetic SwiftPM package.
                        it.binaries.framework {
                            baseName = "Shared"
                            isStatic = false
                        }
                    }

                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                    // Producer publishes only iOS variants, so keep the dependency in iosMain;
                    // macosArm64Main can't resolve it. iOS compilations still feed the transitive
                    // SwiftPM metadata used to generate the macOS synthetic package.
                    sourceSets.iosMain {
                        dependencies {
                            implementation(producer.rootCoordinate)
                        }
                    }
                }
            }

            // linkDebugFrameworkMacosArm64 builds the macOS synthetic package with xcodebuild.
            // Without the fix iOSOnlyLib is pulled in on macOS too and fails on `import UIKit`;
            // with it, macOS is excluded. The iOS link is a control that always passes.
            build("linkDebugFrameworkIosArm64", "linkDebugFrameworkMacosArm64")
        }
    }
}
