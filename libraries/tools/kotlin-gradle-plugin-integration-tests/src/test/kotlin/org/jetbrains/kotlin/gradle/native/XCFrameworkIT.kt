/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.jetbrains.kotlin.gradle.BaseGradleIT
import org.jetbrains.kotlin.gradle.GradleVersionRequired
import org.jetbrains.kotlin.gradle.transformProjectWithPluginsDsl
import org.jetbrains.kotlin.gradle.util.AGPVersion
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.junit.Assume
import org.junit.BeforeClass
import kotlin.test.Test

class XCFrameworkIT : BaseGradleIT() {
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
    fun `assemble XCFramework for all available ios and watchos targets`() {
        with(Project("appleXCFramework")) {
            build("assembleSharedDebugXCFramework") {
                assertSuccessful()
                assertTasksExecuted(":shared:linkDebugFrameworkIosArm64")
                assertTasksExecuted(":shared:linkDebugFrameworkIosX64")
                assertTasksExecuted(":shared:linkDebugFrameworkWatchosArm32")
                assertTasksExecuted(":shared:linkDebugFrameworkWatchosArm64")
                assertTasksExecuted(":shared:linkDebugFrameworkWatchosX64")
                assertTasksExecuted(":shared:assembleDebugWatchosFatFrameworkForSharedXCFramework")
                assertTasksExecuted(":shared:assembleSharedDebugXCFramework")
                assertFileExists("/shared/build/XCFrameworks/debug/shared.xcframework")
                assertFileExists("/shared/build/XCFrameworks/debug/shared.xcframework/ios-arm64/dSYMs/shared.framework.dSYM")
                assertFileExists("/shared/build/sharedXCFrameworkTemp/fatframework/debug/watchos/shared.framework")
                assertFileExists("/shared/build/sharedXCFrameworkTemp/fatframework/debug/watchos/shared.framework.dSYM")
            }

            build("assembleSharedDebugXCFramework") {
                assertSuccessful()
                assertTasksUpToDate(":shared:linkDebugFrameworkIosArm64")
                assertTasksUpToDate(":shared:linkDebugFrameworkIosX64")
                assertTasksUpToDate(":shared:linkDebugFrameworkWatchosArm32")
                assertTasksUpToDate(":shared:linkDebugFrameworkWatchosArm64")
                assertTasksUpToDate(":shared:linkDebugFrameworkWatchosX64")
                assertTasksUpToDate(":shared:assembleDebugWatchosFatFrameworkForSharedXCFramework")
                assertTasksUpToDate(":shared:assembleSharedDebugXCFramework")
            }
        }
    }

    @Test
    fun `assemble other XCFramework for ios targets`() {
        with(Project("appleXCFramework")) {
            build("assembleOtherXCFramework") {
                assertSuccessful()
                assertTasksExecuted(":shared:linkReleaseFrameworkIosArm64")
                assertTasksExecuted(":shared:linkReleaseFrameworkIosX64")
                assertTasksExecuted(":shared:assembleOtherReleaseXCFramework")
                assertFileExists("/shared/build/XCFrameworks/release/other.xcframework")
                assertFileExists("/shared/build/XCFrameworks/release/other.xcframework/ios-arm64/dSYMs/shared.framework.dSYM")
                assertTasksExecuted(":shared:linkDebugFrameworkIosArm64")
                assertTasksExecuted(":shared:linkDebugFrameworkIosX64")
                assertTasksExecuted(":shared:assembleOtherDebugXCFramework")
                assertFileExists("/shared/build/XCFrameworks/debug/other.xcframework")
                assertFileExists("/shared/build/XCFrameworks/debug/other.xcframework/ios-arm64/dSYMs/shared.framework.dSYM")
                assertContains("Name of XCFramework 'other' differs from inner frameworks name 'shared'! Framework renaming is not supported yet")
            }
        }
    }

    @Test
    fun `check there aren't XCFramework tasks without declaration in build script`() {
        with(Project("sharedAppleFramework")) {
            build("tasks") {
                assertSuccessful()
                assertTasksNotRegistered(
                    ":shared:assembleSharedDebugXCFramework",
                    ":shared:assembleSharedReleaseXCFramework",
                    ":shared:assembleXCFramework"
                )
            }
        }
    }

    @Test
    fun `check configuration error if two XCFrameworks were registered with same name`() {
        with(transformProjectWithPluginsDsl("appleXCFramework")) {
            with(gradleBuildScript("shared")) {
                var text = readText()
                text = text.replace("XCFramework(\"other\")", "XCFramework()")
                writeText(text)
            }

            build("tasks") {
                assertFailed()
                assertContains("Cannot add task 'assembleSharedReleaseXCFramework' as a task with that name already exists.")
            }
        }
    }

    @Test
    fun `check configuration error if XCFramework contains frameworks with different names`() {
        with(transformProjectWithPluginsDsl("appleXCFramework")) {
            with(gradleBuildScript("shared")) {
                var text = readText()
                text = text.replaceFirst("baseName = \"shared\"", "baseName = \"awesome\"")
                writeText(text)
            }

            build("tasks") {
                assertFailed()
                assertContains("All inner frameworks in XCFramework 'shared' should have same names. But there are two with 'awesome' and 'shared' names")
            }
        }
    }
}