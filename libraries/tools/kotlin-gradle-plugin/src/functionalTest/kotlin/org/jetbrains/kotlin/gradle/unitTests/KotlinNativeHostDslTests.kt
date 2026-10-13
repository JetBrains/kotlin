/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_NATIVE_ENABLE_KLIBS_CROSSCOMPILATION
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.util.applyMultiplatformPlugin
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.util.propertiesExtension
import org.jetbrains.kotlin.gradle.util.withModifiedSystemProperties
import org.junit.jupiter.api.parallel.Isolated
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Isolated("Modifies system properties")
class KotlinNativeHostDslTests {

    @Test
    fun `isCompilableOnCurrentHost is true for a linkable target and a cross-compiled target without cinterops`() {
        withModifiedSystemProperties("os.name" to "Linux", "os.arch" to "amd64") {
            val project = buildProjectWithMPP {
                kotlin {
                    linuxX64()
                    iosArm64()
                }
            }.evaluate()

            assertEquals(true, project.nativeTarget("linuxX64").isCompilableOnCurrentHost.get())
            assertEquals(true, project.nativeTarget("iosArm64").isCompilableOnCurrentHost.get())
        }
    }

    @Test
    fun `isCompilableOnCurrentHost is false for a cross-compiled target with cinterops`() {
        withModifiedSystemProperties("os.name" to "Linux", "os.arch" to "amd64") {
            val project = buildProjectWithMPP {
                kotlin {
                    linuxX64()
                    iosArm64 {
                        compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME).cinterops.create("dummy") {
                            it.defFile("dummy.def")
                        }
                    }
                }
            }.evaluate()

            assertEquals(true, project.nativeTarget("linuxX64").isCompilableOnCurrentHost.get())
            assertEquals(false, project.nativeTarget("iosArm64").isCompilableOnCurrentHost.get())
        }
    }

    @Test
    fun `isCompilableOnCurrentHost is false for a cross-compiled target when cross-compilation is disabled`() {
        withModifiedSystemProperties("os.name" to "Linux", "os.arch" to "amd64") {
            val project = buildProject {
                propertiesExtension.set(KOTLIN_NATIVE_ENABLE_KLIBS_CROSSCOMPILATION, "false")
                applyMultiplatformPlugin()
                kotlin {
                    linuxX64()
                    iosArm64()
                }
            }.evaluate()

            assertEquals(true, project.nativeTarget("linuxX64").isCompilableOnCurrentHost.get())
            assertEquals(false, project.nativeTarget("iosArm64").isCompilableOnCurrentHost.get())
        }
    }

    @Test
    fun `isCompilableOnCurrentHost is false on an unsupported host`() {
        withModifiedSystemProperties("os.name" to "Linux", "os.arch" to "aarch64") {
            val project = buildProjectWithMPP {
                kotlin { linuxX64() }
            }.evaluate()

            assertEquals(false, project.nativeTarget("linuxX64").isCompilableOnCurrentHost.get())
        }
    }

    @Test
    fun `isCompilableOnCurrentHost read before the compilations are finalised fails with a message`() {
        // A target the host cannot link: the answer waits for the compilations to be finalised.
        withModifiedSystemProperties("os.name" to "Linux", "os.arch" to "amd64") {
            val project = buildProjectWithMPP {
                kotlin { macosArm64() }
            }

            val failure = assertFailsWith<IllegalStateException> {
                project.nativeTarget("macosArm64").isCompilableOnCurrentHost.get()
            }
            assertEquals(
                "'isCompilableOnCurrentHost' of target 'macosArm64' is not available yet: it is known once the compilations " +
                        "of the target are finalised. Read it from a task action or wire it into a task input.",
                failure.message,
            )
        }
    }

    private fun org.gradle.api.Project.nativeTarget(name: String): KotlinNativeTarget =
        multiplatformExtension.targets.getByName(name) as KotlinNativeTarget
}
