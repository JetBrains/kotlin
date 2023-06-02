/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.tasks.FrameworkLayout
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import java.util.*
import kotlin.io.path.absolutePathString
import kotlin.io.path.appendText
import kotlin.test.assertTrue

@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@DisplayName("Tests for K/N builds with Fat Framework")
@NativeGradlePluginTests
class FatFrameworkIT : KGPBaseTest() {

    @DisplayName("Fat framework for iOS smoke test")
    @GradleTest
    fun smokeIos(gradleVersion: GradleVersion) {
        nativeProject("native-fat-framework/smoke", gradleVersion) {
            checkSmokeBuild(
                archs = listOf("x64", "arm64"),
                targetPrefix = "ios",
                expectedPlistPlatform = "iPhoneOS"
            )

            val binary = projectPath.resolve("build/fat-framework/smoke.framework/smoke").absolutePathString()
            assertProcessRunResult(runProcess(listOf("file", binary), projectPath.toFile())) {
                assertTrue(isSuccessful)
                assertTrue(output.contains("\\(for architecture x86_64\\):\\s+Mach-O 64-bit dynamically linked shared library x86_64".toRegex()))
                assertTrue(output.contains("\\(for architecture arm64\\):\\s+Mach-O 64-bit dynamically linked shared library arm64".toRegex()))
            }
        }
    }

    @DisplayName("Fat framework for watchOS smoke test")
    @GradleTest
    fun smokeWatchos(gradleVersion: GradleVersion) {
        nativeProject(
            "native-fat-framework/smoke",
            gradleVersion,
        ) {
            buildGradleKts.modify {
                it.checkedReplace("iosArm64()", "watchosArm64(); watchosDeviceArm64()")
                    .checkedReplace("iosX64()", "watchosX64()")
            }
            checkSmokeBuild(
                archs = listOf("x64", "arm64", "deviceArm64"),
                targetPrefix = "watchos",
                expectedPlistPlatform = "WatchOS"
            )
            val binary = projectPath.resolve("build/fat-framework/smoke.framework/smoke").absolutePathString()
            assertProcessRunResult(runProcess(listOf("file", binary), projectPath.toFile())) {
                assertTrue(isSuccessful)
                assertTrue(output.contains("\\(for architecture x86_64\\):\\s+Mach-O 64-bit dynamically linked shared library x86_64".toRegex()))
                assertTrue(output.contains("\\(for architecture arm64_32\\):\\s+Mach-O dynamically linked shared library arm64_32_v8".toRegex()))
                assertTrue(output.contains("\\(for architecture arm64\\):\\s+Mach-O 64-bit dynamically linked shared library arm64".toRegex()))
            }
        }
    }

    @DisplayName("Fat framework with MacOS smoke test")
    @GradleTest
    fun smokeMacos(gradleVersion: GradleVersion) {
        nativeProject(
            "native-fat-framework/smoke",
            gradleVersion
        ) {
            buildGradleKts.modify {
                it.checkedReplace("iosArm64()", "macosArm64()")
                    .checkedReplace("iosX64()", "macosX64()")
            }
            checkSmokeBuild(
                archs = listOf("x64", "arm64"),
                targetPrefix = "macos",
                expectedPlistPlatform = "MacOSX",
                true
            )
            val binary = projectPath.resolve("build/fat-framework/smoke.framework/Versions/A/smoke").absolutePathString()
            assertProcessRunResult(runProcess(listOf("file", binary), projectPath.toFile())) {
                assertTrue(isSuccessful)
                assertTrue(output.contains("\\(for architecture x86_64\\):\\s+Mach-O 64-bit dynamically linked shared library x86_64".toRegex()))
                assertTrue(output.contains("\\(for architecture arm64\\):\\s+Mach-O 64-bit dynamically linked shared library arm64".toRegex()))
            }
        }
    }

    private fun TestProject.checkSmokeBuild(
        archs: List<String>,
        targetPrefix: String,
        expectedPlistPlatform: String,
        isMacosFramework: Boolean = false
    ) {
        build("fat") {
            val linkTasks = archs.map {
                ":linkDebugFramework${targetPrefix.capitalize()}${it.capitalize()}"
            }

            assertTasksExecuted(linkTasks)
            assertTasksExecuted(":fat")

            val frameworkLayout = FrameworkLayout(projectPath.resolve("build/fat-framework/smoke.framework").toFile(), isMacosFramework)

            assertFileExists(frameworkLayout.binary.toPath())
            assertFileExists(frameworkLayout.header.toPath())
            assertFileExists(frameworkLayout.dSYM.binary.toPath())

            assertFileContains(
                frameworkLayout.header.toPath(),
                "+ (int32_t)foo __attribute__((swift_name(\"foo()\")));"
            )

            assertFileContains(
                frameworkLayout.infoPlist.toPath(),
                """
                |    <key>CFBundleSupportedPlatforms</key>
                |    <array>
                |        <string>$expectedPlistPlatform</string>
                |    </array>
                """.trimMargin()
                    .replace(Regex(" {4}"), "\t")
            )
        }
    }

