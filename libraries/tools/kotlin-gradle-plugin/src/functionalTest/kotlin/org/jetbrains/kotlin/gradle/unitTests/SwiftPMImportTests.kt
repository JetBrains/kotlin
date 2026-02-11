/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.swiftPMDependenciesExtension
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import kotlin.test.Test
import kotlin.test.assertEquals

class SwiftPMImportTests {
    @Test
    fun `hasSwiftPMDependencies - project without SwiftPM dependencies`() {
        assertEquals(
            false,
            buildProjectWithMPP {
                kotlin {
                    iosArm64()
                }
            }.multiplatformExtension.hasSwiftPMDependencies
        )
    }

    // The rest of this suite has to be implemented as an integration test because interproject SwiftPM dependencies work by serializing using a task
    @Test
    fun `hasSwiftPMDependencies - project with direct SwiftPM dependency`() {
        assertEquals(
            true,
            buildProjectWithMPP {
                kotlin {
                    iosArm64()
                    swiftPMDependenciesExtension().`package`(url = "foo", version = "1.0.0", products = listOf("bar"))
                }
            }.multiplatformExtension.hasSwiftPMDependencies
        )
    }
}