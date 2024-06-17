/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XcodeVersionTask
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.konan.target.HostManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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
        assertEquals(HostManager.hostIsMac, xcodeVersionTask.onlyIf.isSatisfiedBy(xcodeVersionTask))
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