    @DisplayName("Fat framework with duplicated iOS Arm 64")
    @GradleTest
    fun testDuplicatedArchitecture(gradleVersion: GradleVersion) {
        nativeProject("native-fat-framework/smoke", gradleVersion) {
            buildGradleKts.modify {
                it +
                        //language=kotlin
                        """
                        val anotherDeviceTarget = kotlin.iosArm64("another") {
                            binaries.framework("DEBUG")
                        }
                        fat.from(anotherDeviceTarget.binaries.getFramework("DEBUG"))
                        """.trimIndent()
            }
            buildAndFail("fat") {
                assertOutputContains("This fat framework already has a binary for architecture `arm64`")
            }
        }
    }

    @DisplayName("Fat framework with incorrect 'osx' family")
    @GradleTest
    fun testIncorrectFamily(gradleVersion: GradleVersion) {
        nativeProject("native-fat-framework/smoke", gradleVersion) {
            buildGradleKts.modify {
                it +
                        //language=kotlin
                        """
                        val macos = kotlin.macosX64 {
                            binaries.framework("DEBUG")
                        }
                        fat.from(macos.binaries.getFramework("DEBUG"))
                        """.trimIndent()
            }
            buildAndFail("fat") {
                assertOutputContains("Cannot add a binary with platform family 'osx' to the fat framework")
            }
        }
    }

    @DisplayName("Fat framework with custom base name")
    @GradleTest
    fun testCustomName(gradleVersion: GradleVersion) {
        nativeProject("native-fat-framework/smoke", gradleVersion) {
            buildGradleKts.modify {
                it.addBeforeSubstring("baseName = \"custom\"\n", "from(frameworksToMerge)")
            }

            build("fat") {
                val binary = projectPath.resolve("build/fat-framework/custom.framework/custom").absolutePathString()
                assertProcessRunResult(runProcess(listOf("otool", "-D", binary), projectPath.toFile())) {
                    assertTrue(isSuccessful)
                    assertTrue { output.lines().any { it.contains("@rpath/custom.framework/custom") } }
                }
            }
        }
    }

    @DisplayName("Fat framework with different input types")
    @GradleTest
    fun testDifferentTypes(gradleVersion: GradleVersion) {
        nativeProject("native-fat-framework/smoke", gradleVersion) {
            buildGradleKts.modify {
                it.checkedReplace("iosArm64()", "iosArm64{binaries.framework {isStatic = false}}")
                    .checkedReplace("iosX64()", "iosX64{binaries.framework {isStatic = true}}")
                    .addBeforeSubstring("//", "binaries.framework(listOf(DEBUG))")
            }
            buildAndFail("fat") {
                assertOutputContains("All input frameworks must be either static or dynamic")
            }
        }
    }

    @DisplayName("Fat framework with all static inputs")
    @GradleTest
    fun testAllStatic(gradleVersion: GradleVersion) {
        nativeProject("native-fat-framework/smoke", gradleVersion) {
            buildGradleKts.modify {
                it.checkedReplace("iosArm64()", "iosArm64{binaries.framework {isStatic = true}}")
                    .checkedReplace("iosX64()", "iosX64{binaries.framework {isStatic = true}}")
                    .addBeforeSubstring("//", "binaries.framework(listOf(DEBUG))")
            }
            build("fat")
        }
    }

    @DisplayName("Test that the configurations exposing the frameworks don't interfere with variant-aware dependency resolution")
    @GradleTest
    fun testDependencyResolution(gradleVersion: GradleVersion) {
        nativeProject("native-fat-framework/smoke", gradleVersion) {
            val nestedProjectName = "nested"
            includeOtherProjectAsSubmodule("smoke", "native-fat-framework", nestedProjectName, true)
            buildGradleKts.appendText("dependencies { \"commonMainImplementation\"(project(\":$nestedProjectName\")) }")
            testResolveAllConfigurations()
        }
    }

}