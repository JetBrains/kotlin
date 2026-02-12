/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.GenerateSyntheticLinkageImportProject.Companion.SYNTHETIC_IMPORT_TARGET_MAGIC_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.IntegrateLinkagePackageIntoXcodeProject
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMImportIdeContext
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.swiftPMDependenciesExtension
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import kotlin.test.Test
import kotlin.test.assertEquals

class SwiftPMImportTests {
    @Test
    fun `swiftPMImportIdeContext - project without SwiftPM dependencies`() {
        assertEquals(
            SwiftPMImportIdeContext(
                hasSwiftPMDependencies = false,
                integrateLinkagePackageTaskPath = ":${IntegrateLinkagePackageIntoXcodeProject.TASK_NAME}",
                magicPackageName = SYNTHETIC_IMPORT_TARGET_MAGIC_NAME,
            ),
            buildProjectWithMPP {
                kotlin {
                    iosArm64()
                }
            }.multiplatformExtension.swiftPMImportIdeContext
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
            }.multiplatformExtension.swiftPMImportIdeContext.integrateLinkagePackageTaskPath
        )
    }

    // The rest of this suite has to be implemented as an integration test because interproject SwiftPM dependencies work by serializing using a task
    @Test
    fun `swiftPMImportIdeContext - project with direct SwiftPM dependency`() {
        assertEquals(
            SwiftPMImportIdeContext(
                hasSwiftPMDependencies = true,
                integrateLinkagePackageTaskPath = ":${IntegrateLinkagePackageIntoXcodeProject.TASK_NAME}",
                magicPackageName = SYNTHETIC_IMPORT_TARGET_MAGIC_NAME,
            ),
            buildProjectWithMPP {
                kotlin {
                    iosArm64()
                    swiftPMDependenciesExtension().`package`(url = "foo", version = "1.0.0", products = listOf("bar"))
                }
            }.multiplatformExtension.swiftPMImportIdeContext
        )
    }
}