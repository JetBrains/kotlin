/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.jetbrains.kotlin.gradle.BaseGradleIT
import org.jetbrains.kotlin.gradle.GradleVersionRequired
import org.jetbrains.kotlin.gradle.transformProjectWithPluginsDsl
import org.jetbrains.kotlin.gradle.util.checkedReplace
import org.jetbrains.kotlin.gradle.util.modify
import org.jetbrains.kotlin.gradle.util.runProcess
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.Assume
import org.junit.Test
import kotlin.test.assertTrue

class FatFrameworkIT : BaseGradleIT() {

    override val defaultGradleVersion: GradleVersionRequired
        get() = GradleVersionRequired.FOR_MPP_SUPPORT

    @Test
    fun smokeIos() {
        Assume.assumeTrue(HostManager.hostIsMac)
        transformProjectWithPluginsDsl(
            "smoke",
            directoryPrefix = "new-mpp-fat-framework"
        ).build("fat") {
            checkSmokeBuild(
                archs = listOf("x64", "arm64", "arm32"),
                targetPrefix = "ios",
                expectedPlistPlatform = "iPhoneOS"
            )

            val binary = fileInWorkingDir("build/fat-framework/smoke.framework/smoke")
            with(runProcess(listOf("file", binary.absolutePath), project.projectDir)) {
                assertTrue(isSuccessful)
                assertTrue(output.contains("\\(for architecture x86_64\\):\\s+Mach-O 64-bit dynamically linked shared library x86_64".toRegex()))
                assertTrue(output.contains("\\(for architecture armv7\\):\\s+Mach-O dynamically linked shared library arm_v7".toRegex()))
                assertTrue(output.contains("\\(for architecture arm64\\):\\s+Mach-O 64-bit dynamically linked shared library arm64".toRegex()))
            }
        }
    }

    @Test
    fun smokeWatchos() {
        Assume.assumeTrue(HostManager.hostIsMac)
        with(transformProjectWithPluginsDsl("smoke", directoryPrefix = "new-mpp-fat-framework")) {

            gradleBuildScript().modify {
                it.checkedReplace("iosArm32()", "watchosArm32()")
                    .checkedReplace("iosArm64()", "watchosArm64()")
                    .checkedReplace("iosX64()", "watchosX86()")
            }

            build("fat") {
                checkSmokeBuild(
                    archs = listOf("x86", "arm64", "arm32"),
                    targetPrefix = "watchos",
                    expectedPlistPlatform = "WatchOS"
                )

                val binary = fileInWorkingDir("build/fat-framework/smoke.framework/smoke")
                with(runProcess(listOf("file", binary.absolutePath), projectDir)) {
                    assertTrue(isSuccessful)
                    assertTrue(output.contains("\\(for architecture i386\\):\\s+Mach-O dynamically linked shared library i386".toRegex()))
                    assertTrue(output.contains("\\(for architecture armv7k\\):\\s+Mach-O dynamically linked shared library arm_v7k".toRegex()))
                    assertTrue(output.contains("\\(for architecture arm64_32\\):\\s+Mach-O dynamically linked shared library arm64_32_v8".toRegex()))
                }
            }
        }
    }

    private fun CompiledProject.checkSmokeBuild(
        archs: List<String>,
        targetPrefix: String,
        expectedPlistPlatform: String
    ) {
        assertSuccessful()
        val linkTasks = archs.map { ":linkDebugFramework${targetPrefix.capitalize()}${it.capitalize()}" }

        assertTasksExecuted(linkTasks)
        assertTasksExecuted(":fat")

        assertFileExists("build/fat-framework/smoke.framework/smoke")
        assertFileExists("build/fat-framework/smoke.framework/Headers/smoke.h")
        assertFileExists("build/fat-framework/smoke.framework.dSYM/Contents/Resources/DWARF/smoke")

        val headerContent = fileInWorkingDir("build/fat-framework/smoke.framework/Headers/smoke.h").readText()
        assertTrue(
            headerContent.contains("+ (int32_t)foo __attribute__((swift_name(\"foo()\")));"),
            "Unexpected header content:\n$headerContent"
        )

        val plistContent = fileInWorkingDir("build/fat-framework/smoke.framework/Info.plist")
            .readLines()
            .joinToString(separator = "\n") { it.trim() }

        assertTrue(
            plistContent.contains(
                """
                        <key>CFBundleSupportedPlatforms</key>
                        <array>
                        <string>$expectedPlistPlatform</string>
                        </array>
                    """.trimIndent()
            ),
            "Unexpected Info.plist content:\n$plistContent"
        )
    }

    @Test
    fun testDuplicatedArchitecture() {
        Assume.assumeTrue(HostManager.hostIsMac)
        with(transformProjectWithPluginsDsl("smoke", directoryPrefix = "new-mpp-fat-framework")) {
            gradleBuildScript().modify {
                it + """
                val anotherDeviceTarget = kotlin.iosArm64("another") {
                    binaries.framework("DEBUG")
                }
                fat.from(anotherDeviceTarget.binaries.getFramework("DEBUG"))
            """.trimIndent()
            }
            build("fat") {
                assertFailed()
                assertContains("This fat framework already has a binary for architecture `arm64`")
            }
        }
    }

    @Test
    fun testIncorrectFamily() {
        Assume.assumeTrue(HostManager.hostIsMac)
        with(transformProjectWithPluginsDsl("smoke", directoryPrefix = "new-mpp-fat-framework")) {
            gradleBuildScript().modify {
                it + """
                val macos = kotlin.macosX64 {
                    binaries.framework("DEBUG")
                }
                fat.from(macos.binaries.getFramework("DEBUG"))
            """.trimIndent()
            }
            build("fat") {
                assertFailed()
                assertContains("Cannot add a binary with platform family 'osx' to the fat framework")
            }
        }
    }
}

