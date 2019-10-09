/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.modify
import org.jetbrains.kotlin.gradle.util.runProcess
import org.junit.Test
import kotlin.test.assertTrue

class FatFrameworkIT : BaseGradleIT() {

    @Test
    fun smoke() = with(transformProjectWithPluginsDsl("smoke", directoryPrefix = "new-mpp-fat-framework")) {
        build("fat") {
            assertSuccessful()
            val archs = listOf("x64", "arm64", "arm32")
            val linkTasks = archs.map { ":linkDebugFrameworkIos${it.capitalize()}" }

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
                        <string>iPhoneOS</string>
                        </array>
                    """.trimIndent()
                ),
                "Unexpected Info.plist content:\n$plistContent"
            )

            val binary = fileInWorkingDir("build/fat-framework/smoke.framework/smoke")
            with(runProcess(listOf("file", binary.absolutePath), projectDir)) {
                assertTrue(isSuccessful)
                assertTrue(output.contains("\\(for architecture x86_64\\):\\s+Mach-O 64-bit dynamically linked shared library x86_64".toRegex()))
                assertTrue(output.contains("\\(for architecture armv7\\):\\s+Mach-O dynamically linked shared library arm_v7".toRegex()))
                assertTrue(output.contains("\\(for architecture arm64\\):\\s+Mach-O 64-bit dynamically linked shared library arm64".toRegex()))
            }
        }
    }

    @Test
    fun testDuplicatedArchitecture()= with(
        transformProjectWithPluginsDsl("smoke", directoryPrefix = "new-mpp-fat-framework")
    ) {
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

    @Test
    fun testIncorrectTarget() = with(
        transformProjectWithPluginsDsl("smoke", directoryPrefix = "new-mpp-fat-framework")
    ) {
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
            assertContains("Cannot add a framework with target 'macos_x64' to the fat framework")
        }
    }

}

