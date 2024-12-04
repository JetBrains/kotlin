/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublication
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.resourcesPublicationExtension
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.enableMppResourcesPublication
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.util.runLifecycleAwareTest
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RegisterMultiplatformResourcesPublicationExtensionActionTests {

    @Test
    fun `test mppResourcesPublication toggle - prevents extension creation`() {
        assertNull(
            buildProjectWithMPP(
                preApplyCode = {
                    enableMppResourcesPublication(false)
                }
            ) {
                kotlin { jvm() }
            }.evaluate().multiplatformExtension.resourcesPublicationExtension
        )
    }

    @Test
    fun `test mppResourcesPublication toggle - enables extension creation`() {
        assertNotNull(
            buildProjectWithMPP {
                kotlin { jvm() }
                enableMppResourcesPublication(true)
            }.evaluate().multiplatformExtension.resourcesPublicationExtension
        )
    }

}