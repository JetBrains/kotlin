/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.jetbrains.kotlin.gradle.BaseGradleIT
import org.jetbrains.kotlin.gradle.GradleVersionRequired
import org.jetbrains.kotlin.gradle.transformProjectWithPluginsDsl
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.Assume
import org.junit.BeforeClass
import org.junit.Test
import kotlin.test.assertTrue

class AppleFrameworkNonMacIT : BaseGradleIT() {

    override val defaultGradleVersion: GradleVersionRequired
        get() = GradleVersionRequired.FOR_MPP_SUPPORT

    @Test
    fun `check success podImport`() {
        transformProjectWithPluginsDsl(
            "native-cocoapods-tests"
        ).build("podImport", "-Pkotlin.native.cocoapods.generate.wrapper=true") {
            assertSuccessful()
            assertTrue { output.contains("Kotlin Cocoapods Plugin is fully supported on mac machines only. Gradle tasks that can not run on non-mac hosts will be skipped.") }
        }
    }

    @Test
    fun `check XCFramework and FatFramework tasks are SKIPPED`() {
        transformProjectWithPluginsDsl("appleXCFramework")
            .build("assembleSharedDebugXCFramework") {
                assertSuccessful()
                assertTasksSkipped(":shared:linkDebugFrameworkIosArm64")
                assertTasksSkipped(":shared:linkDebugFrameworkIosX64")
                assertTasksSkipped(":shared:linkDebugFrameworkWatchosArm32")
                assertTasksSkipped(":shared:linkDebugFrameworkWatchosArm64")
                assertTasksSkipped(":shared:linkDebugFrameworkWatchosDeviceArm64")
                assertTasksSkipped(":shared:linkDebugFrameworkWatchosX64")
                assertTasksSkipped(":shared:assembleDebugWatchosFatFrameworkForSharedXCFramework")
                assertTasksSkipped(":shared:assembleSharedDebugXCFramework")
            }
    }

    companion object {
        @BeforeClass
        @JvmStatic
        fun assumeItsNonMac() {
            Assume.assumeFalse(HostManager.hostIsMac)
        }
    }
}