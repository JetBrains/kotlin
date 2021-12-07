/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.jetbrains.kotlin.gradle.BaseGradleIT
import org.jetbrains.kotlin.gradle.GradleVersionRequired
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.Assume
import kotlin.test.Test

class NativeLibraryDslIT : BaseGradleIT() {
    override val defaultGradleVersion = GradleVersionRequired.FOR_MPP_SUPPORT

    @Test
    fun `check registered gradle tasks`() {
        with(Project("new-kn-library-dsl")) {
            build(":shared:tasks") {
                assertSuccessful()
                assertTasksRegistered(
                    ":shared:assembleMyfatframeFatFramework",
                    ":shared:assembleMyframeFramework",
                    ":shared:assembleMylibSharedLibrary",
                    ":shared:assembleMyslibSharedLibrary",
                    ":shared:assembleSharedXCFramework"
                )
                assertTasksNotRegistered(
                    ":shared:assembleMyslibReleaseSharedLibraryLinuxX64"
                )
            }
            build(":lib:tasks") {
                assertSuccessful()
                assertTasksRegistered(
                    ":lib:assembleGroofatframeFatFramework",
                    ":lib:assembleGrooframeFramework",
                    ":lib:assembleGroolibSharedLibrary",
                    ":lib:assembleLibXCFramework"
                )
            }
        }
    }

    @Test
    fun `link shared library from two gradle modules`() {
        with(Project("new-kn-library-dsl")) {
            build(":shared:assembleMyslibDebugSharedLibraryLinuxX64") {
                assertSuccessful()
                assertTasksExecuted(
                    ":lib:compileKotlinLinuxX64",
                    ":shared:compileKotlinLinuxX64",
                    ":shared:assembleMyslibDebugSharedLibraryLinuxX64"
                )
                assertFileExists("/shared/build/out/dynamic/linux_x64/debug/libmyslib.so")
                assertFileExists("/shared/build/out/dynamic/linux_x64/debug/libmyslib_api.h")
            }
        }
    }

    @Test
    fun `link shared library from single gradle module`() {
        with(Project("new-kn-library-dsl")) {
            build(":shared:assembleMylibDebugSharedLibraryLinuxX64") {
                assertSuccessful()
                assertTasksExecuted(
                    ":shared:compileKotlinLinuxX64",
                    ":shared:assembleMylibDebugSharedLibraryLinuxX64"
                )
                assertTasksNotExecuted(
                    ":lib:compileKotlinLinuxX64"
                )
                assertFileExists("/shared/build/out/dynamic/linux_x64/debug/libmylib.so")
                assertFileExists("/shared/build/out/dynamic/linux_x64/debug/libmylib_api.h")
            }
        }
    }

    @Test
    fun `link shared library from single gradle module with custom link args`() {
        with(Project("new-kn-library-dsl")) {
            setupWorkingDir()
            gradleProperties().appendText("\nkotlin.native.linkArgs=-Xfoo=bar -Xbaz=qux")
            gradleBuildScript("shared").appendText("""
                kotlinArtifact("car", Library) {
                    target = LINUX_X64
                    kotlinOptions {
                        freeCompilerArgs += "-Xmen=pool"
                    }
                }
            """.trimIndent())
            build(":shared:assembleCarDebugSharedLibraryLinuxX64") {
                assertSuccessful()
                assertTasksExecuted(
                    ":shared:compileKotlinLinuxX64",
                    ":shared:assembleCarDebugSharedLibraryLinuxX64"
                )
                assertTasksNotExecuted(
                    ":lib:compileKotlinLinuxX64"
                )
                assertContains("-Xfoo=bar")
                assertContains("-Xbaz=qux")
                assertContains("-Xmen=pool")
                assertContains("w: Flag is not supported by this version of the compiler: -Xfoo=bar")
                assertContains("w: Flag is not supported by this version of the compiler: -Xbaz=qux")
                assertContains("w: Flag is not supported by this version of the compiler: -Xmen=pool")
                assertFileExists("/shared/build/out/dynamic/linux_x64/debug/libcar.so")
                assertFileExists("/shared/build/out/dynamic/linux_x64/debug/libcar_api.h")
            }
        }
    }

    @Test
    fun `link release XCFramework from two gradle modules`() {
        Assume.assumeTrue(HostManager.hostIsMac)
        with(Project("new-kn-library-dsl")) {
            build(":shared:assembleSharedReleaseXCFramework") {
                assertSuccessful()
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
                assertFileExists("/shared/build/out/xcframework/release/shared.xcframework")
            }
        }
    }
}