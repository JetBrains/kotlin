/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.jetbrains.kotlin.gradle.BaseGradleIT
import org.jetbrains.kotlin.gradle.transformProjectWithPluginsDsl
import org.junit.Test

class KpmCinteropModuleIT : BaseGradleIT() {

    @Test
    fun `check cinterop and commonization for KPM module`() {
        val project = transformProjectWithPluginsDsl("kpm-cinterop-module")
        with(project) {
            build("compileLinuxMainKotlinNativeMetadata") {
                assertSuccessful()
                assertTasksExecuted(
                    ":cinteropSampleInteropLinuxArm64",
                    ":cinteropSampleInteropLinuxX64",
                    ":commonizeSampleInterop",
                    ":commonizeSampleInteropForLinux"
                )
                assertTasksNotRegistered(
                    ":cinteropSampleInteropMacosX64",
                    ":commonizeSampleInteropForDesktop"
                )
            }

            projectDir.resolve("src/nativeInterop/cinterop/sampleInterop.h").writeText("")
            build("compileLinuxMainKotlinNativeMetadata") {
                assertFailed()
                assertTasksExecuted(
                    ":cinteropSampleInteropLinuxArm64",
                    ":cinteropSampleInteropLinuxX64",
                    ":commonizeSampleInterop"
                )
                assertTasksUpToDate(":commonizeSampleInteropForLinux")
                assertContains("e: ${projectDir.absolutePath}/src/linuxMain/kotlin/Main.kt: (3, 22): Unresolved reference: sampleInterop")
                assertContains("e: ${projectDir.absolutePath}/src/linuxMain/kotlin/Main.kt: (6, 15): Unresolved reference: sampleInterop")
            }
        }
    }
}