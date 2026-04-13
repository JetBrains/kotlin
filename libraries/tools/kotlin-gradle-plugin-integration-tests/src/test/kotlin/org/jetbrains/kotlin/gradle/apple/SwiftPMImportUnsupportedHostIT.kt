/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.gradle.apple

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.testbase.EnvironmentalVariablesOverride
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.GradleTestVersions
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.NativeGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.OsCondition
import org.jetbrains.kotlin.gradle.testbase.TestVersions
import org.jetbrains.kotlin.gradle.testbase.assertFileExists
import org.jetbrains.kotlin.gradle.testbase.assertTasksExecuted
import org.jetbrains.kotlin.gradle.testbase.assertTasksNoSource
import org.jetbrains.kotlin.gradle.testbase.assertTasksSkipped
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
import org.jetbrains.kotlin.gradle.testbase.compileSource
import org.jetbrains.kotlin.gradle.testbase.compileStubSourceWithSourceSetName
import org.jetbrains.kotlin.gradle.testbase.plugins
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.uklibs.PublisherConfiguration
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.publish
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS

@GradleTestVersions(
    minVersion = TestVersions.Gradle.G_8_1
)
@OsCondition(
    supportedOn = [OS.LINUX, OS.WINDOWS],
    enabledOnCI = [OS.LINUX, OS.WINDOWS],
)
@OptIn(EnvironmentalVariablesOverride::class)
@DisplayName("SwiftPM import integration tests on unsupported hosts")
@NativeGradlePluginTests
class SwiftPMImportUnsupportedHostIT : KGPBaseTest() {

    @GradleTest
    fun `test that project with all apple targets and SPM import assembles`(version: GradleVersion) {
        project("empty", version) {
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

                    sourceSets.iosArm64Main.get().compileStubSourceWithSourceSetName()

                    swiftPMDependencies {
                        swiftPackage(
                            url = url("https://github.com/apple/swift-protobuf.git"),
                            version = exact("1.32.0"),
                            products = listOf(),
                        )
                    }
                }
            }

            build("assemble")
        }
    }

    @GradleTest
    fun `test that project with apple and other native targets and SPM import assembles`(version: GradleVersion) {
        project("empty", version) {
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
                    linuxArm64()

                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                    sourceSets.appleMain.get().compileStubSourceWithSourceSetName()
                    sourceSets.iosArm64Main.get().compileSource(
                        """
                            @file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
                            package producer
                            fun localGreeting(): String {
                                return swiftPMImport.empty.LocalHelper.greeting()
                            }
                        """.trimIndent()
                    )

                    swiftPMDependencies {
                        swiftPackage(
                            url = url("https://github.com/apple/swift-protobuf.git"),
                            version = exact("1.32.0"),
                            products = listOf(),
                        )
                        localSwiftPackage(
                            directory = project.layout.projectDirectory.dir(localSwiftPackageRelativePath),
                            products = listOf(targetName),
                        )
                    }
                }
            }

            build(
                "assemble",
                "-Pkotlin.mpp.enableCInteropCommonization=false" // to avoid KT-73136 Kotlin/Native cinterop commonization bug
            )
        }
    }

    @GradleTest
    fun `check that we publish spm metadata from non apple host`(version: GradleVersion) {
        val producer = project("empty", version) {
            plugins {
                kotlin("multiplatform")
            }

            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    iosSimulatorArm64()
                    linuxArm64()

                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()

                    swiftPMDependencies {
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

        assertFileExists(producer.rootComponent.swiftPmMetadata, "SwiftPM metadata file should exist")
    }
}
