/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.gradle.apple

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.FingerprintSyntheticPackage
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.OsCondition
import org.jetbrains.kotlin.gradle.testbase.SwiftPMImportGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.assertTasksExecuted
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.uklibs.include
import org.junit.jupiter.api.condition.OS
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
    fun `fingerprint task generates same fingerprint given the two target with same flattened dependency graph`(version: GradleVersion) {
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
    fun `fingerprint task generates different fingerprint given the two target with different flattened dependency graph`(version: GradleVersion) {
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
