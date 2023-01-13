/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests.kpm

import kotlin.test.Test
import kotlin.test.assertEquals

class TransitiveRuntimeOnlyConfigurationTest : AbstractKpmExtensionTest() {

    @Test
    fun `test transitiveRuntimeOnlyConfiguration contains dependencies from refines parents`() {
        project.applyKpmPlugin()
        kotlin.mainAndTest {
            val left = fragments.create("left") { it.refines(common) }
            val right = fragments.create("right") { it.refines(common) }
            val bottomLeft = fragments.create("bottom") { it.refines(left) }

            common.dependencies {
                implementation(project.files("common-implementation.jar"))
                runtimeOnly(project.files("common-runtimeOnly.jar"))
            }

            left.dependencies {
                implementation(project.files("left-implementation.jar"))
                runtimeOnly(project.files("left-runtimeOnly.jar"))
            }

            right.dependencies {
                implementation(project.files("right-implementation.jar"))
                runtimeOnly(project.files("right-runtimeOnly.jar"))
            }

            bottomLeft.dependencies {
                implementation(project.files("bottomLeft-implementation.jar"))
                runtimeOnly(project.files("bottomLeft-runtimeOnly.jar"))
            }

            bottomLeft.transitiveRuntimeOnlyConfiguration.isCanBeResolved = true
            assertEquals(
                project.files("common-runtimeOnly.jar", "left-runtimeOnly.jar", "bottomLeft-runtimeOnly.jar").toSet(),
                bottomLeft.transitiveRuntimeOnlyConfiguration.files
            )
        }
    }
}
