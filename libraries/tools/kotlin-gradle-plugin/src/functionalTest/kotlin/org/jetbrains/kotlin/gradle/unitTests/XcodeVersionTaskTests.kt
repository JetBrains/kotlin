/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHost
import org.jetbrains.kotlin.gradle.plugin.mpp.isSupportedMacOs
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XcodeVersionTask
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.util.withModifiedSystemProperties
import org.junit.jupiter.api.parallel.Isolated
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@Isolated("Modifies system properties")
class XcodeVersionTaskTests {

    @Test
    fun `xcodeVersion task is registered with apple targets`() {
        val project = buildProjectWithMPP {
            kotlin {
                iosArm64() {
                    binaries {
                        framework {}
                    }
                }
            }
        }.evaluate()


        val xcodeVersionTask = project.tasks.findByName("xcodeVersion") as XcodeVersionTask?
        assertNotNull(xcodeVersionTask)
        assertEquals(KotlinNativeHost.current().isSupportedMacOs, xcodeVersionTask.onlyIf.isSatisfiedBy(xcodeVersionTask))
    }

    @Test
    fun `xcodeVersion task is skipped on an Intel Mac, which has no Kotlin Native distribution`() {
        withModifiedSystemProperties("os.name" to "Mac OS X", "os.arch" to "x86_64") {
            val project = buildProjectWithMPP {
                kotlin {
                    iosArm64 {
                        binaries {
                            framework {}
                        }
                    }
                }
            }.evaluate()

            val xcodeVersionTask = project.tasks.findByName("xcodeVersion") as XcodeVersionTask?
            assertNotNull(xcodeVersionTask)
            assertFalse(xcodeVersionTask.onlyIf.isSatisfiedBy(xcodeVersionTask))
        }
    }

    @Test
    fun `xcodeVersion task is not registered without apple targets`() {
        val project = buildProjectWithMPP {
            kotlin {
                linuxArm64 {
                    binaries {
                        staticLib()
                    }
                }
            }
        }.evaluate()

        assertNull(project.tasks.findByName("xcodeVersion"))
    }
}