/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import kotlin.io.path.appendText

@DisplayName("Tests for K/N library dsl builds")
@NativeGradlePluginTests
class NativeLibraryDslIT : KGPBaseTest() {

    @DisplayName("K/N project with custom registered gradle tasks")
    @GradleTest
    fun shouldSharedAndLibRegisteredTasks(gradleVersion: GradleVersion) {
        nativeProject("new-kn-library-dsl", gradleVersion) {
            buildAndAssertAllTasks(
                listOf(
                    "shared:assembleMyfatframeFatFramework",
                    "shared:assembleMyframeFrameworkIosArm64",
                    "shared:assembleMylibSharedLibraryLinuxX64",
                    "shared:assembleMyslibSharedLibraryLinuxX64",
                    "shared:assembleSharedXCFramework",
                    "lib:assembleGroofatframeFatFramework",
                    "lib:assembleGrooframeFrameworkIosArm64",
                    "lib:assembleGroolibSharedLibraryIosX64",
                    "lib:assembleLibXCFramework"
                ),
                listOf("shared:assembleMyslibReleaseSharedLibraryLinuxX64")
            )
        }
    }

    @DisplayName("Link shared libraries from two gradle modules")
    @GradleTest
    fun shouldLinkSharedLibrariesFromTwoModules(gradleVersion: GradleVersion) {
        nativeProject("new-kn-library-dsl", gradleVersion) {
            build(":shared:assembleMyslibDebugSharedLibraryLinuxX64") {
                assertTasksExecuted(
                    ":lib:compileKotlinLinuxX64",
                    ":shared:compileKotlinLinuxX64",
                    ":shared:assembleMyslibDebugSharedLibraryLinuxX64"
                )
                assertFileInProjectExists("shared/build/out/dynamic/linux_x64/debug/libmyslib.so")
                assertFileInProjectExists("shared/build/out/dynamic/linux_x64/debug/libmyslib_api.h")
            }
        }
    }

    @DisplayName("Link shared library from single gradle module")
    @GradleTest
    fun shouldLinkSharedLibrariesFromSingleModule(gradleVersion: GradleVersion) {
        nativeProject("new-kn-library-dsl", gradleVersion) {
            build(":shared:assembleMylibDebugSharedLibraryLinuxX64") {
                assertTasksExecuted(
                    ":shared:compileKotlinLinuxX64",
                    ":shared:assembleMylibDebugSharedLibraryLinuxX64"
                )
                assertTasksNotExecuted(
                    ":lib:compileKotlinLinuxX64"
                )
                extractNativeTasksCommandLineArgumentsFromOutput(":shared:assembleMylibDebugSharedLibraryLinuxX64") {
                    assertCommandLineArgumentsDoNotContain("-Xfoo=bar", "-Xbaz=qux")
                    assertCommandLineArgumentsContain("-Xmen=pool")
                }
                assertFileInProjectExists("shared/build/out/dynamic/linux_x64/debug/libmylib.so")
                assertFileInProjectExists("shared/build/out/dynamic/linux_x64/debug/libmylib_api.h")
            }
        }
    }

    @DisplayName("Links shared library from single gradle module with additional link args")
    @GradleTest
    fun shouldLinkSharedLibrariesFromSingleModuleWithAdditionalLinkArgs(gradleVersion: GradleVersion) {
        nativeProject("new-kn-library-dsl", gradleVersion) {
            gradleProperties.appendText("\nkotlin.native.linkArgs=-Xfoo=bar -Xbaz=qux")
            build(":shared:assembleMylibDebugSharedLibraryLinuxX64") {
                assertTasksExecuted(
                    ":shared:compileKotlinLinuxX64",
                    ":shared:assembleMylibDebugSharedLibraryLinuxX64"
                )
                assertTasksNotExecuted(
                    ":lib:compileKotlinLinuxX64"
                )
                extractNativeTasksCommandLineArgumentsFromOutput(":shared:assembleMylibDebugSharedLibraryLinuxX64") {
                    assertCommandLineArgumentsContain("-Xfoo=bar", "-Xbaz=qux", "-Xmen=pool")
                }
                assertFileInProjectExists("shared/build/out/dynamic/linux_x64/debug/libmylib.so")
                assertFileInProjectExists("shared/build/out/dynamic/linux_x64/debug/libmylib_api.h")
            }
        }
    }

    @OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
    @DisplayName("Links release XCFramework from two gradle modules")
    @GradleTest
    fun shouldLinkXCFrameworkFromTwoModules(gradleVersion: GradleVersion) {
        nativeProject("new-kn-library-dsl", gradleVersion) {
            build(":shared:assembleSharedReleaseXCFramework") {
                assertTasksExecuted(
                    ":shared:compileKotlinIosX64",
                    ":shared:compileKotlinIosArm64",
                    ":shared:compileKotlinIosSimulatorArm64",
                    ":lib:compileKotlinIosX64",
                    ":lib:compileKotlinIosArm64",
                    ":lib:compileKotlinIosSimulatorArm64",
                    ":shared:assembleSharedReleaseXCFramework"
                )
                assertTasksNotExecuted(
                    ":shared:assembleSharedDebugXCFramework"
                )
                assertDirectoryInProjectExists("shared/build/out/xcframework/release/shared.xcframework")
            }
        }
    }

}