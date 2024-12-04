/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.regressionTests

import org.gradle.api.internal.provider.MissingValueException
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublication
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.resourcesPublicationExtension
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFailsWith
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
                    withJava()
                }
            }
        }.evaluate()
    }

    // FIXME: withJava forces providers in resources to be eagerly evaluated. See KT-67636
    @Test
    fun `jvm withJava target - provider in compilation's resources - fails project evaluation`() {
        assertFailsWith(MissingValueException::class) {
            buildProjectWithMPP {
                kotlin {
                    val prop = objects.property(File::class.java)

                    jvm {
                        compilations.getByName("main").defaultSourceSet.resources.srcDir(prop)
                        withJava()
                    }

                    prop.set(File("test"))
                }
            }.evaluate()
        }
    }

}