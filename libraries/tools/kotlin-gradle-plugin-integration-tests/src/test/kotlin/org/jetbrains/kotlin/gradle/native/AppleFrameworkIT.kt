/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.jetbrains.kotlin.gradle.BaseGradleIT
import org.jetbrains.kotlin.gradle.GradleVersionRequired
import org.jetbrains.kotlin.gradle.util.AGPVersion
import org.jetbrains.kotlin.gradle.util.modify
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
        androidGradlePluginVersion = AGPVersion.v4_1_0
    )

    @Test
    fun `assemble AppleFrameworkForXcode tasks for IosArm64`() {
        with(Project("sharedAppleFramework")) {
            val options: BuildOptions = defaultBuildOptions().copy(
                customEnvironmentVariables = mapOf(
                    "CONFIGURATION" to "debug",
                    "SDK_NAME" to "iphoneos123",
                    "ARCHS" to "arm64",
                    "TARGET_BUILD_DIR" to "no use",
                    "FRAMEWORKS_FOLDER_PATH" to "no use"
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
    fun `assemble fat AppleFrameworkForXcode tasks for Arm64 and X64 simulators`() {
        with(Project("sharedAppleFramework")) {
            val options: BuildOptions = defaultBuildOptions().copy(
                customEnvironmentVariables = mapOf(
                    "CONFIGURATION" to "Release",
                    "SDK_NAME" to "iphonesimulator",
                    "ARCHS" to "arm64 x86_64",
                    "TARGET_BUILD_DIR" to "no use",
                    "FRAMEWORKS_FOLDER_PATH" to "no use"
                )
            )
            build("assembleReleaseAppleFrameworkForXcode", options = options) {
                assertSuccessful()
                assertTasksExecuted(":shared:linkReleaseFrameworkIosSimulatorArm64")
                assertTasksExecuted(":shared:linkReleaseFrameworkIosX64")
                assertTasksExecuted(":shared:assembleReleaseAppleFrameworkForXcode")
                assertFileExists("/shared/build/xcode-frameworks/Release/iphonesimulator/sdk.framework")
                assertFileExists("/shared/build/xcode-frameworks/Release/iphonesimulator/sdk.framework.dSYM")
            }
        }
    }

    @Test
    fun `check that macOS framework has symlinks`() {
        with(Project("sharedAppleFramework")) {
            val options: BuildOptions = defaultBuildOptions().copy(
                customEnvironmentVariables = mapOf(
                    "CONFIGURATION" to "debug",
                    "SDK_NAME" to "macosx",
                    "ARCHS" to "x86_64",
                    "EXPANDED_CODE_SIGN_IDENTITY" to "-",
                    "TARGET_BUILD_DIR" to workingDir.absolutePath,
                    "FRAMEWORKS_FOLDER_PATH" to "${projectName}/build/xcode-derived"
                )
            )
            build(":shared:embedAndSignAppleFrameworkForXcode", options = options) {
                assertSuccessful()
                assertTasksExecuted(":shared:assembleDebugAppleFrameworkForXcodeMacosX64")
                // Verify symlinks in gradle build folder
                assertFileExists("/shared/build/xcode-frameworks/debug/macosx/sdk.framework/Headers")
                assertFileIsSymlink("/shared/build/xcode-frameworks/debug/macosx/sdk.framework/Headers")
                // Verify symlinks in xcode derived folder
                assertFileExists("/build/xcode-derived/sdk.framework/Headers")
                assertFileIsSymlink("/build/xcode-derived/sdk.framework/Headers")
            }
        }
    }

    @Test
    fun `check embedAndSignAppleFrameworkForXcode fail`() {
        with(Project("sharedAppleFramework")) {
            build(":shared:embedAndSignAppleFrameworkForXcode") {
                assertTasksFailed(":shared:embedAndSignAppleFrameworkForXcode")
                assertContains("Please run the embedAndSignAppleFrameworkForXcode task from Xcode")
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
                    "ARCHS" to "arm64",
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
                    ":shared:embedAndSignCustomAppleFrameworkForXcode",
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
    fun `check embedAndSignAppleFrameworkForXcode was registered without required Xcode environments`() {
        with(Project("sharedAppleFramework")) {
            val options: BuildOptions = defaultBuildOptions().copy(
                customEnvironmentVariables = mapOf(
                    "CONFIGURATION" to "Debug",
                    "SDK_NAME" to "iphoneos",
                    "ARCHS" to "arm64"
                )
            )
            build("tasks", options = options) {
                assertSuccessful()
                assertTasksRegistered(
                    ":shared:embedAndSignAppleFrameworkForXcode",
                    ":shared:embedAndSignCustomAppleFrameworkForXcode"
                )
                assertTasksNotRegistered(
                    ":shared:assembleReleaseAppleFrameworkForXcodeIosX64",
                    ":shared:assembleDebugAppleFrameworkForXcodeIosX64",
                    ":shared:assembleReleaseAppleFrameworkForXcodeIosArm64",
                    ":shared:assembleDebugAppleFrameworkForXcodeIosArm64",
                    ":shared:assembleCustomDebugAppleFrameworkForXcodeIosX64",
                    ":shared:assembleCustomReleaseAppleFrameworkForXcodeIosX64",
                    ":shared:assembleCustomDebugAppleFrameworkForXcodeIosArm64",
                    ":shared:assembleCustomReleaseAppleFrameworkForXcodeIosArm64"
                )
            }
            build(":shared:embedAndSignCustomAppleFrameworkForXcode", options = options) {
                assertTasksFailed(":shared:embedAndSignCustomAppleFrameworkForXcode")
                assertContains("Please run the embedAndSignCustomAppleFrameworkForXcode task from Xcode")
            }
        }
    }

    @Test
    fun `check that static framework for Arm64 is built but is not embedded`() {
        with(Project("sharedAppleFramework")) {
            val options: BuildOptions = defaultBuildOptions().copy(
                customEnvironmentVariables = mapOf(
                    "CONFIGURATION" to "debug",
                    "SDK_NAME" to "iphoneos123",
                    "ARCHS" to "arm64",
                    "TARGET_BUILD_DIR" to "no use",
                    "FRAMEWORKS_FOLDER_PATH" to "no use"
                )
            )
            setupWorkingDir()
            projectDir.resolve("shared/build.gradle.kts").modify {
                it.replace(
                    "baseName = \"sdk\"",
                    "baseName = \"sdk\"\nisStatic = true"
                )
            }

            build("embedAndSignAppleFrameworkForXcode", options = options) {
                assertSuccessful()
                assertTasksExecuted(":shared:assembleDebugAppleFrameworkForXcodeIosArm64")
                assertTasksNotExecuted(":shared:embedAndSignAppleFrameworkForXcode")
                assertFileExists("/shared/build/xcode-frameworks/debug/iphoneos123/sdk.framework")
                assertNoSuchFile("/shared/build/xcode-frameworks/debug/iphoneos123/sdk.framework.dSYM")
            }
        }
    }
}