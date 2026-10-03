/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.regressionTests

import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublication
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.resourcesPublicationExtension
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import kotlin.test.Test
import kotlin.test.assertNotNull

class KT67636JvmWithJavaSetSrcDirsTest {

    @Test
    fun `resources publication - for jvm withJava target - doesn't fail project evaluation`() {
        buildProjectWithMPP {
            kotlin {
                assertNotNull(resourcesPublicationExtension).publishResourcesAsKotlinComponent(
                    target = jvm(),
                    resourcePathForSourceSet = { _ ->
                        KotlinTargetResourcesPublication.ResourceRoot(
                            layout.buildDirectory.dir("foo").map { it.asFile },
                            emptyList(),
                            emptyList(),
                        )
                    },
                    relativeResourcePlacement = layout.buildDirectory.dir("bar").map { it.asFile },
                )

                jvm {
                    @Suppress("DEPRECATION_ERROR")
                    withJava()
                }
            }
        }.evaluate()
    }

}
