/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.jetbrains.kotlin.gradle.*
import org.jetbrains.kotlin.gradle.embedProject
import org.jetbrains.kotlin.gradle.transformProjectWithPluginsDsl
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.Assume
import org.junit.BeforeClass
import org.junit.Test
import kotlin.test.assertTrue

class FatFrameworkIT : BaseGradleIT() {

    override val defaultGradleVersion: GradleVersionRequired
        get() = GradleVersionRequired.FOR_MPP_SUPPORT

    @Test
    fun smokeIos() {
        transformProjectWithPluginsDsl(
            "smoke",
            directoryPrefix = "native-fat-framework"
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
        with(transformProjectWithPluginsDsl("smoke", directoryPrefix = "native-fat-framework")) {

            gradleBuildScript().modify {
                it.checkedReplace("iosArm32()", "watchosArm32()")
                    .checkedReplace("iosArm64()", "watchosArm64()")
                    .checkedReplace("iosX64()", "watchosX64()")
            }

            build("fat") {
                checkSmokeBuild(
                    archs = listOf("x64", "arm64", "arm32"),
                    targetPrefix = "watchos",
                    expectedPlistPlatform = "WatchOS"
                )

                val binary = fileInWorkingDir("build/fat-framework/smoke.framework/smoke")
                with(runProcess(listOf("file", binary.absolutePath), projectDir)) {
                    assertTrue(isSuccessful)
                    assertTrue(output.contains("\\(for architecture x86_64\\):\\s+Mach-O 64-bit dynamically linked shared library x86_64".toRegex()))
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
        with(transformProjectWithPluginsDsl("smoke", directoryPrefix = "native-fat-framework")) {
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
        with(transformProjectWithPluginsDsl("smoke", directoryPrefix = "native-fat-framework")) {
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

    @Test
    fun testCustomName() {
        with(transformProjectWithPluginsDsl("smoke", directoryPrefix = "native-fat-framework")) {
            gradleBuildScript().modify {
                it.addBeforeSubstring("baseName = \"custom\"\n","from(frameworksToMerge)")
            }

            build("fat") {
                val binary = fileInWorkingDir("build/fat-framework/custom.framework/custom")
                with(runProcess(listOf("otool", "-D", binary.absolutePath), project.projectDir)) {
                    assertSuccessful()
                    assertTrue { output.lines().any { it.contains("@rpath/custom.framework/custom") } }
                }
            }
        }
    }

    @Test
    fun testDifferentTypes() {
        with(transformProjectWithPluginsDsl("smoke", directoryPrefix = "native-fat-framework")) {
            gradleBuildScript().modify {
                it.checkedReplace("iosArm32()", "iosArm32{binaries.framework {isStatic = true}}")
                    .checkedReplace("iosArm64()", "iosArm64{binaries.framework {isStatic = false}}")
                    .checkedReplace("iosX64()", "iosX64{binaries.framework {isStatic = false}}")
                    .addBeforeSubstring("//", "binaries.framework(listOf(DEBUG))")
            }
            build("fat") {
                assertFailed()
                assertContains("All input frameworks must be either static or dynamic")
            }
        }
    }

    @Test
    fun testAllStatic() {
        with(transformProjectWithPluginsDsl("smoke", directoryPrefix = "native-fat-framework")) {
            gradleBuildScript().modify {
                it.checkedReplace("iosArm32()", "iosArm32{binaries.framework {isStatic = true}}")
                    .checkedReplace("iosArm64()", "iosArm64{binaries.framework {isStatic = true}}")
                    .checkedReplace("iosX64()", "iosX64{binaries.framework {isStatic = true}}")
                    .addBeforeSubstring("//", "binaries.framework(listOf(DEBUG))")
            }
            build("fat") {
                assertSuccessful()
            }
        }
    }

    /**
     * Test that the configurations exposing the frameworks don't interfere with variant-aware dependency resolution
     */
    @Test
    fun testDependencyResolution() = with(transformProjectWithPluginsDsl("smoke", directoryPrefix = "native-fat-framework")) {
        setupWorkingDir()
        val nestedProjectName = "nested"
        embedProject(this, nestedProjectName)
        gradleBuildScript(nestedProjectName).modify { it.replace(".version(\"$KOTLIN_VERSION\")", "") }
        gradleBuildScript().appendText("dependencies { \"commonMainImplementation\"(project(\":$nestedProjectName\")) }")
        testResolveAllConfigurations()
    }

    companion object {
        @BeforeClass
        @JvmStatic
        fun assumeItsMac() {
            Assume.assumeTrue(HostManager.hostIsMac)
        }
    }
}

