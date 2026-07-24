/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.DeclaredSwiftPMDependencies
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.FetchSyntheticImportProjectPackages
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.GenerateSyntheticLinkageImportProject.Companion.SYNTHETIC_IMPORT_TARGET_MAGIC_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.IntegrateLinkagePackageIntoXcodeProject
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.LocalSwiftPMDependencyForIde
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.RemoteSwiftPMDependencyForIde
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMImportIdeModel
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.locateOrRegisterSwiftPMDependenciesExtension
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import kotlin.test.Test
import kotlin.test.assertEquals

class SwiftPMImportIdeModelTests {
    @Test
    fun `swiftPMImportIdeModel - project without SwiftPM dependencies`() {
        assertEquals(
            SwiftPMImportIdeModel(
                hasSwiftPMDependencies = false,
                integrateLinkagePackageTaskPath = ":${IntegrateLinkagePackageIntoXcodeProject.TASK_NAME}",
                magicPackageName = SYNTHETIC_IMPORT_TARGET_MAGIC_NAME,
                declaredSwiftPMDependencies = null,
            ),
            buildProjectWithMPP {
                kotlin {
                    iosArm64()
                }
            }.multiplatformExtension.swiftPMImportIdeModel
        )
    }

    @Test
    fun `integrateLinkagePackageTaskPath - for subproject`() {
        val root = buildProject()
        assertEquals(
            ":foo:${IntegrateLinkagePackageIntoXcodeProject.TASK_NAME}",
            buildProjectWithMPP(projectBuilder = {
                withParent(root)
                withName("foo")
            }) {
                kotlin {
                    iosArm64()
                }
            }.multiplatformExtension.swiftPMImportIdeModel?.integrateLinkagePackageTaskPath
        )
    }

    // The rest of this suite has to be implemented as an integration test because interproject SwiftPM dependencies work by serializing using a task
    @Test
    fun `swiftPMImportIdeModel - project with direct SwiftPM dependency`() {
        val project = buildProjectWithMPP {
            kotlin {
                iosArm64()
                locateOrRegisterSwiftPMDependenciesExtension().swiftPackage(url = "foo", version = "1.0.0", products = listOf("bar"))
            }
        }

        val fetchTask = project.tasks.getByName(FetchSyntheticImportProjectPackages.TASK_NAME) as FetchSyntheticImportProjectPackages

        assertEquals(
            SwiftPMImportIdeModel(
                hasSwiftPMDependencies = true,
                integrateLinkagePackageTaskPath = ":${IntegrateLinkagePackageIntoXcodeProject.TASK_NAME}",
                magicPackageName = SYNTHETIC_IMPORT_TARGET_MAGIC_NAME,
                declaredSwiftPMDependencies = DeclaredSwiftPMDependencies(
                    dependencies = listOf(
                        RemoteSwiftPMDependencyForIde("foo")
                    ),
                    checkoutPath = fetchTask.swiftPMDependenciesCheckout.get().asFile,
                    swiftPackageResolveTaskPath = ":${FetchSyntheticImportProjectPackages.TASK_NAME}",
                )
            ),
            project.multiplatformExtension.swiftPMImportIdeModel
        )
    }

    @Test
    fun `swiftPMImportIdeModel - project with direct local SwiftPM dependency`() {
        val project = buildProjectWithMPP {
            kotlin {
                iosArm64()

                val localPackage = layout.projectDirectory.dir("localPackage")

                locateOrRegisterSwiftPMDependenciesExtension().localSwiftPackage(
                    directory = localPackage,
                    products = listOf("bar"),
                )
            }
        }

        val localPackage = project.layout.projectDirectory.dir("localPackage").asFile
        val fetchTask = project.tasks.getByName(FetchSyntheticImportProjectPackages.TASK_NAME) as FetchSyntheticImportProjectPackages

        assertEquals(
            SwiftPMImportIdeModel(
                hasSwiftPMDependencies = true,
                integrateLinkagePackageTaskPath = ":${IntegrateLinkagePackageIntoXcodeProject.TASK_NAME}",
                magicPackageName = SYNTHETIC_IMPORT_TARGET_MAGIC_NAME,
                declaredSwiftPMDependencies = DeclaredSwiftPMDependencies(
                    dependencies = listOf(
                        LocalSwiftPMDependencyForIde(localPackage)
                    ),
                    checkoutPath = fetchTask.swiftPMDependenciesCheckout.get().asFile,
                    swiftPackageResolveTaskPath = ":${FetchSyntheticImportProjectPackages.TASK_NAME}",
                )
            ),
            project.multiplatformExtension.swiftPMImportIdeModel
        )
    }
}
