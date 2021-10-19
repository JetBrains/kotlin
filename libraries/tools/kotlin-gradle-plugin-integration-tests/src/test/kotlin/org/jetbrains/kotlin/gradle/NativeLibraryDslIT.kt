/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import kotlin.test.Test

class NativeLibraryDslIT : BaseGradleIT() {
    override val defaultGradleVersion = GradleVersionRequired.FOR_MPP_SUPPORT

    @Test
    fun `check registered gradle tasks`() {
        with(Project("new-kn-library-dsl")) {
            build(":tasks") {
                assertSuccessful()
                assertTasksRegistered(
                    ":assembleDebugSharedMylibLinuxX64",
                    ":assembleDebugStaticMyslibLinuxX64",
                    ":assembleReleaseSharedMylibLinuxX64",
                    ":assembleSharedMylib",
                    ":assembleStaticMyslib"
                )
                assertTasksNotRegistered(
                    ":assembleReleaseStaticMyslibLinuxX64"
                )
            }
        }
    }

    @Test
    fun `link static library from two gradle modules`() {
        with(Project("new-kn-library-dsl")) {
            build(":assembleDebugStaticMyslibLinuxX64") {
                assertSuccessful()
                assertTasksExecuted(
                    ":lib:compileKotlinLinuxX64",
                    ":shared:compileKotlinLinuxX64",
                    ":assembleDebugStaticMyslibLinuxX64"
                )
                assertFileExists("/build/out/static/linux_x64/Debug/libmyslib.a")
                assertFileExists("/build/out/static/linux_x64/Debug/libmyslib_api.h")
            }
        }
    }

    @Test
    fun `link shared library from single gradle module`() {
        with(Project("new-kn-library-dsl")) {
            build(":assembleDebugSharedMylibLinuxX64") {
                assertSuccessful()
                assertTasksExecuted(
                    ":shared:compileKotlinLinuxX64",
                    ":assembleDebugSharedMylibLinuxX64"
                )
                assertTasksNotExecuted(
                    ":lib:compileKotlinLinuxX64"
                )
                assertFileExists("/build/out/dynamic/linux_x64/Debug/libmylib.so")
                assertFileExists("/build/out/dynamic/linux_x64/Debug/libmylib_api.h")
            }
        }
    }
}