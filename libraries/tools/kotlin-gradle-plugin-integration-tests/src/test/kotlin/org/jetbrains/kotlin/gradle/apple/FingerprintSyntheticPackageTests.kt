/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.gradle.apple

import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.register
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.FingerprintSyntheticPackage
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.PackageResolvedSynchronization
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMDependency
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMDependencyIdentifier
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMImportMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.TransitiveSwiftPMMetadata
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.OsCondition
import org.jetbrains.kotlin.gradle.testbase.SwiftPMImportGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.assertTasksExecuted
import org.jetbrains.kotlin.gradle.testbase.assertTasksUpToDate
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
import org.jetbrains.kotlin.gradle.testbase.buildScriptReturn
import org.jetbrains.kotlin.gradle.testbase.plugins
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.uklibs.include
import org.junit.jupiter.api.condition.OS
import kotlin.String
import kotlin.io.path.readText
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)
@SwiftPMImportGradlePluginTests
class FingerprintSyntheticPackageTests : KGPBaseTest() {

    @GradleTest
    fun `test fingerprint task - ordering of arguments invalidates the fingeprint task, but the fingerprint remains stable`(version: GradleVersion) {
        project("empty", version) {
            plugins {
                kotlin("multiplatform").apply(false)
            }
            val product1Parameter = "product1"
            val product2Parameter = "product2"
            buildScriptInjection {
                project.tasks.register<FingerprintSyntheticPackage>("fingerprint") {
                    packageResolvedSynchronizationFingerprint.set(PackageResolvedSynchronization.Identifier("Foo"))
                    transitiveSwiftPMMetadata.set(
                        TransitiveSwiftPMMetadata(
                            mapOf(
                                SwiftPMDependencyIdentifier("dep", true) to SwiftPMImportMetadata(
                                    konanTargets = setOf("ios_arm64"),
                                    iosDeploymentVersion = "123.0",
                                    macosDeploymentVersion = "234.0",
                                    watchosDeploymentVersion = null,
                                    tvosDeploymentVersion = null,
                                    isModulesDiscoveryEnabled = true,
                                    dependencies = setOf(
                                        SwiftPMDependency.Remote(
                                            repository = SwiftPMDependency.Remote.Repository.Url("https://foo.bar/baz"),
                                            version = SwiftPMDependency.Remote.Version.Exact("1.0.0"),
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
                    directSwiftPMMetadata.set(
                        SwiftPMImportMetadata(
                            konanTargets = setOf("foo"),
                            iosDeploymentVersion = null,
                            macosDeploymentVersion = null,
                            watchosDeploymentVersion = null,
                            tvosDeploymentVersion = null,
                            isModulesDiscoveryEnabled = false,
                            dependencies = setOf(
                                SwiftPMDependency.Remote(
                                    repository = SwiftPMDependency.Remote.Repository.Url("https://foo.bar/baz"),
                                    version = SwiftPMDependency.Remote.Version.Exact("1.0.0"),
                                    products = listOf(
                                        SwiftPMDependency.Product(project.property(product1Parameter) as String),
                                        SwiftPMDependency.Product(project.property(product2Parameter) as String),
                                    ),
                                    cinteropClangModules = emptyList(),
                                    packageName = "baz",
                                    traits = setOf()
                                )
                            )
                        )
                    )
                }
            }

            val outputPath = buildScriptReturn {
                project.tasks.withType(FingerprintSyntheticPackage::class.java).single().syntheticPackageFingerprint.get().asFile
            }.buildAndReturn("tasks", "-P${product1Parameter}=a", "-P${product2Parameter}=b")

            build("fingerprint", "-P${product1Parameter}=a", "-P${product2Parameter}=b")
            val initialHash = outputPath.readText()

            // If ordering didn't change, then the task should be UTD
            build("fingerprint", "-P${product1Parameter}=a", "-P${product2Parameter}=b") {
                assertTasksUpToDate(":fingerprint")
            }

            // If ordering changed, then the task should be executed
            build("fingerprint", "-P${product1Parameter}=b", "-P${product2Parameter}=a") {
                assertTasksExecuted(":fingerprint")
            }
            // but the fingerprint normalization should produce the same fingerprint
            val secondHash = outputPath.readText()
            assertEquals(initialHash, secondHash)

            // And if we change values, then task and the fingerprint change
            build("fingerprint", "-P${product1Parameter}=b", "-P${product2Parameter}=c") {
                assertTasksExecuted(":fingerprint")
            }
            val thirdHash = outputPath.readText()
            assertNotEquals(initialHash, thirdHash)
        }
    }

    @GradleTest
    fun `fingerprint task - root project fingerprint is identical to left and right because of dependency flattening and product ordering normalization`(version: GradleVersion) {
        val rightProjectName = "rightProject"
        val leftProjectName = "leftProject"
        project("empty", version) {
            withLockFileFixture {
                val swiftPmPackage =
                    repoRef("Maps").also { createRepo(it.name, listOf("1.0.0"), products = listOf("MapsCore", "MapsUtils")) }

                initSwiftPmProject(cacheDirFile) {
                    sourceSets.appleMain.dependencies {
                        api(project(":$rightProjectName"))
                    }
                }

                val leftProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                            swiftPackage(
                                url = url(swiftPmPackage.url),
                                version = exact("1.0.0"),
                                products = listOf(product("MapsCore"), product("MapsUtils"))
                            )
                        }
                    }
                }

                val rightProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                            swiftPackage(
                                url = url(swiftPmPackage.url),
                                version = exact("1.0.0"),
                                products = listOf(product("MapsUtils"), product("MapsCore"))
                            )
                        }
                    }
                }

                include(rightProject, rightProjectName)
                include(leftProject, leftProjectName)

                val prepareFingerPrint = FingerprintSyntheticPackage.TASK_NAME

                build(
                    ":$prepareFingerPrint",
                    ":$rightProjectName:$prepareFingerPrint",
                    ":$leftProjectName:$prepareFingerPrint",
                ) {

                    assertTasksExecuted(
                        ":$prepareFingerPrint",
                        ":$rightProjectName:$prepareFingerPrint",
                        ":$leftProjectName:$prepareFingerPrint",
                    )

                    assertEquals(
                        rightProject.projectPath.resolve(SYNTHETIC_PACKAGE_FINGERPRINT_BUILD_DIR_PATH)
                            .readText()
                            .trim(),
                        projectPath.resolve(SYNTHETIC_PACKAGE_FINGERPRINT_BUILD_DIR_PATH)
                            .readText()
                            .trim(),
                        "Projects with same flattened dependency graphs and same build settings should have same fingerprint"
                    )

                    assertEquals(
                        rightProject.projectPath.resolve(SYNTHETIC_PACKAGE_FINGERPRINT_BUILD_DIR_PATH)
                            .readText()
                            .trim(),
                        leftProject.projectPath.resolve(SYNTHETIC_PACKAGE_FINGERPRINT_BUILD_DIR_PATH)
                            .readText()
                            .trim(),
                        "Projects with same flattened dependency products and same build settings should have same fingerprint"
                    )
                }

            }
        }
    }

