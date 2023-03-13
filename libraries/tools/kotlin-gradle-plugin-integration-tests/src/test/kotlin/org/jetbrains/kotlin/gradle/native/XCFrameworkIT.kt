/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.*
import org.jetbrains.kotlin.gradle.testbase.TestVersions
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
        androidGradlePluginVersion = AGPVersion.v7_4_0,
        javaHome = KtTestUtil.getJdk17Home()
    )

    @Test
    fun `assemble XCFramework for all available ios and watchos targets`() {
        with(Project("appleXCFramework")) {
            build("assembleSharedDebugXCFramework") {
                assertSuccessful()
                assertTasksExecuted(":shared:linkDebugFrameworkIosX64")
                assertTasksExecuted(":shared:linkDebugFrameworkIosArm64")
                assertTasksExecuted(":shared:linkDebugFrameworkIosArm32")
                assertTasksExecuted(":shared:linkDebugFrameworkIosSimulatorArm64")
                assertTasksExecuted(":shared:assembleDebugIosFatFrameworkForSharedXCFramework")
                assertTasksExecuted(":shared:assembleDebugIosSimulatorFatFrameworkForSharedXCFramework")
                assertTasksExecuted(":shared:linkDebugFrameworkWatchosArm32")
                assertTasksExecuted(":shared:linkDebugFrameworkWatchosArm64")
                assertTasksExecuted(":shared:linkDebugFrameworkWatchosDeviceArm64")
                assertTasksExecuted(":shared:linkDebugFrameworkWatchosSimulatorArm64")
                assertTasksExecuted(":shared:linkDebugFrameworkWatchosX86")
                assertTasksExecuted(":shared:linkDebugFrameworkWatchosX64")
                assertTasksExecuted(":shared:assembleDebugWatchosFatFrameworkForSharedXCFramework")
                assertTasksExecuted(":shared:assembleDebugWatchosSimulatorFatFrameworkForSharedXCFramework")
                assertTasksExecuted(":shared:assembleSharedDebugXCFramework")
                assertFileExists("/shared/build/XCFrameworks/debug/shared.xcframework")
                assertFileExists("/shared/build/XCFrameworks/debug/shared.xcframework/ios-arm64_x86_64-simulator/dSYMs/shared.framework.dSYM")
                assertFileExists("/shared/build/sharedXCFrameworkTemp/fatframework/debug/watchos/shared.framework")
                assertFileExists("/shared/build/sharedXCFrameworkTemp/fatframework/debug/watchos/shared.framework.dSYM")
            }

            build("assembleSharedDebugXCFramework") {
                assertSuccessful()
                assertTasksUpToDate(":shared:linkDebugFrameworkIosArm64")
                assertTasksUpToDate(":shared:linkDebugFrameworkIosX64")
                assertTasksUpToDate(":shared:linkDebugFrameworkWatchosArm32")
                assertTasksUpToDate(":shared:linkDebugFrameworkWatchosArm64")
                assertTasksUpToDate(":shared:linkDebugFrameworkWatchosDeviceArm64")
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
            val currentGradleVersion = chooseWrapperVersionOrFinishTest()
            val options = defaultBuildOptions().suppressDeprecationWarningsOn(
                "AGP uses deprecated IncrementalTaskInputs (Gradle 7.5)"
            ) { options ->
                GradleVersion.version(currentGradleVersion) >= GradleVersion.version(TestVersions.Gradle.G_7_5) && options.safeAndroidGradlePluginVersion < AGPVersion.v7_3_0
            }
            build("tasks", options = options) {
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

    @Test
    fun `assemble framework does nothing when no sources rather than fail`() {
        with(transformProjectWithPluginsDsl("appleXCFramework")) {
            projectDir.resolve("shared/src").deleteRecursively()
            build(":shared:assembleXCFramework") {
                assertSuccessful()
                assertTasksSkipped(
                    ":shared:assembleSharedDebugXCFramework",
                    ":shared:assembleSharedReleaseXCFramework",
                )
            }
        }
    }

    @Test
    fun `check assemble framework handles dashes in its name correctly`() {
        with(transformProjectWithPluginsDsl("appleXCFramework")) {
            with(gradleBuildScript("shared")) {
                var text = readText()
                text = text.replace("baseName = \"shared\"", "baseName = \"sha-red\"")
                text = text.replace("XCFramework()", "XCFramework(\"sha-red\")")
                writeText(text)
            }

            build(":shared:assembleSha-redXCFramework") {
                assertSuccessful()
                assertTasksExecuted(
                    ":shared:assembleSha-redDebugXCFramework",
                    ":shared:assembleSha-redReleaseXCFramework",
                )
                assertNotContains("differs from inner frameworks name")
            }
        }
    }
}