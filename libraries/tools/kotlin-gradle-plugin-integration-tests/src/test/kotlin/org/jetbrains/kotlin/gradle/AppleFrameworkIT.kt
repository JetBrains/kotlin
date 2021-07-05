/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.AGPVersion
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.junit.Assume
import org.junit.BeforeClass
import kotlin.test.Test

class AppleFrameworkIT : BaseGradleIT() {
    companion object {
        @BeforeClass
        @JvmStatic
        fun assumeItsMac() {
            Assume.assumeTrue(HostManager.hostIsMac)
        }
    }

    override val defaultGradleVersion = GradleVersionRequired.FOR_MPP_SUPPORT
    override fun defaultBuildOptions() = super.defaultBuildOptions().copy(
        androidHome = KtTestUtil.findAndroidSdk(),
        androidGradlePluginVersion = AGPVersion.v3_6_0
    )

    @Test
    fun `assemble AppleFrameworkForXcode tasks for IosArm64`() {
        with(Project("sharedAppleFramework")) {
            val options: BuildOptions = defaultBuildOptions().copy(
                customEnvironmentVariables = mapOf(
                    "CONFIGURATION" to "debug",
                    "SDK_NAME" to "iphoneos123",
                    "NATIVE_ARCH" to "armv7"
                )
            )
            build("assembleDebugAppleFrameworkForXcodeIosArm64", options = options) {
                assertSuccessful()
                assertTasksExecuted(":shared:assembleDebugAppleFrameworkForXcodeIosArm64")
                assertFileExists("/shared/build/xcode-frameworks/debug/iphoneos123/sdk.framework")
                assertFileExists("/shared/build/xcode-frameworks/debug/iphoneos123/sdk.framework.dSYM")
            }

            build("assembleCustomDebugAppleFrameworkForXcodeIosArm64", options = options) {
                assertSuccessful()
                assertTasksExecuted(":shared:assembleCustomDebugAppleFrameworkForXcodeIosArm64")
                assertFileExists("/shared/build/xcode-frameworks/debug/iphoneos123/lib.framework")
                assertFileExists("/shared/build/xcode-frameworks/debug/iphoneos123/lib.framework.dSYM")
            }
        }
    }

    @Test
    fun `check there aren't Xcode tasks without Xcode environment`() {
        with(Project("sharedAppleFramework")) {
            build("tasks") {
                assertSuccessful()
                assertTasksNotRegistered(
                    ":shared:assembleDebugAppleFrameworkForXcodeIosArm64",
                    ":shared:embedAndSignAppleFrameworkForXcode"
                )
            }
        }
    }

    @Test
    fun `check all registered tasks with Xcode environment for Debug IosArm64 configuration`() {
        with(Project("sharedAppleFramework")) {
            val options: BuildOptions = defaultBuildOptions().copy(
                customEnvironmentVariables = mapOf(
                    "CONFIGURATION" to "Debug",
                    "SDK_NAME" to "iphoneos",
                    "NATIVE_ARCH" to "armv7",
                    "EXPANDED_CODE_SIGN_IDENTITY" to "-",
                    "TARGET_BUILD_DIR" to "testBuildDir",
                    "FRAMEWORKS_FOLDER_PATH" to "testFrameworksDir"
                )
            )
            build("tasks", options = options) {
                assertSuccessful()
                assertTasksRegistered(
                    ":shared:assembleDebugAppleFrameworkForXcodeIosArm64",
                    ":shared:embedAndSignAppleFrameworkForXcode",
                    ":shared:assembleCustomDebugAppleFrameworkForXcodeIosArm64",
                    ":shared:embedAndSignCustomAppleFrameworkForXcode"
                )
                assertTasksNotRegistered(
                    ":shared:assembleDebugAppleFrameworkForXcodeIosX64",
                    ":shared:assembleReleaseAppleFrameworkForXcodeIosX64",
                    ":shared:assembleReleaseAppleFrameworkForXcodeIosArm64",
                    ":shared:assembleCustomDebugAppleFrameworkForXcodeIosX64",
                    ":shared:assembleCustomReleaseAppleFrameworkForXcodeIosX64",
                    ":shared:assembleCustomReleaseAppleFrameworkForXcodeIosArm64"
                )
            }
        }
    }

    @Test
    fun `check there isn't embedAndSignAppleFrameworkForXcode task without required Xcode environments`() {
        with(Project("sharedAppleFramework")) {
            val options: BuildOptions = defaultBuildOptions().copy(
                customEnvironmentVariables = mapOf(
                    "CONFIGURATION" to "Debug",
                    "SDK_NAME" to "iphoneos",
                    "NATIVE_ARCH" to "armv7"
                )
            )
            build("tasks", options = options) {
                assertSuccessful()
                assertTasksRegistered(
                    ":shared:assembleDebugAppleFrameworkForXcodeIosArm64",
                    ":shared:assembleCustomDebugAppleFrameworkForXcodeIosArm64"
                )
                assertTasksNotRegistered(
                    ":shared:embedAndSignAppleFrameworkForXcode",
                    ":shared:assembleDebugAppleFrameworkForXcodeIosX64",
                    ":shared:embedAndSignCustomAppleFrameworkForXcode",
                    ":shared:assembleCustomDebugAppleFrameworkForXcodeIosX64"
                )
            }
        }
    }
}