    @GradleTest
    fun `fingerprint task - root project fingerprint is different - when root has additional dependencies`(version: GradleVersion) {
        val subProjectName = "subProject"
        project("empty", version) {
            withLockFileFixture {
                val mapsPackage = repoRef("Maps").also { createRepo(it.name, listOf("1.0.0")) }
                val crpytoPackage = repoRef("Crypto").also { createRepo(it.name, listOf("1.0.0")) }

                initSwiftPmProject(cacheDirFile) {
                    swiftPMDependencies {
                        swiftPackage(
                            url = url(crpytoPackage.url),
                            version = exact("1.0.0"),
                            products = listOf(product(crpytoPackage.name))
                        )
                    }
                    sourceSets.appleMain.dependencies {
                        api(project(":$subProjectName"))
                    }
                }

                val subProject = project("empty", version) {
                    initSwiftPmProject(cacheDirFile) {
                        swiftPMDependencies {
                            swiftPackage(
                                url = url(mapsPackage.url),
                                version = exact("1.0.0"),
                                products = listOf(product(mapsPackage.name))
                            )
                        }
                    }
                }


                include(subProject, subProjectName)

                val prepareFingerPrint = FingerprintSyntheticPackage.TASK_NAME

                build(
                    ":$prepareFingerPrint",
                    ":$subProjectName:$prepareFingerPrint",
                ) {

                    assertTasksExecuted(
                        ":$prepareFingerPrint",
                        ":$subProjectName:$prepareFingerPrint",
                    )

                    assertNotEquals(
                        subProject.projectPath.resolve(SYNTHETIC_PACKAGE_FINGERPRINT_BUILD_DIR_PATH)
                            .readText()
                            .trim(),
                        projectPath.resolve(SYNTHETIC_PACKAGE_FINGERPRINT_BUILD_DIR_PATH)
                            .readText()
                            .trim(),
                        "Projects with different flattened dependency graphs should have different fingerprint"
                    )
                }

            }
        }
    }

    @GradleTest
    fun `fingerprint task changing ios deployment version changes fingerprint`(version: GradleVersion) {
        val useIosDeploymentTarget16 = "useIosDeploymentTarget16"
        project("empty", version) {
            withLockFileFixture {
                val crpytoPackage = repoRef("Crypto").also { createRepo(it.name, listOf("1.0.0")) }

                initSwiftPmProject(cacheDirFile) {
                    swiftPMDependencies {
                        if (project.hasProperty(useIosDeploymentTarget16)) {
                            iosMinimumDeploymentTarget.set("16.0")
                        } else {
                            iosMinimumDeploymentTarget.set("15.0")
                        }
                        swiftPackage(
                            url = url(crpytoPackage.url),
                            version = exact("1.0.0"),
                            products = listOf(product(crpytoPackage.name))
                        )
                    }
                }

                build(
                    ":${FingerprintSyntheticPackage.TASK_NAME}",
                ) {
                    assertTasksExecuted(
                        ":${FingerprintSyntheticPackage.TASK_NAME}",
                    )

                    val firstFingerprint = projectPath.resolve(SYNTHETIC_PACKAGE_FINGERPRINT_BUILD_DIR_PATH)
                        .readText()
                        .trim()

                    // Rebuild with different deployment target
                    build(
                        ":${FingerprintSyntheticPackage.TASK_NAME}",
                        "-P$useIosDeploymentTarget16=true"
                    ) {
                        assertTasksExecuted(
                            ":${FingerprintSyntheticPackage.TASK_NAME}",
                        )

                        val secondFingerprint = projectPath.resolve(SYNTHETIC_PACKAGE_FINGERPRINT_BUILD_DIR_PATH)
                            .readText()
                            .trim()

                        assertNotEquals(
                            firstFingerprint,
                            secondFingerprint,
                            "Changing iOS deployment target should change the fingerprint"
                        )
                    }
                }
            }
        }
    }
}
